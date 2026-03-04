package com.ntcoverage.scraper

import com.ntcoverage.config.SourceFileCache
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.slf4j.LoggerFactory

class SchaffExtractor(
    private val fileCache: SourceFileCache,
    private val academicTextExtractor: AcademicTextExtractor
) : CouncilSourceExtractor {
    override val sourceName: String = "schaff"
    private val log = LoggerFactory.getLogger(SchaffExtractor::class.java)

    private val client = HttpClient(CIO) {
        engine { requestTimeout = 30_000 }
    }

    override suspend fun extract(): List<ExtractedCouncilClaim> {
        log.info("SCHAFF_EXTRACTOR: starting extraction")
        val text = fileCache.getOrFetch("schaff/npnf214.txt") {
            log.info("SCHAFF_EXTRACTOR: downloading source text")
            downloadText("https://ccel.org/ccel/s/schaff/npnf214/cache/npnf214.txt")
        }
        log.info("SCHAFF_EXTRACTOR: source text length={} chars", text.length)

        val sections = splitSections(text)
        log.info("SCHAFF_EXTRACTOR: identified {} candidate sections", sections.size)
        val claims = mutableListOf<ExtractedCouncilClaim>()

        for ((idx, section) in sections.withIndex()) {
            val structured = academicTextExtractor.extractCouncilData(section.content, "Schaff/Percival NPNF2-14")
            val year = structured?.yearStart ?: section.estimatedYear
            val normalizedKey = year?.let { CouncilNameNormalizer.matchKey(section.title, it) }
                ?: CouncilNameNormalizer.normalize(section.title)

            claims += ExtractedCouncilClaim(
                councilNameRaw = structured?.councilName ?: section.title,
                normalizedKey = normalizedKey,
                claimedYear = year,
                claimedYearEnd = structured?.yearEnd,
                claimedLocation = structured?.location,
                claimedParticipants = structured?.participantsCount,
                rawText = section.content.take(5_000),
                sourcePage = section.anchor
            )
            val pct = if (sections.isNotEmpty()) ((idx + 1) * 100) / sections.size else 100
            log.info(
                "SCHAFF_EXTRACTOR: progress {}/{} ({}%) last='{}' year={}",
                idx + 1, sections.size, pct, section.title, year
            )
        }

        log.info("SCHAFF_EXTRACTOR: extracted {} claims", claims.size)
        return claims
    }

    private suspend fun downloadText(url: String): String {
        val response: HttpResponse = client.get(url) {
            headers.append("User-Agent", "ManuscriptumAtlasBot/1.0 (historical research)")
        }
        return response.bodyAsText()
    }

    private fun splitSections(text: String): List<SchaffSection> {
        val candidates = listOf(
            SchaffSectionCandidate("First Council of Nicaea", 325, Regex("(?i)first\\s+ecumenical\\s+council|council\\s+of\\s+nicaea")),
            SchaffSectionCandidate("First Council of Constantinople", 381, Regex("(?i)council\\s+of\\s+constantinople")),
            SchaffSectionCandidate("Council of Ephesus", 431, Regex("(?i)council\\s+of\\s+ephesus")),
            SchaffSectionCandidate("Council of Chalcedon", 451, Regex("(?i)council\\s+of\\s+chalcedon")),
            SchaffSectionCandidate("Second Council of Constantinople", 553, Regex("(?i)second\\s+council\\s+of\\s+constantinople")),
            SchaffSectionCandidate("Third Council of Constantinople", 680, Regex("(?i)third\\s+council\\s+of\\s+constantinople")),
            SchaffSectionCandidate("Second Council of Nicaea", 787, Regex("(?i)second\\s+council\\s+of\\s+nicaea"))
        )

        val sections = mutableListOf<SchaffSection>()
        for (candidate in candidates) {
            val match = candidate.pattern.find(text) ?: continue
            val start = (match.range.first - 1500).coerceAtLeast(0)
            val end = (match.range.last + 6500).coerceAtMost(text.length)
            sections += SchaffSection(
                title = candidate.title,
                estimatedYear = candidate.year,
                anchor = "${candidate.title}#${match.range.first}",
                content = text.substring(start, end)
            )
        }
        return sections
    }

    private data class SchaffSectionCandidate(
        val title: String,
        val year: Int,
        val pattern: Regex
    )

    private data class SchaffSection(
        val title: String,
        val estimatedYear: Int?,
        val anchor: String,
        val content: String
    )
}
