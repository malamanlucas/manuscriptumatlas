package com.ntcoverage.scraper

import com.ntcoverage.config.SourceFileCache
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

class FordhamExtractor(
    private val fileCache: SourceFileCache,
    private val academicTextExtractor: AcademicTextExtractor
) : CouncilSourceExtractor {
    override val sourceName: String = "fordham"
    private val log = LoggerFactory.getLogger(FordhamExtractor::class.java)

    private val documents: Map<String, String> = mapOf(
        "first-council-of-nicaea-325" to "https://sourcebooks.fordham.edu/basis/nicea1.txt",
        "second-council-of-nicaea-787" to "https://sourcebooks.fordham.edu/basis/nicea2.asp",
        "council-of-chalcedon-451" to "https://sourcebooks.fordham.edu/basis/chalcedon.asp",
        "council-of-ephesus-431" to "https://sourcebooks.fordham.edu/basis/ephesus.txt",
        "third-council-of-toledo-589" to "https://sourcebooks.fordham.edu/source/toledo3.asp"
    )

    override suspend fun extract(): List<ExtractedCouncilClaim> {
        log.info("FORDHAM_EXTRACTOR: starting extraction for {} documents", documents.size)
        val claims = mutableListOf<ExtractedCouncilClaim>()

        for ((idx, item) in documents.entries.withIndex()) {
            val slug = item.key
            val url = item.value
            log.info("FORDHAM_EXTRACTOR: document {}/{} slug={} url={}", idx + 1, documents.size, slug, url)
            try {
                val cacheKey = "fordham/${url.substringAfterLast("/")}"
                val body = fileCache.getOrFetch(cacheKey) {
                    log.info("FORDHAM_EXTRACTOR: downloading {}", url)
                    Jsoup.connect(url)
                        .userAgent("ManuscriptumAtlasBot/1.0 (historical research)")
                        .timeout(20_000)
                        .ignoreContentType(true)
                        .execute()
                        .body()
                }
                log.info("FORDHAM_EXTRACTOR: raw body length={} chars (cacheKey={})", body.length, cacheKey)

                val doc = Jsoup.parse(body)
                val text = if (body.trimStart().startsWith("<")) {
                    doc.select("p, pre, blockquote, body")
                        .joinToString("\n") { it.text() }
                        .take(10_000)
                } else {
                    body.take(10_000)
                }

                val extracted = academicTextExtractor.extractCouncilData(text, "Fordham Sourcebook")
                val fallbackName = slug.substringBeforeLast("-").replace('-', ' ')
                val year = extracted?.yearStart ?: slug.substringAfterLast("-").toIntOrNull()
                val name = extracted?.councilName ?: fallbackName

                claims += ExtractedCouncilClaim(
                    councilNameRaw = name,
                    normalizedKey = year?.let { CouncilNameNormalizer.matchKey(name, it) }
                        ?: CouncilNameNormalizer.normalize(name),
                    claimedYear = year,
                    claimedYearEnd = extracted?.yearEnd,
                    claimedLocation = extracted?.location,
                    claimedParticipants = extracted?.participantsCount,
                    rawText = text.take(5_000),
                    sourcePage = url
                )

                val pct = ((idx + 1) * 100) / documents.size
                log.info(
                    "FORDHAM_EXTRACTOR: progress {}/{} ({}%) last='{}' year={}",
                    idx + 1, documents.size, pct, name, year
                )
            } catch (e: Exception) {
                log.warn("FORDHAM_EXTRACTOR: skipping slug={} url={} — {}: {}", slug, url, e.javaClass.simpleName, e.message)
            }
        }

        log.info("FORDHAM_EXTRACTOR: extracted {} claims", claims.size)
        return claims
    }
}
