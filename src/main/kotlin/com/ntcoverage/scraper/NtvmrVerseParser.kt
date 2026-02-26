package com.ntcoverage.scraper

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.slf4j.LoggerFactory

/**
 * Parses TEI/XML responses from the NTVMR API to extract which verses
 * are actually present in a manuscript.
 *
 * In the TEI format:
 *   - <ab n="B04K8V12" ...><w>παλιν</w>...</ab>  → verse present (has child elements)
 *   - <ab n="B04K8V1" .../>                        → verse absent (empty/self-closing)
 *
 * The `n` attribute encodes: BxxKyyVzz where xx=book, yy=chapter, zz=verse
 * using the standard NTVMR/INTF numbering (B01=Matt ... B27=Rev).
 */
class NtvmrVerseParser {

    private val log = LoggerFactory.getLogger(NtvmrVerseParser::class.java)

    companion object {
        val BOOK_NUM_TO_NAME = mapOf(
            1 to "Matthew", 2 to "Mark", 3 to "Luke", 4 to "John",
            5 to "Acts", 6 to "Romans", 7 to "1 Corinthians", 8 to "2 Corinthians",
            9 to "Galatians", 10 to "Ephesians", 11 to "Philippians", 12 to "Colossians",
            13 to "1 Thessalonians", 14 to "2 Thessalonians",
            15 to "1 Timothy", 16 to "2 Timothy", 17 to "Titus", 18 to "Philemon",
            19 to "Hebrews", 20 to "James", 21 to "1 Peter", 22 to "2 Peter",
            23 to "1 John", 24 to "2 John", 25 to "3 John", 26 to "Jude",
            27 to "Revelation"
        )

        val BOOK_NAME_TO_NUM = BOOK_NUM_TO_NAME.entries.associate { (k, v) -> v to k }
    }

    private val verseIdRegex = Regex("""B(\d{2})K(\d+)V(\d+)""")

    /**
     * Extracts present verses from NTVMR TEI/XML.
     * A verse is considered present if the <ab> element has any child elements
     * (words, gaps, etc.), indicating the manuscript contained it — even if
     * parts are now damaged.
     *
     * @return Map of canonical book name -> list of (chapter, verse) pairs
     */
    fun extractPresentVerses(teiXml: String): Map<String, List<Pair<Int, Int>>> {
        try {
            val doc = Jsoup.parse(teiXml, "", Parser.xmlParser())
            val result = mutableMapOf<String, MutableList<Pair<Int, Int>>>()

            for (ab in doc.select("ab")) {
                val nAttr = ab.attr("n")
                val match = verseIdRegex.find(nAttr) ?: continue

                val bookNum = match.groupValues[1].toInt()
                val chapter = match.groupValues[2].toInt()
                val verse = match.groupValues[3].toInt()

                val isPresent = ab.children().isNotEmpty()

                if (isPresent) {
                    val bookName = BOOK_NUM_TO_NAME[bookNum] ?: continue
                    result.getOrPut(bookName) { mutableListOf() }.add(chapter to verse)
                }
            }

            return result
        } catch (e: Exception) {
            log.error("Failed to parse NTVMR TEI/XML: ${e.message}")
            return emptyMap()
        }
    }

    /**
     * Counts total <ab> elements and present ones, useful for diagnostics.
     */
    fun summarize(teiXml: String): String {
        val doc = Jsoup.parse(teiXml, "", Parser.xmlParser())
        val abs = doc.select("ab")
        val total = abs.size
        val present = abs.count { it.children().isNotEmpty() }
        val absent = total - present
        return "total=$total present=$present absent=$absent"
    }
}
