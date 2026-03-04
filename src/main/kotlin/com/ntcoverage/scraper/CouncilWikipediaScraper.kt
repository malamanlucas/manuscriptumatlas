package com.ntcoverage.scraper

import com.ntcoverage.config.SourceFileCache
import com.ntcoverage.seed.CouncilsSeedData
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

class CouncilWikipediaScraper(
    private val fileCache: SourceFileCache
) : CouncilSourceExtractor {
    override val sourceName: String = "wikipedia"
    private val log = LoggerFactory.getLogger(CouncilWikipediaScraper::class.java)

    override suspend fun extract(): List<ExtractedCouncilClaim> {
        log.info("COUNCIL_WIKIPEDIA_SCRAPER: starting extraction")
        val claims = mutableListOf<ExtractedCouncilClaim>()
        val councils = CouncilsSeedData.entries.filter { !it.wikipediaUrl.isNullOrBlank() }
        log.info("COUNCIL_WIKIPEDIA_SCRAPER: {} seed councils with wikipedia URL", councils.size)

        for ((idx, entry) in councils.withIndex()) {
            val url = entry.wikipediaUrl ?: continue
            val pageSlug = url.substringAfterLast("/")
            val cacheKey = "wikipedia/$pageSlug.html"
            log.info(
                "COUNCIL_WIKIPEDIA_SCRAPER: page {}/{} slug={} council={}",
                idx + 1, councils.size, pageSlug, entry.displayName
            )
            val html = fileCache.getOrFetch(cacheKey) {
                log.info("COUNCIL_WIKIPEDIA_SCRAPER: downloading {}", url)
                Jsoup.connect(url)
                    .userAgent("ManuscriptumAtlasBot/1.0 (historical research)")
                    .timeout(20_000)
                    .ignoreContentType(true)
                    .execute()
                    .body()
            }
            log.info("COUNCIL_WIKIPEDIA_SCRAPER: html length={} chars (cacheKey={})", html.length, cacheKey)

            val doc = Jsoup.parse(html)
            val text = doc.select("#mw-content-text p")
                .filterNot { it.select("sup.reference, span.mw-editsection").isNotEmpty() }
                .joinToString("\n") { it.text() }
                .ifBlank { doc.text() }
                .take(12_000)

            claims += ExtractedCouncilClaim(
                councilNameRaw = entry.displayName,
                normalizedKey = CouncilNameNormalizer.matchKey(entry.displayName, entry.year),
                claimedYear = entry.year,
                claimedYearEnd = entry.yearEnd,
                claimedLocation = entry.location,
                claimedParticipants = entry.numberOfParticipants,
                rawText = text.take(5_000),
                sourcePage = url
            )

            val pct = ((idx + 1) * 100) / councils.size
            log.info(
                "COUNCIL_WIKIPEDIA_SCRAPER: progress {}/{} ({}%) last='{}'",
                idx + 1, councils.size, pct, entry.displayName
            )
        }

        log.info("COUNCIL_WIKIPEDIA_SCRAPER: extracted {} claims", claims.size)
        return claims
    }
}
