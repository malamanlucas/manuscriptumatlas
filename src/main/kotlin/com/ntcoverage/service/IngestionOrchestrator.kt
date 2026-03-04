package com.ntcoverage.service

import com.ntcoverage.config.IngestionConfig
import com.ntcoverage.model.*
import com.ntcoverage.repository.IngestionMetadataRepository
import com.ntcoverage.repository.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes

class IngestionOrchestrator(
    private val ingestionService: IngestionService,
    private val patristicIngestionService: PatristicIngestionService,
    private val councilIngestionService: CouncilIngestionService,
    private val metadataRepository: IngestionMetadataRepository,
    private val statsRepository: StatsRepository
) {
    private val log = LoggerFactory.getLogger(IngestionOrchestrator::class.java)
    private val isRunning = AtomicBoolean(false)

    suspend fun launchIfEnabled() {
        if (!IngestionConfig.enableIngestion) {
            log.info("INGESTION_SKIPPED — ENABLE_INGESTION=false")
            return
        }

        if (IngestionConfig.skipIfPopulated) {
            val count = statsRepository.getTotalManuscripts()
            if (count > 0) {
                log.info("INGESTION_SKIPPED_ALREADY_POPULATED — $count manuscripts in database")
                return
            }
        }

        executeIngestion()
    }

    fun triggerManual(scope: CoroutineScope) {
        if (!isRunning.compareAndSet(false, true)) {
            throw IllegalStateException("Ingestion already running")
        }
        scope.launch {
            try {
                executeIngestionInner()
            } finally {
                isRunning.set(false)
            }
        }
    }

    fun isCurrentlyRunning(): Boolean = isRunning.get()

    fun resetAndReIngest(scope: CoroutineScope) {
        if (!isRunning.compareAndSet(false, true)) {
            throw IllegalStateException("Ingestion already running")
        }
        scope.launch {
            try {
                log.warn("DATABASE_RESET requested — wiping all data")
                transaction {
                    CouncilSourceClaims.deleteAll()
                    CouncilCanons.deleteAll()
                    CouncilHeresies.deleteAll()
                    CouncilFathers.deleteAll()
                    CouncilTranslations.deleteAll()
                    HeresyTranslations.deleteAll()
                    Heresies.deleteAll()
                    CouncilIngestionPhases.deleteAll()
                    Councils.deleteAll()
                    Sources.deleteAll()
                    FatherStatementTranslations.deleteAll()
                    ChurchFatherTranslations.deleteAll()
                    FatherTextualStatements.deleteAll()
                    ChurchFathers.deleteAll()
                    CoverageByCentury.deleteAll()
                    BookTranslations.deleteAll()
                    ManuscriptVerses.deleteAll()
                    ManuscriptSources.deleteAll()
                    Manuscripts.deleteAll()
                    Verses.deleteAll()
                    Books.deleteAll()
                    IngestionMetadata.deleteAll()
                }
                log.info("DATABASE_RESET complete — re-seeding canonical data")
                ingestionService.seedBooksAndVerses()
                log.info("Re-seed complete — starting fresh ingestion")
                executeIngestionInner()
            } catch (e: Throwable) {
                log.error("RESET_AND_REINGEST failed: ${e.message}", e)
            } finally {
                isRunning.set(false)
            }
        }
    }

    private suspend fun executeIngestion() {
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("INGESTION_SKIPPED — already running")
            return
        }
        try {
            executeWithRetry(maxRetries = 3)
        } finally {
            isRunning.set(false)
        }
    }

    private suspend fun executeWithRetry(maxRetries: Int) {
        var lastError: Throwable? = null
        for (attempt in 0 until maxRetries) {
            try {
                executeIngestionInner()
                return
            } catch (e: Throwable) {
                lastError = e
                if (attempt < maxRetries - 1) {
                    val delayMs = 2000L * (1 shl attempt)
                    log.warn("INGESTION_FAILED attempt ${attempt + 1}/$maxRetries, retrying in ${delayMs}ms: ${e.message}")
                    delay(delayMs)
                }
            }
        }
        log.error("INGESTION_FAILED after $maxRetries attempts", lastError)
    }

    private suspend fun executeIngestionInner() {
        log.info("INGESTION_STARTED (manuscripts=${IngestionConfig.enableManuscriptIngestion}, patristic=${IngestionConfig.enablePatristicIngestion})")
        metadataRepository.markRunning()
        val startTime = System.currentTimeMillis()

        try {
            var manuscriptsIngested = 0
            var versesLinked = 0

            if (IngestionConfig.enableManuscriptIngestion) {
                val timeoutMinutes = IngestionConfig.timeoutMinutes
                val result = withTimeout(timeoutMinutes.minutes) {
                    ingestionService.ingestManuscriptsAsync()
                }

                withTimeout(timeoutMinutes.minutes) {
                    ingestionService.materializeCoverageAsync()
                }

                manuscriptsIngested = result.manuscriptsIngested
                versesLinked = result.versesLinked
            } else {
                log.info("MANUSCRIPT_INGESTION_SKIPPED — ENABLE_MANUSCRIPT_INGESTION=false")
            }

            if (IngestionConfig.enablePatristicIngestion) {
                val patristicStart = System.currentTimeMillis()
                val patristicCount = patristicIngestionService.ingestFromSeed()
                val patristicDuration = System.currentTimeMillis() - patristicStart
                log.info("PATRISTIC_INGESTION: $patristicCount new fathers ingested from seed, durationMs=$patristicDuration")
            } else {
                log.info("PATRISTIC_INGESTION_SKIPPED — ENABLE_PATRISTIC_INGESTION=false")
            }

            if (IngestionConfig.enableCouncilIngestion) {
                val councilsStart = System.currentTimeMillis()
                councilIngestionService.fullIngestion()
                val councilsDuration = System.currentTimeMillis() - councilsStart
                log.info("COUNCIL_INGESTION: completed in durationMs=$councilsDuration")
            } else {
                log.info("COUNCIL_INGESTION_SKIPPED — ENABLE_COUNCIL_INGESTION=false")
            }

            val durationMs = System.currentTimeMillis() - startTime
            metadataRepository.markSuccess(durationMs, manuscriptsIngested, versesLinked)
            log.info("INGESTION_FINISHED duration=${durationMs}ms manuscripts=$manuscriptsIngested verses=$versesLinked")
        } catch (e: Throwable) {
            val durationMs = System.currentTimeMillis() - startTime
            metadataRepository.markFailed(durationMs, e.message ?: "Unknown error")
            throw e
        }
    }
}
