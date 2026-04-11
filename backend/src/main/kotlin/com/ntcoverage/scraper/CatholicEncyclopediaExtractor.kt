package com.ntcoverage.scraper

import com.ntcoverage.config.SourceFileCache
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

class CatholicEncyclopediaExtractor(
    private val fileCache: SourceFileCache,
    private val academicTextExtractor: AcademicTextExtractor
) : CouncilSourceExtractor {
    override val sourceName: String = "catholic_encyclopedia"
    private val log = LoggerFactory.getLogger(CatholicEncyclopediaExtractor::class.java)

    private val councilArticles: Map<String, String> = mapOf(
        "first-council-of-nicaea-325" to "https://www.newadvent.org/cathen/11044a.htm",
        "first-council-of-constantinople-381" to "https://www.newadvent.org/cathen/04308a.htm",
        "council-of-ephesus-431" to "https://www.newadvent.org/cathen/05491a.htm",
        "council-of-chalcedon-451" to "https://www.newadvent.org/cathen/03555b.htm",
        "second-council-of-constantinople-553" to "https://www.newadvent.org/cathen/04311a.htm",
        "third-council-of-constantinople-680" to "https://www.newadvent.org/cathen/04311b.htm",
        "second-council-of-nicaea-787" to "https://www.newadvent.org/cathen/11045a.htm",
        "fourth-council-of-constantinople-869" to "https://www.newadvent.org/cathen/04312a.htm",
        "synod-of-elvira-306" to "https://www.newadvent.org/cathen/05388a.htm",
        "council-of-sardica-343" to "https://www.newadvent.org/cathen/13488a.htm",
        "council-of-orange-ii-529" to "https://www.newadvent.org/cathen/11336b.htm",
        "third-council-of-toledo-589" to "https://www.newadvent.org/cathen/14718a.htm"
    )

    override suspend fun extract(): List<ExtractedCouncilClaim> {
        log.info("CATHOLIC_ENC_EXTRACTOR: starting extraction for {} articles", councilArticles.size)
        val claims = mutableListOf<ExtractedCouncilClaim>()

        for ((idx, item) in councilArticles.entries.withIndex()) {
            val slug = item.key
            val url = item.value
            log.info(
                "CATHOLIC_ENC_EXTRACTOR: article {}/{} slug={} url={}",
                idx + 1, councilArticles.size, slug, url
            )
            val cacheKey = "catholic_encyclopedia/${url.substringAfterLast("/")}"
            val html = fileCache.getOrFetch(cacheKey) {
                log.info("CATHOLIC_ENC_EXTRACTOR: downloading {}", url)
                Jsoup.connect(url)
                    .userAgent("ManuscriptumAtlasBot/1.0 (historical research)")
                    .timeout(20_000)
                    .ignoreContentType(true)
                    .execute()
                    .body()
            }
            log.info("CATHOLIC_ENC_EXTRACTOR: html length={} chars (cacheKey={})", html.length, cacheKey)

            val doc = Jsoup.parse(html)
            val text = doc.select("p")
                .joinToString("\n") { it.text().trim() }
                .ifBlank { doc.text() }
                .take(10_000)

            val data = academicTextExtractor.extractCouncilData(text, "Catholic Encyclopedia")
            val nameFromSlug = slug.substringBeforeLast("-").replace('-', ' ')
            val year = data?.yearStart ?: slug.substringAfterLast("-").toIntOrNull()
            val name = data?.councilName ?: nameFromSlug
            val normalizedKey = year?.let { CouncilNameNormalizer.matchKey(name, it) }
                ?: CouncilNameNormalizer.normalize(name)

            claims += ExtractedCouncilClaim(
                councilNameRaw = name,
                normalizedKey = normalizedKey,
                claimedYear = year,
                claimedYearEnd = data?.yearEnd,
                claimedLocation = data?.location,
                claimedParticipants = data?.participantsCount,
                rawText = text.take(5_000),
                sourcePage = url
            )

            val pct = ((idx + 1) * 100) / councilArticles.size
            log.info(
                "CATHOLIC_ENC_EXTRACTOR: progress {}/{} ({}%) last='{}' year={}",
                idx + 1, councilArticles.size, pct, name, year
            )
        }

        log.info("CATHOLIC_ENC_EXTRACTOR: extracted {} claims", claims.size)
        return claims
    }
}
