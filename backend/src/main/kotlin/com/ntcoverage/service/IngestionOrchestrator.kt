package com.ntcoverage.service

import com.ntcoverage.config.IngestionConfig
import com.ntcoverage.model.*
import com.ntcoverage.repository.IngestionMetadataRepository
import com.ntcoverage.repository.LlmQueueRepository
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
    private val statsRepository: StatsRepository,
    private val phaseTracker: IngestionPhaseTracker,
    private val llmQueueRepository: LlmQueueRepository
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

    fun resetManuscripts(): Int {
        log.warn("RESET_MANUSCRIPTS requested")
        return transaction {
            val deleted = ManuscriptVerses.deleteAll() +
                ManuscriptSources.deleteAll() +
                CoverageByCentury.deleteAll() +
                Manuscripts.deleteAll()
            phaseTracker.deleteByPrefix("manuscript_")
            log.info("RESET_MANUSCRIPTS complete: $deleted rows deleted (phases cleared)")
            deleted
        }
    }

    fun resetPatristic(): Int {
        log.warn("RESET_PATRISTIC requested")
        return transaction {
            val deleted = CouncilFathers.deleteAll() +
                FatherStatementTranslations.deleteAll() +
                ChurchFatherTranslations.deleteAll() +
                FatherTextualStatements.deleteAll() +
                ChurchFathers.deleteAll()
            phaseTracker.deleteByPrefix("patristic_")
            log.info("RESET_PATRISTIC complete: $deleted rows deleted (phases cleared)")
            deleted
        }
    }

    fun resetBibleLayer1(): Int {
        log.warn("RESET_BIBLE_LAYER1 (structure) requested")
        val db = com.ntcoverage.config.BibleDatabaseConfig.database
        val deleted = org.jetbrains.exposed.sql.transactions.transaction(db) {
            com.ntcoverage.model.WordAlignments.deleteAll() +
                com.ntcoverage.model.GreekLexiconTranslations.deleteAll() +
                com.ntcoverage.model.InterlinearWords.deleteAll() +
                com.ntcoverage.model.GreekLexicon.deleteAll() +
                com.ntcoverage.model.HebrewLexicon.deleteAll() +
                com.ntcoverage.model.BibleVerseTexts.deleteAll() +
                com.ntcoverage.model.BibleBookAbbreviations.deleteAll() +
                com.ntcoverage.model.BibleChapters.deleteAll() +
                com.ntcoverage.model.BibleVerses.deleteAll() +
                com.ntcoverage.model.BibleBooks.deleteAll() +
                com.ntcoverage.model.BibleVersions.deleteAll()
        }
        phaseTracker.deleteByPrefix("bible_")
        clearLlmQueueForPhases(listOf("bible_"))
        log.info("RESET_BIBLE_LAYER1 complete: $deleted rows deleted")
        return deleted.toInt()
    }

    fun resetBibleLayer2(): Int {
        log.warn("RESET_BIBLE_LAYER2 (greek/lexicon) requested")
        val db = com.ntcoverage.config.BibleDatabaseConfig.database
        val deleted = org.jetbrains.exposed.sql.transactions.transaction(db) {
            // Delete in FK-safe order: children before parents
            com.ntcoverage.model.WordAlignments.deleteAll() +
                com.ntcoverage.model.GreekLexiconTranslations.deleteAll() +
                com.ntcoverage.model.HebrewLexiconTranslations.deleteAll() +
                com.ntcoverage.model.InterlinearWords.deleteAll() +
                com.ntcoverage.model.GreekLexicon.deleteAll() +
                com.ntcoverage.model.HebrewLexicon.deleteAll()
        }
        // Delete ALL bible_ phases except layer1 (abbreviations/books/versions) and layer3 (verse texts)
        // Using broad prefix "bible_" then excluding layer1/3 via selective deletion
        val layer2Phases = listOf(
            "bible_ingest_nt_interlinear", "bible_ingest_ot_interlinear",
            "bible_ingest_greek_lexicon", "bible_ingest_hebrew_lexicon",
            "bible_fill",  // covers bible_fill_missing_hebrew
            "bible_translate_lexicon", "bible_translate_hebrew_lexicon",
            "bible_translate_glosses",
            "bible_align", "bible_word_align",
            "bible_enrich",
            "bible_reenrich",  // covers bible_reenrich_greek/hebrew_lexicon
            "bible_translate_enrichment"  // covers bible_translate_enrichment_greek/hebrew
        )
        layer2Phases.forEach { phaseTracker.deleteByPrefix(it) }
        clearLlmQueueForPhases(layer2Phases)
        log.info("RESET_BIBLE_LAYER2 complete: $deleted rows deleted")
        return deleted.toInt()
    }

    fun resetBibleLayer3(): Int {
        log.warn("RESET_BIBLE_LAYER3 (verse texts) requested")
        val db = com.ntcoverage.config.BibleDatabaseConfig.database
        val deleted = org.jetbrains.exposed.sql.transactions.transaction(db) {
            com.ntcoverage.model.BibleVerseTexts.deleteAll()
        }
        val layer3Phases = listOf("bible_ingest_text_kjv", "bible_ingest_text_aa",
            "bible_ingest_text_acf", "bible_ingest_text_arc69")
        layer3Phases.forEach { phaseTracker.deleteByPrefix(it) }
        clearLlmQueueForPhases(layer3Phases)
        log.info("RESET_BIBLE_LAYER3 complete: $deleted rows deleted")
        return deleted.toInt()
    }

    fun resetBibleLayer4(): Int {
        log.warn("RESET_BIBLE_LAYER4 (tokenization + word alignments) requested")
        val db = com.ntcoverage.config.BibleDatabaseConfig.database
        val deleted = org.jetbrains.exposed.sql.transactions.transaction(db) {
            com.ntcoverage.model.WordAlignments.deleteAll() +
                com.ntcoverage.model.BibleVerseTokens.deleteAll()
        }
        val layer4Phases = listOf(
            "bible_tokenize_arc69", "bible_tokenize_kjv",
            "bible_lemmatize_arc69", "bible_lemmatize_kjv",
            "bible_align_kjv", "bible_align_arc69",
            "bible_align_hebrew_kjv", "bible_align_hebrew_arc69"
        )
        layer4Phases.forEach { phaseTracker.deleteByPrefix(it) }
        clearLlmQueueForPhases(layer4Phases)
        log.info("RESET_BIBLE_LAYER4 complete: $deleted rows deleted (tokens + alignments)")
        return deleted.toInt()
    }

    fun resetBible(): Int {
        log.warn("RESET_BIBLE requested")
        val db = com.ntcoverage.config.BibleDatabaseConfig.database
        val deleted = org.jetbrains.exposed.sql.transactions.transaction(db) {
            com.ntcoverage.model.BibleVerseTokens.deleteAll() +
                com.ntcoverage.model.WordAlignments.deleteAll() +
                com.ntcoverage.model.GreekLexiconTranslations.deleteAll() +
                com.ntcoverage.model.InterlinearWords.deleteAll() +
                com.ntcoverage.model.GreekLexicon.deleteAll() +
                com.ntcoverage.model.HebrewLexicon.deleteAll() +
                com.ntcoverage.model.BibleVerseTexts.deleteAll() +
                com.ntcoverage.model.BibleBookAbbreviations.deleteAll() +
                com.ntcoverage.model.BibleChapters.deleteAll() +
                com.ntcoverage.model.BibleVerses.deleteAll() +
                com.ntcoverage.model.BibleBooks.deleteAll() +
                com.ntcoverage.model.BibleVersions.deleteAll()
        }
        // Phase tracker uses atlas_db (default), must run outside bible_db transaction
        phaseTracker.deleteByPrefix("bible_")
        clearLlmQueueForPhases(listOf("bible_"))
        log.info("RESET_BIBLE complete: $deleted rows deleted (phases cleared)")
        return deleted.toInt()
    }

    private fun clearLlmQueueForPhases(prefixes: List<String>) {
        var total = 0
        prefixes.forEach { prefix -> total += llmQueueRepository.clearByPrefix(prefix) }
        if (total > 0) log.info("LLM_QUEUE_CLEARED: $total items for prefixes $prefixes")
    }

    fun resetCouncils(): Int {
        log.warn("RESET_COUNCILS requested")
        return transaction {
            val deleted = CouncilSourceClaims.deleteAll() +
                CouncilCanons.deleteAll() +
                CouncilHeresies.deleteAll() +
                CouncilFathers.deleteAll() +
                CouncilTranslations.deleteAll() +
                HeresyTranslations.deleteAll() +
                Heresies.deleteAll() +
                Councils.deleteAll() +
                Sources.deleteAll()
            phaseTracker.deleteByPrefix("council_")
            phaseTracker.deleteByPrefix("heresy_")
            log.info("RESET_COUNCILS complete: $deleted rows deleted (phases cleared)")
            deleted
        }
    }

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
