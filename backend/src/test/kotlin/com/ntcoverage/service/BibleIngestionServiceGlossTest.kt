package com.ntcoverage.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for BibleIngestionService.processGlossResponse — the JSON-first
 * LLM response parser that maps translated gloss lines to English source glosses.
 *
 * Strategy: try JSON parsing first (most robust, no positional issues),
 * fall back to line-by-line with two-pass filtering for legacy responses.
 */
class BibleIngestionServiceGlossTest {

    // ═══════════════════════════════════════════════════════════
    // JSON parsing tests (primary strategy)
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `JSON response maps correctly`() {
        val chunk = listOf("In [the]", "beginning", "was")
        val llmOutput = """{"In [the]": "Em", "beginning": "princípio", "was": "era"}"""

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("Em", result["In [the]"])
        assertEquals("princípio", result["beginning"])
        assertEquals("era", result["was"])
        assertEquals(3, result.size)
    }

    @Test
    fun `JSON response with markdown code block`() {
        val chunk = listOf("the", "Word", "and")
        val llmOutput = """```json
{"the": "o", "Word": "Verbo", "and": "e"}
```"""

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("o", result["the"])
        assertEquals("Verbo", result["Word"])
        assertEquals("e", result["and"])
    }

    @Test
    fun `JSON response with preamble text before JSON`() {
        val chunk = listOf("In [the]", "beginning", "was")
        val llmOutput = """Aqui está a tradução:
{"In [the]": "Em", "beginning": "princípio", "was": "era"}"""

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("Em", result["In [the]"])
        assertEquals("princípio", result["beginning"])
        assertEquals("era", result["was"])
    }

    @Test
    fun `JSON response preserves brackets and punctuation`() {
        val chunk = listOf("[The] book", "<the>", "God,", "Word.")
        val llmOutput = """{"[The] book": "[O] livro", "<the>": "<o>", "God,": "Deus,", "Word.": "Verbo."}"""

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("[O] livro", result["[The] book"])
        assertEquals("<o>", result["<the>"])
        assertEquals("Deus,", result["God,"])
        assertEquals("Verbo.", result["Word."])
    }

    @Test
    fun `JSON response for Spanish`() {
        val chunk = listOf("and", "God", "was")
        val llmOutput = """{"and": "y", "God": "Dios", "was": "era"}"""

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Spanish")

        assertEquals("y", result["and"])
        assertEquals("Dios", result["God"])
        assertEquals("era", result["was"])
    }

    @Test
    fun `real-world John 1-1 scenario with JSON`() {
        val chunk = listOf("In [the]", "beginning", "was", "the", "Word,", "and", "with", "God,", "God")
        val llmOutput = """{
            "In [the]": "Em",
            "beginning": "princípio",
            "was": "era",
            "the": "o",
            "Word,": "Verbo,",
            "and": "e",
            "with": "com",
            "God,": "Deus,",
            "God": "Deus"
        }"""

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("Em", result["In [the]"])
        assertEquals("princípio", result["beginning"])
        assertEquals("era", result["was"])
        assertEquals("o", result["the"])
        assertEquals("Verbo,", result["Word,"])
        assertEquals("e", result["and"])
        assertEquals("com", result["with"])
        assertEquals("Deus,", result["God,"])
        assertEquals("Deus", result["God"])
        assertEquals(9, result.size)
    }

    // ═══════════════════════════════════════════════════════════
    // tryParseJsonGlosses unit tests
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `tryParseJsonGlosses returns null for non-JSON`() {
        val chunk = listOf("and", "God")
        val result = BibleIngestionService.tryParseJsonGlosses("e\nDeus", chunk)
        assertNull(result)
    }

    @Test
    fun `tryParseJsonGlosses returns null for empty input`() {
        val chunk = listOf("and")
        val result = BibleIngestionService.tryParseJsonGlosses("", chunk)
        assertNull(result)
    }

    @Test
    fun `JSON with completely mismatched keys returns empty map`() {
        val chunk = listOf("In [the]", "beginning", "was")
        val llmOutput = """{"[thing]": "[coisa]", "that": "que", "it": "isso"}"""

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertTrue(result.isEmpty(), "Mismatched JSON keys should produce empty result, not garbage")
    }

    @Test
    fun `partial JSON match returns only matched entries without gap-filling`() {
        val chunk = listOf("In [the]", "beginning", "was", "the", "Word")
        val llmOutput = """{
            "In [the]": "Em",
            "beginning": "princípio",
            "[thing]": "[coisa]",
            "random": "aleatório",
            "other": "outro"
        }"""

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("Em", result["In [the]"])
        assertEquals("princípio", result["beginning"])
        assertEquals(2, result.size, "Only matched keys should be in result — no gap-filling")
        assertNull(result["was"])
        assertNull(result["the"])
        assertNull(result["Word"])
    }

    @Test
    fun `JSON-like content with parse failure does not fall through to line-by-line`() {
        val chunk = listOf("and", "God", "was")
        // Missing comma after "Deus" causes JSON parse failure
        val llmOutput = """{
            "and": "e",
            "God": "Deus"
            "was": "era"
        }"""

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        // Should return empty (JSON content detected but parsing failed)
        // rather than line-by-line garbage
        assertTrue(result.isEmpty() || result.values.none { it.contains("\":") },
            "No result should contain JSON syntax fragments")
    }

    @Test
    fun `plain text response still uses line-by-line correctly after JSON guard`() {
        val chunk = listOf("and", "God", "was")
        val llmOutput = "e\nDeus\nera"

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("e", result["and"])
        assertEquals("Deus", result["God"])
        assertEquals("era", result["was"])
    }

    @Test
    fun `tryParseJsonGlosses ignores extra keys not in chunk`() {
        val chunk = listOf("and", "God")
        val result = BibleIngestionService.tryParseJsonGlosses(
            """{"and": "e", "God": "Deus", "extra": "valor"}""", chunk
        )
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("e", result["and"])
        assertEquals("Deus", result["God"])
    }

    // ═══════════════════════════════════════════════════════════
    // Line-by-line fallback tests (legacy/fallback strategy)
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `clean line-by-line response maps correctly`() {
        val chunk = listOf("In [the]", "beginning", "was")
        val llmOutput = "No\nprincípio\nera"

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("No", result["In [the]"])
        assertEquals("princípio", result["beginning"])
        assertEquals("era", result["was"])
        assertEquals(3, result.size)
    }

    @Test
    fun `preamble with exact line count does not shift mapping`() {
        val chunk = listOf("In [the]", "beginning", "was", "the", "Word")
        val llmOutput = """
            Aqui está a tradução para o português:
            No
            princípio
            era
            o
        """.trimIndent()

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("No", result["In [the]"])
        assertEquals("princípio", result["beginning"])
        assertEquals("era", result["was"])
        assertEquals("o", result["the"])
        assertNull(result["Word"])
    }

    @Test
    fun `preamble with extra lines stripped after meta filtering`() {
        val chunk = listOf("and", "God")
        val llmOutput = """
            Here is the translation:
            e
            Deus
        """.trimIndent()

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("e", result["and"])
        assertEquals("Deus", result["God"])
    }

    @Test
    fun `multiple meta lines are all filtered`() {
        val chunk = listOf("was", "the", "Word")
        val llmOutput = """
            Sure, here are the translations:
            Below are the Portuguese equivalents:
            era
            o
            Palavra
        """.trimIndent()

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("era", result["was"])
        assertEquals("o", result["the"])
        assertEquals("Palavra", result["Word"])
    }

    @Test
    fun `blank lines are ignored`() {
        val chunk = listOf("and", "God", "Word")
        val llmOutput = "e\n\n\nDeus\n\nPalavra"

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("e", result["and"])
        assertEquals("Deus", result["God"])
        assertEquals("Palavra", result["Word"])
    }

    @Test
    fun `lines longer than 80 chars are filtered as meta`() {
        val chunk = listOf("In [the]", "beginning")
        val longLine = "A".repeat(81)
        val llmOutput = "$longLine\nNo\nprincípio"

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("No", result["In [the]"])
        assertEquals("princípio", result["beginning"])
    }

    @Test
    fun `fewer translations than glosses leaves some untranslated`() {
        val chunk = listOf("In [the]", "beginning", "was", "the", "Word")
        val llmOutput = "No\nprincípio\nera"

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("No", result["In [the]"])
        assertEquals("princípio", result["beginning"])
        assertEquals("era", result["was"])
        assertNull(result["the"])
        assertNull(result["Word"])
        assertEquals(3, result.size)
    }

    @Test
    fun `spanish meta patterns are also filtered`() {
        val chunk = listOf("and", "God")
        val llmOutput = """
            Aquí está la traducción:
            y
            Dios
        """.trimIndent()

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Spanish")

        assertEquals("y", result["and"])
        assertEquals("Dios", result["God"])
    }

    @Test
    fun `excess clean lines after filtering drops leading lines`() {
        val chunk = listOf("God", "Word")
        val llmOutput = "No\nprincípio\nDeus\nPalavra"

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("Deus", result["God"])
        assertEquals("Palavra", result["Word"])
    }

    @Test
    fun `empty response returns empty map`() {
        val chunk = listOf("In [the]", "beginning")
        val llmOutput = ""

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `real-world John 1-1 scenario with preamble shift (line fallback)`() {
        // Simulates the exact bug seen in production: LLM adds Portuguese preamble,
        // causing shift in line-by-line mode. The two-pass fix handles this.
        val chunk = listOf("In [the]", "beginning", "was", "the", "Word,", "and", "with", "God,", "God")
        val llmOutput = """
            Aqui está a tradução para o português:
            Em
            princípio
            era
            o
            Palavra,
            e
            com
            Deus,
            Deus
        """.trimIndent()

        val result = BibleIngestionService.processGlossResponse(llmOutput, chunk, "Portuguese")

        assertEquals("Em", result["In [the]"])
        assertEquals("princípio", result["beginning"])
        assertEquals("era", result["was"])
        assertEquals("o", result["the"])
        assertEquals("Palavra,", result["Word,"])
        assertEquals("e", result["and"])
        assertEquals("com", result["with"])
        assertEquals("Deus,", result["God,"])
        assertEquals("Deus", result["God"])
    }
}
