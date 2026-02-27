package com.ntcoverage.service

import com.ntcoverage.config.IngestionConfig
import com.ntcoverage.repository.IngestionMetadataRepository
import com.ntcoverage.repository.StatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes

class IngestionOrchestrator(
    private val ingestionService: IngestionService,
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
        log.info("INGESTION_STARTED")
        metadataRepository.markRunning()
        val startTime = System.currentTimeMillis()

        try {
            val timeoutMinutes = IngestionConfig.timeoutMinutes
            val result = withTimeout(timeoutMinutes.minutes) {
                ingestionService.ingestManuscriptsAsync()
            }

            withTimeout(timeoutMinutes.minutes) {
                ingestionService.materializeCoverageAsync()
            }

            val durationMs = System.currentTimeMillis() - startTime
            metadataRepository.markSuccess(durationMs, result.manuscriptsIngested, result.versesLinked)
            log.info("INGESTION_FINISHED duration=${durationMs}ms manuscripts=${result.manuscriptsIngested} verses=${result.versesLinked}")
        } catch (e: Throwable) {
            val durationMs = System.currentTimeMillis() - startTime
            metadataRepository.markFailed(durationMs, e.message ?: "Unknown error")
            throw e
        }
    }
}
