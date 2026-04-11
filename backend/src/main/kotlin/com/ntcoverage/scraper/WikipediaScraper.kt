package com.ntcoverage.scraper

import com.ntcoverage.model.ManuscriptSeed
import com.ntcoverage.model.BookContent
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

/**
 * Scraper for extracting New Testament manuscript data from Wikipedia.
 * Targets the "List of New Testament papyri" and "List of New Testament uncials" pages.
 *
 * This is a supplementary data source — the primary data comes from the curated JSON seed.
 * Wikipedia table structures may change; this scraper provides best-effort extraction.
 */
class WikipediaScraper {

    private val log = LoggerFactory.getLogger(WikipediaScraper::class.java)

    companion object {
        const val PAPYRI_URL = "https://en.wikipedia.org/wiki/List_of_New_Testament_papyri"
        const val UNCIALS_URL = "https://en.wikipedia.org/wiki/List_of_New_Testament_uncials"
    }

    fun scrapePapyri(maxCentury: Int = 5): List<ManuscriptSeed> {
        return try {
            val doc = Jsoup.connect(PAPYRI_URL)
                .userAgent("NTCoverageBot/1.0 (academic research)")
                .timeout(15_000)
                .get()

            val table = doc.select("table.wikitable").firstOrNull()
            if (table == null) {
                log.warn("No wikitable found on papyri page")
                return emptyList()
            }

            val rows = table.select("tbody tr").drop(1)
            val manuscripts = mutableListOf<ManuscriptSeed>()

            for (row in rows) {
                val cells = row.select("td")
                if (cells.size < 4) continue

                val nameCell = cells[0].text().trim()
                val gaId = extractPapyrusId(nameCell) ?: continue
                val dateText = cells[1].text().trim()
                val century = parseCentury(dateText) ?: continue

                if (century > maxCentury) continue

                val contentText = cells[2].text().trim()
                val bookContents = parseContentColumn(contentText)

                if (bookContents.isNotEmpty()) {
                    manuscripts.add(
                        ManuscriptSeed(
                            gaId = gaId,
                            name = nameCell,
                            centuryMin = century,
                            centuryMax = century,
                            type = "papyrus",
                            content = bookContents
                        )
                    )
                }
            }

            log.info("Scraped ${manuscripts.size} papyri from Wikipedia (up to century $maxCentury)")
            manuscripts
        } catch (e: Exception) {
            log.error("Failed to scrape papyri from Wikipedia: ${e.message}")
            emptyList()
        }
    }

    private fun extractPapyrusId(text: String): String? {
        val match = Regex("""[Pp𝔓]\s*(\d+)""").find(text) ?: return null
        return "P${match.groupValues[1]}"
    }

    internal fun parseCentury(dateText: String): Int? {
        val centuryMatch = Regex("""(\d+)(?:st|nd|rd|th)\s*century""", RegexOption.IGNORE_CASE).find(dateText)
        if (centuryMatch != null) return centuryMatch.groupValues[1].toIntOrNull()

        val yearMatch = Regex("""c\.?\s*(\d{2,4})""").find(dateText)
        if (yearMatch != null) {
            val year = yearMatch.groupValues[1].toInt()
            return (year / 100) + 1
        }

        val romanMatch = Regex("""(I{1,3}V?|VI{0,3})""").find(dateText)
        if (romanMatch != null) return romanToInt(romanMatch.groupValues[1])

        return null
    }

    private fun romanToInt(roman: String): Int? {
        val map = mapOf('I' to 1, 'V' to 5, 'X' to 10)
        var result = 0
        var prev = 0
        for (ch in roman.reversed()) {
            val value = map[ch] ?: return null
            result += if (value < prev) -value else value
            prev = value
        }
        return if (result > 0) result else null
    }

    /**
     * Best-effort parsing of Wikipedia content column (e.g. "Matt 1; John 18:31-33").
     * Returns empty list if content is not parseable as verse ranges.
     */
    internal fun parseContentColumn(text: String): List<BookContent> {
        val bookAbbrevs = mapOf(
            "matt" to "Matthew", "matthew" to "Matthew",
            "mark" to "Mark", "mk" to "Mark",
            "luke" to "Luke", "lk" to "Luke",
            "john" to "John", "jn" to "John",
            "acts" to "Acts",
            "rom" to "Romans", "romans" to "Romans",
            "1 cor" to "1 Corinthians", "1cor" to "1 Corinthians",
            "2 cor" to "2 Corinthians", "2cor" to "2 Corinthians",
            "gal" to "Galatians", "galatians" to "Galatians",
            "eph" to "Ephesians", "ephesians" to "Ephesians",
            "phil" to "Philippians", "philippians" to "Philippians",
            "col" to "Colossians", "colossians" to "Colossians",
            "1 thess" to "1 Thessalonians", "2 thess" to "2 Thessalonians",
            "1 tim" to "1 Timothy", "2 tim" to "2 Timothy",
            "titus" to "Titus", "tit" to "Titus",
            "philem" to "Philemon", "phlm" to "Philemon",
            "heb" to "Hebrews", "hebrews" to "Hebrews",
            "james" to "James", "jas" to "James",
            "1 pet" to "1 Peter", "2 pet" to "2 Peter",
            "1 john" to "1 John", "2 john" to "2 John", "3 john" to "3 John",
            "jude" to "Jude",
            "rev" to "Revelation", "revelation" to "Revelation"
        )

        val result = mutableListOf<BookContent>()
        val parts = text.split(";", ",").map { it.trim() }

        for (part in parts) {
            val lower = part.lowercase()
            val matchedBook = bookAbbrevs.entries.find { (abbrev, _) ->
                lower.startsWith(abbrev)
            }
            if (matchedBook != null) {
                val rest = part.substring(matchedBook.key.length).trim()
                val ranges = if (rest.isNotEmpty()) listOf(rest) else emptyList()
                if (ranges.isNotEmpty()) {
                    result.add(BookContent(book = matchedBook.value, ranges = ranges))
                }
            }
        }

        return result
    }
}
