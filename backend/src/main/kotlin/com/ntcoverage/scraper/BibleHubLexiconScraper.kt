package com.ntcoverage.scraper

import com.ntcoverage.config.SourceFileCache
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

data class BibleHubLexiconData(
    val pronunciation: String? = null,
    val phoneticSpelling: String? = null,
    val kjvTranslation: String? = null,
    val kjvUsageCount: Int? = null,
    val nasbTranslation: String? = null,
    val wordOrigin: String? = null,
    val strongsExhaustive: String? = null,
    val nasExhaustiveOrigin: String? = null,
    val nasExhaustiveDefinition: String? = null,
    val nasExhaustiveTranslation: String? = null,
    val shortDefinition: String? = null,
    val lemma: String? = null,
    val transliteration: String? = null,
    val partOfSpeech: String? = null
)

class BibleHubLexiconScraper(private val sourceFileCache: SourceFileCache) {

    private val log = LoggerFactory.getLogger(BibleHubLexiconScraper::class.java)

    companion object {
        private val BOUNDARY_LABELS = listOf(
            "Pronunciation:", "Phonetic Spelling:", "Short Definition:",
            "Definition:", "KJV:", "NASB Translation:", "NASB Word Usage:",
            "Word Origin:", "NAS Exhaustive", "Strong's Exhaustive",
            "Occurrences", "Part of Speech:", "Transliteration:",
            "Original Word:", "Usage:", "Forms and Transliterations"
        )
    }

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build()

    suspend fun scrapeGreekEntry(strongsNum: Int): BibleHubLexiconData? =
        scrapeEntry("greek", strongsNum)

    suspend fun scrapeHebrewEntry(strongsNum: Int): BibleHubLexiconData? =
        scrapeEntry("hebrew", strongsNum)

    private suspend fun scrapeEntry(language: String, strongsNum: Int): BibleHubLexiconData? {
        val url = "https://biblehub.com/$language/$strongsNum.htm"
        val cacheKey = "biblehub_${language}_$strongsNum"

        val html = try {
            sourceFileCache.getOrFetch(cacheKey) {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .header("Accept", "text/html")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build()

                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) {
                    response.body()
                } else {
                    throw RuntimeException("HTTP ${response.statusCode()}")
                }
            }
        } catch (e: Exception) {
            log.warn("BIBLEHUB_SCRAPER: failed to fetch $language/$strongsNum: ${e.message}")
            return null
        }

        return parseHtml(html)
    }

    private fun parseHtml(html: String): BibleHubLexiconData {
        val text = html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ")

        return BibleHubLexiconData(
            pronunciation = extractAfterLabel(text, "Pronunciation:"),
            phoneticSpelling = extractAfterLabel(text, "Phonetic Spelling:"),
            kjvTranslation = extractKjvTranslation(text),
            kjvUsageCount = extractKjvUsageCount(text),
            nasbTranslation = extractNasField(text, "NASB Translation"),
            wordOrigin = extractAfterLabel(text, "Word Origin:"),
            strongsExhaustive = extractSection(html, "Strong's Exhaustive Concordance", "NAS Exhaustive Concordance"),
            nasExhaustiveOrigin = extractNasField(text, "Word Origin"),
            nasExhaustiveDefinition = extractNasField(text, "Definition"),
            nasExhaustiveTranslation = extractNasField(text, "NASB Translation"),
            shortDefinition = extractAfterLabel(text, "Short Definition:"),
            lemma = extractOriginalWord(html),
            transliteration = extractAfterLabel(text, "Transliteration:"),
            partOfSpeech = extractAfterLabel(text, "Part of Speech:")
        )
    }

    private fun extractAfterLabel(text: String, label: String): String? {
        val idx = text.indexOf(label, ignoreCase = true)
        if (idx < 0) return null
        val start = idx + label.length
        val rest = text.substring(start).trim()

        // Find the next known label as boundary instead of newline
        val nextBoundary = BOUNDARY_LABELS
            .filter { it != label }
            .mapNotNull { boundary ->
                val bIdx = rest.indexOf(boundary, ignoreCase = true)
                if (bIdx > 0) bIdx else null
            }
            .minOrNull() ?: minOf(rest.length, 500)

        val value = rest.substring(0, minOf(nextBoundary, 500)).trim()
            .removeSuffix(".")
            .trim()
        return value.takeIf { it.isNotBlank() && it.length > 1 }
    }

    private fun extractOriginalWord(html: String): String? {
        // BibleHub has the original word in <span class="heb"> or <span class="grk">
        val pattern = Regex("""<span class="(?:heb|grk)"[^>]*>([^<]+)</span>""")
        return pattern.find(html)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun extractKjvTranslation(text: String): String? {
        // Look for "KJV:" pattern
        val pattern = Regex("KJV:\\s*(.+?)(?=\\s+(?:NASB|NAS|Word Origin|Strong|$))", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return null
        return match.groupValues[1].trim().takeIf { it.isNotBlank() }
    }

    private fun extractKjvUsageCount(text: String): Int? {
        // Look for patterns like "4 Occurrences" or "(169x)"
        val pattern = Regex("(\\d+)\\s*(?:Occurrence|x\\b)", RegexOption.IGNORE_CASE)
        val match = pattern.find(text) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun extractSection(html: String, startTitle: String, endTitle: String?): String? {
        val startIdx = html.indexOf(startTitle, ignoreCase = true)
        if (startIdx < 0) return null

        val endIdx = if (endTitle != null) {
            val e = html.indexOf(endTitle, startIdx + startTitle.length, ignoreCase = true)
            if (e < 0) html.length else e
        } else {
            html.length
        }

        val section = html.substring(startIdx + startTitle.length, endIdx)
        val cleaned = section
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(2000)
        return cleaned.takeIf { it.isNotBlank() && it.length > 5 }
    }

    private fun extractNasField(text: String, fieldName: String): String? {
        // In NAS Exhaustive Concordance section, look for field patterns
        val nasIdx = text.indexOf("NAS Exhaustive Concordance", ignoreCase = true)
        if (nasIdx < 0) return null

        val nasText = text.substring(nasIdx)
        val fieldIdx = nasText.indexOf(fieldName, ignoreCase = true)
        if (fieldIdx < 0) return null

        val start = fieldIdx + fieldName.length
        val rest = nasText.substring(start).trim()
        // Take content until next field or section
        val nextField = listOf("Word Origin", "Definition", "NASB Translation", "Forms and Transliterations", "Englishman")
            .filter { it != fieldName }
            .mapNotNull { f ->
                val idx = rest.indexOf(f, ignoreCase = true)
                if (idx > 0) idx else null
            }
            .minOrNull() ?: minOf(rest.length, 500)

        val value = rest.substring(0, nextField).trim()
        return value.takeIf { it.isNotBlank() && it.length > 1 }
    }
}
