package com.ntcoverage.service

import com.ntcoverage.repository.BibleBookRepository
import com.ntcoverage.repository.BibleLayer4ApplicationRepository
import com.ntcoverage.repository.BibleTokenRepository
import com.ntcoverage.repository.BibleVersionRepository
import com.ntcoverage.repository.BibleVerseRepository
import com.ntcoverage.repository.InterlinearRepository
import com.ntcoverage.repository.LexiconRepository
import com.ntcoverage.repository.LlmQueueRepository
import kotlinx.serialization.encodeToString
import com.ntcoverage.seed.BibleAbbreviationsSeedData
import com.ntcoverage.seed.BibleBookNamesSeedData
import com.ntcoverage.seed.BibleBooksSeedData
import com.ntcoverage.seed.BibleVersionsSeedData
import com.ntcoverage.scraper.BibleOnlineScraper
import com.ntcoverage.llm.LlmConfig
import com.ntcoverage.model.LexiconEntryDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Runtime scope filter for Layer 4 phases — selects a single book, chapter, or verse.
 * Replaces the former `bible-ingestion-filter.txt` approach (deleted 2026-04-17).
 *
 * - `bookName == null` → todos os livros
 * - `bookName != null, chapter == null` → livro inteiro
 * - `bookName != null, chapter != null, verse == null` → capítulo inteiro
 * - `bookName != null, chapter != null, verse != null` → único versículo
 */
data class IngestionScope(val bookName: String, val chapter: Int?, val verse: Int?) {
    init {
        require(verse == null || chapter != null) { "verse scope requires chapter" }
    }
}

class BibleIngestionService(
    private val versionRepository: BibleVersionRepository,
    private val bookRepository: BibleBookRepository,
    private val verseRepository: BibleVerseRepository,
    private val interlinearRepository: InterlinearRepository,
    private val lexiconRepository: LexiconRepository,
    private val phaseTracker: IngestionPhaseTracker,
    private val bibleOnlineScraper: BibleOnlineScraper,
    private val wordAlignmentService: WordAlignmentService,
    private val lexiconEnrichmentService: LexiconEnrichmentService,
    private val llmConfig: LlmConfig = LlmConfig(),
    private val llmQueueRepository: LlmQueueRepository? = null,
    private val tokenizationService: BibleTokenizationService? = null,
    private val applicationRepository: BibleLayer4ApplicationRepository? = null,
    private val bibleTokenRepository: BibleTokenRepository? = null
) {
    private val log = LoggerFactory.getLogger(BibleIngestionService::class.java)

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build()

    companion object {
        private val companionLog = LoggerFactory.getLogger("BibleIngestionService.GlossParser")
        private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

        /** Patterns that indicate LLM preamble/meta-text instead of actual translations */
        internal val GLOSS_META_PATTERNS = listOf(
            "tradução", "traducao", "traducción", "traduccion",
            "translation", "here is", "here are",
            "voici", "aqui está", "aqui esta", "aquí está", "aquí esta",
            "sure,", "certainly", "below are", "following are"
        )

        /**
         * Processes raw LLM response into a map of transliteration → translatedGloss.
         * Strategy: try JSON parsing first (most robust), fall back to line-by-line.
         */
        internal fun processGlossResponse(rawContent: String, chunk: List<String>, language: String): Map<String, String> {
            // ── Strategy 1: JSON parsing (key-based matching — most robust) ──
            val jsonResult = tryParseJsonGlosses(rawContent, chunk)
            if (jsonResult != null && jsonResult.isNotEmpty()) {
                // Return whatever matched. Missing glosses stay null → retried next run.
                if (jsonResult.size < chunk.size) {
                    companionLog.warn("GLOSS_PARSE: JSON partial match ${jsonResult.size}/${chunk.size} for $language — missing glosses will be retried")
                }
                return jsonResult
            }

            // If content looks like JSON but parsing failed/matched nothing,
            // do NOT fall through to line-by-line (would produce garbage positional mapping)
            val stripped = rawContent.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            if (stripped.contains('{') && stripped.contains('}')) {
                companionLog.warn("GLOSS_PARSE: JSON-like content but no key matches for $language — skipping (raw length=${stripped.length})")
                return emptyMap()
            }

            // ── Strategy 2: Line-by-line fallback (only for genuinely plain-text responses) ──
            return processGlossResponseLineBased(rawContent, chunk)
        }

        private fun processGlossResponseLineBased(rawContent: String, chunk: List<String>): Map<String, String> {
            val results = mutableMapOf<String, String>()
            val rawLines = rawContent.trim().lines()
            val nonBlankLines = rawLines.filter { it.isNotBlank() }

            // Pass 1: Remove meta/preamble lines BEFORE positional mapping
            val cleanLines = nonBlankLines.filter { line ->
                val trimmed = line.trim()
                val isMetaText = trimmed.length > 80 ||
                    GLOSS_META_PATTERNS.any { p -> trimmed.lowercase().contains(p) }
                !isMetaText
            }

            // Pass 1b: Detect JSON key-value lines and extract only the value
            val jsonKvRegex = Regex("""^\s*"([^"]+)"\s*:\s*"([^"]+)"\s*,?\s*$""")
            val parsedLines = cleanLines.map { line ->
                val match = jsonKvRegex.find(line)
                if (match != null) match.groupValues[2] else line.trim()
            }

            // Pass 1c: Strip excess leading lines if still more than expected
            val translated = if (parsedLines.size > chunk.size) {
                parsedLines.drop(parsedLines.size - chunk.size)
            } else {
                parsedLines
            }

            // Pass 2: Map clean lines to glosses positionally
            for (i in chunk.indices) {
                if (i < translated.size) {
                    val line = translated[i].trim()
                    if (line.isNotBlank()) {
                        results[chunk[i]] = line
                    }
                }
            }

            return results
        }

        /**
         * Tries to parse LLM response as JSON object { "english gloss": "translated gloss", ... }
         * Returns null if JSON parsing fails.
         */
        internal fun tryParseJsonGlosses(rawContent: String, chunk: List<String>): Map<String, String>? {
            try {
                // Extract JSON from potential markdown code blocks
                val jsonStr = rawContent.trim()
                    .removePrefix("```json").removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                // Find the JSON object boundaries
                val start = jsonStr.indexOf('{')
                val end = jsonStr.lastIndexOf('}')
                if (start < 0 || end <= start) return null

                val jsonBody = jsonStr.substring(start, end + 1)
                val parsed = lenientJson.parseToJsonElement(jsonBody)

                val obj = parsed.jsonObject
                val results = mutableMapOf<String, String>()
                val chunkSet = chunk.toSet()
                val chunkByNormalized = mutableMapOf<String, String>()
                for (entry in chunk) {
                    chunkByNormalized[entry.trim().lowercase()] = entry
                }

                for ((key, value) in obj) {
                    val translatedValue = value.jsonPrimitive.contentOrNull ?: continue
                    if (translatedValue.isBlank()) continue
                    // Match: exato → trimmed → case-insensitive → sem pontuação
                    val matchKey = if (key in chunkSet) key
                        else chunkByNormalized[key.trim().lowercase()]
                        ?: chunk.find { normalizeGlossKey(it) == normalizeGlossKey(key) }
                    if (matchKey != null) {
                        results[matchKey] = translatedValue.trim()
                    }
                }

                return if (results.isNotEmpty()) results else null
            } catch (_: Exception) {
                return null
            }
        }

        internal fun normalizeGlossKey(s: String): String =
            s.trim().lowercase().replace(Regex("[\\[\\]<>(){}\"']"), "").trim()

        val ALL_PHASES = listOf(
            "bible_seed_versions",
            "bible_seed_books",
            "bible_seed_abbreviations",
            "bible_seed_book_names",
            "bible_ingest_text_kjv",
            "bible_ingest_text_aa",
            "bible_ingest_text_acf",
            "bible_ingest_text_arc69",
            "bible_ingest_nt_interlinear",
            "bible_ingest_ot_interlinear",
            "bible_ingest_greek_lexicon",
            "bible_ingest_hebrew_lexicon",
            "bible_fill_missing_hebrew",
            "bible_translate_lexicon",
            "bible_translate_hebrew_lexicon",
            "bible_translate_glosses",
            "bible_tokenize_arc69",
            "bible_tokenize_kjv",
            "bible_lemmatize_arc69",
            "bible_lemmatize_kjv",
            "bible_align_kjv",
            "bible_align_arc69",
            "bible_enrich_semantics_arc69",
            "bible_enrich_greek_lexicon",
            "bible_enrich_hebrew_lexicon",
            "bible_reenrich_greek_lexicon",
            "bible_reenrich_hebrew_lexicon",
            "bible_translate_enrichment_greek",
            "bible_translate_enrichment_hebrew",
            "bible_align_hebrew_kjv",
            "bible_align_hebrew_arc69",
            "bible_audit_glosses_pt"
        )

        /** Phases that accept IngestionScope filtering (book/chapter/verse). */
        val LAYER_4_PHASES = setOf(
            "bible_tokenize_arc69",
            "bible_tokenize_kjv",
            "bible_lemmatize_arc69",
            "bible_lemmatize_kjv",
            "bible_align_kjv",
            "bible_align_kjv_prepare",
            "bible_align_arc69",
            "bible_align_arc69_prepare",
            "bible_align_hebrew_kjv",
            "bible_align_hebrew_kjv_prepare",
            "bible_align_hebrew_arc69",
            "bible_align_hebrew_arc69_prepare",
            "bible_enrich_semantics_arc69",
            "bible_enrich_semantics_arc69_prepare",
            "bible_translate_glosses",
            "bible_translate_glosses_prepare",
            "bible_audit_glosses_pt",
            "bible_audit_glosses_pt_prepare"
        )

        // GitHub raw URLs for Bible text datasets
        // KJV from public domain repository (JSON format with book/chapter/verse structure)
        private const val KJV_BASE = "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/en_kjv.json"
        private const val ARC_BASE = "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/pt_aa.json"
        private const val ACF_BASE = "https://raw.githubusercontent.com/thiagobodruk/bible/master/json/pt_acf.json"
        private const val ARC69_BASE = "https://raw.githubusercontent.com/damarals/biblias/main/inst/json/ARC.json"

        // TAGNT (Translators Amalgamated Greek NT) from STEPBible — CC BY 4.0
        // Contains: Greek text, transliteration, English gloss, Strong's, morphology, lemma
        private val TAGNT_URLS = listOf(
            "https://raw.githubusercontent.com/STEPBible/STEPBible-Data/master/Translators%20Amalgamated%20OT%2BNT/TAGNT%20Mat-Jhn%20-%20Translators%20Amalgamated%20Greek%20NT%20-%20STEPBible.org%20CC-BY.txt",
            "https://raw.githubusercontent.com/STEPBible/STEPBible-Data/master/Translators%20Amalgamated%20OT%2BNT/TAGNT%20Act-Rev%20-%20Translators%20Amalgamated%20Greek%20NT%20-%20STEPBible.org%20CC-BY.txt"
        )

        // TAGNT book abbreviation → our book name mapping
        private val TAGNT_BOOK_MAP = mapOf(
            "Mat" to "Matthew", "Mrk" to "Mark", "Luk" to "Luke", "Jhn" to "John",
            "Act" to "Acts", "Rom" to "Romans", "1Co" to "1 Corinthians", "2Co" to "2 Corinthians",
            "Gal" to "Galatians", "Eph" to "Ephesians", "Php" to "Philippians", "Col" to "Colossians",
            "1Th" to "1 Thessalonians", "2Th" to "2 Thessalonians", "1Ti" to "1 Timothy",
            "2Ti" to "2 Timothy", "Tit" to "Titus", "Phm" to "Philemon", "Heb" to "Hebrews",
            "Jas" to "James", "1Pe" to "1 Peter", "2Pe" to "2 Peter", "1Jn" to "1 John",
            "2Jn" to "2 John", "3Jn" to "3 John", "Jud" to "Jude", "Rev" to "Revelation"
        )
    }

    /** Key for deduplicating Greek words before LLM translation */
    private data class GlossTranslationEntry(val transliteration: String, val morphology: String, val englishGloss: String, val lemma: String)

    suspend fun runPhase(phase: String, scope: IngestionScope? = null) {
        val applicationId = if (scope != null && phase in LAYER_4_PHASES) {
            applicationRepository?.insert(phase, scope.bookName, scope.chapter, scope.verse)
        } else null

        try {
            when (phase) {
                "bible_seed_versions" -> seedVersions()
                "bible_seed_books" -> seedBooks()
                "bible_seed_abbreviations" -> seedAbbreviations()
                "bible_seed_book_names" -> seedBookNames()
                "bible_ingest_text_kjv" -> ingestTextFromUrl("KJV", KJV_BASE)
                "bible_ingest_text_aa" -> ingestTextFromUrl("AA", ARC_BASE)
                "bible_ingest_text_acf" -> ingestTextFromUrl("ACF", ACF_BASE)
                "bible_ingest_text_arc69" -> ingestTextFromUrl("ARC69", ARC69_BASE)
                "bible_ingest_nt_interlinear" -> ingestTAGNT()
                "bible_ingest_ot_interlinear" -> ingestOTInterlinearPlaceholder()
                "bible_ingest_greek_lexicon" -> ingestGreekLexiconFromSTEP()
                "bible_ingest_hebrew_lexicon" -> ingestHebrewLexiconFromSTEP()
                "bible_fill_missing_hebrew" -> fillMissingHebrew()
                // Layer 4 — Tokenization (deterministic, no LLM)
                "bible_tokenize_arc69" -> tokenizeVersion("ARC69", scope)
                "bible_tokenize_kjv" -> tokenizeVersion("KJV", scope)
                // Layer 4 — Lemmatization (LLM batch, one-time)
                "bible_lemmatize_arc69" -> lemmatizeVersionPlaceholder("ARC69")
                "bible_lemmatize_kjv" -> lemmatizeVersionPlaceholder("KJV")
                // LLM phases → redirect to queue-based prepare (enqueue prompts, then run /run-llm)
                "bible_translate_lexicon", "bible_translate_lexicon_prepare" -> translateLexiconPrepare("greek")
                "bible_translate_hebrew_lexicon", "bible_translate_hebrew_lexicon_prepare" -> translateLexiconPrepare("hebrew")
                "bible_translate_glosses", "bible_translate_glosses_prepare" -> translateGlossesPrepare(scope)
                "bible_audit_glosses_pt", "bible_audit_glosses_pt_prepare" -> auditGlossesPrepare(scope)
                "bible_translate_enrichment_greek", "bible_translate_enrichment_greek_prepare" -> translateEnrichmentPrepare("greek")
                "bible_translate_enrichment_hebrew", "bible_translate_enrichment_hebrew_prepare" -> translateEnrichmentPrepare("hebrew")
                "bible_align_kjv", "bible_align_kjv_prepare" -> alignVersionPrepare("KJV", scope)
                "bible_align_arc69", "bible_align_arc69_prepare" -> alignVersionPrepare("ARC69", scope)
                "bible_align_hebrew_kjv", "bible_align_hebrew_kjv_prepare" -> alignVersionPrepare("KJV", scope)
                "bible_align_hebrew_arc69", "bible_align_hebrew_arc69_prepare" -> alignVersionPrepare("ARC69", scope)
                // Layer 4 — Semantic enrichment (N4b): attaches contextual_sense + semantic_relation to every alignment
                "bible_enrich_semantics_arc69", "bible_enrich_semantics_arc69_prepare" -> enrichSemanticsPrepare("ARC69", scope)
                // Non-LLM phases (scrapers, no change)
                "bible_enrich_greek_lexicon" -> enrichGreekLexicon()
                "bible_enrich_hebrew_lexicon" -> enrichHebrewLexicon()
                "bible_reenrich_greek_lexicon" -> reEnrichGreekLexicon()
                "bible_reenrich_hebrew_lexicon" -> reEnrichHebrewLexicon()
                else -> throw IllegalArgumentException("Unknown phase: $phase")
            }
            if (applicationId != null) {
                val status = phaseTracker.getPhaseStatus(phase)
                val items = status?.itemsProcessed ?: 0
                applicationRepository?.markSuccess(applicationId, items, items)
            }
        } catch (e: Throwable) {
            if (applicationId != null) {
                applicationRepository?.markFailed(applicationId, e.message ?: "Unknown error")
            }
            throw e
        }
    }

    suspend fun runPhases(phases: List<String>, scope: IngestionScope? = null) {
        for (phase in phases) {
            runPhase(phase, scope)
        }
    }

    suspend fun fullIngestion() {
        runPhases(ALL_PHASES)
    }

    fun clearGlosses(): Int {
        val cleared = interlinearRepository.clearTranslatedGlosses()
        phaseTracker.deleteByPrefix("bible_translate_glosses")
        log.info("CLEAR_GLOSSES: nullified $cleared rows, reset bible_translate_glosses phase")
        return cleared
    }

    fun fixCorruptedPortugueseGlosses(): Int {
        val cleared = interlinearRepository.clearCorruptedPortugueseGlosses()
        log.info("FIX_CORRUPTED_GLOSSES: nullified $cleared corrupted Portuguese gloss rows")
        return cleared
    }

    // ── Seeds ──

    private suspend fun seedVersions() = runPhaseTracked("bible_seed_versions") {
        val entries = BibleVersionsSeedData.entries
        phaseTracker.markProgress("bible_seed_versions", 0, entries.size)
        var count = 0
        for (entry in entries) {
            versionRepository.upsert(
                code = entry.code, name = entry.name, language = entry.language,
                description = entry.description, isPrimary = entry.isPrimary, testamentScope = entry.testamentScope
            )
            count++
            phaseTracker.markProgress("bible_seed_versions", count)
        }
        log.info("BIBLE_SEED_VERSIONS: seeded $count versions")
    }

    private suspend fun seedBooks() = runPhaseTracked("bible_seed_books") {
        val entries = BibleBooksSeedData.entries
        val totalItems = entries.sumOf { 1 + it.totalChapters + it.totalVerses }
        phaseTracker.markProgress("bible_seed_books", 0, totalItems)
        var processed = 0

        log.info("BIBLE_SEED_BOOKS: processing ${entries.size} books")

        for (entry in entries) {
            val bookId = bookRepository.upsertBook(
                name = entry.name, abbreviation = entry.abbreviation, totalChapters = entry.totalChapters,
                totalVerses = entry.totalVerses, bookOrder = entry.bookOrder, testament = entry.testament
            )
            processed++

            for ((chapterIdx, verseCount) in entry.chaptersVerses.withIndex()) {
                val chapterNumber = chapterIdx + 1
                bookRepository.upsertChapter(bookId, chapterNumber, verseCount)
                processed++
                for (verseNum in 1..verseCount) {
                    verseRepository.upsertVerse(bookId, chapterNumber, verseNum)
                    processed++
                }
                if (processed % 1000 == 0) phaseTracker.markProgress("bible_seed_books", processed)
            }
        }
        phaseTracker.markProgress("bible_seed_books", processed)
        log.info("BIBLE_SEED_BOOKS: seeded ${entries.size} books, ${verseRepository.countVerses()} verses")
    }

    private suspend fun seedAbbreviations() = runPhaseTracked("bible_seed_abbreviations") {
        val entries = BibleAbbreviationsSeedData.entries
        phaseTracker.markProgress("bible_seed_abbreviations", 0, entries.size)
        var processed = 0
        var total = 0
        for (entry in entries) {
            val book = bookRepository.findByName(entry.bookName)
            if (book == null) { processed++; continue }
            for (abbrev in entry.abbreviations) {
                bookRepository.upsertAbbreviation(book.id, entry.locale, abbrev)
                total++
            }
            processed++
            phaseTracker.markProgress("bible_seed_abbreviations", processed)
        }
        log.info("BIBLE_SEED_ABBREVIATIONS: seeded $total abbreviations")
    }

    private suspend fun seedBookNames() = runPhaseTracked("bible_seed_book_names") {
        val entries = BibleBookNamesSeedData.entries
        phaseTracker.markProgress("bible_seed_book_names", 0, entries.size)
        var processed = 0
        for (entry in entries) {
            val book = bookRepository.findByName(entry.canonicalName)
            if (book != null) {
                bookRepository.upsertBookTranslation(book.id, entry.locale, entry.name)
            }
            processed++
            phaseTracker.markProgress("bible_seed_book_names", processed)
        }
        log.info("BIBLE_SEED_BOOK_NAMES: seeded $processed book name translations")
    }

    // ── Text Ingestion (download JSON from GitHub) ──

    private suspend fun ingestTextFromUrl(versionCode: String, url: String) {
        val phaseName = "bible_ingest_text_${versionCode.lowercase()}"
        runPhaseTracked(phaseName) {
            val version = versionRepository.findByCode(versionCode)
                ?: throw IllegalStateException("Version $versionCode not found. Run bible_seed_versions first.")

            // Check if already ingested
            val existing = verseRepository.countVerseTexts(version.id)
            if (existing > 0) {
                log.info("BIBLE_INGEST_TEXT_$versionCode: already has $existing verses, skipping")
                phaseTracker.markProgress(phaseName, existing.toInt(), existing.toInt())
                return@runPhaseTracked
            }

            log.info("BIBLE_INGEST_TEXT_$versionCode: downloading from $url")
            val json = downloadText(url)

            // Parse JSON format: array of books, each with chapters array, each with verses array
            // Format: [{"abbrev":"gn","chapters":[["verse1","verse2",...],...]},...]
            val books = bookRepository.findAll()
            val booksByOrder = books.associateBy { it.bookOrder }

            val booksJson = parseJsonBooksArray(json)
            phaseTracker.markProgress(phaseName, 0, booksJson.size)
            var totalVerses = 0

            for ((bookIdx, bookJson) in booksJson.withIndex()) {
                val bookOrder = bookIdx + 1
                val book = booksByOrder[bookOrder]
                if (book == null) continue

                val chapters = bookJson.chapters
                for ((chapterIdx, verses) in chapters.withIndex()) {
                    val chapterNum = chapterIdx + 1
                    for ((verseIdx, verseText) in verses.withIndex()) {
                        val verseNum = verseIdx + 1
                        val verseId = verseRepository.getVerseId(book.id, chapterNum, verseNum)
                        if (verseId != null && verseText.isNotBlank()) {
                            verseRepository.upsertVerseText(version.id, verseId, verseText.trim())
                            totalVerses++
                        }
                    }
                }
                phaseTracker.markProgress(phaseName, bookIdx + 1)
            }
            log.info("BIBLE_INGEST_TEXT_$versionCode: ingested $totalVerses verses from $url")
        }
    }

    // ── Scraper-based text ingestion (ARC from bibliaonline.com.br) ──

    private suspend fun ingestTextViaScraper(versionCode: String, siteVersionSlug: String) {
        val phaseName = "bible_ingest_text_${versionCode.lowercase()}"
        runPhaseTracked(phaseName) {
            val version = versionRepository.findByCode(versionCode)
                ?: throw IllegalStateException("Version $versionCode not found. Run bible_seed_versions first.")

            val books = bookRepository.findAll()
            log.info("BIBLE_INGEST_TEXT_$versionCode: processing ${books.size} books")
            phaseTracker.markProgress(phaseName, 0, books.size)
            var totalVerses = 0

            for ((idx, book) in books.withIndex()) {
                val slug = bibleOnlineScraper.getSlug(book.name)
                if (slug == null) {
                    log.warn("BIBLE_SCRAPER: no slug mapping for ${book.name}")
                    phaseTracker.markProgress(phaseName, idx + 1)
                    continue
                }

                try {
                    for (chapter in 1..book.totalChapters) {
                        val verses = bibleOnlineScraper.scrapeChapter(siteVersionSlug, slug, chapter)
                        for ((verseNum, text) in verses) {
                            val verseId = verseRepository.getVerseId(book.id, chapter, verseNum)
                            if (verseId != null) {
                                verseRepository.upsertVerseText(version.id, verseId, text)
                                totalVerses++
                            }
                        }
                    }
                    log.debug("BIBLE_SCRAPER: ${book.name} → scraped ${book.totalChapters} chapters")
                } catch (e: Exception) {
                    log.warn("BIBLE_SCRAPER: failed ${book.name}: ${e.message}")
                }
                phaseTracker.markProgress(phaseName, idx + 1)
            }

            log.info("BIBLE_INGEST_TEXT_$versionCode: ingested $totalVerses verses via scraper")
        }
    }

    // ── TAGNT Interlinear (NT Greek with English Gloss + Strong's) ──

    private suspend fun ingestTAGNT() = runPhaseTracked("bible_ingest_nt_interlinear") {
        // Clear existing interlinear data
        log.info("TAGNT: clearing existing interlinear words...")
        interlinearRepository.deleteAllWords()

        val books = bookRepository.findAll("NT")
        val bookIdByName = books.associate { it.name to it.id }
        phaseTracker.markProgress("bible_ingest_nt_interlinear", 0, TAGNT_URLS.size)
        var totalWords = 0

        for ((fileIdx, url) in TAGNT_URLS.withIndex()) {
            log.info("TAGNT: downloading file ${fileIdx + 1}/${TAGNT_URLS.size}...")
            try {
                val content = downloadText(url)
                val words = parseTAGNTAndInsert(content, bookIdByName)
                totalWords += words
                log.info("TAGNT: file ${fileIdx + 1} → $words words")
            } catch (e: Exception) {
                log.error("TAGNT: failed to process file ${fileIdx + 1}: ${e.message}", e)
            }
            phaseTracker.markProgress("bible_ingest_nt_interlinear", fileIdx + 1)
        }
        log.info("BIBLE_INGEST_NT_INTERLINEAR: ingested $totalWords words from TAGNT")
    }

    private fun parseTAGNTAndInsert(content: String, bookIdByName: Map<String, Int>): Int {
        // TAGNT format (tab-separated):
        // Col 0: Mat.1.1#01=NKO  → reference + word position + manuscript type
        // Col 1: Βίβλος (Biblos) → Greek text (transliteration)
        // Col 2: [The] book      → English gloss
        // Col 3: G0976=N-NSF     → Strong's=morphology
        // Col 4: βίβλος=book     → lemma=definition
        val refPattern = Regex("""^(\w+)\.(\d+)\.(\d+)#(\d+)""")
        var count = 0

        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("=") || trimmed.startsWith("TAGNT")
                || trimmed.startsWith("(") || trimmed.startsWith("FIELD")
                || !trimmed.contains("\t")) continue

            val cols = trimmed.split("\t")
            if (cols.size < 5) continue

            // Parse reference: Mat.1.1#01=NKO
            val refMatch = refPattern.find(cols[0]) ?: continue
            val bookAbbr = refMatch.groupValues[1]
            val chapter = refMatch.groupValues[2].toIntOrNull() ?: continue
            val verseNum = refMatch.groupValues[3].toIntOrNull() ?: continue
            val wordPos = refMatch.groupValues[4].toIntOrNull() ?: continue

            val bookName = TAGNT_BOOK_MAP[bookAbbr] ?: continue
            val bookId = bookIdByName[bookName] ?: continue

            // Parse Greek + transliteration: "Βίβλος (Biblos)"
            val greekCol = cols[1].trim()
            val greekParenMatch = Regex("""^(.+?)\s*\(([^)]+)\)\s*$""").find(greekCol)
            val originalWord = greekParenMatch?.groupValues?.get(1)?.trim() ?: greekCol
            val transliteration = greekParenMatch?.groupValues?.get(2)?.trim()

            // English gloss
            val englishGloss = cols[2].trim().ifBlank { null }

            // Strong's + morphology: "G0976=N-NSF"
            val strongsMorph = cols[3].trim()
            val strongsMatch = Regex("""^(G\d+)\w*=(.+)$""").find(strongsMorph)
            val strongsNumber = strongsMatch?.groupValues?.get(1)
            val morphology = strongsMatch?.groupValues?.get(2)

            // Lemma: "βίβλος=book"
            val lemmaCol = cols[4].trim()
            val lemma = lemmaCol.split("=").firstOrNull()?.trim() ?: originalWord

            val verseId = verseRepository.getVerseId(bookId, chapter, verseNum) ?: continue

            interlinearRepository.upsertWord(
                verseId = verseId,
                wordPosition = wordPos.toShort(),
                originalWord = originalWord,
                transliteration = transliteration,
                lemma = lemma,
                morphology = morphology,
                strongsNumber = strongsNumber,
                englishGloss = englishGloss,
                language = "greek"
            )
            count++
        }
        return count
    }

    // ── Placeholders for OT interlinear and lexicons ──
    // These will be implemented when OSHB and STEP Bible parsers are ready

    private suspend fun ingestOTInterlinearPlaceholder() = runPhaseTracked("bible_ingest_ot_interlinear") {
        log.info("BIBLE_INGEST_OT_INTERLINEAR: OSHB parser not yet implemented. Skipping.")
        phaseTracker.markProgress("bible_ingest_ot_interlinear", 0, 0)
    }

    private suspend fun ingestGreekLexiconFromSTEP() = runPhaseTracked("bible_ingest_greek_lexicon") {
        val url = "https://raw.githubusercontent.com/STEPBible/STEPBible-Data/master/Lexicons/TBESG%20-%20Translators%20Brief%20lexicon%20of%20Extended%20Strongs%20for%20Greek%20-%20STEPBible.org%20CC%20BY.txt"
        log.info("STEP_LEXICON: downloading Greek lexicon from STEPBible...")
        val content = downloadText(url)
        var count = 0

        // Format: G0001\tG0001G =\tG0001G\tα, Ἀλφα\tAlpha\tG:N-LI\tAlpha\t<full definition HTML>
        // Cols: 0=strongsBase 1=extendedRef 2=strongsExt 3=greekWord 4=transliteration 5=morphCategory 6=shortDef 7=fullDef
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || !trimmed.startsWith("G")) continue

            val cols = trimmed.split("\t")
            if (cols.size < 7) continue

            val strongsNumber = cols[0].trim() // G0001, G0026, etc.
            if (!strongsNumber.matches(Regex("G\\d+"))) continue

            val greekWord = cols[3].trim().split(",").firstOrNull()?.trim() ?: continue
            val transliteration = cols[4].trim().ifBlank { null }
            val morphCategory = cols[5].trim() // e.g., "G:N-M", "G:V"
            val partOfSpeech = morphCategory.removePrefix("G:").removePrefix("N:").ifBlank { null }
            val shortDefinition = cols[6].trim().ifBlank { null }

            // Full definition: strip HTML tags for clean text
            val fullDefHtml = cols.getOrNull(7)?.trim() ?: ""
            val fullDefinition = fullDefHtml
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(2000)
                .ifBlank { null }

            lexiconRepository.upsertGreek(
                strongsNumber = strongsNumber,
                lemma = greekWord,
                transliteration = transliteration,
                pronunciation = null,
                shortDefinition = shortDefinition,
                fullDefinition = fullDefinition,
                partOfSpeech = partOfSpeech,
                usageCount = 0
            )
            count++
            if (count % 500 == 0) phaseTracker.markProgress("bible_ingest_greek_lexicon", count)
        }
        phaseTracker.markProgress("bible_ingest_greek_lexicon", count)
        log.info("STEP_LEXICON: ingested $count Greek lexicon entries")
    }

    private suspend fun ingestHebrewLexiconFromSTEP() = runPhaseTracked("bible_ingest_hebrew_lexicon") {
        val url = "https://raw.githubusercontent.com/STEPBible/STEPBible-Data/master/Lexicons/TBESH%20-%20Translators%20Brief%20lexicon%20of%20Extended%20Strongs%20for%20Hebrew%20-%20STEPBible.org%20CC%20BY.txt"
        log.info("STEP_LEXICON: downloading Hebrew lexicon from STEPBible...")
        val content = downloadText(url)
        var count = 0

        // Format similar to TBESG but with H-prefix Strong's and TEHMC morphology codes
        // Cols: 0=strongsBase 1=extendedRef 2=strongsExt 3=hebrewWord 4=transliteration 5=morphCategory 6=shortDef 7=fullDef
        // Note: TBESH may have variable column count (8-9 cols due to extra tabs)
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || !trimmed.startsWith("H")) continue

            val cols = trimmed.split("\t")
            if (cols.size < 7) continue

            val strongsNumber = cols[0].trim()
            if (!strongsNumber.matches(Regex("H\\d+"))) continue

            val hebrewWord = cols[3].trim().split(",").firstOrNull()?.trim() ?: continue
            val transliteration = cols[4].trim().ifBlank { null }
            val morphCategory = cols[5].trim()
            val partOfSpeech = morphCategory.removePrefix("H:").removePrefix("N:").ifBlank { null }
            val shortDefinition = cols[6].trim().ifBlank { null }

            val fullDefHtml = cols.getOrNull(7)?.trim() ?: ""
            val fullDefinition = fullDefHtml
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(2000)
                .ifBlank { null }

            lexiconRepository.upsertHebrew(
                strongsNumber = strongsNumber,
                lemma = hebrewWord,
                transliteration = transliteration,
                pronunciation = null,
                shortDefinition = shortDefinition,
                fullDefinition = fullDefinition,
                partOfSpeech = partOfSpeech,
                usageCount = 0
            )
            count++
            if (count % 500 == 0) phaseTracker.markProgress("bible_ingest_hebrew_lexicon", count)
        }
        phaseTracker.markProgress("bible_ingest_hebrew_lexicon", count)
        log.info("STEP_LEXICON: ingested $count Hebrew lexicon entries")
    }

    // ── Lexicon Translation via LLM ──

    private suspend fun translateLexicon() = runPhaseTracked("bible_translate_lexicon") {
        val targetLocales = listOf("pt", "es")
        val localeToLanguage = mapOf("pt" to "Portuguese", "es" to "Spanish")

        // Direct LLM removed — lexicon translation now uses queue
        log.info("BIBLE_TRANSLATE_LEXICON: direct LLM removed — skipping (use queue)")
        return@runPhaseTracked

        val allEntries = lexiconRepository.getAllGreekEntries()
        if (allEntries.isEmpty()) {
            log.info("BIBLE_TRANSLATE_LEXICON: no Greek lexicon entries found — skipping")
            return@runPhaseTracked
        }

        val total = allEntries.size * targetLocales.size
        phaseTracker.markProgress("bible_translate_lexicon", 0, total)
        val processed = AtomicInteger(0)
        val translated = AtomicInteger(0)
        val batchSize = llmConfig.mediumBatchSize

        for (locale in targetLocales) {
            val language = localeToLanguage[locale] ?: continue

            val untranslated = allEntries.filter { entry ->
                !lexiconRepository.hasTranslation(entry.id, locale)
                    && (!entry.shortDefinition.isNullOrBlank() || !entry.fullDefinition.isNullOrBlank())
            }

            val alreadyDone = allEntries.size - untranslated.size
            processed.addAndGet(alreadyDone)
            if (alreadyDone > 0) phaseTracker.markProgress("bible_translate_lexicon", processed.get())

            if (untranslated.isEmpty()) {
                log.info("TRANSLATE_LEXICON: [$locale] all entries already translated")
                continue
            }

            val batches = untranslated.chunked(batchSize)
            log.info("TRANSLATE_LEXICON: [$locale] ${untranslated.size} entries in ${batches.size} batches of $batchSize")

            coroutineScope {
                for (batch in batches) {
                    launch {
                        try {
                            val results = translateLexiconBatch(batch, language, "Greek")
                            if (results.isNotEmpty()) {
                                for ((id, shortDef, fullDef) in results) {
                                    lexiconRepository.upsertGreekTranslation(id, locale, shortDef, fullDef)
                                }
                                translated.addAndGet(results.size)
                            }
                        } catch (e: Exception) {
                            log.warn("TRANSLATE_LEXICON: batch failed (${batch.size} entries, locale=$locale): ${e.message}")
                            for (entry in batch) {
                                try {
                                    val result = translateLexiconEntry(
                                        strongsNumber = entry.strongsNumber,
                                        shortDefinition = entry.shortDefinition,
                                        fullDefinition = entry.fullDefinition,
                                        language = language
                                    )
                                    if (result != null) {
                                        lexiconRepository.upsertGreekTranslation(entry.id, locale, result.first, result.second)
                                        translated.incrementAndGet()
                                    }
                                } catch (inner: Exception) {
                                    log.warn("TRANSLATE_LEXICON: fallback failed ${entry.strongsNumber}: ${inner.message}")
                                }
                            }
                        }

                        val p = processed.addAndGet(batch.size)
                        phaseTracker.markProgress("bible_translate_lexicon", p)
                        if (p % 50 == 0 || p == total) {
                            log.info("TRANSLATE_LEXICON: [$locale] progress=$p/$total translated=${translated.get()}")
                        }
                    }
                }
            }
        }
        phaseTracker.markProgress("bible_translate_lexicon", processed.get())
        log.info("TRANSLATE_LEXICON: DONE | processed=${processed.get()} translated=${translated.get()}")
    }

    private suspend fun translateLexiconEntry(
        strongsNumber: String,
        shortDefinition: String?,
        fullDefinition: String?,
        language: String
    ): Pair<String?, String?>? {
        val textToTranslate = buildString {
            if (!shortDefinition.isNullOrBlank()) append("SHORT: $shortDefinition\n")
            if (!fullDefinition.isNullOrBlank()) append("FULL: ${fullDefinition.take(1500)}")
        }.trim()

        if (textToTranslate.isBlank()) return null

        val systemPrompt = """Translate this Greek lexicon entry to $language.
Return ONLY the translation in this exact format:
SHORT: [translated short definition]
FULL: [translated full definition]

Rules:
- Translate ALL English text to $language
- Keep Greek words, Hebrew words, and Bible references (Mat.1:1, Jhn.3:16) unchanged
- Keep Strong's numbers (G3056, H7225) unchanged
- Use academic biblical terminology
- Preserve numbered definitions (1., 2., etc.)
- Keep the same structure and formatting"""

        // Direct LLM removed — should use queue
        log.warn("translateLexiconEntry: direct LLM removed, use queue instead")
        return null
    }

    // ── Hebrew Lexicon Translation via LLM (batched) ──

    private suspend fun translateHebrewLexicon() = runPhaseTracked("bible_translate_hebrew_lexicon") {
        val targetLocales = listOf("pt", "es")
        val localeToLanguage = mapOf("pt" to "Portuguese", "es" to "Spanish")

        // Direct LLM removed — Hebrew lexicon translation now uses queue
        log.info("BIBLE_TRANSLATE_HEBREW_LEXICON: direct LLM removed — skipping (use queue)")
        return@runPhaseTracked

        val allEntries = lexiconRepository.getAllHebrewEntries()
        if (allEntries.isEmpty()) {
            log.info("BIBLE_TRANSLATE_HEBREW_LEXICON: no Hebrew lexicon entries found — skipping")
            return@runPhaseTracked
        }

        val total = allEntries.size * targetLocales.size
        phaseTracker.markProgress("bible_translate_hebrew_lexicon", 0, total)
        val processed = AtomicInteger(0)
        val translated = AtomicInteger(0)
        val batchSize = llmConfig.mediumBatchSize

        for (locale in targetLocales) {
            val language = localeToLanguage[locale] ?: continue

            val untranslated = allEntries.filter { entry ->
                !lexiconRepository.hasHebrewTranslation(entry.id, locale)
                    && (!entry.shortDefinition.isNullOrBlank() || !entry.fullDefinition.isNullOrBlank())
            }

            val alreadyDone = allEntries.size - untranslated.size
            processed.addAndGet(alreadyDone)
            if (alreadyDone > 0) {
                phaseTracker.markProgress("bible_translate_hebrew_lexicon", processed.get())
            }

            if (untranslated.isEmpty()) {
                log.info("TRANSLATE_HEBREW_LEXICON: [$locale] all entries already translated")
                continue
            }

            val batches = untranslated.chunked(batchSize)
            log.info("TRANSLATE_HEBREW_LEXICON: [$locale] ${untranslated.size} entries in ${batches.size} batches of $batchSize")

            coroutineScope {
                for (batch in batches) {
                    launch {
                        try {
                            val results = translateLexiconBatch(batch, language, "Hebrew")
                            if (results.isNotEmpty()) {
                                lexiconRepository.batchUpsertHebrewTranslations(results, locale)
                                translated.addAndGet(results.size)
                            }
                        } catch (e: Exception) {
                            log.warn("TRANSLATE_HEBREW_LEXICON: batch failed (${batch.size} entries, locale=$locale): ${e.message}")
                            for (entry in batch) {
                                try {
                                    val result = translateLexiconEntry(
                                        strongsNumber = entry.strongsNumber,
                                        shortDefinition = entry.shortDefinition,
                                        fullDefinition = entry.fullDefinition,
                                        language = language
                                    )
                                    if (result != null) {
                                        lexiconRepository.upsertHebrewTranslation(entry.id, locale, result.first, result.second)
                                        translated.incrementAndGet()
                                    }
                                } catch (inner: Exception) {
                                    log.warn("TRANSLATE_HEBREW_LEXICON: fallback failed ${entry.strongsNumber}: ${inner.message}")
                                }
                            }
                        }

                        val p = processed.addAndGet(batch.size)
                        phaseTracker.markProgress("bible_translate_hebrew_lexicon", p)
                        if (p % 50 == 0 || p == total) {
                            log.info("TRANSLATE_HEBREW_LEXICON: [$locale] progress=$p/$total translated=${translated.get()}")
                        }
                    }
                }
            }
        }
        phaseTracker.markProgress("bible_translate_hebrew_lexicon", processed.get())
        log.info("TRANSLATE_HEBREW_LEXICON: DONE | processed=${processed.get()} translated=${translated.get()}")
    }

    private suspend fun translateLexiconBatch(
        entries: List<LexiconEntryDTO>,
        language: String,
        lexiconType: String
    ): List<Triple<Int, String?, String?>> {
        val entriesBlock = entries.joinToString("\n\n") { entry ->
            val short = entry.shortDefinition?.take(200) ?: ""
            val full = entry.fullDefinition?.take(800) ?: ""
            "[${entry.strongsNumber}] short: \"$short\" | full: \"$full\""
        }

        val systemPrompt = """Translate these $lexiconType lexicon entries to $language.
Return ONLY translations in this exact format, one block per entry:

[STRONG_NUMBER]
SHORT: translated short definition
FULL: translated full definition

Rules:
- Translate ALL English text to $language
- Keep Hebrew words, Greek words, and Bible references unchanged
- Keep Strong's numbers (H1234, G3056) unchanged
- Use academic biblical terminology
- Preserve numbered definitions (1., 2., etc.)
- Keep the same structure and formatting
- Return one [STRONG_NUMBER] block for EACH entry below"""

        // Direct LLM removed — should use queue
        log.warn("translateLexiconBatch: direct LLM removed, use queue instead")
        return emptyList()
    }

    // ── Gloss Translation via LLM (batch per chapter) ──

    private suspend fun translateGlosses() = runPhaseTracked("bible_translate_glosses") {
        // Direct LLM removed — gloss translation now uses queue (translateGlossesPrepare)
        log.info("BIBLE_TRANSLATE_GLOSSES: direct LLM removed — skipping (use queue)")
    }

    /**
     * Deterministic gloss for Greek articles (G3588) — bypasses LLM entirely.
     * Morphology format: T-{Case}{Number}{Gender} e.g. T-NSM, T-GPF, T-DPN
     *   Case:   N=nominative, G=genitive, D=dative, A=accusative
     *   Number: S=singular, P=plural
     *   Gender: M=masculine, F=feminine, N=neuter
     */
    private fun deterministicGloss(strongsNumber: String?, morphology: String?, lang: String): String? {
        if (strongsNumber != "G3588" || morphology == null || morphology.length < 5) return null
        val case = morphology[2]   // N, G, D, A
        val number = morphology[3] // S, P
        val gender = morphology[4] // M, F, N

        val isFem = gender == 'F'
        val isPlural = number == 'P'

        return when (lang) {
            "pt" -> when (case) {
                'G' -> if (isPlural) if (isFem) "das" else "dos" else if (isFem) "da" else "do"
                'D' -> if (isPlural) if (isFem) "às" else "aos" else if (isFem) "à" else "ao"
                else -> if (isPlural) if (isFem) "as" else "os" else if (isFem) "a" else "o"
            }
            "es" -> when (case) {
                'G' -> if (isPlural) if (isFem) "de las" else "de los" else if (isFem) "de la" else "del"
                'D' -> if (isPlural) if (isFem) "a las" else "a los" else if (isFem) "a la" else "al"
                else -> if (isPlural) if (isFem) "las" else "los" else if (isFem) "la" else "el"
            }
            else -> null
        }
    }

    private suspend fun translateGlossBatch(
        entries: List<GlossTranslationEntry>, language: String
    ): Map<GlossTranslationEntry, String> {
        if (entries.isEmpty()) return emptyMap()

        val results = mutableMapOf<GlossTranslationEntry, String>()
        val chunks = entries.chunked(80)

        for ((chunkIdx, chunk) in chunks.withIndex()) {
            // Format: transliteration | morphology | english_gloss | lemma (one per line)
            val input = chunk.joinToString("\n") { "${it.transliteration} | ${it.morphology} | ${it.englishGloss} | ${it.lemma}" }
            // Keys sent to LLM (transliteration only — used as JSON key in response)
            val chunkKeys = chunk.map { it.transliteration }

            val systemPrompt = """You are a biblical Greek-to-$language translator for interlinear Bible glosses.
Each line contains 4 fields separated by |: transliteration | morphology | english_gloss | lemma

Translate each Greek word to a short $language gloss (1-3 words).
Use the english_gloss as the semantic reference, then adapt to $language grammar using the morphology code.

Rules:
- Match grammatical number: 3P verbs → plural, 3S → singular
- Match grammatical gender for articles/pronouns: M→masculine, F→feminine, N→neuter
- For pronouns (P-DPM, P-DSF, etc), translate the grammatical function (dative=to/a, genitive=of/de, accusative=direct object)
- Prefer the contextual meaning of the english_gloss over the primary dictionary meaning of the lemma
- For 2P imperatives (V-*M-2P), use classical Portuguese forms: "enchei", "fazei", "tirai" (not "encham", "façam", "retirem")
- Nouns marked singular (-S) must be singular; plural (-P) must be plural. Never add plural to a singular noun

Morphology codes: V=verb, N=noun, A=adjective, T=article, P=pronoun, ADV=adverb, PREP=preposition, CONJ=conjunction, PRT=particle, X=indeterminate.
Verb suffixes: P=present, A=aorist, F=future, I=imperfect, R=perfect, L=pluperfect | A=active, M=middle, P=passive | I=indicative, S=subjunctive, M=imperative, N=infinitive, P=participle.
Noun/Adj suffixes: N=nominative, G=genitive, D=dative, A=accusative | S=singular, P=plural | M=masculine, F=feminine, N=neuter.

Return a JSON object mapping each transliteration to its $language translation.

Example input:
en | PREP | in | ἐν
archē | N-DSF | beginning | ἀρχή
ēn | V-IAI-3S | was | εἰμί
ho | T-NSM | the | ὁ
logos | N-NSM | Word | λόγος

Example output:
{"en": "em", "archē": "princípio", "ēn": "era", "ho": "o", "logos": "Verbo"}

IMPORTANT: Return ONLY the JSON object. No preamble, no explanation."""

            // Direct LLM removed — should use queue
            log.warn("GLOSS_TRANSLATE: direct LLM removed, skipping chunk $chunkIdx for $language")
        }

        return results
    }

    // ── HTTP download utility ──

    private suspend fun downloadText(url: String): String = withContext(Dispatchers.IO) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(60))
            .header("User-Agent", "ManuscriptumAtlas/1.0")
            .GET()
            .build()

        var lastError: Exception? = null
        for (attempt in 1..3) {
            try {
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) {
                    return@withContext response.body()
                }
                throw RuntimeException("HTTP ${response.statusCode()} for $url")
            } catch (e: Exception) {
                lastError = e
                if (attempt < 3) {
                    val delayMs = 2000L * attempt
                    log.warn("DOWNLOAD: attempt $attempt/3 failed for $url: ${e.message}. Retrying in ${delayMs}ms")
                    delay(delayMs)
                }
            }
        }
        throw lastError ?: RuntimeException("Failed to download $url after 3 attempts")
    }

    // ── JSON parser for thiagobodruk/bible format ──

    @Serializable
    private data class BibleJsonBook(
        val abbrev: String = "",
        val chapters: List<List<String>> = emptyList()
    )

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    private data class ParsedBook(val chapters: List<List<String>>)

    private fun parseJsonBooksArray(json: String): List<ParsedBook> {
        return try {
            // Remove BOM if present
            val clean = json.trimStart('\uFEFF').trim()
            val parsed = jsonParser.decodeFromString<List<BibleJsonBook>>(clean)
            parsed.map { ParsedBook(it.chapters) }
        } catch (e: Exception) {
            log.error("JSON_PARSE: failed to parse Bible JSON: ${e.message}")
            emptyList()
        }
    }

    // ── Phase tracking ──

    // ── Word Alignment ──

    // ── Enrichment Translation ──

    private suspend fun translateEnrichmentGreek() = runPhaseTracked("bible_translate_enrichment_greek") {
        translateEnrichmentLexicon("greek")
    }

    private suspend fun translateEnrichmentHebrew() = runPhaseTracked("bible_translate_enrichment_hebrew") {
        translateEnrichmentLexicon("hebrew")
    }

    private suspend fun translateEnrichmentLexicon(language: String) {
        val locales = mapOf("pt" to "Portuguese", "es" to "Spanish")
        val entries = if (language == "greek") lexiconRepository.getAllGreekEntries() else lexiconRepository.getAllHebrewEntries()
        val enrichedEntries = entries.filter { it.phoneticSpelling != null || it.wordOrigin != null }
        val phaseName = "bible_translate_enrichment_$language"

        val totalItems = enrichedEntries.size * locales.size
        phaseTracker.markProgress(phaseName, 0, totalItems)

        val processed = java.util.concurrent.atomic.AtomicInteger(0)
        val progressMutex = kotlinx.coroutines.sync.Mutex()

        coroutineScope {
            for (entry in enrichedEntries) {
                for ((locale, langName) in locales) {
                    val hasTranslation = if (language == "greek")
                        lexiconRepository.hasGreekEnrichmentTranslation(entry.id, locale)
                    else
                        lexiconRepository.hasHebrewEnrichmentTranslation(entry.id, locale)

                    if (hasTranslation) {
                        processed.incrementAndGet()
                        continue
                    }

                    launch {
                        try {
                            retryOnSerializationError {
                                translateEnrichmentEntry(entry, locale, langName, language)
                            }
                        } catch (e: Exception) {
                            log.warn("TRANSLATE_ENRICHMENT: error on ${entry.strongsNumber} ($locale): ${e.message}")
                        }
                        val p = processed.incrementAndGet()
                        if (p % 50 == 0) {
                            progressMutex.withLock {
                                phaseTracker.markProgress(phaseName, p)
                            }
                            log.info("TRANSLATE_ENRICHMENT_${language.uppercase()}: progress [$p/$totalItems]")
                        }
                    }
                }
            }
        }

        phaseTracker.markProgress(phaseName, processed.get())
        log.info("TRANSLATE_ENRICHMENT_${language.uppercase()}: completed ${processed.get()}/$totalItems")
    }

    private suspend fun translateEnrichmentEntry(entry: com.ntcoverage.model.LexiconEntryDTO, locale: String, langName: String, lexiconType: String) {
        val fieldsToTranslate = buildString {
            entry.kjvTranslation?.let { appendLine("KJV_TRANSLATION: $it") }
            entry.wordOrigin?.let { appendLine("WORD_ORIGIN: $it") }
            entry.strongsExhaustive?.let { appendLine("STRONGS_EXHAUSTIVE: $it") }
            entry.nasExhaustiveOrigin?.let { appendLine("NAS_ORIGIN: $it") }
            entry.nasExhaustiveDefinition?.let { appendLine("NAS_DEFINITION: $it") }
            entry.nasExhaustiveTranslation?.let { appendLine("NAS_TRANSLATION: $it") }
        }.trim()

        if (fieldsToTranslate.isBlank()) return

        val prompt = """Translate these lexicon fields to $langName.
Keep Greek/Hebrew words, Strong's numbers (G1234, H5678), and Bible references unchanged.
Use academic biblical terminology.

$fieldsToTranslate

Return in the EXACT same format with translated values:"""

        // Direct LLM removed — should use queue
        log.warn("translateEnrichmentEntry: direct LLM removed, use queue instead")
        return
    }

    private fun extractFieldValue(content: String, fieldName: String): String? {
        val pattern = Regex("$fieldName:\\s*(.+?)(?=\\n[A-Z_]+:|$)", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(content)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }

    private suspend fun <T> retryOnSerializationError(maxRetries: Int = 3, block: suspend () -> T): T {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                val isSerialization = e.message?.contains("could not serialize access") == true ||
                    e.cause?.message?.contains("could not serialize access") == true
                if (isSerialization && attempt < maxRetries - 1) {
                    lastException = e
                    delay(100L * (1 shl attempt)) // exponential backoff: 100ms, 200ms, 400ms
                } else {
                    throw e
                }
            }
        }
        throw lastException!!
    }

    private suspend fun enrichGreekLexicon() = runPhaseTracked("bible_enrich_greek_lexicon") {
        lexiconEnrichmentService.enrichGreekLexicon(phaseTracker)
    }

    private suspend fun enrichHebrewLexicon() = runPhaseTracked("bible_enrich_hebrew_lexicon") {
        lexiconEnrichmentService.enrichHebrewLexicon(phaseTracker)
    }

    private suspend fun fillMissingHebrew() = runPhaseTracked("bible_fill_missing_hebrew") {
        lexiconEnrichmentService.fillMissingHebrewEntries(phaseTracker)
    }

    // ── Layer 4: Tokenization ──

    private suspend fun tokenizeVersion(versionCode: String, scope: IngestionScope? = null) {
        val svc = tokenizationService
            ?: throw IllegalStateException("tokenizationService not configured — cannot tokenize $versionCode")
        val phaseName = "bible_tokenize_${versionCode.lowercase()}"
        runPhaseTracked(phaseName) {
            val count = svc.tokenizeVersion(versionCode, phaseTracker, phaseName, scope)
            log.info("TOKENIZE_$versionCode: completed with $count tokens${scope?.let { " (scope=${it.bookName}${it.chapter?.let { c -> " $c" } ?: ""}${it.verse?.let { v -> ":$v" } ?: ""})" } ?: ""}")
        }
    }

    private suspend fun lemmatizeVersionPlaceholder(versionCode: String) {
        val phaseName = "bible_lemmatize_${versionCode.lowercase()}"
        runPhaseTracked(phaseName) {
            // Lemmatization is LLM-powered and will be enqueued to llm_prompt_queue.
            // For now, this is a placeholder — tokens are created without lemmas,
            // and the lemmatization phase will populate them via batch LLM calls.
            log.info("LEMMATIZE_$versionCode: placeholder — lemmatization requires LLM batch processing")
        }
    }

    private suspend fun reEnrichGreekLexicon() = runPhaseTracked("bible_reenrich_greek_lexicon") {
        lexiconEnrichmentService.reEnrichGreekLexicon(phaseTracker)
    }

    private suspend fun reEnrichHebrewLexicon() = runPhaseTracked("bible_reenrich_hebrew_lexicon") {
        lexiconEnrichmentService.reEnrichHebrewLexicon(phaseTracker)
    }

    private suspend fun alignForVersion(versionCode: String, testament: String = "NT", overridePhaseName: String? = null, scope: IngestionScope? = null) {
        val phaseName = overridePhaseName ?: "bible_align_${versionCode.lowercase()}"
        runPhaseTracked(phaseName) {
            val ntBooks = bookRepository.findAll(testament)
                .filter { scope?.bookName == null || it.name == scope.bookName }
            val bookChapters = ntBooks.map { it.name to it.totalChapters }
            val totalExpected = bookChapters.sumOf { (_, chapters) ->
                (1..chapters).count { ch -> scope?.chapter == null || ch == scope.chapter }
            }
            phaseTracker.markProgress(phaseName, 0, totalExpected)

            var aligned = 0
            var skipped = 0
            var failed = 0
            var visited = 0
            for ((bookName, chapters) in bookChapters) {
                for (chapter in 1..chapters) {
                    if (scope?.chapter != null && chapter != scope.chapter) continue
                    visited++
                    try {
                        when (wordAlignmentService.alignChapter(bookName, chapter, versionCode)) {
                            is WordAlignmentService.AlignChapterResult.Aligned,
                            is WordAlignmentService.AlignChapterResult.AlreadyAligned -> aligned++
                            is WordAlignmentService.AlignChapterResult.SkippedNoInterlinear,
                            is WordAlignmentService.AlignChapterResult.SkippedNoVersionText -> skipped++
                        }
                    } catch (e: Exception) {
                        failed++
                        log.error("BIBLE_ALIGN_${versionCode}: failed $bookName $chapter: ${e.message}")
                    }
                    if (visited % 5 == 0) {
                        phaseTracker.markProgress(phaseName, visited)
                        log.info("BIBLE_ALIGN_${versionCode}: progress [$visited/$totalExpected] (aligned=$aligned skipped=$skipped failed=$failed)")
                    }
                }
            }

            phaseTracker.markProgress(phaseName, visited)
            val totalAlignments = interlinearRepository.countAlignments(versionCode)
            log.info("BIBLE_ALIGN_${versionCode}: done — visited=$visited aligned=$aligned skipped=$skipped failed=$failed totalAlignments=$totalAlignments")

            if (aligned == 0 && visited > 0) {
                throw IllegalStateException(
                    "Zero chapters aligned out of $visited (skipped=$skipped, failed=$failed). " +
                    "Prerequisite data may be missing — run Layer 2 (interlinear) and Layer 3 (text) first."
                )
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Prepare phases — enqueue prompts to LLM queue
    // ══════════════════════════════════════════════════════════════

    private val queueJson = Json { encodeDefaults = true }

    suspend fun translateLexiconPrepare(lexiconType: String = "greek") = runPhaseTracked("bible_translate_${if (lexiconType == "greek") "lexicon" else "hebrew_lexicon"}") {
        val repo = llmQueueRepository ?: throw IllegalStateException("llmQueueRepository not configured")
        val targetLocales = mapOf("pt" to "Portuguese", "es" to "Spanish")
        val allEntries = if (lexiconType == "greek") lexiconRepository.getAllGreekEntries() else lexiconRepository.getAllHebrewEntries()
        val phaseName = "bible_translate_${if (lexiconType == "greek") "lexicon" else "hebrew_lexicon"}"

        var enqueued = 0
        for ((locale, language) in targetLocales) {
            val untranslated = allEntries.filter { entry ->
                val hasTranslation = if (lexiconType == "greek") lexiconRepository.hasTranslation(entry.id, locale)
                else lexiconRepository.hasHebrewTranslation(entry.id, locale)
                !hasTranslation && (!entry.shortDefinition.isNullOrBlank() || !entry.fullDefinition.isNullOrBlank())
            }

            val batchSize = if (lexiconType == "hebrew" && locale == "pt") 10 else llmConfig.mediumBatchSize
            val batches = untranslated.chunked(batchSize)

            for (batch in batches) {
                val entriesBlock = batch.joinToString("\n\n") { entry ->
                    val short = entry.shortDefinition?.take(200) ?: ""
                    val full = entry.fullDefinition?.take(800) ?: ""
                    "[${entry.strongsNumber}] short: \"$short\" | full: \"$full\""
                }
                val typeLabel = if (lexiconType == "greek") "Greek" else "Hebrew"
                val systemPrompt = """Translate these $typeLabel lexicon entries to $language.
Return ONLY translations in this exact format, one block per entry:

[STRONG_NUMBER]
SHORT: translated short definition
FULL: translated full definition

Rules:
- Translate ALL English text to $language
- Keep Hebrew words, Greek words, and Bible references unchanged
- Keep Strong's numbers (H1234, G3056) unchanged
- Use academic biblical terminology
- Preserve numbered definitions (1., 2., etc.)
- Keep the same structure and formatting
- Return one [STRONG_NUMBER] block for EACH entry below"""

                val ctx = LlmResponseProcessor.LexiconBatchContext(
                    locale = locale,
                    lexiconType = lexiconType,
                    entries = batch.map { LlmResponseProcessor.LexiconBatchEntry(it.id, it.strongsNumber) }
                )

                repo.enqueue(
                    phaseName = phaseName,
                    label = "LEXICON_BATCH_${typeLabel}_${batch.size}",
                    systemPrompt = systemPrompt,
                    userContent = entriesBlock,
                    temperature = 0.1,
                    maxTokens = if (lexiconType == "hebrew" && locale == "pt") 5000 else 8000,
                    tier = "MEDIUM",
                    callbackContext = queueJson.encodeToString(ctx)
                )
                enqueued += batch.size
            }
        }
        phaseTracker.markProgress(phaseName, enqueued, enqueued)
        log.info("LEXICON_PREPARE: enqueued $enqueued items for $lexiconType")
    }

    suspend fun translateEnrichmentPrepare(lexiconType: String = "greek") = runPhaseTracked("bible_translate_enrichment_${lexiconType}") {
        val repo = llmQueueRepository ?: throw IllegalStateException("llmQueueRepository not configured")
        val locales = mapOf("pt" to "Portuguese", "es" to "Spanish")
        val entries = if (lexiconType == "greek") lexiconRepository.getAllGreekEntries() else lexiconRepository.getAllHebrewEntries()
        val enrichedEntries = entries.filter { it.phoneticSpelling != null || it.wordOrigin != null }
        val phaseName = "bible_translate_enrichment_${lexiconType}"

        var enqueued = 0
        for (entry in enrichedEntries) {
            for ((locale, langName) in locales) {
                val hasTranslation = if (lexiconType == "greek")
                    lexiconRepository.hasGreekEnrichmentTranslation(entry.id, locale)
                else
                    lexiconRepository.hasHebrewEnrichmentTranslation(entry.id, locale)
                if (hasTranslation) continue

                val fieldsToTranslate = buildString {
                    entry.kjvTranslation?.let { appendLine("KJV_TRANSLATION: $it") }
                    entry.wordOrigin?.let { appendLine("WORD_ORIGIN: $it") }
                    entry.strongsExhaustive?.let { appendLine("STRONGS_EXHAUSTIVE: $it") }
                    entry.nasExhaustiveOrigin?.let { appendLine("NAS_ORIGIN: $it") }
                    entry.nasExhaustiveDefinition?.let { appendLine("NAS_DEFINITION: $it") }
                    entry.nasExhaustiveTranslation?.let { appendLine("NAS_TRANSLATION: $it") }
                }.trim()
                if (fieldsToTranslate.isBlank()) continue

                val userContent = """Translate these lexicon fields to $langName.
Keep Greek/Hebrew words, Strong's numbers (G1234, H5678), and Bible references unchanged.
Use academic biblical terminology.

$fieldsToTranslate

Return in the EXACT same format with translated values:"""

                val ctx = LlmResponseProcessor.EnrichmentTranslateContext(entry.id, locale, lexiconType)
                repo.enqueue(
                    phaseName = phaseName,
                    label = "ENRICHMENT_TRANSLATE_${entry.strongsNumber}_$locale",
                    systemPrompt = "You are a biblical lexicon translator. Return ONLY the translated fields in the same KEY: VALUE format.",
                    userContent = userContent,
                    temperature = 0.3,
                    maxTokens = 2000,
                    tier = "MEDIUM",
                    callbackContext = queueJson.encodeToString(ctx)
                )
                enqueued++
            }
        }
        phaseTracker.markProgress(phaseName, enqueued, enqueued)
        log.info("ENRICHMENT_PREPARE: enqueued $enqueued items for $lexiconType")
    }

    suspend fun alignVersionPrepare(versionCode: String, scope: IngestionScope? = null) = runPhaseTracked("bible_align_${versionCode.lowercase()}") {
        val repo = llmQueueRepository ?: throw IllegalStateException("llmQueueRepository not configured")
        val phaseName = "bible_align_${versionCode.lowercase()}"

        val version = versionRepository.findByCode(versionCode)
            ?: throw IllegalArgumentException("Version not found: $versionCode")
        val language = version.language
        val allBooks = bookRepository.findAll("NT")
        val booksToProcess = allBooks.filter { scope?.bookName == null || it.name == scope.bookName }

        var enqueued = 0
        for (book in booksToProcess) {
            for (chapter in 1..book.totalChapters) {
                if (scope?.chapter != null && chapter != scope.chapter) continue

                val interlinearByVerse = interlinearRepository.getWordsForChapter(book.id, chapter)
                if (interlinearByVerse.isEmpty()) continue

                val versionTexts = verseRepository.getChapterTexts(version.id, book.id, chapter)
                if (versionTexts.isEmpty()) continue

                val versionTextByVerse = versionTexts.associate { it.verseNumber to it.text }

                for (verseNumber in interlinearByVerse.keys.sorted()) {
                    if (scope?.verse != null && verseNumber != scope.verse) continue
                    val versionText = versionTextByVerse[verseNumber] ?: continue
                    val verseId = verseRepository.getVerseId(book.id, chapter, verseNumber) ?: continue
                    if (interlinearRepository.hasAlignmentsForVerse(verseId, versionCode)) continue

                    val words = interlinearByVerse[verseNumber] ?: continue

                    // Level 1-3 deterministic pre-alignment (when tokens available)
                    val tokens = bibleTokenRepository?.getTokensForVerse(verseId, version.id) ?: emptyList()
                    val wordsForLlm = if (tokens.isNotEmpty()) {
                        val localResult = wordAlignmentService.alignVerseLocal(words, tokens, language)
                        for (resolved in localResult.resolved) {
                            interlinearRepository.upsertAlignment(
                                verseId = verseId,
                                wordPosition = resolved.greekPosition.toShort(),
                                versionCode = versionCode,
                                kjvIndices = null,
                                alignedText = resolved.alignedText,
                                isDivergent = false,
                                confidence = resolved.confidence,
                                tokenPositions = "[${resolved.tokenPositions.joinToString(",")}]",
                                method = resolved.method
                            )
                        }
                        if (localResult.unresolvedPositions.isEmpty()) {
                            log.info("WORD_ALIGN_LOCAL: ${book.name} $chapter:$verseNumber fully resolved by L1-3 (${localResult.resolved.size} words, no LLM)")
                            continue
                        }
                        if (localResult.resolved.isNotEmpty()) {
                            log.debug("WORD_ALIGN_LOCAL: ${book.name} $chapter:$verseNumber resolved ${localResult.resolved.size}, unresolved ${localResult.unresolvedPositions.size} → LLM")
                        }
                        words.filter { it.wordPosition in localResult.unresolvedPositions }
                    } else {
                        words
                    }

                    val targetWords = wordAlignmentService.splitKjvText(versionText)
                    val isEnglishVersion = language == "en"

                    val (expressionMap, consumedIndices) = if (isEnglishVersion) {
                        wordAlignmentService.detectExpressions(wordsForLlm, targetWords)
                    } else {
                        wordAlignmentService.detectExpressionsWithTranslatedGlosses(wordsForLlm, targetWords, language)
                    }

                    val prompt = wordAlignmentService.buildAlignmentPrompt(wordsForLlm, targetWords, expressionMap, consumedIndices, isEnglishVersion, language)
                    val systemPrompt = wordAlignmentService.buildSystemPrompt(language)

                    val ctx = LlmResponseProcessor.WordAlignContext(verseId, versionCode, book.name, chapter, verseNumber)
                    repo.enqueue(
                        phaseName = phaseName,
                        label = "WORD_ALIGN_${versionCode}_${book.name}_${chapter}v$verseNumber",
                        systemPrompt = systemPrompt,
                        userContent = prompt,
                        temperature = 0.0,
                        maxTokens = 1500,
                        tier = "HIGH",
                        callbackContext = queueJson.encodeToString(ctx)
                    )
                    enqueued++
                }
            }
        }
        phaseTracker.markProgress(phaseName, enqueued, enqueued)
        log.info("ALIGN_PREPARE: enqueued $enqueued verses for $versionCode")
    }

    /**
     * N4b — Semantic Enrichment.
     *
     * For every verse that already has word alignments (from N1-N3 + N4a), enqueues an LLM
     * prompt that returns:
     *   - contextual_sense: what each Greek word means specifically in this verse
     *   - semantic_relation: equivalent | synonymous | related | divergent
     *
     * Updates word_alignments.contextual_sense + word_alignments.semantic_relation.
     * Does NOT modify alignment indices, aligned_text or confidence.
     */
    suspend fun enrichSemanticsPrepare(versionCode: String, scope: IngestionScope? = null) = runPhaseTracked("bible_enrich_semantics_${versionCode.lowercase()}") {
        val repo = llmQueueRepository ?: throw IllegalStateException("llmQueueRepository not configured")
        val phaseName = "bible_enrich_semantics_${versionCode.lowercase()}"

        val version = versionRepository.findByCode(versionCode)
            ?: throw IllegalArgumentException("Version not found: $versionCode")
        val allBooks = bookRepository.findAll("NT")
        val booksToProcess = allBooks.filter { scope?.bookName == null || it.name == scope.bookName }

        val systemPrompt = """You are a biblical Greek-Portuguese semantic analysis expert.
Given a New Testament verse with each Greek word already aligned to its Portuguese translation (ARC69),
for EVERY alignment provide:
  "s": the contextual sense — what the Greek word MEANS in this specific verse (not just the dictionary gloss)
  "r": semantic relationship between the contextual sense and the Portuguese token:
       "equivalent"  — translation accurately expresses the contextual sense
       "synonymous"  — translation uses a near-synonym (valid choice, meaning preserved)
       "related"     — same semantic field but a freer/interpretive translation choice
       "divergent"   — translation falls outside the expected semantic field for this word

CRITICAL:
- For polysemous words (e.g. κρίνω = "judge/condemn"), the contextual sense resolves the ambiguity.
  Example: κρίνεται in John 3:18 → "s":"condenar", "r":"equivalent" (NOT divergent)
- Idiomatic renderings are NOT divergent if they convey the same meaning.
  Example: Οὕτως → "de tal maneira" → "r":"equivalent"
- Only mark "divergent" for genuine mistranslations outside the semantic field.
- Include EVERY alignment in the response, even if relation is obvious.

Return ONLY valid JSON: {"e":[{"g":<pos>,"s":"<contextual_sense>","r":"<relation>"},...]}"""

        var enqueued = 0
        for (book in booksToProcess) {
            for (chapter in 1..book.totalChapters) {
                if (scope?.chapter != null && chapter != scope.chapter) continue

                val interlinearByVerse = interlinearRepository.getWordsForChapter(book.id, chapter)
                if (interlinearByVerse.isEmpty()) continue

                val versionTexts = verseRepository.getChapterTexts(version.id, book.id, chapter)
                val versionTextByVerse = versionTexts.associate { it.verseNumber to it.text }

                val alignmentsByVerse = interlinearRepository.getAlignmentsForChapter(book.id, chapter, versionCode)

                for (verseNumber in interlinearByVerse.keys.sorted()) {
                    if (scope?.verse != null && verseNumber != scope.verse) continue
                    val versionText = versionTextByVerse[verseNumber] ?: continue
                    val verseId = verseRepository.getVerseId(book.id, chapter, verseNumber) ?: continue

                    // Only enrich verses that already have alignments
                    val verseAlignments = alignmentsByVerse
                        .filter { (k, _) -> k.first == verseNumber }
                        .mapValues { it.value }
                    if (verseAlignments.isEmpty()) continue

                    val words = interlinearByVerse[verseNumber] ?: continue

                    // Build compact prompt with greek word + gloss + aligned ARC69 text
                    val alignmentsJson = words.joinToString(",") { w ->
                        val alignment = verseAlignments[Pair(verseNumber, w.wordPosition)]
                        val ptGloss = w.portugueseGloss ?: w.englishGloss ?: ""
                        val aligned = alignment?.alignedText ?: ""
                        """{"g":${w.wordPosition},"w":"${escapeJsonStr(w.originalWord)}","gloss":"${escapeJsonStr(ptGloss)}","aligned":"${escapeJsonStr(aligned)}"}"""
                    }

                    val userContent = """Verse: ${book.name} ${chapter}:${verseNumber}
Portuguese (ARC69): "${escapeJsonStr(versionText)}"
Greek alignments: [$alignmentsJson]"""

                    val ctx = LlmResponseProcessor.SemanticEnrichContext(verseId, versionCode, book.name, chapter, verseNumber)
                    repo.enqueue(
                        phaseName = phaseName,
                        label = "SEMANTIC_ENRICH_${versionCode}_${book.name}_${chapter}v$verseNumber",
                        systemPrompt = systemPrompt,
                        userContent = userContent,
                        temperature = 0.0,
                        maxTokens = words.size * 30 + 100,
                        tier = "HIGH",
                        callbackContext = queueJson.encodeToString(ctx)
                    )
                    enqueued++
                }
            }
        }
        phaseTracker.markProgress(phaseName, enqueued, enqueued)
        log.info("SEMANTIC_ENRICH_PREPARE: enqueued $enqueued verses for $versionCode")
    }

    suspend fun translateGlossesPrepare(scope: IngestionScope? = null) = runPhaseTracked("bible_translate_glosses") {
        val repo = llmQueueRepository ?: throw IllegalStateException("llmQueueRepository not configured")
        val phaseName = "bible_translate_glosses"

        val sanitizedRows = if (scope != null) {
            val scopedBookId = bookRepository.findByName(scope.bookName)?.id
            interlinearRepository.clearCorruptedPortugueseGlossesScoped(scopedBookId, scope.chapter)
        } else {
            interlinearRepository.clearCorruptedPortugueseGlosses()
        }
        if (sanitizedRows > 0) {
            log.info("TRANSLATE_GLOSSES_PREPARE: nullified $sanitizedRows corrupted PT gloss rows before re-translation (scope=$scope)")
        }

        val booksToProcess = if (scope != null) {
            listOfNotNull(bookRepository.findByName(scope.bookName))
        } else {
            bookRepository.findAll("NT")
        }

        var enqueued = 0
        for (book in booksToProcess) {
            val chapterRange = if (scope?.chapter != null) scope.chapter..scope.chapter else 1..book.totalChapters
            for (chapter in chapterRange) {
                val wordsWithIds = interlinearRepository.getWordsForChapterWithIds(book.id, chapter)
                val untranslated = wordsWithIds.filter {
                    !it.second.transliteration.isNullOrBlank() && it.second.portugueseGloss.isNullOrBlank()
                }
                if (untranslated.isEmpty()) continue

                val uniqueEntries = untranslated
                    .map { GlossTranslationEntry(it.second.transliteration!!.trim(), it.second.morphology?.trim() ?: "", it.second.englishGloss?.trim()?.removeSurrounding("<", ">") ?: "", it.second.lemma?.trim() ?: "") }
                    .distinct()

                // Build word → wordId map for the callback context
                val translitToWordIds = untranslated.groupBy {
                    GlossTranslationEntry(it.second.transliteration!!.trim(), it.second.morphology?.trim() ?: "", it.second.englishGloss?.trim()?.removeSurrounding("<", ">") ?: "", it.second.lemma?.trim() ?: "")
                }.mapValues { (_, pairs) -> pairs.first().first }

                for (locale in listOf("pt", "es")) {
                    val language = if (locale == "pt") "Portuguese" else "Spanish"
                    val chunks = uniqueEntries.chunked(20)

                    for ((chunkIdx, chunk) in chunks.withIndex()) {
                        val input = chunk.joinToString("\n") { "${it.transliteration} | ${it.morphology} | ${it.englishGloss} | ${it.lemma}" }

                        val systemPrompt = """You are a biblical Greek-to-$language translator for interlinear Bible glosses.
Each line contains 4 fields separated by |: transliteration | morphology | english_gloss | lemma

Translate each Greek word to a short $language gloss (1-3 words).
Use the english_gloss as the semantic reference, then adapt to $language grammar using the morphology code.

Rules:
- Match grammatical number: 3P verbs → plural, 3S → singular
- Match grammatical gender for articles/pronouns: M→masculine, F→feminine, N→neuter
- For pronouns, translate the grammatical function (dative=to/a, genitive=of/de, accusative=direct object)
- Prefer the contextual meaning of the english_gloss over the primary dictionary meaning of the lemma

LANGUAGE LOCK — CRITICAL:
- Every value MUST be in $language only. NEVER output English (the, was, and, of, word, god, is, be, were, has, had).
- NEVER mix languages across values. If target is Portuguese, do NOT emit Spanish (el, la, los, palabra, y, fue, era, ese); if target is Spanish, do NOT emit Portuguese (o, a, os, palavra, e, foi, verbo).
- If you do not know the equivalent in $language, copy the transliteration verbatim.

Few-shot (target=$language):
${if (language == "Portuguese") """- "en" → "em"
- "logos" → "Palavra"
- "theos" → "Deus"
- "ho" → "o"""" else """- "en" → "en"
- "logos" → "Palabra"
- "theos" → "Dios"
- "ho" → "el""""}

Return a JSON object mapping each transliteration to its $language translation.
IMPORTANT: Return ONLY the JSON object. No preamble, no explanation."""

                        val keys = chunk.map { it.transliteration }
                        val entries = chunk.map { entry ->
                            val wordId = translitToWordIds[entry] ?: 0
                            LlmResponseProcessor.GlossEntryContext(entry.transliteration, wordId)
                        }
                        val ctx = LlmResponseProcessor.GlossTranslateContext(locale, keys, entries)

                        repo.enqueue(
                            phaseName = phaseName,
                            label = "GLOSS_TRANSLATE_${language}_chunk${chunkIdx}_${book.name}_$chapter",
                            systemPrompt = systemPrompt,
                            userContent = input,
                            temperature = 0.0,
                            maxTokens = chunk.size * 40,
                            tier = "MEDIUM",
                            callbackContext = queueJson.encodeToString(ctx)
                        )
                        enqueued += chunk.size
                    }
                }
            }
        }
        phaseTracker.markProgress(phaseName, enqueued, enqueued)
        log.info("GLOSS_PREPARE: enqueued $enqueued gloss items (scope=${scope ?: "all"})")
    }

    // ── Gloss Audit (PT via LLM-as-judge) ──

    suspend fun auditGlossesPrepare(scope: IngestionScope? = null) = runPhaseTracked("bible_audit_glosses_pt") {
        val repo = llmQueueRepository ?: throw IllegalStateException("llmQueueRepository not configured")
        val phaseName = "bible_audit_glosses_pt"
        val booksToProcess = if (scope != null) {
            listOfNotNull(bookRepository.findByName(scope.bookName))
        } else {
            bookRepository.findAll("NT")
        }

        var enqueued = 0
        for (book in booksToProcess) {
            val chapterRange = if (scope?.chapter != null) scope.chapter..scope.chapter else 1..book.totalChapters
            for (chapter in chapterRange) {
                val wordsWithIds = interlinearRepository.getWordsForChapterWithIds(book.id, chapter)
                val auditable = wordsWithIds.filter {
                    val gloss = it.second.portugueseGloss
                    gloss != null && gloss.isNotBlank()
                }
                if (auditable.isEmpty()) continue

                val chunks = auditable.chunked(25)
                for ((chunkIdx, chunk) in chunks.withIndex()) {
                    val entries = chunk.mapIndexed { idx, (wordId, dto) ->
                        LlmResponseProcessor.GlossAuditEntry(
                            tempId = idx + 1,
                            wordId = wordId,
                            gloss = dto.portugueseGloss ?: "",
                            transliteration = dto.transliteration ?: "",
                            englishGloss = dto.englishGloss ?: ""
                        )
                    }

                    val inputJson = entries.joinToString(",\n", prefix = "[", postfix = "]") { e ->
                        """{"id":${e.tempId},"gloss":"${escapeJsonStr(e.gloss)}","english_gloss":"${escapeJsonStr(e.englishGloss)}","transliteration":"${escapeJsonStr(e.transliteration)}"}"""
                    }

                    val systemPrompt = """You audit interlinear Greek→Portuguese (ARC69) Bible glosses for language correctness.
For each entry {id, gloss, english_gloss, transliteration}, output ONE line per id in this exact format:
- id:ok                        — gloss is correct natural Portuguese and consistent with english_gloss+transliteration
- id:bad:en:<pt_suggestion>    — gloss is in English
- id:bad:es:<pt_suggestion>    — gloss is in Spanish
- id:bad:other:<pt_suggestion_or_null> — unreadable, JSON leak, empty, or other language

Examples:
Input [{"id":1,"gloss":"em","english_gloss":"in","transliteration":"en"},
       {"id":2,"gloss":"the","english_gloss":"the","transliteration":"ho"},
       {"id":3,"gloss":"palabra","english_gloss":"word","transliteration":"logos"}]
Output:
1:ok
2:bad:en:o
3:bad:es:Palavra

Rules:
- Portuguese diacritics (ã, ç, ó) are REQUIRED where applicable ("Deus" ok, "Deus" ok; "Espiritu" is Spanish → bad).
- Short function words like "o", "a", "e", "em" are valid Portuguese (not Spanish).
- Contractions ("no"="em+o", "do"="de+o") are valid.
- Answer ONE line per id. Nothing else. No preamble, no trailing text, no JSON."""

                    val ctx = LlmResponseProcessor.GlossAuditContext(
                        locale = "pt",
                        bookName = book.name,
                        chapter = chapter,
                        entries = entries
                    )

                    repo.enqueue(
                        phaseName = phaseName,
                        label = "GLOSS_AUDIT_PT_${book.name}_${chapter}_chunk$chunkIdx",
                        systemPrompt = systemPrompt,
                        userContent = inputJson,
                        temperature = 0.0,
                        maxTokens = chunk.size * 30,
                        tier = "HIGH",
                        callbackContext = queueJson.encodeToString(ctx)
                    )
                    enqueued += chunk.size
                }
            }
        }
        phaseTracker.markProgress(phaseName, enqueued, enqueued)
        log.info("GLOSS_AUDIT_PREPARE: enqueued $enqueued audit items (scope=${scope ?: "all"})")
    }

    private fun escapeJsonStr(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")

    private suspend fun runPhaseTracked(phaseName: String, block: suspend () -> Unit) {
        val phaseStartedAt = System.currentTimeMillis()
        log.info("BIBLE_PHASE: '{}' → RUNNING", phaseName)
        phaseTracker.markRunning(phaseName, runBy = "manual")
        try {
            block()
            val status = phaseTracker.getPhaseStatus(phaseName)
            phaseTracker.markSuccess(phaseName, status?.itemsProcessed ?: 0)
            val elapsed = System.currentTimeMillis() - phaseStartedAt
            log.info("BIBLE_PHASE: '{}' → SUCCESS (items={} elapsed={}ms)", phaseName, status?.itemsProcessed ?: 0, elapsed)
        } catch (e: Throwable) {
            phaseTracker.markFailed(phaseName, e.message ?: "Unknown error")
            val elapsed = System.currentTimeMillis() - phaseStartedAt
            log.error("BIBLE_PHASE: '{}' → FAILED after {}ms: {}", phaseName, elapsed, e.message, e)
            throw e
        }
    }
}
