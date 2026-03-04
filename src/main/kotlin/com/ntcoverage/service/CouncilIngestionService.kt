package com.ntcoverage.service

import com.ntcoverage.config.SourceFileCache
import com.ntcoverage.model.CouncilFathers
import com.ntcoverage.model.Councils
import com.ntcoverage.repository.*
import com.ntcoverage.scraper.CouncilSourceExtractor
import com.ntcoverage.seed.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class CouncilIngestionService(
    private val councilRepository: CouncilRepository,
    private val heresyRepository: HeresyRepository,
    private val canonRepository: CouncilCanonRepository,
    private val sourceRepository: SourceRepository,
    private val claimRepository: CouncilSourceClaimRepository,
    private val churchFatherRepository: ChurchFatherRepository,
    private val extractors: List<CouncilSourceExtractor>,
    private val consensusEngine: SourceConsensusEngine,
    private val summarizationService: BiographySummarizationService,
    private val phaseTracker: CouncilPhaseTracker,
    private val fileCache: SourceFileCache
) {
    private val log = LoggerFactory.getLogger(CouncilIngestionService::class.java)

    suspend fun phase1Seed() = runPhaseTracked("council_seed", runBy = "manual") {
        log.info("COUNCIL_SEED: inserting {} sources", SourcesSeedData.entries.size)
        val sourceTotal = SourcesSeedData.entries.size
        var processed = 0
        SourcesSeedData.entries.forEach { source ->
            sourceRepository.insertOrUpdate(source)
            processed++
            phaseTracker.markProgress("council_seed", processed, sourceTotal)
        }
        log.info("COUNCIL_SEED: sources done ({})", processed)

        log.info("COUNCIL_SEED: inserting {} heresies", HeresiesSeedData.entries.size)
        HeresiesSeedData.entries.forEach { heresyRepository.insertOrUpdate(it) }
        HeresyTranslationsSeedData.entries.forEach { heresyRepository.insertOrUpdateTranslation(it) }
        log.info("COUNCIL_SEED: heresies + translations done")

        val seedSourceId = sourceRepository.findIdByName("seed")
        log.info("COUNCIL_SEED: inserting {} councils (seedSourceId={})", CouncilsSeedData.entries.size, seedSourceId)
        CouncilsSeedData.entries.forEachIndexed { idx, council ->
            try {
                val councilId = councilRepository.insertOrUpdate(council)

                council.heresyNames.forEach { heresyName ->
                    val normalized = normalizeHeresyName(heresyName)
                    val heresyId = heresyRepository.findByNormalizedName(normalized)
                    if (heresyId != null) {
                        heresyRepository.linkCouncilHeresy(councilId, heresyId, action = "condemned")
                    }
                }

                council.fatherNames.forEach { fatherName ->
                    val father = churchFatherRepository.findByNormalizedName(fatherName)
                    if (father != null) {
                        linkCouncilFather(councilId, father.id)
                    }
                }

                if (seedSourceId != null) {
                    claimRepository.upsertClaim(
                        councilId = councilId,
                        sourceId = seedSourceId,
                        claimedYear = council.year,
                        claimedYearEnd = council.yearEnd,
                        claimedLocation = council.location,
                        claimedParticipants = council.numberOfParticipants,
                        rawText = council.shortDescription,
                        sourcePage = "seed"
                    )
                }

                if ((idx + 1) % 20 == 0 || idx == CouncilsSeedData.entries.size - 1) {
                    log.info("COUNCIL_SEED: progress {}/{} (last: {})", idx + 1, CouncilsSeedData.entries.size, council.displayName)
                }
                phaseTracker.markProgress("council_seed", idx + 1, CouncilsSeedData.entries.size)
            } catch (e: Exception) {
                log.error("COUNCIL_SEED: failed on council '{}' (year={}): {}", council.displayName, council.year, e.message)
                throw e
            }
        }

        log.info("COUNCIL_SEED: inserting {} translations", CouncilTranslationsSeedData.entries.size)
        var translationsApplied = 0
        CouncilTranslationsSeedData.entries.forEach { translation ->
            val councilId = councilRepository.findIdBySlug(translation.councilSlug)
            if (councilId == null) {
                log.warn("COUNCIL_SEED: translation slug not found: '{}' (locale={})", translation.councilSlug, translation.locale)
                return@forEach
            }
            councilRepository.insertOrUpdateTranslation(
                councilId = councilId,
                locale = translation.locale,
                displayName = translation.displayName,
                shortDescription = translation.shortDescription,
                location = translation.location,
                mainTopics = translation.mainTopics,
                summary = translation.summary,
                translationSource = "seed"
            )
            translationsApplied++
        }
        log.info("COUNCIL_SEED: translations applied={} of {} total entries", translationsApplied, CouncilTranslationsSeedData.entries.size)
    }

    suspend fun phase2aSchaff() = processExtractorPhase("council_extract_schaff", "schaff")
    suspend fun phase2bHefele() = processExtractorPhase("council_extract_hefele", "hefele")
    suspend fun phase2cCatholicEnc() = processExtractorPhase("council_extract_catholic_enc", "catholic_encyclopedia")
    suspend fun phase2dFordham() = processExtractorPhase("council_extract_fordham", "fordham")
    suspend fun phase3Wikidata() = processExtractorPhase("council_extract_wikidata", "wikidata")
    suspend fun phase4Wikipedia() = processExtractorPhase("council_extract_wikipedia", "wikipedia", saveOriginalText = true)

    suspend fun phase5Consensus() = runPhaseTracked("council_consensus", runBy = "manual") {
        val councilRows = transaction {
            Councils.selectAll().map { it[Councils.id].value to it[Councils.displayName] }
        }
        log.info("COUNCIL_CONSENSUS: calculating consensus for {} councils", councilRows.size)
        councilRows.forEachIndexed { idx, (councilId, councilName) ->
            val result = consensusEngine.calculateConsensus(councilId, councilName)
            councilRepository.updateConsensus(
                councilId = councilId,
                year = result.consensusYear,
                yearEnd = result.consensusYearEnd,
                location = result.consensusLocation,
                participants = result.consensusParticipants,
                confidence = result.confidenceScore,
                dataConfidence = result.dataConfidence,
                sourceCount = result.sourceCount,
                conflictResolution = result.conflictResolution
            )
            if ((idx + 1) % 25 == 0 || idx == councilRows.size - 1) {
                log.info("COUNCIL_CONSENSUS: progress {}/{} (last: {} confidence={})", idx + 1, councilRows.size, councilName, String.format("%.2f", result.confidenceScore))
            }
            phaseTracker.markProgress("council_consensus", idx + 1, councilRows.size)
        }

        log.info("COUNCIL_CONSENSUS: updating source reliability scores")
        sourceRepository.findAll().forEach { source ->
            consensusEngine.updateSourceReliability(source.id)
        }
        log.info("COUNCIL_CONSENSUS: done")
    }

    suspend fun phase6Summaries(limit: Int = 20) = runPhaseTracked("council_summaries", runBy = "manual") {
        val rows = transaction {
            Councils.selectAll()
                .where { Councils.summary.isNull() and Councils.originalText.isNotNull() }
                .limit(limit)
                .map { row ->
                    SummaryTarget(
                        id = row[Councils.id].value,
                        displayName = row[Councils.displayName],
                        originalText = row[Councils.originalText]
                    )
                }
        }
        log.info("COUNCIL_SUMMARIES: {} councils need summarization (limit={})", rows.size, limit)

        rows.forEachIndexed { idx, target ->
            val original = target.originalText ?: return@forEachIndexed
            log.info("COUNCIL_SUMMARIES: summarizing '{}'", target.displayName)
            val summary = summarizationService.summarizeIfNeeded(original)
            if (!summary.isNullOrBlank()) {
                councilRepository.updateSummary(target.id, summary, reviewed = false)
                listOf("pt", "es").forEach { locale ->
                    val translated = summarizationService.translateBiography(summary, locale, target.displayName)
                    if (!translated.isNullOrBlank()) {
                        councilRepository.insertOrUpdateTranslation(
                            councilId = target.id,
                            locale = locale,
                            displayName = councilRepository.findById(target.id, "en")?.displayName ?: target.displayName,
                            summary = translated,
                            translationSource = "machine"
                        )
                    }
                }
                log.info("COUNCIL_SUMMARIES: done '{}'", target.displayName)
            } else {
                log.warn("COUNCIL_SUMMARIES: empty summary for '{}'", target.displayName)
            }
            phaseTracker.markProgress("council_summaries", idx + 1, rows.size)
        }
        log.info("COUNCIL_SUMMARIES: finished {} councils", rows.size)
    }

    suspend fun runPhases(phases: List<String>) {
        val runStartedAt = System.currentTimeMillis()
        log.info("COUNCIL_INGESTION: starting {} phases: {}", phases.size, phases)
        for ((idx, phase) in phases.withIndex()) {
            val phaseStartedAt = System.currentTimeMillis()
            log.info("COUNCIL_INGESTION: >>> phase {}/{} '{}' START", idx + 1, phases.size, phase)
            when (phase) {
                "council_seed" -> phase1Seed()
                "council_extract_schaff" -> phase2aSchaff()
                "council_extract_hefele" -> phase2bHefele()
                "council_extract_catholic_enc" -> phase2cCatholicEnc()
                "council_extract_fordham" -> phase2dFordham()
                "council_extract_wikidata" -> phase3Wikidata()
                "council_extract_wikipedia" -> phase4Wikipedia()
                "council_consensus" -> phase5Consensus()
                "council_summaries" -> phase6Summaries()
            }
            val phaseElapsed = System.currentTimeMillis() - phaseStartedAt
            val pct = ((idx + 1) * 100) / phases.size
            log.info(
                "COUNCIL_INGESTION: <<< phase {}/{} '{}' END elapsed={} overallProgress={}%",
                idx + 1, phases.size, phase, formatDuration(phaseElapsed), pct
            )
        }
        val totalElapsed = System.currentTimeMillis() - runStartedAt
        log.info("COUNCIL_INGESTION: all {} phases completed in {}", phases.size, formatDuration(totalElapsed))
    }

    suspend fun fullIngestion() = runPhases(ALL_PHASES)

    fun getCacheStats() = fileCache.getStats()

    private suspend fun processExtractorPhase(
        phaseName: String,
        sourceName: String,
        saveOriginalText: Boolean = false
    ) = runPhaseTracked(phaseName, runBy = "manual") {
        val extractor = extractors.firstOrNull { it.sourceName == sourceName }
        if (extractor == null) {
            log.error("COUNCIL_EXTRACT: no extractor found for source '{}'", sourceName)
            return@runPhaseTracked
        }
        val sourceId = sourceRepository.findIdByName(sourceName)
        if (sourceId == null) {
            log.error("COUNCIL_EXTRACT: source '{}' not found in DB", sourceName)
            return@runPhaseTracked
        }

        log.info("COUNCIL_EXTRACT: [{}] starting extraction", sourceName)
        val extractionStartedAt = System.currentTimeMillis()
        val claims = extractor.extract()
        val extractionElapsed = System.currentTimeMillis() - extractionStartedAt
        log.info(
            "COUNCIL_EXTRACT: [{}] got {} claims in {}, matching to councils",
            sourceName, claims.size, formatDuration(extractionElapsed)
        )

        var matched = 0
        var unmatched = 0
        val matchingStartedAt = System.currentTimeMillis()
        claims.forEachIndexed { idx, claim ->
            val councilId = resolveCouncilIdForClaim(claim)
            if (councilId != null) {
                claimRepository.upsertClaim(
                    councilId = councilId,
                    sourceId = sourceId,
                    claimedYear = claim.claimedYear,
                    claimedYearEnd = claim.claimedYearEnd,
                    claimedLocation = claim.claimedLocation,
                    claimedParticipants = claim.claimedParticipants,
                    rawText = claim.rawText,
                    sourcePage = claim.sourcePage
                )
                if (saveOriginalText && !claim.rawText.isNullOrBlank()) {
                    councilRepository.updateOriginalText(councilId, claim.rawText)
                }
                matched++
            } else {
                log.debug("COUNCIL_EXTRACT: [{}] unmatched claim: '{}' (key={})", sourceName, claim.councilNameRaw, claim.normalizedKey)
                unmatched++
            }
            phaseTracker.markProgress(phaseName, idx + 1, claims.size)
            if ((idx + 1) % 10 == 0 || idx == claims.lastIndex) {
                val pct = if (claims.isNotEmpty()) ((idx + 1) * 100) / claims.size else 100
                log.info(
                    "COUNCIL_EXTRACT: [{}] match progress {}/{} ({}%) matched={} unmatched={}",
                    sourceName, idx + 1, claims.size, pct, matched, unmatched
                )
            }
        }
        val matchingElapsed = System.currentTimeMillis() - matchingStartedAt
        log.info(
            "COUNCIL_EXTRACT: [{}] done — matched={}, unmatched={}, total={}, matchingElapsed={}",
            sourceName, matched, unmatched, claims.size, formatDuration(matchingElapsed)
        )
    }

    private suspend fun runPhaseTracked(
        phaseName: String,
        runBy: String = "manual",
        block: suspend () -> Unit
    ) {
        val phaseStartedAt = System.currentTimeMillis()
        log.info("PHASE_TRACKER: '{}' → RUNNING (by={})", phaseName, runBy)
        phaseTracker.markRunning(phaseName, runBy = runBy)
        try {
            block()
            val status = phaseTracker.getPhaseStatus(phaseName)
            phaseTracker.markSuccess(phaseName, status?.itemsProcessed ?: 0)
            val elapsed = System.currentTimeMillis() - phaseStartedAt
            log.info(
                "PHASE_TRACKER: '{}' → SUCCESS (items={} elapsed={})",
                phaseName,
                status?.itemsProcessed ?: 0,
                formatDuration(elapsed)
            )
        } catch (e: Throwable) {
            phaseTracker.markFailed(phaseName, e.message ?: "Unknown error")
            val elapsed = System.currentTimeMillis() - phaseStartedAt
            log.error("PHASE_TRACKER: '{}' → FAILED after {}: {}", phaseName, formatDuration(elapsed), e.message, e)
            throw e
        }
    }

    private fun resolveCouncilIdForClaim(claim: com.ntcoverage.scraper.ExtractedCouncilClaim): Int? {
        val byKey = councilRepository.findIdByMatchKey(claim.normalizedKey)
        if (byKey != null) return byKey

        val year = claim.claimedYear ?: return null
        return councilRepository.findIdByNameAndYear(claim.councilNameRaw, year)
    }

    private fun normalizeHeresyName(name: String): String =
        name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

    private fun linkCouncilFather(councilId: Int, fatherId: Int) {
        transaction {
            val exists = CouncilFathers.selectAll().where {
                (CouncilFathers.councilId eq councilId) and (CouncilFathers.fatherId eq fatherId)
            }.count() > 0
            if (!exists) {
                CouncilFathers.insert {
                    it[CouncilFathers.councilId] = councilId
                    it[CouncilFathers.fatherId] = fatherId
                    it[role] = "attended"
                }
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes}m ${seconds}s"
    }

    private data class SummaryTarget(
        val id: Int,
        val displayName: String,
        val originalText: String?
    )

    companion object {
        val ALL_PHASES = listOf(
            "council_seed",
            "council_extract_schaff",
            "council_extract_hefele",
            "council_extract_catholic_enc",
            "council_extract_fordham",
            "council_extract_wikidata",
            "council_extract_wikipedia",
            "council_consensus",
            "council_summaries"
        )
    }
}
