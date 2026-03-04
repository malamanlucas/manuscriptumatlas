package com.ntcoverage.scraper

import com.ntcoverage.config.SourceFileCache
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.slf4j.LoggerFactory

class HefeleExtractor(
    private val fileCache: SourceFileCache,
    private val academicTextExtractor: AcademicTextExtractor
) : CouncilSourceExtractor {
    override val sourceName: String = "hefele"
    private val log = LoggerFactory.getLogger(HefeleExtractor::class.java)

    private val client = HttpClient(CIO) {
        engine { requestTimeout = 45_000 }
    }

    private val volumes = listOf(
        ArchiveVolume("historyofcouncil01hefeuoft", "hefele/vol1_djvu.txt"),
        ArchiveVolume("historyofcouncil02hefeuoft", "hefele/vol2_djvu.txt"),
        ArchiveVolume("historyofcouncil03hefeuoft", "hefele/vol3_djvu.txt"),
        ArchiveVolume("historyofcouncil04hefeuoft", "hefele/vol4_djvu.txt"),
        ArchiveVolume("historyofcouncil05hefeuoft", "hefele/vol5_djvu.txt")
    )

    override suspend fun extract(): List<ExtractedCouncilClaim> {
        val claims = mutableListOf<ExtractedCouncilClaim>()
        log.info("HEFELE_EXTRACTOR: starting extraction for {} volumes", volumes.size)

        for ((volumeIndex, volume) in volumes.withIndex()) {
            log.info(
                "HEFELE_EXTRACTOR: volume {}/{} [{}] cacheKey={}",
                volumeIndex + 1,
                volumes.size,
                volume.archiveId,
                volume.cacheKey
            )
            val text = fileCache.getOrFetch(volume.cacheKey) {
                log.info("HEFELE_EXTRACTOR: downloading archive text for {}", volume.archiveId)
                downloadArchiveText(volume.archiveId)
            }
            log.info("HEFELE_EXTRACTOR: volume {} text length={} chars", volume.archiveId, text.length)

            val chunks = extractCouncilChunks(text)
            log.info(
                "HEFELE_EXTRACTOR: volume {} generated {} candidate chunks",
                volume.archiveId,
                chunks.size
            )
            for ((idx, chunk) in chunks.withIndex()) {
                val data = academicTextExtractor.extractCouncilData(chunk, "Hefele ${volume.archiveId}")
                val year = data?.yearStart
                val rawName = data?.councilName ?: inferCouncilName(chunk) ?: "Unknown Council ${volume.archiveId}#${idx + 1}"
                val normalizedKey = year?.let { CouncilNameNormalizer.matchKey(rawName, it) }
                    ?: CouncilNameNormalizer.normalize(rawName)

                claims += ExtractedCouncilClaim(
                    councilNameRaw = rawName,
                    normalizedKey = normalizedKey,
                    claimedYear = year,
                    claimedYearEnd = data?.yearEnd,
                    claimedLocation = data?.location,
                    claimedParticipants = data?.participantsCount,
                    rawText = chunk.take(5_000),
                    sourcePage = "${volume.archiveId}#chunk-${idx + 1}"
                )

                if ((idx + 1) % 10 == 0 || idx == chunks.lastIndex) {
                    val pct = if (chunks.isNotEmpty()) ((idx + 1) * 100) / chunks.size else 100
                    log.info(
                        "HEFELE_EXTRACTOR: volume {} progress {}/{} ({}%) totalClaimsSoFar={}",
                        volume.archiveId,
                        idx + 1,
                        chunks.size,
                        pct,
                        claims.size
                    )
                }
            }
        }

        log.info("HEFELE_EXTRACTOR: extracted {} claims", claims.size)
        return claims
    }

    private suspend fun downloadArchiveText(archiveId: String): String {
        val url = "https://archive.org/download/$archiveId/${archiveId}_djvu.txt"
        val response: HttpResponse = client.get(url) {
            headers.append("User-Agent", "ManuscriptumAtlasBot/1.0 (historical research)")
        }
        return response.bodyAsText()
    }

    private fun extractCouncilChunks(text: String): List<String> {
        val matches = Regex("(?i)(council|synod)\\s+of\\s+[A-Za-z\\-\\s]{3,80}").findAll(text).toList()
        if (matches.isEmpty()) return emptyList()

        return matches.take(120).map { match ->
            val start = (match.range.first - 1200).coerceAtLeast(0)
            val end = (match.range.last + 5200).coerceAtMost(text.length)
            text.substring(start, end)
        }
    }

    private fun inferCouncilName(text: String): String? =
        Regex("(?i)(council|synod)\\s+of\\s+([A-Za-z\\-\\s]{3,80})")
            .find(text)
            ?.value
            ?.replace("\n", " ")
            ?.trim()

    private data class ArchiveVolume(
        val archiveId: String,
        val cacheKey: String
    )
}
