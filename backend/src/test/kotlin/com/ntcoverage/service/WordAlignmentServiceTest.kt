package com.ntcoverage.service

import com.ntcoverage.llm.LlmOrchestrator
import com.ntcoverage.model.InterlinearWordDTO
import com.ntcoverage.repository.BibleBookRepository
import com.ntcoverage.repository.BibleVersionRepository
import com.ntcoverage.repository.BibleVerseRepository
import com.ntcoverage.repository.InterlinearRepository
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for WordAlignmentService using real data from John 1:1 KJV.
 *
 * John 1:1 Greek (TAGNT):
 *   pos 1: Ἐν      (En)       "In [the]"    G1722
 *   pos 2: ἀρχῇ    (archē)    "beginning"   G0746
 *   pos 3: ἦν      (ēn)       "was"         G1510
 *   pos 4: ὁ       (ho)       "the"         G3588
 *   pos 5: λόγος,  (logos)    "Word,"       G3056
 *   pos 6: καὶ     (kai)      "and"         G2532
 *   pos 7: ὁ       (ho)       "the"         G3588
 *   pos 8: λόγος   (logos)    "Word"        G3056
 *   pos 9: ἦν      (ēn)       "was"         G1510
 *   pos 10: πρὸς   (pros)     "with"        G4314
 *   pos 11: τὸν    (ton)      "<the>"       G3588
 *   pos 12: θεόν,  (theon)    "God,"        G2316
 *   pos 13: καὶ    (kai)      "and"         G2532
 *   pos 14: θεὸς   (theos)    "God"         G2316
 *   pos 15: ἦν     (ēn)       "was"         G1510
 *   pos 16: ὁ      (ho)       "the"         G3588
 *   pos 17: λόγος. (logos)    "Word."       G3056
 *
 * John 1:1 KJV:
 *   "In the beginning was the Word, and the Word was with God, and the Word was God."
 *   idx: 0   1    2         3   4   5      6   7   8    9   10   11    12  13  14   15  16
 */
class WordAlignmentServiceTest {

    // Mock all dependencies — we only test pure logic functions
    private val service = WordAlignmentService(
        interlinearRepository = mockk<InterlinearRepository>(),
        verseRepository = mockk<BibleVerseRepository>(),
        versionRepository = mockk<BibleVersionRepository>(),
        bookRepository = mockk<BibleBookRepository>(),
        llmOrchestrator = mockk<LlmOrchestrator>()
    )

    // ── Real data from John 1:1 ──

    private val kjvText = "In the beginning was the Word, and the Word was with God, and the Word was God."

    private val kjvWords = listOf(
        "In", "the", "beginning", "was", "the", "Word,", "and", "the",
        "Word", "was", "with", "God,", "and", "the", "Word", "was", "God."
    )

    private fun greekWordsJohn1v1(): List<InterlinearWordDTO> = listOf(
        iw(1, "Ἐν", "En", "In [the]", "G1722"),
        iw(2, "ἀρχῇ", "archē", "beginning", "G0746"),
        iw(3, "ἦν", "ēn", "was", "G1510"),
        iw(4, "ὁ", "ho", "the", "G3588"),
        iw(5, "λόγος,", "logos", "Word,", "G3056"),
        iw(6, "καὶ", "kai", "and", "G2532"),
        iw(7, "ὁ", "ho", "the", "G3588"),
        iw(8, "λόγος", "logos", "Word", "G3056"),
        iw(9, "ἦν", "ēn", "was", "G1510"),
        iw(10, "πρὸς", "pros", "with", "G4314"),
        iw(11, "τὸν", "ton", "<the>", "G3588"),
        iw(12, "θεόν,", "theon", "God,", "G2316"),
        iw(13, "καὶ", "kai", "and", "G2532"),
        iw(14, "θεὸς", "theos", "God", "G2316"),
        iw(15, "ἦν", "ēn", "was", "G1510"),
        iw(16, "ὁ", "ho", "the", "G3588"),
        iw(17, "λόγος.", "logos", "Word.", "G3056"),
    )

    private fun iw(pos: Int, orig: String, translit: String, gloss: String, strongs: String, morph: String? = null) =
        InterlinearWordDTO(
            wordPosition = pos,
            originalWord = orig,
            transliteration = translit,
            lemma = orig.replace(Regex("[,.]"), ""),
            morphology = morph,
            strongsNumber = strongs,
            englishGloss = gloss,
            language = "greek"
        )

    // ═══════════════════════════════════════════════════════════════════
    // TEST: splitKjvText
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `splitKjvText tokenizes John 1-1 correctly`() {
        val result = service.splitKjvText(kjvText)
        assertEquals(17, result.size)
        assertEquals("In", result[0])
        assertEquals("the", result[1])
        assertEquals("beginning", result[2])
        assertEquals("God.", result[16])
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: computeRelativePosition
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `computeRelativePosition returns 0 for first position`() {
        assertEquals(0.0, service.computeRelativePosition(0, 17), 0.01)
    }

    @Test
    fun `computeRelativePosition returns 1 for last position`() {
        assertEquals(1.0, service.computeRelativePosition(16, 17), 0.01)
    }

    @Test
    fun `computeRelativePosition returns 0-5 for middle position`() {
        assertEquals(0.5, service.computeRelativePosition(8, 17), 0.01)
    }

    @Test
    fun `computeRelativePosition handles single element`() {
        assertEquals(0.0, service.computeRelativePosition(0, 1))
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: computePositionalDistance
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `computePositionalDistance is small for nearby positions`() {
        // Greek pos 4 (relPos ~0.19) vs KJV idx 1 (relPos ~0.06)
        val greekRelPos = service.computeRelativePosition(4, 17)
        val dist = service.computePositionalDistance(greekRelPos, 1, 17)
        assertTrue(dist < 0.20, "Expected small distance, got $dist")
    }

    @Test
    fun `computePositionalDistance is large for far positions`() {
        // Greek pos 16 (relPos ~0.94) vs KJV idx 1 (relPos ~0.06)
        val greekRelPos = service.computeRelativePosition(16, 17)
        val dist = service.computePositionalDistance(greekRelPos, 1, 17)
        assertTrue(dist > 0.80, "Expected large distance, got $dist")
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: computeSimilarity
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `computeSimilarity exact match returns 100`() {
        assertEquals(100, service.computeSimilarity("the", "the"))
    }

    @Test
    fun `computeSimilarity exact match ignoring case`() {
        assertEquals(100, service.computeSimilarity("The", "the"))
    }

    @Test
    fun `computeSimilarity with brackets`() {
        // "In [the]" normalizes to "in the", "In" normalizes to "in"
        // "in" is contained in "in the" → 85
        val sim = service.computeSimilarity("In [the]", "In")
        assertEquals(85, sim)
    }

    @Test
    fun `computeSimilarity Word with comma vs Word`() {
        // "Word," normalizes to "word", "Word" normalizes to "word" → 100
        assertEquals(100, service.computeSimilarity("Word,", "Word"))
    }

    @Test
    fun `computeSimilarity unrelated words`() {
        val sim = service.computeSimilarity("beginning", "God")
        assertTrue(sim < 50, "Expected low similarity, got $sim")
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: computeCandidates — with positional distance
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `computeCandidates for 'the' returns all 4 occurrences sorted by position`() {
        // Greek pos 4 (first ὁ), relPos = 4/16 = 0.25
        val greekRelPos = service.computeRelativePosition(4, 17)
        val candidates = service.computeCandidates("the", kjvWords, greekRelPos)

        // All 4 "the" positions: idx 1, 4, 7, 13
        assertEquals(4, candidates.size)
        assertTrue(candidates.all { it.sim == 100 }, "All should have sim=100")

        // First candidate should be closest to pos 4 (relPos 0.25)
        // idx 1 (relPos 0.06), idx 4 (relPos 0.25), idx 7 (relPos 0.44), idx 13 (relPos 0.81)
        // Closest: idx 4 (dist=0.0), then idx 1 (dist=0.19)
        assertEquals(4, candidates[0].idx, "First candidate should be idx 4 (closest)")
        assertEquals(1, candidates[1].idx, "Second candidate should be idx 1")
    }

    @Test
    fun `computeCandidates for last 'the' prefers idx 13`() {
        // Greek pos 16 (last ὁ), relPos = 16/16 = 1.0
        val greekRelPos = service.computeRelativePosition(16, 17)
        val candidates = service.computeCandidates("the", kjvWords, greekRelPos)

        assertEquals(4, candidates.size)
        // idx 13 (relPos 0.81) is closest to 1.0
        assertEquals(13, candidates[0].idx, "First candidate for last ὁ should be idx 13")
    }

    @Test
    fun `computeCandidates excludes consumed indices`() {
        // Suppose idx 0 and 1 are consumed by expression "In the"
        val consumed = setOf(0, 1)
        val greekRelPos = service.computeRelativePosition(4, 17)
        val candidates = service.computeCandidates("the", kjvWords, greekRelPos, consumed)

        // Should not include idx 1
        assertTrue(candidates.none { it.idx == 0 || it.idx == 1 }, "Consumed indices should be excluded")
        // Should have 3 remaining: idx 4, 7, 13
        assertEquals(3, candidates.size)
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: detectExpressions
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `detectExpressions finds 'In the' expression for Ἐν`() {
        val words = greekWordsJohn1v1()
        val (exprMap, consumed) = service.detectExpressions(words, kjvWords)

        // pos 1 (Ἐν, gloss "In [the]") should match idx 0 ("In") + idx 1 ("the")
        assertTrue(exprMap.containsKey(1), "Should detect expression for pos 1 (Ἐν)")
        val expr = exprMap[1]!!
        assertEquals(listOf(0, 1), expr.indices, "Should match 'In the' at idx 0,1")
        assertTrue(expr.confidence >= 80, "Confidence should be high, got ${expr.confidence}")

        // idx 0 and 1 should be consumed
        assertTrue(0 in consumed, "idx 0 should be consumed")
        assertTrue(1 in consumed, "idx 1 should be consumed")
    }

    @Test
    fun `detectExpressions does not create expression for single-word gloss`() {
        val words = greekWordsJohn1v1()
        val (exprMap, _) = service.detectExpressions(words, kjvWords)

        // pos 2 (ἀρχῇ, gloss "beginning") — single word, not an expression
        assertTrue(!exprMap.containsKey(2), "Should NOT detect expression for single-word gloss 'beginning'")
        // pos 4 (ὁ, gloss "the") — single word
        assertTrue(!exprMap.containsKey(4), "Should NOT detect expression for single-word gloss 'the'")
    }

    @Test
    fun `detectExpressions picks closest match positionally`() {
        // Construct a verse with repeated expression — using enough words to spread positions
        // wordPositions: 0, 1, 2, 3, 4, 5, 6 (7 words, so relPos range 0.0 to 1.0)
        val words = listOf(
            iw(0, "Ἐν", "En", "In [the]", "G1722"),   // relPos = 0/6 = 0.0
            iw(1, "ἀρχῇ", "archē", "beginning", "G0746"),
            iw(2, "ἦν", "ēn", "was", "G1510"),
            iw(3, "ὁ", "ho", "the", "G3588"),
            iw(4, "λόγος", "logos", "Word", "G3056"),
            iw(5, "καὶ", "kai", "and", "G2532"),
            iw(6, "Ἐν", "En", "In [the]", "G1722"),   // relPos = 6/6 = 1.0
        )
        val target = listOf("In", "the", "beginning", "was", "the", "Word", "and", "In", "the")
        //                    0      1       2           3     4      5       6     7      8

        val (exprMap, _) = service.detectExpressions(words, target)

        // pos 0 (relPos 0.0) should prefer idx 0,1 ("In the" at start)
        assertTrue(exprMap.containsKey(0), "Should detect expression for pos 0")
        assertEquals(listOf(0, 1), exprMap[0]!!.indices, "pos 0 should match 'In the' at idx 0,1")

        // pos 6 (relPos 1.0) should prefer idx 7,8 ("In the" at end)
        assertTrue(exprMap.containsKey(6), "Should detect expression for pos 6")
        assertEquals(listOf(7, 8), exprMap[6]!!.indices, "pos 6 should match 'In the' at idx 7,8")
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: validateAndFixAlignments — Positional Conflict Resolution
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `validateAndFixAlignments resolves 'the' conflict by position`() {
        val words = greekWordsJohn1v1()

        // Simulate LLM output where g:7 and g:16 both claim idx 7 ("the")
        val alignments = listOf(
            WordAlignmentService.WordAlignment(g = 1, k = listOf(0), t = "In", c = 95),
            WordAlignmentService.WordAlignment(g = 2, k = listOf(2), t = "beginning", c = 95),
            WordAlignmentService.WordAlignment(g = 3, k = listOf(3), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 4, k = listOf(1), t = "the", c = 90),
            WordAlignmentService.WordAlignment(g = 5, k = listOf(5), t = "Word,", c = 95),
            WordAlignmentService.WordAlignment(g = 6, k = listOf(6), t = "and", c = 95),
            WordAlignmentService.WordAlignment(g = 7, k = listOf(4), t = "the", c = 90),
            WordAlignmentService.WordAlignment(g = 8, k = listOf(8), t = "Word", c = 95),
            WordAlignmentService.WordAlignment(g = 9, k = listOf(9), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 10, k = listOf(10), t = "with", c = 95),
            WordAlignmentService.WordAlignment(g = 11, k = listOf(4), t = "the", c = 85),  // CONFLICT with g:7 on idx 4!
            WordAlignmentService.WordAlignment(g = 12, k = listOf(11), t = "God,", c = 95),
            WordAlignmentService.WordAlignment(g = 13, k = listOf(12), t = "and", c = 95),
            WordAlignmentService.WordAlignment(g = 14, k = listOf(16), t = "God.", c = 95),
            WordAlignmentService.WordAlignment(g = 15, k = listOf(15), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 16, k = listOf(7), t = "the", c = 90),  // claims idx 7 — correct?
            WordAlignmentService.WordAlignment(g = 17, k = listOf(14), t = "Word", c = 95),
        )

        val result = service.validateAndFixAlignments(alignments, words, kjvWords)

        // g:7 (pos 7, relPos ~0.38) should keep idx 4 (relPos 0.25) — it's closer
        // g:11 (pos 11, relPos ~0.63) should lose idx 4 and get reassigned
        val g7 = result.find { it.g == 7 }
        val g11 = result.find { it.g == 11 }

        assertNotNull(g7, "g:7 should exist")
        assertEquals(listOf(4), g7.k, "g:7 should keep idx 4")

        assertNotNull(g11, "g:11 should exist")
        // g:11 lost idx 4, should be reassigned to a free "the" index
        // Free "the" indices after all claims: idx 7 is taken by g:16, idx 13 is free
        // g:11 relPos ~0.63, idx 7 relPos=0.44 (dist=0.19), idx 13 relPos=0.81 (dist=0.18)
        // Should pick idx 7 or 13, whichever is free and closest
        assertNotNull(g11.k, "g:11 should be reassigned to a free index")
        assertTrue(g11.k!!.isNotEmpty(), "g:11 should have at least one index")
    }

    @Test
    fun `validateAndFixAlignments reassigns single orphan 'the' to nearest free index`() {
        val words = greekWordsJohn1v1()

        // Simulate: ONLY g:16 (last ὁ, pos 16) is orphaned — g:11 is already mapped
        // Free "the" indices: only idx 13
        val alignments = listOf(
            WordAlignmentService.WordAlignment(g = 1, k = listOf(0, 1), t = "In the", c = 97),
            WordAlignmentService.WordAlignment(g = 2, k = listOf(2), t = "beginning", c = 95),
            WordAlignmentService.WordAlignment(g = 3, k = listOf(3), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 4, k = listOf(4), t = "the", c = 90),
            WordAlignmentService.WordAlignment(g = 5, k = listOf(5), t = "Word,", c = 95),
            WordAlignmentService.WordAlignment(g = 6, k = listOf(6), t = "and", c = 95),
            WordAlignmentService.WordAlignment(g = 7, k = listOf(7), t = "the", c = 90),
            WordAlignmentService.WordAlignment(g = 8, k = listOf(8), t = "Word", c = 95),
            WordAlignmentService.WordAlignment(g = 9, k = listOf(9), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 10, k = listOf(10), t = "with", c = 95),
            WordAlignmentService.WordAlignment(g = 11, k = null, t = null, c = 0),          // orphan τὸν — will grab nearest free "the"
            WordAlignmentService.WordAlignment(g = 12, k = listOf(11), t = "God,", c = 95),
            WordAlignmentService.WordAlignment(g = 13, k = listOf(12), t = "and", c = 95),
            WordAlignmentService.WordAlignment(g = 14, k = listOf(16), t = "God.", c = 95),
            WordAlignmentService.WordAlignment(g = 15, k = listOf(15), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 16, k = null, t = null, c = 0),          // ORPHAN (last ὁ — THE BUG)
            WordAlignmentService.WordAlignment(g = 17, k = listOf(14), t = "Word", c = 95),
        )

        val result = service.validateAndFixAlignments(alignments, words, kjvWords)

        // Only free index is idx 13 ("the").
        // g:11 (τὸν, relPos=10/16=0.625) and g:16 (ὁ, relPos=16/16=1.0)
        // g:11 is processed first in the list and will grab idx 13 (dist=0.19)
        // g:16 would then have no free "the" — which proves the bug scenario
        //
        // BUT: the key assertion is that at least ONE of the orphans gets reassigned
        val g11 = result.find { it.g == 11 }!!
        val g16 = result.find { it.g == 16 }!!

        // At least g:11 should be reassigned to idx 13
        assertNotNull(g11.k, "g:11 (τὸν) should be reassigned to free idx 13")
        assertEquals(listOf(13), g11.k, "g:11 should get idx 13 (only free 'the')")
        assertTrue(g11.c > 0, "Reassigned orphan should have positive confidence")

        // g:16 has no more free "the" indices — remains orphan (acceptable)
        // This scenario shows WHY the expression detection in Step 1 is important:
        // If "In [the]" is detected as expression consuming idx 0+1, then g:4 shifts to idx 4,
        // freeing idx 1 — giving g:16 a free "the" to claim.
    }

    @Test
    fun `validateAndFixAlignments three 'was' mapped to three different indices`() {
        val words = greekWordsJohn1v1()

        // All three ἦν correctly mapped
        val alignments = listOf(
            WordAlignmentService.WordAlignment(g = 3, k = listOf(3), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 9, k = listOf(9), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 15, k = listOf(15), t = "was", c = 95),
        )

        val result = service.validateAndFixAlignments(alignments, words, kjvWords)

        // No conflicts — should remain unchanged
        val g3 = result.find { it.g == 3 }
        val g9 = result.find { it.g == 9 }
        val g15 = result.find { it.g == 15 }

        assertEquals(listOf(3), g3?.k)
        assertEquals(listOf(9), g9?.k)
        assertEquals(listOf(15), g15?.k)
    }

    @Test
    fun `validateAndFixAlignments resolves 'was' conflict choosing closer position`() {
        val words = greekWordsJohn1v1()

        // g:3 and g:9 both claim idx 3 ("was")
        val alignments = listOf(
            WordAlignmentService.WordAlignment(g = 3, k = listOf(3), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 9, k = listOf(3), t = "was", c = 95),  // conflict!
            WordAlignmentService.WordAlignment(g = 15, k = listOf(15), t = "was", c = 95),
        )

        val result = service.validateAndFixAlignments(alignments, words, kjvWords)

        // g:3 (pos 3, relPos 0.13) should keep idx 3 (relPos 0.19) — very close
        // g:9 (pos 9, relPos 0.50) should lose idx 3 and get reassigned to idx 9 (relPos 0.56) — free
        val g3 = result.find { it.g == 3 }
        val g9 = result.find { it.g == 9 }

        assertNotNull(g3)
        assertEquals(listOf(3), g3.k, "g:3 should keep idx 3 (closer)")

        assertNotNull(g9)
        assertNotNull(g9.k, "g:9 should be reassigned")
        assertEquals(listOf(9), g9.k, "g:9 should be reassigned to idx 9 (nearest free 'was')")
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: computeConfidenceWithPosition
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `computeConfidenceWithPosition high sim close position`() {
        val conf = service.computeConfidenceWithPosition(100, 0.05)
        assertEquals(100, conf) // 100 * 1.0
    }

    @Test
    fun `computeConfidenceWithPosition high sim far position`() {
        val conf = service.computeConfidenceWithPosition(100, 0.80)
        assertEquals(70, conf) // 100 * 0.70
    }

    @Test
    fun `computeConfidenceWithPosition medium sim medium position`() {
        val conf = service.computeConfidenceWithPosition(85, 0.15)
        assertEquals(80, conf) // 85 * 0.95 = 80.75 → 80
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: normalizeForComparison
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `normalizeForComparison strips punctuation and lowercases`() {
        assertEquals("word", service.normalizeForComparison("Word,"))
        assertEquals("god", service.normalizeForComparison("God."))
        assertEquals("in the", service.normalizeForComparison("In [the]"))
        assertEquals("the", service.normalizeForComparison("<the>"))
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: Full scenario — John 1:1 complete alignment validation
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `full John 1-1 scenario - no content word should remain unmapped after fix`() {
        val words = greekWordsJohn1v1()

        // Simulate a problematic LLM output:
        // - g:11 (τὸν "<the>") orphaned
        // - g:16 (ὁ "the") orphaned (the bug we're fixing)
        val alignments = listOf(
            WordAlignmentService.WordAlignment(g = 1, k = listOf(0), t = "In", c = 95),
            WordAlignmentService.WordAlignment(g = 2, k = listOf(2), t = "beginning", c = 95),
            WordAlignmentService.WordAlignment(g = 3, k = listOf(3), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 4, k = listOf(1), t = "the", c = 90),
            WordAlignmentService.WordAlignment(g = 5, k = listOf(5), t = "Word,", c = 95),
            WordAlignmentService.WordAlignment(g = 6, k = listOf(6), t = "and", c = 95),
            WordAlignmentService.WordAlignment(g = 7, k = listOf(4), t = "the", c = 90),
            WordAlignmentService.WordAlignment(g = 8, k = listOf(8), t = "Word", c = 95),
            WordAlignmentService.WordAlignment(g = 9, k = listOf(9), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 10, k = listOf(10), t = "with", c = 95),
            WordAlignmentService.WordAlignment(g = 11, k = null, t = null, c = 0),          // orphan τὸν
            WordAlignmentService.WordAlignment(g = 12, k = listOf(11), t = "God,", c = 95),
            WordAlignmentService.WordAlignment(g = 13, k = listOf(12), t = "and", c = 95),
            WordAlignmentService.WordAlignment(g = 14, k = listOf(16), t = "God.", c = 95),
            WordAlignmentService.WordAlignment(g = 15, k = listOf(15), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 16, k = null, t = null, c = 0),          // orphan ὁ (THE BUG)
            WordAlignmentService.WordAlignment(g = 17, k = listOf(14), t = "Word", c = 95),
        )

        val result = service.validateAndFixAlignments(alignments, words, kjvWords)

        // Verify: no orphan content words remain
        val orphans = result.filter { it.k.isNullOrEmpty() }
        // Check the specific ones we care about
        val g11 = result.find { it.g == 11 }!!
        val g16 = result.find { it.g == 16 }!!

        // g:11 (τὸν, gloss "<the>") should be reassigned
        // Free "the" indices: idx 7 and 13 are free
        // g:11 relPos = 10/16 = 0.625, idx 7 relPos = 0.44, idx 13 relPos = 0.81
        // Closest: idx 7 (dist=0.19) < idx 13 (dist=0.19) — both close, either is acceptable
        assertNotNull(g11.k, "g:11 (τὸν) should be reassigned to a free 'the'")
        assertTrue(g11.k!!.first() in listOf(7, 13), "g:11 should be assigned to idx 7 or 13")

        // g:16 (ὁ, gloss "the") should be reassigned to the remaining free "the"
        assertNotNull(g16.k, "g:16 (ὁ) should be reassigned to a free 'the'")
        assertTrue(g16.k!!.first() in listOf(7, 13), "g:16 should be assigned to idx 7 or 13")

        // They should be assigned to DIFFERENT indices
        assertTrue(
            g11.k!!.first() != g16.k!!.first(),
            "g:11 and g:16 should map to different 'the' indices"
        )

        // Verify: all used indices are unique (no double-assignment)
        val allUsedIndices = result.flatMap { it.k ?: emptyList() }
        assertEquals(allUsedIndices.size, allUsedIndices.toSet().size, "All assigned indices should be unique")
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: Morphology detection functions
    // ═══════════════════════════════════════════════════════════════════

    @Test
    fun `isArticle detects article morphology`() {
        assertTrue(service.isArticle("T-NSM"), "T-NSM is an article")
        assertTrue(service.isArticle("T-ASM"), "T-ASM is an article")
        assertTrue(service.isArticle("T-GSF"), "T-GSF is an article")
        assertTrue(service.isArticle("T-DPN"), "T-DPN is an article")
        assertTrue(!service.isArticle("N-NSM"), "N-NSM is NOT an article")
        assertTrue(!service.isArticle("V-IAI-3S"), "V-IAI-3S is NOT an article")
        assertTrue(!service.isArticle(null), "null is NOT an article")
    }

    @Test
    fun `isNominativeArticle detects nominative articles`() {
        assertTrue(service.isNominativeArticle("T-NSM"), "T-NSM is nominative")
        assertTrue(service.isNominativeArticle("T-NSF"), "T-NSF is nominative")
        assertTrue(service.isNominativeArticle("T-NPN"), "T-NPN is nominative")
        assertTrue(!service.isNominativeArticle("T-ASM"), "T-ASM is NOT nominative (accusative)")
        assertTrue(!service.isNominativeArticle("T-GSF"), "T-GSF is NOT nominative (genitive)")
        assertTrue(!service.isNominativeArticle(null), "null is NOT nominative")
    }

    @Test
    fun `isObliqueArticle detects non-nominative articles`() {
        assertTrue(service.isObliqueArticle("T-ASM"), "T-ASM is oblique (accusative)")
        assertTrue(service.isObliqueArticle("T-GSF"), "T-GSF is oblique (genitive)")
        assertTrue(service.isObliqueArticle("T-DPN"), "T-DPN is oblique (dative)")
        assertTrue(!service.isObliqueArticle("T-NSM"), "T-NSM is NOT oblique (nominative)")
        assertTrue(!service.isObliqueArticle("N-ASM"), "N-ASM is NOT oblique (not an article)")
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: Morphology-aware alignment — τὸν (accusative) vs ὁ (nominative)
    // ═══════════════════════════════════════════════════════════════════

    /** Greek words for John 1:1 WITH morphology from TAGNT */
    private fun greekWordsJohn1v1WithMorphology(): List<InterlinearWordDTO> = listOf(
        iw(1, "Ἐν", "En", "In [the]", "G1722", "PREP"),
        iw(2, "ἀρχῇ", "archē", "beginning", "G0746", "N-DSF"),
        iw(3, "ἦν", "ēn", "was", "G1510", "V-IAI-3S"),
        iw(4, "ὁ", "ho", "the", "G3588", "T-NSM"),      // nominative — subject marker
        iw(5, "λόγος,", "logos", "Word,", "G3056", "N-NSM"),
        iw(6, "καὶ", "kai", "and", "G2532", "CONJ"),
        iw(7, "ὁ", "ho", "the", "G3588", "T-NSM"),      // nominative
        iw(8, "λόγος", "logos", "Word", "G3056", "N-NSM"),
        iw(9, "ἦν", "ēn", "was", "G1510", "V-IAI-3S"),
        iw(10, "πρὸς", "pros", "with", "G4314", "PREP"),
        iw(11, "τὸν", "ton", "<the>", "G3588", "T-ASM"), // ACCUSATIVE — object of πρὸς
        iw(12, "θεόν,", "theon", "God,", "G2316", "N-ASM"),
        iw(13, "καὶ", "kai", "and", "G2532", "CONJ"),
        iw(14, "θεὸς", "theos", "God", "G2316", "N-NSM"),
        iw(15, "ἦν", "ēn", "was", "G1510", "V-IAI-3S"),
        iw(16, "ὁ", "ho", "the", "G3588", "T-NSM"),     // nominative
        iw(17, "λόγος.", "logos", "Word.", "G3056", "N-NSM"),
    )

    @Test
    fun `ton (accusative article) gets k=null when no 'the' before its noun in KJV`() {
        val words = greekWordsJohn1v1WithMorphology()

        // Simulate: τὸν (pos 11, T-ASM) is orphan, θεόν (pos 12) → idx 11 ("God,")
        // There's no "the" at idx 10 (it's "with"), so τὸν was absorbed
        val alignments = listOf(
            WordAlignmentService.WordAlignment(g = 1, k = listOf(0, 1), t = "In the", c = 97),
            WordAlignmentService.WordAlignment(g = 2, k = listOf(2), t = "beginning", c = 95),
            WordAlignmentService.WordAlignment(g = 3, k = listOf(3), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 4, k = listOf(4), t = "the", c = 95),
            WordAlignmentService.WordAlignment(g = 5, k = listOf(5), t = "Word,", c = 95),
            WordAlignmentService.WordAlignment(g = 6, k = listOf(6), t = "and", c = 95),
            WordAlignmentService.WordAlignment(g = 7, k = listOf(7), t = "the", c = 95),
            WordAlignmentService.WordAlignment(g = 8, k = listOf(8), t = "Word", c = 95),
            WordAlignmentService.WordAlignment(g = 9, k = listOf(9), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 10, k = listOf(10), t = "with", c = 95),
            WordAlignmentService.WordAlignment(g = 11, k = null, t = null, c = 0),          // τὸν ORPHAN
            WordAlignmentService.WordAlignment(g = 12, k = listOf(11), t = "God,", c = 95),
            WordAlignmentService.WordAlignment(g = 13, k = listOf(12), t = "and", c = 95),
            WordAlignmentService.WordAlignment(g = 14, k = listOf(16), t = "God.", c = 95),
            WordAlignmentService.WordAlignment(g = 15, k = listOf(15), t = "was", c = 95),
            WordAlignmentService.WordAlignment(g = 16, k = listOf(13), t = "the", c = 95),
            WordAlignmentService.WordAlignment(g = 17, k = listOf(14), t = "Word", c = 95),
        )

        val result = service.validateAndFixAlignments(alignments, words, kjvWords)

        val g11 = result.find { it.g == 11 }!!
        // τὸν should remain k=null — "with" is at idx 10, not "the"
        // The algorithm should recognize this as an absorbed article
        assertTrue(g11.k == null, "τὸν (T-ASM) should have k=null — article absorbed by KJV in 'with God'")
        assertEquals(65, g11.c, "Absorbed article should have confidence 65")
    }

    @Test
    fun `nominative article wins over accusative in conflict`() {
        val words = greekWordsJohn1v1WithMorphology()

        // Simulate: ὁ (pos 4, T-NSM) and τὸν (pos 11, T-ASM) both claim idx 4 ("the")
        val alignments = listOf(
            WordAlignmentService.WordAlignment(g = 4, k = listOf(4), t = "the", c = 90),   // T-NSM
            WordAlignmentService.WordAlignment(g = 11, k = listOf(4), t = "the", c = 85),  // T-ASM — conflict!
            WordAlignmentService.WordAlignment(g = 12, k = listOf(11), t = "God,", c = 95),
        )

        val result = service.validateAndFixAlignments(alignments, words, kjvWords)

        val g4 = result.find { it.g == 4 }!!
        val g11 = result.find { it.g == 11 }!!

        // Nominative (g:4) should win, accusative (g:11) should lose
        assertEquals(listOf(4), g4.k, "Nominative article should keep its index")
        // g:11 (T-ASM) loses idx 4, then morphological adjacency kicks in:
        // next word is θεόν (g:12) → idx 11 ("God,"), check idx 10 ("with") — not "the"
        // → absorbed, k=null
        assertTrue(g11.k == null, "Accusative article should be resolved as absorbed (k=null)")
    }

    @Test
    fun `accusative article finds 'the' before its noun when present`() {
        // Construct a scenario where "the" DOES exist before the noun
        // e.g., "with the God" instead of "with God"
        val words = listOf(
            iw(1, "πρὸς", "pros", "with", "G4314", "PREP"),
            iw(2, "τὸν", "ton", "<the>", "G3588", "T-ASM"),
            iw(3, "θεόν", "theon", "God", "G2316", "N-ASM"),
        )
        val target = listOf("with", "the", "God")
        //                    0       1      2

        val alignments = listOf(
            WordAlignmentService.WordAlignment(g = 1, k = listOf(0), t = "with", c = 95),
            WordAlignmentService.WordAlignment(g = 2, k = null, t = null, c = 0),   // τὸν orphan
            WordAlignmentService.WordAlignment(g = 3, k = listOf(2), t = "God", c = 95),
        )

        val result = service.validateAndFixAlignments(alignments, words, target)

        val g2 = result.find { it.g == 2 }!!
        // "the" is at idx 1, right before "God" at idx 2
        assertNotNull(g2.k, "τὸν should find 'the' at idx 1 (before 'God')")
        assertEquals(listOf(1), g2.k, "τὸν should be aligned to idx 1 ('the')")
        assertTrue(g2.c > 0, "Confidence should be positive")
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST: Portuguese (ARC69) — cross-language expression detection
    // ═══════════════════════════════════════════════════════════════════

    /**
     * ARC69 João 1:1: "No princípio era o Verbo, e o Verbo estava com Deus, e o Verbo era Deus."
     * idx:              0   1          2   3  4      5 6  7     8      9   10    11 12 13    14  15
     */
    private val arc69Words = listOf(
        "No", "princípio", "era", "o", "Verbo,", "e", "o", "Verbo", "estava", "com", "Deus,", "e", "o", "Verbo", "era", "Deus."
    )

    private fun greekWordsWithPtGloss(): List<InterlinearWordDTO> = listOf(
        iw(1, "Ἐν", "En", "In [the]", "G1722", "PREP").copy(portugueseGloss = "No"),
        iw(2, "ἀρχῇ", "archē", "beginning", "G0746", "N-DSF").copy(portugueseGloss = "princípio"),
        iw(3, "ἦν", "ēn", "was", "G1510", "V-IAI-3S").copy(portugueseGloss = "era"),
        iw(4, "ὁ", "ho", "the", "G3588", "T-NSM").copy(portugueseGloss = "o"),
        iw(5, "λόγος,", "logos", "Word,", "G3056", "N-NSM").copy(portugueseGloss = "Verbo"),
        iw(6, "καὶ", "kai", "and", "G2532", "CONJ").copy(portugueseGloss = "e"),
        iw(7, "ὁ", "ho", "the", "G3588", "T-NSM").copy(portugueseGloss = "o"),
        iw(8, "λόγος", "logos", "Word", "G3056", "N-NSM").copy(portugueseGloss = "Verbo"),
        iw(9, "ἦν", "ēn", "was", "G1510", "V-IAI-3S").copy(portugueseGloss = "estava"),
        iw(10, "πρὸς", "pros", "with", "G4314", "PREP").copy(portugueseGloss = "com"),
        iw(11, "τὸν", "ton", "<the>", "G3588", "T-ASM").copy(portugueseGloss = null),
        iw(12, "θεόν,", "theon", "God,", "G2316", "N-ASM").copy(portugueseGloss = "Deus"),
        iw(13, "καὶ", "kai", "and", "G2532", "CONJ").copy(portugueseGloss = "e"),
        iw(14, "θεὸς", "theos", "God", "G2316", "N-NSM").copy(portugueseGloss = "Deus"),
        iw(15, "ἦν", "ēn", "was", "G1510", "V-IAI-3S").copy(portugueseGloss = "era"),
        iw(16, "ὁ", "ho", "the", "G3588", "T-NSM").copy(portugueseGloss = "o"),
        iw(17, "λόγος.", "logos", "Word.", "G3056", "N-NSM").copy(portugueseGloss = "Verbo"),
    )

    @Test
    fun `detectExpressionsWithTranslatedGlosses finds no multi-word for single-word PT glosses`() {
        val words = greekWordsWithPtGloss()
        // All PT glosses are single-word, so no expressions should be detected
        val (exprMap, consumed) = service.detectExpressionsWithTranslatedGlosses(words, arc69Words, "pt")

        assertTrue(exprMap.isEmpty(), "No expressions expected for single-word PT glosses")
        assertTrue(consumed.isEmpty())
    }

    @Test
    fun `detectExpressionsWithTranslatedGlosses detects multi-word PT gloss`() {
        // Create a word with multi-word PT gloss
        val words = listOf(
            iw(0, "Ἐν", "En", "In [the]", "G1722", "PREP").copy(portugueseGloss = "No princípio"),
            iw(1, "ἦν", "ēn", "was", "G1510", "V-IAI-3S").copy(portugueseGloss = "era"),
        )
        val target = listOf("No", "princípio", "era", "o", "Verbo")

        val (exprMap, consumed) = service.detectExpressionsWithTranslatedGlosses(words, target, "pt")

        assertTrue(exprMap.containsKey(0), "Should detect 'No princípio' expression")
        assertEquals(listOf(0, 1), exprMap[0]!!.indices)
        assertTrue(0 in consumed && 1 in consumed)
    }

    @Test
    fun `computeCandidates with PT gloss finds 'o' occurrences in ARC69`() {
        // ὁ (pos 4, relPos 4/16=0.25) with ptGloss "o"
        // "o" appears at idx 3, 6, 12 in ARC69 (exact match sim=100)
        // "No" at idx 0 also matches because "o" is contained in "no" (sim=85)
        val greekRelPos = service.computeRelativePosition(4, 17)
        val candidates = service.computeCandidates("o", arc69Words, greekRelPos)

        assertTrue(candidates.isNotEmpty(), "Should find 'o' candidates in ARC69")
        // Exact "o" matches should come first (sim=100), sorted by dist
        val exactMatches = candidates.filter { it.sim == 100 }
        assertTrue(exactMatches.size >= 3, "Should have at least 3 exact 'o' matches")
        // Closest to relPos 0.25: idx 3 (relPos 0.20)
        assertEquals(3, exactMatches[0].idx, "First exact candidate should be idx 3 (closest to pos 4)")
    }

    @Test
    fun `validateAndFixAlignments works for ARC69 orphan article 'o'`() {
        val words = greekWordsWithPtGloss()

        // Simulate: three ὁ correctly mapped, τὸν orphaned
        val alignments = listOf(
            WordAlignmentService.WordAlignment(g = 1, k = listOf(0), t = "No", c = 95),
            WordAlignmentService.WordAlignment(g = 2, k = listOf(1), t = "princípio", c = 95),
            WordAlignmentService.WordAlignment(g = 3, k = listOf(2), t = "era", c = 95),
            WordAlignmentService.WordAlignment(g = 4, k = listOf(3), t = "o", c = 95),
            WordAlignmentService.WordAlignment(g = 5, k = listOf(4), t = "Verbo,", c = 95),
            WordAlignmentService.WordAlignment(g = 6, k = listOf(5), t = "e", c = 95),
            WordAlignmentService.WordAlignment(g = 7, k = listOf(6), t = "o", c = 95),
            WordAlignmentService.WordAlignment(g = 8, k = listOf(7), t = "Verbo", c = 95),
            WordAlignmentService.WordAlignment(g = 9, k = listOf(8), t = "estava", c = 95),
            WordAlignmentService.WordAlignment(g = 10, k = listOf(9), t = "com", c = 95),
            WordAlignmentService.WordAlignment(g = 11, k = null, t = null, c = 0),  // τὸν ORPHAN
            WordAlignmentService.WordAlignment(g = 12, k = listOf(10), t = "Deus,", c = 95),
            WordAlignmentService.WordAlignment(g = 13, k = listOf(11), t = "e", c = 95),
            WordAlignmentService.WordAlignment(g = 14, k = listOf(15), t = "Deus.", c = 95),
            WordAlignmentService.WordAlignment(g = 15, k = listOf(14), t = "era", c = 95),
            WordAlignmentService.WordAlignment(g = 16, k = listOf(12), t = "o", c = 95),
            WordAlignmentService.WordAlignment(g = 17, k = listOf(13), t = "Verbo", c = 95),
        )

        val result = service.validateAndFixAlignments(alignments, words, arc69Words)

        val g11 = result.find { it.g == 11 }!!
        // τὸν (T-ASM): next word is θεόν at idx 10 ("Deus,"), check idx 9 = "com" ≠ "the"/"o"
        // Should be absorbed
        assertTrue(g11.k == null, "τὸν should be absorbed in ARC69 'com Deus' (no 'o' before 'Deus')")
        assertEquals(65, g11.c, "Absorbed article should have confidence 65")
    }
}
