package com.ntcoverage.scraper

import com.ntcoverage.config.SourceFileCache
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import org.slf4j.LoggerFactory

class BibleOnlineScraper(
    private val fileCache: SourceFileCache
) {
    private val log = LoggerFactory.getLogger(BibleOnlineScraper::class.java)

    companion object {
        // nocr.net has the correct Almeida Revista e Corrigida 1969 (ARC69)
        private const val BASE_URL = "https://nocr.net/hbm/portuguese/porarc/index.php"
        private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val TIMEOUT_MS = 30_000
        private const val DELAY_MS = 300L

        private val VERSE_REF_PATTERN = Regex("""(\d+):(\d+)""")

        // Book name → nocr.net abbreviation
        val BOOK_SLUGS = mapOf(
            // OT
            "Genesis" to "Gen", "Exodus" to "Exo", "Leviticus" to "Lev",
            "Numbers" to "Num", "Deuteronomy" to "Deu",
            "Joshua" to "Jos", "Judges" to "Jdg", "Ruth" to "Rut",
            "1 Samuel" to "1Sa", "2 Samuel" to "2Sa",
            "1 Kings" to "1Ki", "2 Kings" to "2Ki",
            "1 Chronicles" to "1Ch", "2 Chronicles" to "2Ch",
            "Ezra" to "Ezr", "Nehemiah" to "Neh", "Esther" to "Est",
            "Job" to "Job", "Psalms" to "Psa", "Proverbs" to "Pro",
            "Ecclesiastes" to "Ecc", "Song of Solomon" to "Song",
            "Isaiah" to "Isa", "Jeremiah" to "Jer", "Lamentations" to "Lam",
            "Ezekiel" to "Eze", "Daniel" to "Dan", "Hosea" to "Hos",
            "Joel" to "Joe", "Amos" to "Amo", "Obadiah" to "Oba",
            "Jonah" to "Jon", "Micah" to "Mic", "Nahum" to "Nah",
            "Habakkuk" to "Hab", "Zephaniah" to "Zep",
            "Haggai" to "Hag", "Zechariah" to "Zac", "Malachi" to "Mal",
            // NT
            "Matthew" to "Mat", "Mark" to "Mar", "Luke" to "Luk", "John" to "Joh",
            "Acts" to "Act", "Romans" to "Rom",
            "1 Corinthians" to "1Co", "2 Corinthians" to "2Co",
            "Galatians" to "Gal", "Ephesians" to "Eph",
            "Philippians" to "Php", "Colossians" to "Col",
            "1 Thessalonians" to "1Th", "2 Thessalonians" to "2Th",
            "1 Timothy" to "1Ti", "2 Timothy" to "2Ti",
            "Titus" to "Tit", "Philemon" to "Phm", "Hebrews" to "Heb",
            "James" to "Jas", "1 Peter" to "1Pe", "2 Peter" to "2Pe",
            "1 John" to "1Jo", "2 John" to "2Jo", "3 John" to "3Jo",
            "Jude" to "Jud", "Revelation" to "Rev"
        )
    }

    suspend fun scrapeChapter(version: String, bookSlug: String, chapter: Int): List<Pair<Int, String>> {
        val cacheKey = "nocr-arc69/${version}_${bookSlug}_${chapter}.html"
        val url = "$BASE_URL/$bookSlug/$chapter/"

        val html = fileCache.getOrFetch(cacheKey) {
            log.debug("BIBLE_SCRAPER: downloading $url")
            val response = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .execute()
            if (response.statusCode() != 200) {
                throw RuntimeException("HTTP ${response.statusCode()} for $url")
            }
            response.body()
        }

        delay(DELAY_MS)

        // If cached response is an error page, re-fetch
        if (html.contains("403") && !html.contains("<sup>")) {
            log.warn("BIBLE_SCRAPER: cached error page for $bookSlug $chapter, re-fetching")
            val freshHtml = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .execute()
                .body()
            fileCache.put(cacheKey, freshHtml)
            delay(DELAY_MS)
            return try {
                parseVerses(freshHtml)
            } catch (e: Exception) {
                log.warn("BIBLE_SCRAPER: failed to parse $bookSlug $chapter after re-fetch: ${e.message}")
                emptyList()
            }
        }

        return try {
            parseVerses(html)
        } catch (e: Exception) {
            log.warn("BIBLE_SCRAPER: failed to parse $bookSlug $chapter: ${e.message}")
            emptyList()
        }
    }

    private fun parseVerses(html: String): List<Pair<Int, String>> {
        val doc = Jsoup.parse(html)
        val verses = mutableListOf<Pair<Int, String>>()

        // Structure: <sup><a href="...">2:1</a></sup> Verse text here...<br>
        val sups = doc.select("sup:has(a)")
        for (sup in sups) {
            val link = sup.selectFirst("a") ?: continue
            val match = VERSE_REF_PATTERN.matchEntire(link.text().trim()) ?: continue
            val verseNum = match.groupValues[2].toIntOrNull() ?: continue

            // Collect text from siblings AFTER the <sup> element
            val sb = StringBuilder()
            var sibling = sup.nextSibling()
            while (sibling != null) {
                when {
                    sibling is TextNode -> sb.append(sibling.text())
                    sibling is org.jsoup.nodes.Element && sibling.tagName() in listOf("sup", "br") -> break
                    sibling is org.jsoup.nodes.Element -> sb.append(sibling.text())
                }
                sibling = sibling.nextSibling()
            }

            val text = sb.toString().trim()
            if (text.isNotBlank()) {
                verses.add(Pair(verseNum, text))
            }
        }

        return verses
    }

    fun getSlug(bookName: String): String? = BOOK_SLUGS[bookName]
}
