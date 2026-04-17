package com.ntcoverage.service

import com.ntcoverage.model.BibleVerseTokenDTO
import com.ntcoverage.repository.BibleTokenRepository
import com.ntcoverage.repository.BibleVerseRepository
import com.ntcoverage.repository.BibleVersionRepository
import com.ntcoverage.repository.BibleBookRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class BibleTokenizationService(
    private val tokenRepository: BibleTokenRepository,
    private val verseRepository: BibleVerseRepository,
    private val versionRepository: BibleVersionRepository,
    private val bookRepository: BibleBookRepository
) {
    private val log = LoggerFactory.getLogger(BibleTokenizationService::class.java)
    private val json = Json { encodeDefaults = false }

    // ── Punctuation stripping regexes ──
    // Strips leading/trailing punctuation but preserves internal hyphens (enclíticos)
    // Handles Unicode quotation marks: "", '', «»
    private val LEADING_PUNCT = Regex("^[\\p{Punct}\\u201C\\u201D\\u2018\\u2019\\u00AB\\u00BB]+")
    private val TRAILING_PUNCT = Regex("[\\p{Punct}\\u201C\\u201D\\u2018\\u2019\\u00AB\\u00BB]+$")

    // ── Contraction map: Portuguese contractions → decomposed parts ──
    // These are deterministic — finite list covers all standard PT contractions
    private val CONTRACTIONS: Map<String, List<ContractionPart>> = buildContractionMap()

    // ── Enclitic pronoun suffixes (after hyphen) ──
    private val ENCLITIC_PRONOUNS = setOf(
        "me", "te", "se", "lhe", "lhes", "nos", "vos",
        "o", "a", "os", "as",
        "lo", "la", "los", "las",
        "no", "nos" // after nasal: "põe-no", "fizeram-nos"
    )

    @Serializable
    data class ContractionPart(val form: String, val role: String)

    /**
     * Tokenizes a raw verse text into structured tokens with contraction/enclitic decomposition.
     *
     * Rules:
     * 1. Split by whitespace
     * 2. token_raw = word as-is (preserves punctuation)
     * 3. token = strip leading/trailing punctuation + lowercase (preserves accents + internal hyphens)
     * 4. Detect contractions (nele→em+ele) and enclíticos (disse-lhe→disse+lhe)
     * 5. Lemma is populated later by LLM batch (left null here)
     */
    fun tokenize(rawText: String): List<BibleVerseTokenDTO> {
        val words = rawText.split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.mapIndexed { index, raw ->
            val clean = raw
                .replace(LEADING_PUNCT, "")
                .replace(TRAILING_PUNCT, "")
                .lowercase()

            if (clean.isEmpty()) return@mapIndexed null

            // Check for contraction
            val contractionParts = CONTRACTIONS[clean]
            if (contractionParts != null) {
                return@mapIndexed BibleVerseTokenDTO(
                    position = index,
                    token = clean,
                    tokenRaw = raw,
                    lemma = null, // populated by LLM lemmatization phase
                    isContraction = true,
                    contractionParts = json.encodeToString(contractionParts),
                    isEnclitic = false,
                    encliticParts = null
                )
            }

            // Check for enclitic (hyphenated word with pronoun suffix)
            val encliticResult = detectEnclitic(clean)
            if (encliticResult != null) {
                return@mapIndexed BibleVerseTokenDTO(
                    position = index,
                    token = clean,
                    tokenRaw = raw,
                    lemma = null,
                    isContraction = false,
                    contractionParts = null,
                    isEnclitic = true,
                    encliticParts = json.encodeToString(encliticResult)
                )
            }

            // Regular token
            BibleVerseTokenDTO(
                position = index,
                token = clean,
                tokenRaw = raw,
                lemma = null,
                isContraction = false,
                contractionParts = null,
                isEnclitic = false,
                encliticParts = null
            )
        }.filterNotNull()
    }

    /**
     * Detects if a hyphenated word is a verb+pronoun enclitic.
     * "disse-lhe" → [{"form":"disse","role":"VERB"},{"form":"lhe","role":"PRON"}]
     * "fizeram-se" → [{"form":"fizeram","role":"VERB"},{"form":"se","role":"PRON"}]
     *
     * Handles double enclíticos: "deu-no-lo" → verb + pron + pron
     * Excludes compound words that aren't enclíticos: "bem-aventurado", "todo-poderoso"
     */
    internal fun detectEnclitic(token: String): List<ContractionPart>? {
        if (!token.contains('-')) return null

        val parts = token.split('-')
        if (parts.size < 2) return null

        // Check if the last part(s) after hyphen are known pronoun suffixes
        val lastPart = parts.last()
        if (lastPart !in ENCLITIC_PRONOUNS) return null

        // Build result: first part is the verb, rest are pronoun(s)
        val result = mutableListOf<ContractionPart>()
        result.add(ContractionPart(form = parts.first(), role = "VERB"))

        for (i in 1 until parts.size) {
            val pronoun = parts[i]
            if (pronoun in ENCLITIC_PRONOUNS) {
                result.add(ContractionPart(form = pronoun, role = "PRON"))
            } else {
                // Not a pure enclitic pattern (e.g., "bem-aventurado")
                return null
            }
        }

        return result
    }

    /**
     * Tokenizes all verses for a given bible version.
     * Idempotent: skips verses that already have tokens.
     * When `scope` is provided, limits processing to the scoped book/chapter/verse.
     */
    suspend fun tokenizeVersion(
        versionCode: String,
        tracker: IngestionPhaseTracker? = null,
        phaseName: String? = null,
        scope: IngestionScope? = null
    ): Int {
        val version = versionRepository.findByCode(versionCode)
            ?: throw IllegalArgumentException("Version not found: $versionCode")

        val books = bookRepository.findAll()
            .filter { scope?.bookName == null || it.name == scope.bookName }
        var totalTokenized = 0
        var totalVerses = 0

        for (book in books) {
            // Skip OT books for NT-only versions
            if (version.testamentScope == "NT" && book.testament == "OT") continue
            if (version.testamentScope == "OT" && book.testament == "NT") continue

            for (chapter in 1..book.totalChapters) {
                if (scope?.chapter != null && chapter != scope.chapter) continue
                val texts = verseRepository.getChapterTexts(version.id, book.id, chapter)
                for (verseText in texts) {
                    if (scope?.verse != null && verseText.verseNumber != scope.verse) continue
                    val verseId = verseRepository.getVerseId(book.id, chapter, verseText.verseNumber) ?: continue

                    // Skip if already tokenized (idempotent)
                    if (tokenRepository.hasTokensForVerse(verseId, version.id)) continue

                    val tokens = tokenize(verseText.text)
                    if (tokens.isNotEmpty()) {
                        tokenRepository.upsertTokens(verseId, version.id, tokens)
                        totalTokenized += tokens.size
                    }

                    totalVerses++
                    if (totalVerses % 1000 == 0) {
                        log.info("TOKENIZE $versionCode: processed $totalVerses verses, $totalTokenized tokens so far")
                        tracker?.let { phaseName?.let { p -> it.markProgress(p, totalVerses) } }
                    }
                }
            }
        }

        log.info("TOKENIZE $versionCode: completed — $totalVerses verses, $totalTokenized tokens")
        return totalTokenized
    }

    companion object {
        /**
         * Builds the complete map of Portuguese contractions.
         * Key: normalized contraction form (lowercase)
         * Value: list of decomposed parts with grammatical roles
         */
        fun buildContractionMap(): Map<String, List<ContractionPart>> {
            val map = mutableMapOf<String, List<ContractionPart>>()

            // de + artigo
            map["do"] = listOf(ContractionPart("de", "PREP"), ContractionPart("o", "ART"))
            map["da"] = listOf(ContractionPart("de", "PREP"), ContractionPart("a", "ART"))
            map["dos"] = listOf(ContractionPart("de", "PREP"), ContractionPart("os", "ART"))
            map["das"] = listOf(ContractionPart("de", "PREP"), ContractionPart("as", "ART"))

            // em + artigo
            map["no"] = listOf(ContractionPart("em", "PREP"), ContractionPart("o", "ART"))
            map["na"] = listOf(ContractionPart("em", "PREP"), ContractionPart("a", "ART"))
            map["nos"] = listOf(ContractionPart("em", "PREP"), ContractionPart("os", "ART"))
            map["nas"] = listOf(ContractionPart("em", "PREP"), ContractionPart("as", "ART"))

            // a + artigo (crase / contração)
            map["ao"] = listOf(ContractionPart("a", "PREP"), ContractionPart("o", "ART"))
            map["\u00e0"] = listOf(ContractionPart("a", "PREP"), ContractionPart("a", "ART")) // à
            map["aos"] = listOf(ContractionPart("a", "PREP"), ContractionPart("os", "ART"))
            map["\u00e0s"] = listOf(ContractionPart("a", "PREP"), ContractionPart("as", "ART")) // às

            // por + artigo
            map["pelo"] = listOf(ContractionPart("por", "PREP"), ContractionPart("o", "ART"))
            map["pela"] = listOf(ContractionPart("por", "PREP"), ContractionPart("a", "ART"))
            map["pelos"] = listOf(ContractionPart("por", "PREP"), ContractionPart("os", "ART"))
            map["pelas"] = listOf(ContractionPart("por", "PREP"), ContractionPart("as", "ART"))

            // em + pronome pessoal
            map["nele"] = listOf(ContractionPart("em", "PREP"), ContractionPart("ele", "PRON"))
            map["nela"] = listOf(ContractionPart("em", "PREP"), ContractionPart("ela", "PRON"))
            map["neles"] = listOf(ContractionPart("em", "PREP"), ContractionPart("eles", "PRON"))
            map["nelas"] = listOf(ContractionPart("em", "PREP"), ContractionPart("elas", "PRON"))

            // de + pronome pessoal
            map["dele"] = listOf(ContractionPart("de", "PREP"), ContractionPart("ele", "PRON"))
            map["dela"] = listOf(ContractionPart("de", "PREP"), ContractionPart("ela", "PRON"))
            map["deles"] = listOf(ContractionPart("de", "PREP"), ContractionPart("eles", "PRON"))
            map["delas"] = listOf(ContractionPart("de", "PREP"), ContractionPart("elas", "PRON"))

            // em + indefinido
            map["num"] = listOf(ContractionPart("em", "PREP"), ContractionPart("um", "ART"))
            map["numa"] = listOf(ContractionPart("em", "PREP"), ContractionPart("uma", "ART"))
            map["nuns"] = listOf(ContractionPart("em", "PREP"), ContractionPart("uns", "ART"))
            map["numas"] = listOf(ContractionPart("em", "PREP"), ContractionPart("umas", "ART"))

            // de + indefinido
            map["dum"] = listOf(ContractionPart("de", "PREP"), ContractionPart("um", "ART"))
            map["duma"] = listOf(ContractionPart("de", "PREP"), ContractionPart("uma", "ART"))

            // de + advérbio/demonstrativo locativo
            map["daqui"] = listOf(ContractionPart("de", "PREP"), ContractionPart("aqui", "ADV"))
            map["dali"] = listOf(ContractionPart("de", "PREP"), ContractionPart("ali", "ADV"))
            map["da\u00ed"] = listOf(ContractionPart("de", "PREP"), ContractionPart("a\u00ed", "ADV")) // daí

            // em + demonstrativo
            map["nisto"] = listOf(ContractionPart("em", "PREP"), ContractionPart("isto", "DEM"))
            map["nisso"] = listOf(ContractionPart("em", "PREP"), ContractionPart("isso", "DEM"))
            map["naquilo"] = listOf(ContractionPart("em", "PREP"), ContractionPart("aquilo", "DEM"))
            map["neste"] = listOf(ContractionPart("em", "PREP"), ContractionPart("este", "DEM"))
            map["nesta"] = listOf(ContractionPart("em", "PREP"), ContractionPart("esta", "DEM"))
            map["nestes"] = listOf(ContractionPart("em", "PREP"), ContractionPart("estes", "DEM"))
            map["nestas"] = listOf(ContractionPart("em", "PREP"), ContractionPart("estas", "DEM"))
            map["nesse"] = listOf(ContractionPart("em", "PREP"), ContractionPart("esse", "DEM"))
            map["nessa"] = listOf(ContractionPart("em", "PREP"), ContractionPart("essa", "DEM"))
            map["nesses"] = listOf(ContractionPart("em", "PREP"), ContractionPart("esses", "DEM"))
            map["nessas"] = listOf(ContractionPart("em", "PREP"), ContractionPart("essas", "DEM"))
            map["naquele"] = listOf(ContractionPart("em", "PREP"), ContractionPart("aquele", "DEM"))
            map["naquela"] = listOf(ContractionPart("em", "PREP"), ContractionPart("aquela", "DEM"))
            map["naqueles"] = listOf(ContractionPart("em", "PREP"), ContractionPart("aqueles", "DEM"))
            map["naquelas"] = listOf(ContractionPart("em", "PREP"), ContractionPart("aquelas", "DEM"))

            // de + demonstrativo
            map["disto"] = listOf(ContractionPart("de", "PREP"), ContractionPart("isto", "DEM"))
            map["disso"] = listOf(ContractionPart("de", "PREP"), ContractionPart("isso", "DEM"))
            map["daquilo"] = listOf(ContractionPart("de", "PREP"), ContractionPart("aquilo", "DEM"))
            map["deste"] = listOf(ContractionPart("de", "PREP"), ContractionPart("este", "DEM"))
            map["desta"] = listOf(ContractionPart("de", "PREP"), ContractionPart("esta", "DEM"))
            map["destes"] = listOf(ContractionPart("de", "PREP"), ContractionPart("estes", "DEM"))
            map["destas"] = listOf(ContractionPart("de", "PREP"), ContractionPart("estas", "DEM"))
            map["desse"] = listOf(ContractionPart("de", "PREP"), ContractionPart("esse", "DEM"))
            map["dessa"] = listOf(ContractionPart("de", "PREP"), ContractionPart("essa", "DEM"))
            map["desses"] = listOf(ContractionPart("de", "PREP"), ContractionPart("esses", "DEM"))
            map["dessas"] = listOf(ContractionPart("de", "PREP"), ContractionPart("essas", "DEM"))
            map["daquele"] = listOf(ContractionPart("de", "PREP"), ContractionPart("aquele", "DEM"))
            map["daquela"] = listOf(ContractionPart("de", "PREP"), ContractionPart("aquela", "DEM"))
            map["daqueles"] = listOf(ContractionPart("de", "PREP"), ContractionPart("aqueles", "DEM"))
            map["daquelas"] = listOf(ContractionPart("de", "PREP"), ContractionPart("aquelas", "DEM"))

            // a + demonstrativo (crase)
            map["\u00e0quele"] = listOf(ContractionPart("a", "PREP"), ContractionPart("aquele", "DEM")) // àquele
            map["\u00e0quela"] = listOf(ContractionPart("a", "PREP"), ContractionPart("aquela", "DEM")) // àquela
            map["\u00e0queles"] = listOf(ContractionPart("a", "PREP"), ContractionPart("aqueles", "DEM")) // àqueles
            map["\u00e0quelas"] = listOf(ContractionPart("a", "PREP"), ContractionPart("aquelas", "DEM")) // àquelas
            map["\u00e0quilo"] = listOf(ContractionPart("a", "PREP"), ContractionPart("aquilo", "DEM")) // àquilo

            return map
        }
    }
}
