package com.ntcoverage.service

import com.ntcoverage.repository.BibleBookRepository
import com.ntcoverage.repository.BibleVersionRepository
import com.ntcoverage.repository.BibleVerseRepository
import com.ntcoverage.repository.InterlinearRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class WordAlignmentService(
    private val interlinearRepository: InterlinearRepository,
    private val verseRepository: BibleVerseRepository,
    private val versionRepository: BibleVersionRepository,
    private val bookRepository: BibleBookRepository
) {
    private val log = LoggerFactory.getLogger(WordAlignmentService::class.java)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Article words across supported languages (used by oblique article adjacency check) */
    private val ARTICLE_WORDS = setOf(
        "the", "a", "an",           // English
        "o", "a", "os", "as",       // Portuguese
        "el", "la", "los", "las"    // Spanish
    )

    @Serializable
    data class AlignmentResponse(val verses: List<VerseAlignment>)

    @Serializable
    data class VerseAlignment(val v: Int, val a: List<WordAlignment>)

    @Serializable
    data class WordAlignment(val g: Int, val k: List<Int>? = null, val t: String? = null, val c: Int = 0)

    data class Candidate(val idx: Int, val word: String, val sim: Int, val dist: Double = 1.0)

    data class ExpressionMatch(val indices: List<Int>, val confidence: Int)

    sealed class AlignChapterResult {
        data class Aligned(val wordsAligned: Int) : AlignChapterResult()
        data object SkippedNoInterlinear : AlignChapterResult()
        data object SkippedNoVersionText : AlignChapterResult()
        data class AlreadyAligned(val versesSkipped: Int) : AlignChapterResult()
    }

    /** Checks if morphology indicates a Greek article (e.g., "T-NSM", "T-ASM") */
    internal fun isArticle(morphology: String?): Boolean {
        return morphology != null && morphology.startsWith("T-")
    }

    /** Checks if morphology indicates a nominative article (T-N*) — subject marker */
    internal fun isNominativeArticle(morphology: String?): Boolean {
        return morphology != null && morphology.length >= 3 && morphology.startsWith("T-") && morphology[2] == 'N'
    }

    /** Checks if morphology indicates a non-nominative article (accusative, genitive, dative) */
    internal fun isObliqueArticle(morphology: String?): Boolean {
        return isArticle(morphology) && !isNominativeArticle(morphology)
    }

    internal fun computeRelativePosition(pos: Int, total: Int): Double {
        if (total <= 1) return 0.0
        return pos.toDouble() / (total - 1).toDouble()
    }

    internal fun computePositionalDistance(greekRelPos: Double, candidateIdx: Int, totalTargetWords: Int): Double {
        val candidateRelPos = computeRelativePosition(candidateIdx, totalTargetWords)
        return abs(greekRelPos - candidateRelPos)
    }

    /**
     * Detects multi-word expressions from glosses (e.g., "In [the]" → 2 words)
     * and pre-matches them to consecutive target tokens by similarity + position.
     * Returns a map of greekWordPosition → ExpressionMatch(indices, confidence).
     */
    internal fun detectExpressions(
        words: List<com.ntcoverage.model.InterlinearWordDTO>,
        targetWords: List<String>
    ): Pair<Map<Int, ExpressionMatch>, Set<Int>> {
        val expressionMap = mutableMapOf<Int, ExpressionMatch>()
        val consumedIndices = mutableSetOf<Int>()
        val totalGreek = words.size
        val totalTarget = targetWords.size

        for (w in words) {
            val rawGloss = w.englishGloss ?: continue
            // Clean brackets/parens but keep the words: "In [the]" → "In the", "<the>" → "the"
            val cleanGloss = rawGloss.replace(Regex("[\\[\\]<>(){}]"), "").trim()
            val glossParts = cleanGloss.split(Regex("\\s+")).filter { it.isNotBlank() }

            if (glossParts.size < 2) continue // Not a multi-word expression

            val greekRelPos = computeRelativePosition(w.wordPosition, totalGreek)

            // Find all consecutive matches in targetWords
            data class ExprCandidate(val startIdx: Int, val indices: List<Int>, val dist: Double)
            val candidates = mutableListOf<ExprCandidate>()

            for (i in 0..totalTarget - glossParts.size) {
                // Check if indices are already consumed
                val candidateIndices = (i until i + glossParts.size).toList()
                if (candidateIndices.any { it in consumedIndices }) continue

                // Check if all parts match consecutively
                var allMatch = true
                for (j in glossParts.indices) {
                    val partNorm = normalizeForComparison(glossParts[j])
                    val targetNorm = normalizeForComparison(targetWords[i + j])
                    if (partNorm != targetNorm) {
                        allMatch = false
                        break
                    }
                }

                if (allMatch) {
                    val dist = computePositionalDistance(greekRelPos, i, totalTarget)
                    candidates.add(ExprCandidate(i, candidateIndices, dist))
                }
            }

            if (candidates.isEmpty()) continue

            // Pick the closest positionally
            val best = candidates.minByOrNull { it.dist }!!
            val confidence = when {
                best.dist < 0.10 -> 97
                best.dist < 0.25 -> 93
                best.dist < 0.50 -> 88
                else -> 80
            }

            expressionMap[w.wordPosition] = ExpressionMatch(best.indices, confidence)
            consumedIndices.addAll(best.indices)
        }

        return Pair(expressionMap, consumedIndices)
    }

    /**
     * Cross-language expression detection using translated glosses (portugueseGloss/spanishGloss).
     * Also builds single-word candidates for the LLM prompt.
     */
    internal fun detectExpressionsWithTranslatedGlosses(
        words: List<com.ntcoverage.model.InterlinearWordDTO>,
        targetWords: List<String>,
        language: String
    ): Pair<Map<Int, ExpressionMatch>, Set<Int>> {
        val expressionMap = mutableMapOf<Int, ExpressionMatch>()
        val consumedIndices = mutableSetOf<Int>()
        val totalGreek = words.size
        val totalTarget = targetWords.size

        for (w in words) {
            val translatedGloss = when (language) {
                "pt" -> w.portugueseGloss
                "es" -> w.spanishGloss
                else -> null
            } ?: continue

            val cleanGloss = translatedGloss.replace(Regex("[\\[\\]<>(){}]"), "").trim()
            val glossParts = cleanGloss.split(Regex("\\s+")).filter { it.isNotBlank() }

            if (glossParts.size < 2) continue // Only detect multi-word expressions

            val greekRelPos = computeRelativePosition(w.wordPosition, totalGreek)

            data class ExprCandidate(val startIdx: Int, val indices: List<Int>, val dist: Double)
            val candidates = mutableListOf<ExprCandidate>()

            for (i in 0..totalTarget - glossParts.size) {
                val candidateIndices = (i until i + glossParts.size).toList()
                if (candidateIndices.any { it in consumedIndices }) continue

                var allMatch = true
                for (j in glossParts.indices) {
                    val partNorm = normalizeForComparison(glossParts[j])
                    val targetNorm = normalizeForComparison(targetWords[i + j])
                    if (partNorm != targetNorm) {
                        allMatch = false
                        break
                    }
                }

                if (allMatch) {
                    val dist = computePositionalDistance(greekRelPos, i, totalTarget)
                    candidates.add(ExprCandidate(i, candidateIndices, dist))
                }
            }

            if (candidates.isEmpty()) continue

            val best = candidates.minByOrNull { it.dist }!!
            val confidence = when {
                best.dist < 0.10 -> 97
                best.dist < 0.25 -> 93
                best.dist < 0.50 -> 88
                else -> 80
            }

            expressionMap[w.wordPosition] = ExpressionMatch(best.indices, confidence)
            consumedIndices.addAll(best.indices)
        }

        return Pair(expressionMap, consumedIndices)
    }

    internal fun buildSystemPrompt(language: String): String {
        val langName = when (language) {
            "pt" -> "Portuguese"
            "es" -> "Spanish"
            else -> "English"
        }
        val isEnglish = language == "en"
        val candidateRules = if (isEnglish) """
- USE THE "candidates" FIELD as your primary guide. Pick the candidate with the highest "sim" score.
- POSITIONAL TIEBREAKER: When multiple candidates have identical "sim" scores, prefer the one with the smallest "dist" (positional distance). The "relPos" field shows where the Greek word is in the verse (0.0=start, 1.0=end), and "dist" shows how far each candidate is from that position.
- When multiple Greek words share the same gloss (e.g., 3x "Word", 4x "the"), assign each to a DIFFERENT translation occurrence. Each candidate index should be used AT MOST ONCE across all alignments.
- If a Greek word has "preAligned" indices, use those directly — they are pre-resolved expressions with high confidence."""
        else """
- This is a CROSS-LANGUAGE alignment: glosses are in English but translation words are in $langName.
- When a "localGloss" field is provided, it is the pre-translated $langName equivalent of the English gloss. USE IT for direct matching — it is more reliable than semantic guessing.
- If "candidates" are provided alongside "localGloss", prefer the candidate with highest "sim" and smallest "dist".
- Use your semantic knowledge to match English glosses to their $langName equivalents.
  Common mappings: "And"→"E", "day"→"dia", "in"→"em", "was"→"estava"/"era", "mother"→"mãe", "the"→"o"/"a"/"os"/"as", "of"→"de"/"do"/"da", "God"→"Deus", "Word"→"Verbo"/"Palavra"
- Portuguese contractions: "no"="em+o", "do"="de+o", "ao"="a+o", "pelo"="por+o", "na"="em+a", "da"="de+a", "num"="em+um". A single contraction may correspond to 2 Greek words (preposition + article).
- POSITIONAL TIEBREAKER: Each Greek word has a "relPos" field (0.0=start, 1.0=end of verse). When unsure between multiple possible matches, prefer the $langName word closest to the Greek word's relative position.
- When multiple Greek words share the same gloss, assign each to a DIFFERENT $langName occurrence.
- If a Greek word has "preAligned" indices, use those directly — they are pre-resolved expressions with high confidence."""

        return """You are a Greek-$langName word alignment expert for the New Testament.
Given Greek words with their glosses and the $langName translation text, align each Greek word to the best matching translation word(s).

CRITICAL RULES:
$candidateRules
- ALWAYS find a match if there is ANY semantic relationship (synonyms, related words, same concept).
- Only set k to null when the Greek word genuinely has NO equivalent in the translation.
- MORPHOLOGY AWARENESS: Each word includes a "morph" field with its grammatical code.
  - Articles (morph starting with "T-") in oblique cases (accusative "T-A*", genitive "T-G*", dative "T-D*") are often ABSORBED by the translation (e.g., Greek "τὸν θεόν" → English "God", not "the God"). When an oblique article has no clear match, set k to null with confidence 60-70.
  - Nominative articles ("T-N*") usually DO appear in translation as "the".
- For each alignment, provide a confidence score "c" (0-100):
  - 95-100: Exact or near-exact match
  - 80-94: Clear semantic match / synonym
  - 60-79: Reasonable match with some interpretation
  - Below 60: Weak/uncertain match
- Almost every content word (nouns, verbs, adjectives, adverbs, prepositions) MUST have a match.
- Return ONLY valid JSON, no commentary or explanation."""
    }

    suspend fun alignChapter(bookName: String, chapter: Int, versionCode: String): AlignChapterResult {
        val book = bookRepository.findByNameCaseInsensitive(bookName)
            ?: throw IllegalArgumentException("Book not found: $bookName")

        val version = versionRepository.findByCode(versionCode)
            ?: throw IllegalArgumentException("Version not found: $versionCode")

        val interlinearByVerse = interlinearRepository.getWordsForChapter(book.id, chapter)
        if (interlinearByVerse.isEmpty()) {
            log.info("WORD_ALIGN: no interlinear data for $bookName $chapter, skipping")
            return AlignChapterResult.SkippedNoInterlinear
        }

        val kjvTexts = verseRepository.getChapterTexts(version.id, book.id, chapter)
        if (kjvTexts.isEmpty()) {
            log.info("WORD_ALIGN: no $versionCode text for $bookName $chapter, skipping")
            return AlignChapterResult.SkippedNoVersionText
        }

        val kjvTextByVerse = kjvTexts.associate { it.verseNumber to it.text }

        val language = version.language

        var totalAligned = 0
        var versesSkipped = 0
        for (verseNumber in interlinearByVerse.keys.sorted()) {
            val versionText = kjvTextByVerse[verseNumber] ?: continue
            val verseId = verseRepository.getVerseId(book.id, chapter, verseNumber) ?: continue

            if (interlinearRepository.hasAlignmentsForVerse(verseId, versionCode)) {
                versesSkipped++
                continue
            }

            val words = interlinearByVerse[verseNumber] ?: continue
            val count = alignVerse(verseId, verseNumber, words, versionText, versionCode, language, bookName, chapter)
            totalAligned += count

            kotlinx.coroutines.delay(System.getenv("LLM_ALIGNMENT_DELAY_MS")?.toLongOrNull() ?: 50L)
        }

        return if (totalAligned > 0) {
            log.info("WORD_ALIGN: aligned $totalAligned words in $bookName $chapter ($versionCode)")
            AlignChapterResult.Aligned(totalAligned)
        } else {
            log.info("WORD_ALIGN: $bookName $chapter already aligned ($versesSkipped verses)")
            AlignChapterResult.AlreadyAligned(versesSkipped)
        }
    }

    internal fun normalizeForComparison(s: String): String {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
            .replace(Regex("[,.:;!?\"'()\\[\\]<>{}]"), "")
            .trim()
    }

    // ── Layer 4: 4-Level Local Alignment Algorithm ──

    /**
     * Result of local (non-LLM) alignment for a single verse.
     * Contains resolved alignments (Levels 1-3) and unresolved Greek positions (for Level 4 / LLM).
     */
    data class LocalAlignmentResult(
        /** Alignments resolved by Levels 1-3 (deterministic) */
        val resolved: List<ResolvedAlignment>,
        /** Greek word positions that could not be resolved locally — need LLM (Level 4) */
        val unresolvedPositions: List<Int>
    )

    data class ResolvedAlignment(
        val greekPosition: Int,
        val tokenPositions: List<Int>,
        val alignedText: String,
        val method: String,        // exact, lemma, contraction, enclitic
        val confidence: Int
    )

    /**
     * Runs Levels 1-3 of the 4-level alignment algorithm.
     *
     * Level 1: DETERMINISTIC — normalize(gloss_pt) == token (100% correct by definition)
     * Level 2: LEMMA — token.lemma matches gloss lemma (morphological variation)
     * Level 3: DECOMPOSITION — contractions (nele→em+ele) and enclitics (disse-lhe→disse+lhe)
     *
     * Words not resolved by Levels 1-3 are returned in unresolvedPositions for Level 4 (LLM).
     *
     * @param greekWords The interlinear Greek words for this verse
     * @param tokens The tokenized target text (from bible_verse_tokens)
     * @param language The target language ("pt", "en", "es")
     */
    fun alignVerseLocal(
        greekWords: List<com.ntcoverage.model.InterlinearWordDTO>,
        tokens: List<com.ntcoverage.model.BibleVerseTokenDTO>,
        language: String
    ): LocalAlignmentResult {
        val resolved = mutableListOf<ResolvedAlignment>()
        val consumedTokenPositions = mutableSetOf<Int>()
        val resolvedGreekPositions = mutableSetOf<Int>()

        // Get gloss based on language
        fun getGloss(w: com.ntcoverage.model.InterlinearWordDTO): String? = when (language) {
            "pt" -> w.portugueseGloss
            "es" -> w.spanishGloss
            else -> w.englishGloss
        }

        // ── LEVEL 1: EXACT MATCH ──
        // normalize(gloss_pt) == token — 100% certain by definition
        for (w in greekWords) {
            val gloss = getGloss(w) ?: continue
            // Handle multi-value glosses separated by "/" (e.g., "crendo/crê")
            val glossVariants = gloss.split("/").map { normalizeForComparison(it) }.filter { it.isNotBlank() }

            for (variant in glossVariants) {
                // Find first matching token that isn't consumed
                val matchIdx = tokens.indexOfFirst { t ->
                    t.position !in consumedTokenPositions && normalizeForComparison(t.token) == variant
                }
                if (matchIdx >= 0) {
                    val token = tokens[matchIdx]
                    resolved.add(ResolvedAlignment(
                        greekPosition = w.wordPosition,
                        tokenPositions = listOf(token.position),
                        alignedText = token.tokenRaw,
                        method = "exact",
                        confidence = 95
                    ))
                    consumedTokenPositions.add(token.position)
                    resolvedGreekPositions.add(w.wordPosition)
                    break
                }
            }
        }

        // ── LEVEL 2: LEMMA MATCH ──
        // token.lemma == gloss lemma — handles morphological variation
        for (w in greekWords) {
            if (w.wordPosition in resolvedGreekPositions) continue
            val gloss = getGloss(w) ?: continue
            val glossNorms = gloss.split("/").map { normalizeForComparison(it) }.filter { it.isNotBlank() }

            for (token in tokens) {
                if (token.position in consumedTokenPositions) continue
                val tokenLemma = token.lemma ?: continue

                // Compare lemmas: the token has a lemma from LLM batch; check if gloss or gloss-lemma matches
                for (variant in glossNorms) {
                    if (normalizeForComparison(tokenLemma) == variant ||
                        normalizeForComparison(token.token) == variant) {
                        resolved.add(ResolvedAlignment(
                            greekPosition = w.wordPosition,
                            tokenPositions = listOf(token.position),
                            alignedText = token.tokenRaw,
                            method = "lemma",
                            confidence = 90
                        ))
                        consumedTokenPositions.add(token.position)
                        resolvedGreekPositions.add(w.wordPosition)
                        break
                    }
                }
                if (w.wordPosition in resolvedGreekPositions) break
            }
        }

        // ── LEVEL 3: DECOMPOSITION (contractions + enclitics) ──
        // Portuguese contractions and enclitics map to 2+ Greek words
        for (token in tokens) {
            if (token.position in consumedTokenPositions) continue

            if (token.isContraction && token.contractionParts != null) {
                // Parse contraction parts: [{"form":"em","role":"PREP"},{"form":"ele","role":"PRON"}]
                val parts = parseContractionParts(token.contractionParts)
                if (parts.isEmpty()) continue

                // Try to match each part with an unresolved Greek word
                val matchedGreek = mutableListOf<Int>()
                for (part in parts) {
                    val partNorm = normalizeForComparison(part.form)
                    // Find Greek word whose gloss matches this part
                    val greekMatch = greekWords.firstOrNull { gw ->
                        gw.wordPosition !in resolvedGreekPositions &&
                        getGloss(gw)?.split("/")?.any { normalizeForComparison(it) == partNorm } == true
                    }
                    if (greekMatch != null) {
                        matchedGreek.add(greekMatch.wordPosition)
                    }
                }

                // If at least one part matched, mark all matched Greeks as resolved
                if (matchedGreek.isNotEmpty()) {
                    for (gPos in matchedGreek) {
                        resolved.add(ResolvedAlignment(
                            greekPosition = gPos,
                            tokenPositions = listOf(token.position),
                            alignedText = token.tokenRaw,
                            method = "contraction",
                            confidence = 90
                        ))
                        resolvedGreekPositions.add(gPos)
                    }
                    consumedTokenPositions.add(token.position)
                }
            }

            if (token.isEnclitic && token.encliticParts != null) {
                // Parse enclitic parts: [{"form":"disse","role":"VERB"},{"form":"lhe","role":"PRON"}]
                val parts = parseContractionParts(token.encliticParts)
                if (parts.isEmpty()) continue

                val matchedGreek = mutableListOf<Int>()
                for (part in parts) {
                    val partNorm = normalizeForComparison(part.form)
                    val greekMatch = greekWords.firstOrNull { gw ->
                        gw.wordPosition !in resolvedGreekPositions &&
                        getGloss(gw)?.split("/")?.any { normalizeForComparison(it) == partNorm } == true
                    }
                    if (greekMatch != null) {
                        matchedGreek.add(greekMatch.wordPosition)
                    }
                }

                if (matchedGreek.isNotEmpty()) {
                    for (gPos in matchedGreek) {
                        resolved.add(ResolvedAlignment(
                            greekPosition = gPos,
                            tokenPositions = listOf(token.position),
                            alignedText = token.tokenRaw,
                            method = "enclitic",
                            confidence = 90
                        ))
                        resolvedGreekPositions.add(gPos)
                    }
                    consumedTokenPositions.add(token.position)
                }
            }
        }

        // Unresolved positions → need Level 4 (LLM)
        val unresolvedPositions = greekWords
            .map { it.wordPosition }
            .filter { it !in resolvedGreekPositions }

        return LocalAlignmentResult(resolved, unresolvedPositions)
    }

    /** Parse JSON contraction/enclitic parts safely */
    private fun parseContractionParts(jsonStr: String): List<BibleTokenizationService.ContractionPart> {
        return try {
            json.decodeFromString<List<BibleTokenizationService.ContractionPart>>(jsonStr)
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun computeSimilarity(gloss: String, kjvWord: String): Int {
        val g = normalizeForComparison(gloss)
        val w = normalizeForComparison(kjvWord)
        if (g.isEmpty() || w.isEmpty()) return 0

        // Exact match
        if (g == w) return 100

        // One contains the other
        if (g.contains(w) || w.contains(g)) return 85

        // Starts with
        if (g.startsWith(w) || w.startsWith(g)) return 75

        // Levenshtein-based similarity
        val maxLen = max(g.length, w.length)
        if (maxLen == 0) return 0
        val dist = levenshteinDistance(g, w)
        val ratio = ((1.0 - dist.toDouble() / maxLen) * 70).toInt()
        return max(0, ratio)
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
            }
        }
        return dp[m][n]
    }

    internal fun computeCandidates(
        gloss: String, kjvWords: List<String>,
        greekRelPos: Double = 0.5, consumedIndices: Set<Int> = emptySet()
    ): List<Candidate> {
        val totalTarget = kjvWords.size
        return kjvWords.mapIndexed { idx, word ->
            if (idx in consumedIndices) null
            else {
                val sim = computeSimilarity(gloss, word)
                val dist = computePositionalDistance(greekRelPos, idx, totalTarget)
                Candidate(idx, word, sim, dist)
            }
        }
            .filterNotNull()
            .filter { it.sim >= 50 }
            .sortedWith(compareByDescending<Candidate> { it.sim }.thenBy { it.dist })
            .take(5)
    }

    private suspend fun alignVerse(
        verseId: Int, verseNumber: Int,
        words: List<com.ntcoverage.model.InterlinearWordDTO>,
        versionText: String, versionCode: String, language: String,
        bookName: String, chapter: Int
    ): Int {
        val targetWords = splitKjvText(versionText)
        val totalGreek = words.size
        val isEnglishVersion = language == "en"

        // Step 1: Detect multi-word expressions and mark consumed indices
        val (expressionMap, consumedIndices) = if (isEnglishVersion) {
            detectExpressions(words, targetWords)
        } else {
            detectExpressionsWithTranslatedGlosses(words, targetWords, language)
        }

        // Step 2: Build prompt with relPos, dist, and pre-aligned expressions
        val prompt = buildString {
            append("Greek: [")
            append(words.joinToString(",") { w ->
                val gloss = w.englishGloss ?: ""
                val greekRelPos = computeRelativePosition(w.wordPosition, totalGreek)
                val relPosStr = "%.2f".format(greekRelPos)
                val morphField = w.morphology?.let { ""","morph":"${escapeJson(it)}"""" } ?: ""

                // Check if this word has a pre-resolved expression
                val expr = expressionMap[w.wordPosition]
                if (expr != null) {
                    val preAlignedJson = expr.indices.joinToString(",")
                    val alignedText = expr.indices.mapNotNull { targetWords.getOrNull(it) }.joinToString(" ")
                    """{"pos":${w.wordPosition},"word":"${escapeJson(w.originalWord)}","gloss":"${escapeJson(gloss)}"$morphField,"relPos":$relPosStr,"preAligned":[$preAlignedJson],"preText":"${escapeJson(alignedText)}"}"""
                } else if (isEnglishVersion) {
                    val candidates = computeCandidates(gloss, targetWords, greekRelPos, consumedIndices)
                    val candidatesJson = candidates.joinToString(",") { c ->
                        val distStr = "%.2f".format(c.dist)
                        """{"idx":${c.idx},"word":"${escapeJson(c.word)}","sim":${c.sim},"dist":$distStr}"""
                    }
                    """{"pos":${w.wordPosition},"word":"${escapeJson(w.originalWord)}","gloss":"${escapeJson(gloss)}"$morphField,"relPos":$relPosStr,"candidates":[$candidatesJson]}"""
                } else {
                    // Cross-language: use translated gloss for candidates when available
                    val localGloss = when (language) {
                        "pt" -> w.portugueseGloss
                        "es" -> w.spanishGloss
                        else -> null
                    }
                    val localGlossField = localGloss?.let { ""","localGloss":"${escapeJson(it)}"""" } ?: ""
                    val candidatesBlock = if (localGloss != null) {
                        val candidates = computeCandidates(localGloss, targetWords, greekRelPos, consumedIndices)
                        if (candidates.isNotEmpty()) {
                            val candidatesJson = candidates.joinToString(",") { c ->
                                val distStr = "%.2f".format(c.dist)
                                """{"idx":${c.idx},"word":"${escapeJson(c.word)}","sim":${c.sim},"dist":$distStr}"""
                            }
                            ""","candidates":[$candidatesJson]"""
                        } else ""
                    } else ""
                    """{"pos":${w.wordPosition},"word":"${escapeJson(w.originalWord)}","gloss":"${escapeJson(gloss)}"$localGlossField$morphField,"relPos":$relPosStr$candidatesBlock}"""
                }
            })
            appendLine("]")
            append("Translation words: [")
            append(targetWords.mapIndexed { idx, word -> "\"$idx:${escapeJson(word)}\"" }.joinToString(","))
            appendLine("]")
            appendLine()
            appendLine("""Return: {"a":[{"g":<greekPos>,"k":[<translationIdx>,...],"t":"<translationText>","c":<confidence0to100>},...]}""")
        }

        // LLM alignment migrated to queue — direct call removed
        log.debug("WORD_ALIGN: skipping $bookName $chapter:$verseNumber — direct LLM removed, use queue")
        return 0
    }

    internal fun validateAndFixAlignments(
        alignments: List<WordAlignment>,
        greekWords: List<com.ntcoverage.model.InterlinearWordDTO>,
        kjvWords: List<String>
    ): List<WordAlignment> {
        val totalGreek = greekWords.size
        val totalTarget = kjvWords.size
        val greekByPos = greekWords.associateBy { it.wordPosition.toInt() }

        // ── Phase 1: Resolve conflicts using similarity + positional distance ──
        val indexUsage = mutableMapOf<Int, MutableList<WordAlignment>>()
        for (wa in alignments) {
            wa.k?.forEach { idx ->
                indexUsage.getOrPut(idx) { mutableListOf() }.add(wa)
            }
        }

        val conflicts = indexUsage.filter { it.value.size > 1 }
        val result = alignments.toMutableList()

        for ((idx, claimants) in conflicts) {
            val kjvWord = kjvWords.getOrNull(idx) ?: continue

            // Pick the claimant with best similarity; break ties by:
            // 1. Nominative articles (T-N*) get priority over oblique articles (T-A*, T-G*, T-D*)
            // 2. Then positional proximity
            val bestClaimant = claimants.minByOrNull { wa ->
                val greek = greekByPos[wa.g]
                val gloss = greek?.englishGloss ?: ""
                val sim = computeSimilarity(gloss, kjvWord)
                val greekRelPos = computeRelativePosition(wa.g, totalGreek)
                val dist = computePositionalDistance(greekRelPos, idx, totalTarget)
                // Morphology priority: oblique articles get penalty (+500) so nominatives win
                val morphPenalty = if (isObliqueArticle(greek?.morphology)) 500.0 else 0.0
                // Lower score = better: negate similarity, add morph penalty, then dist
                -sim.toDouble() * 1000 + morphPenalty + dist
            }

            // Remove this index from all other claimants
            for (wa in claimants) {
                if (wa != bestClaimant) {
                    val waIdx = result.indexOfFirst { it.g == wa.g }
                    if (waIdx >= 0) {
                        val oldK = result[waIdx].k?.toMutableList() ?: continue
                        oldK.remove(idx)
                        result[waIdx] = result[waIdx].copy(
                            k = oldK.ifEmpty { null },
                            t = if (oldK.isEmpty()) null else result[waIdx].t,
                            c = if (oldK.isEmpty()) 0 else result[waIdx].c
                        )
                    }
                }
            }
        }

        // ── Phase 2: Reassign orphan Greek words to free target indices ──
        // Oblique articles use morphological adjacency; others use standard reassignment
        val usedIndices = mutableSetOf<Int>()
        for (wa in result) {
            wa.k?.forEach { usedIndices.add(it) }
        }

        for (i in result.indices) {
            val wa = result[i]
            if (!wa.k.isNullOrEmpty()) continue // already aligned

            val greek = greekByPos[wa.g] ?: continue
            val gloss = greek.englishGloss ?: continue
            val greekRelPos = computeRelativePosition(wa.g, totalGreek)

            // Special handling for oblique articles (accusative, genitive, dative):
            // Check if the NEXT Greek word (the noun this article modifies) is aligned,
            // and look for "the" immediately before that noun's KJV index
            if (isObliqueArticle(greek.morphology)) {
                val nextGreek = greekWords.find { it.wordPosition == greek.wordPosition + 1 }
                val nextAlignment = if (nextGreek != null) result.find { it.g == nextGreek.wordPosition } else null

                if (nextAlignment?.k != null && nextAlignment.k.isNotEmpty()) {
                    val nounIdx = nextAlignment.k.min()
                    // Look for "the" at nounIdx-1
                    val articleIdx = nounIdx - 1
                    if (articleIdx >= 0 && articleIdx !in usedIndices) {
                        val candidateWord = normalizeForComparison(kjvWords[articleIdx])
                        if (candidateWord in ARTICLE_WORDS) {
                            val conf = computeConfidenceWithPosition(100, computePositionalDistance(greekRelPos, articleIdx, totalTarget))
                            result[i] = wa.copy(k = listOf(articleIdx), t = kjvWords[articleIdx], c = conf)
                            usedIndices.add(articleIdx)
                            continue
                        }
                    }
                    // No "the" before the noun — this article was absorbed by the translation
                    result[i] = wa.copy(k = null, t = null, c = 65)
                    continue
                }
                // If next noun has no alignment either, fall through to standard reassignment
            }

            // Standard reassignment: find free target indices with similarity to this gloss
            val freeCandidates = kjvWords.mapIndexedNotNull { idx, word ->
                if (idx in usedIndices) return@mapIndexedNotNull null
                val sim = computeSimilarity(gloss, word)
                if (sim < 50) return@mapIndexedNotNull null
                val dist = computePositionalDistance(greekRelPos, idx, totalTarget)
                Triple(idx, sim, dist)
            }

            if (freeCandidates.isEmpty()) continue

            // Pick best: highest similarity, then closest position
            val best = freeCandidates.sortedWith(
                compareByDescending<Triple<Int, Int, Double>> { it.second }.thenBy { it.third }
            ).first()

            val conf = computeConfidenceWithPosition(best.second, best.third)
            val alignedText = kjvWords[best.first]
            result[i] = wa.copy(k = listOf(best.first), t = alignedText, c = conf)
            usedIndices.add(best.first)
        }

        // ── Phase 3: Recalculate confidence with positional factor for all alignments ──
        for (i in result.indices) {
            val wa = result[i]
            if (wa.k.isNullOrEmpty()) continue
            val greekRelPos = computeRelativePosition(wa.g, totalGreek)
            // Use the average position of all aligned indices
            val avgIdx = wa.k.average()
            val dist = computePositionalDistance(greekRelPos, avgIdx.toInt(), totalTarget)
            val positionFactor = when {
                dist < 0.10 -> 1.00
                dist < 0.25 -> 0.95
                dist < 0.50 -> 0.85
                else -> 0.70
            }
            val adjustedConf = (wa.c * positionFactor).toInt().coerceIn(0, 100)
            if (adjustedConf != wa.c) {
                result[i] = wa.copy(c = adjustedConf)
            }
        }

        return result
    }

    internal fun computeConfidenceWithPosition(similarity: Int, distance: Double): Int {
        val baseFactor = when {
            distance < 0.10 -> 1.00
            distance < 0.25 -> 0.95
            distance < 0.50 -> 0.85
            else -> 0.70
        }
        return (similarity * baseFactor).toInt().coerceIn(0, 100)
    }

    @Serializable
    data class SingleVerseResponse(val a: List<WordAlignment>)

    private fun parseVerseResponse(content: String): List<WordAlignment>? {
        val jsonStr = extractJson(content) ?: return null
        return try {
            json.decodeFromString<SingleVerseResponse>(jsonStr).a
        } catch (_: Exception) {
            try {
                val multi = json.decodeFromString<AlignmentResponse>(jsonStr)
                multi.verses.firstOrNull()?.a
            } catch (e2: Exception) {
                log.warn("WORD_ALIGN: JSON parse failed: ${e2.message}")
                null
            }
        }
    }

    private fun parseAlignmentResponse(content: String): AlignmentResponse? {
        val jsonStr = extractJson(content) ?: return null
        return try {
            json.decodeFromString<AlignmentResponse>(jsonStr)
        } catch (e: Exception) {
            log.warn("WORD_ALIGN: JSON parse failed: ${e.message}, trying fallback")
            null
        }
    }

    private fun extractJson(text: String): String? {
        val stripped = text
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        val start = stripped.indexOf('{')
        if (start < 0) return null
        var depth = 0
        for (i in start until stripped.length) {
            when (stripped[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return stripped.substring(start, i + 1)
                }
            }
        }
        return null
    }

    internal fun splitKjvText(text: String): List<String> {
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }
    }

    internal fun buildAlignmentPrompt(
        words: List<com.ntcoverage.model.InterlinearWordDTO>,
        targetWords: List<String>,
        expressionMap: Map<Int, ExpressionMatch>,
        consumedIndices: Set<Int>,
        isEnglishVersion: Boolean,
        language: String
    ): String {
        val totalGreek = words.size
        return buildString {
            append("Greek: [")
            append(words.joinToString(",") { w ->
                val gloss = w.englishGloss ?: ""
                val greekRelPos = computeRelativePosition(w.wordPosition, totalGreek)
                val relPosStr = "%.2f".format(greekRelPos)
                val morphField = w.morphology?.let { ""","morph":"${escapeJson(it)}"""" } ?: ""

                val expr = expressionMap[w.wordPosition]
                if (expr != null) {
                    val preAlignedJson = expr.indices.joinToString(",")
                    val alignedText = expr.indices.mapNotNull { targetWords.getOrNull(it) }.joinToString(" ")
                    """{"pos":${w.wordPosition},"word":"${escapeJson(w.originalWord)}","gloss":"${escapeJson(gloss)}"$morphField,"relPos":$relPosStr,"preAligned":[$preAlignedJson],"preText":"${escapeJson(alignedText)}"}"""
                } else if (isEnglishVersion) {
                    val candidates = computeCandidates(gloss, targetWords, greekRelPos, consumedIndices)
                    val candidatesJson = candidates.joinToString(",") { c ->
                        val distStr = "%.2f".format(c.dist)
                        """{"idx":${c.idx},"word":"${escapeJson(c.word)}","sim":${c.sim},"dist":$distStr}"""
                    }
                    """{"pos":${w.wordPosition},"word":"${escapeJson(w.originalWord)}","gloss":"${escapeJson(gloss)}"$morphField,"relPos":$relPosStr,"candidates":[$candidatesJson]}"""
                } else {
                    val localGloss = when (language) {
                        "pt" -> w.portugueseGloss
                        "es" -> w.spanishGloss
                        else -> null
                    }
                    val localGlossField = localGloss?.let { ""","localGloss":"${escapeJson(it)}"""" } ?: ""
                    val candidatesBlock = if (localGloss != null) {
                        val candidates = computeCandidates(localGloss, targetWords, greekRelPos, consumedIndices)
                        if (candidates.isNotEmpty()) {
                            val candidatesJson = candidates.joinToString(",") { c ->
                                val distStr = "%.2f".format(c.dist)
                                """{"idx":${c.idx},"word":"${escapeJson(c.word)}","sim":${c.sim},"dist":$distStr}"""
                            }
                            ""","candidates":[$candidatesJson]"""
                        } else ""
                    } else ""
                    """{"pos":${w.wordPosition},"word":"${escapeJson(w.originalWord)}","gloss":"${escapeJson(gloss)}"$localGlossField$morphField,"relPos":$relPosStr$candidatesBlock}"""
                }
            })
            appendLine("]")
            append("Translation words: [")
            append(targetWords.mapIndexed { idx, word -> "\"$idx:${escapeJson(word)}\"" }.joinToString(","))
            appendLine("]")
            appendLine()
            appendLine("""Return: {"a":[{"g":<greekPos>,"k":[<translationIdx>,...],"t":"<translationText>","c":<confidence0to100>},...]}""")
        }
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }
}
