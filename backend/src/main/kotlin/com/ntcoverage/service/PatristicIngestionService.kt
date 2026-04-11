package com.ntcoverage.service

import com.ntcoverage.repository.ChurchFatherRepository
import com.ntcoverage.repository.FatherTextualStatementRepository
import com.ntcoverage.seed.ChurchFatherTranslationsSeedData
import com.ntcoverage.seed.ChurchFathersSeedData
import com.ntcoverage.seed.TextualStatementTranslationsSeedData
import com.ntcoverage.seed.TextualStatementsSeedData
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class PatristicIngestionService(
    private val repository: ChurchFatherRepository,
    private val statementRepository: FatherTextualStatementRepository,
    private val summarizationService: BiographySummarizationService,
    private val phaseTracker: IngestionPhaseTracker,
    private val datingEnrichmentService: DatingEnrichmentService
) {
    private val log = LoggerFactory.getLogger(PatristicIngestionService::class.java)

    companion object {
        val ALL_PHASES = listOf(
            "patristic_seed_fathers",
            "patristic_seed_statements",
            "patristic_translate_fathers",
            "patristic_translate_statements",
            "patristic_translate_biographies",
            "patristic_enrich_dating"
        )

        fun normalize(displayName: String): String =
            displayName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    }

    /**
     * Optional filter: when set, only fathers whose normalized names are in this set will be processed.
     * Use [withFilter] to run ingestion for specific fathers (e.g., testing with just "clement_of_rome").
     */
    private var filterNames: Set<String>? = null

    fun withFilter(normalizedNames: Set<String>?): PatristicIngestionService {
        this.filterNames = normalizedNames
        return this
    }

    private fun <T> filterEntries(entries: List<T>, nameExtractor: (T) -> String): List<T> {
        val filter = filterNames ?: return entries
        return entries.filter { nameExtractor(it) in filter }
    }

    suspend fun runPhases(phases: List<String>, skipCompleted: Boolean = false) {
        try {
            val requested = ALL_PHASES.filter { it in phases }
            val effectivePhases = if (skipCompleted) {
                val skipped = requested.filter { phaseTracker.isPhaseCompleted(it) }
                if (skipped.isNotEmpty()) {
                    log.info("PATRISTIC_INGESTION: skipping {} already completed phases: {}", skipped.size, skipped)
                }
                requested.filterNot { phaseTracker.isPhaseCompleted(it) }
            } else requested
            if (effectivePhases.isEmpty()) {
                log.info("PATRISTIC_INGESTION: all phases already completed, nothing to run")
                return
            }
            for (phase in effectivePhases) {
                when (phase) {
                    "patristic_seed_fathers" -> runSeedFathers()
                    "patristic_seed_statements" -> runSeedStatements()
                    "patristic_translate_fathers" -> runTranslateFathers()
                    "patristic_translate_statements" -> runTranslateStatements()
                    "patristic_translate_biographies" -> runTranslateBiographies(false)
                    "patristic_enrich_dating" -> runEnrichDating()
                }
            }
        } finally {
            filterNames = null
        }
    }

    suspend fun fullIngestion() = runPhases(ALL_PHASES, skipCompleted = true)

    suspend fun ingestFromSeed(): Int {
        try {
            val totalStart = System.currentTimeMillis()
            log.info("PATRISTIC_INGESTION_START: beginning full patristic seed ingestion${filterNames?.let { " (filter=${it.joinToString(",")})" } ?: ""}")

            val inserted = runSeedFathers()
            runSeedStatements()
            runTranslateFathers()
            runTranslateStatements()
            runTranslateBiographies(false)

            val totalDuration = System.currentTimeMillis() - totalStart
            log.info("PATRISTIC_INGESTION_DONE: fathers=$inserted, totalDurationMs=$totalDuration")
            return inserted
        } finally {
            filterNames = null
        }
    }

    suspend fun seedOnly(): Int {
        try {
            val inserted = runSeedFathers()
            runSeedStatements()
            return inserted
        } finally {
            filterNames = null
        }
    }

    data class TranslationResult(val fatherTranslations: Int, val statementTranslations: Int, val bioTranslations: Int)

    suspend fun translateOnly(force: Boolean = false): TranslationResult {
        try {
            val fatherTranslations = runTranslateFathers()
            val statementTranslations = runTranslateStatements()
            val bioTranslations = runTranslateBiographies(force)
            return TranslationResult(fatherTranslations, statementTranslations, bioTranslations)
        } finally {
            filterNames = null
        }
    }

    private suspend fun runSeedFathers(): Int {
        val entries = filterEntries(ChurchFathersSeedData.entries) { normalize(it.displayName) }
        phaseTracker.markRunning("patristic_seed_fathers", total = entries.size, runBy = "system")
        try {
            var inserted = 0
            var skipped = 0
            log.info("PATRISTIC_SEED_START: processing ${entries.size} church fathers")
            val seedStart = System.currentTimeMillis()

            for ((index, entry) in entries.withIndex()) {
                val normalizedName = normalize(entry.displayName)
                val existing = repository.findByNormalizedName(normalizedName)

                val biographySummary = existing?.biographySummary

                repository.insertOrUpdate(
                    displayName = entry.displayName,
                    normalizedName = normalizedName,
                    centuryMin = entry.centuryMin,
                    centuryMax = entry.centuryMax,
                    shortDescription = entry.shortDescription,
                    primaryLocation = entry.primaryLocation,
                    tradition = entry.tradition,
                    source = "seed",
                    mannerOfDeath = entry.mannerOfDeath,
                    biographyOriginal = entry.biographyOriginal,
                    biographySummary = biographySummary
                )

                if (existing == null) {
                    inserted++
                    log.info("PATRISTIC_SEED: [${index + 1}/${entries.size}] inserted '${entry.displayName}'")
                } else {
                    skipped++
                    log.debug("PATRISTIC_SEED: [${index + 1}/${entries.size}] already exists '${entry.displayName}'")
                }
                phaseTracker.markProgress("patristic_seed_fathers", index + 1)
            }

            val seedDuration = System.currentTimeMillis() - seedStart
            log.info("PATRISTIC_SEED_DONE: $inserted inserted, $skipped already existed (total ${entries.size}) durationMs=$seedDuration")
            phaseTracker.markSuccess("patristic_seed_fathers", inserted)
            return inserted
        } catch (e: Exception) {
            phaseTracker.markFailed("patristic_seed_fathers", e.message ?: "Unknown error")
            throw e
        }
    }

    private fun runSeedStatements(): Int {
        val entries = filterEntries(TextualStatementsSeedData.entries) { it.fatherNormalizedName }
        phaseTracker.markRunning("patristic_seed_statements", total = entries.size, runBy = "system")
        try {
            var inserted = 0
            var skippedExisting = 0
            var skippedNoFather = 0

            log.info("TEXTUAL_STATEMENTS_SEED_START: processing ${entries.size} statements")
            val startMs = System.currentTimeMillis()

            for ((index, entry) in entries.withIndex()) {
                val father = repository.findByNormalizedName(entry.fatherNormalizedName)
                if (father == null) {
                    skippedNoFather++
                    log.warn("STATEMENT_SEED: [${index + 1}/${entries.size}] father not found for '${entry.fatherNormalizedName}', skipping")
                    phaseTracker.markProgress("patristic_seed_statements", index + 1)
                    continue
                }

                val result = statementRepository.insertIfNotExists(
                    fatherId = father.id,
                    topic = entry.topic,
                    statementText = entry.statementText,
                    originalLanguage = entry.originalLanguage,
                    originalText = entry.originalText,
                    sourceWork = entry.sourceWork,
                    sourceReference = entry.sourceReference,
                    approximateYear = entry.approximateYear
                )

                if (result != null) {
                    inserted++
                    log.info("STATEMENT_SEED: [${index + 1}/${entries.size}] inserted statement for '${entry.fatherNormalizedName}'")
                } else {
                    skippedExisting++
                }
                phaseTracker.markProgress("patristic_seed_statements", index + 1)
            }

            val durationMs = System.currentTimeMillis() - startMs
            log.info("STATEMENT_SEED_SUMMARY: inserted=$inserted, alreadyExisted=$skippedExisting, noFather=$skippedNoFather, total=${entries.size}, durationMs=$durationMs")
            phaseTracker.markSuccess("patristic_seed_statements", inserted)
            return inserted
        } catch (e: Exception) {
            phaseTracker.markFailed("patristic_seed_statements", e.message ?: "Unknown error")
            throw e
        }
    }

    private fun runTranslateFathers(): Int {
        val entries = filterEntries(ChurchFatherTranslationsSeedData.entries) { it.normalizedName }
        phaseTracker.markRunning("patristic_translate_fathers", total = entries.size, runBy = "system")
        try {
            var inserted = 0
            var skippedNoFather = 0

            for ((index, entry) in entries.withIndex()) {
                val father = repository.findByNormalizedName(entry.normalizedName)
                if (father == null) {
                    skippedNoFather++
                    phaseTracker.markProgress("patristic_translate_fathers", index + 1)
                    continue
                }

                repository.insertTranslation(
                    fatherId = father.id,
                    locale = entry.locale,
                    displayName = entry.displayName,
                    shortDescription = entry.shortDescription,
                    primaryLocation = entry.primaryLocation,
                    mannerOfDeath = entry.mannerOfDeath,
                    biographyOriginal = entry.biographyOriginal,
                    biographySummary = entry.biographySummary
                )
                inserted++
                phaseTracker.markProgress("patristic_translate_fathers", index + 1)
            }

            log.info("FATHER_TRANSLATION_SEED_SUMMARY: inserted=$inserted, noFather=$skippedNoFather, total=${entries.size}")
            phaseTracker.markSuccess("patristic_translate_fathers", inserted)
            return inserted
        } catch (e: Exception) {
            phaseTracker.markFailed("patristic_translate_fathers", e.message ?: "Unknown error")
            throw e
        }
    }

    private fun runTranslateStatements(): Int {
        val entries = filterEntries(TextualStatementTranslationsSeedData.entries) { it.fatherNormalizedName }
        phaseTracker.markRunning("patristic_translate_statements", total = entries.size, runBy = "system")
        try {
            var inserted = 0
            var skippedNoFather = 0
            var skippedNoStatement = 0

            for ((index, entry) in entries.withIndex()) {
                val father = repository.findByNormalizedName(entry.fatherNormalizedName)
                if (father == null) {
                    skippedNoFather++
                    phaseTracker.markProgress("patristic_translate_statements", index + 1)
                    continue
                }

                val statementId = transaction {
                    findStatementId(father.id, entry.sourceReference)
                }

                if (statementId == null) {
                    skippedNoStatement++
                    phaseTracker.markProgress("patristic_translate_statements", index + 1)
                    continue
                }

                statementRepository.insertTranslation(
                    statementId = statementId,
                    locale = entry.locale,
                    statementText = entry.statementText
                )
                inserted++
                phaseTracker.markProgress("patristic_translate_statements", index + 1)
            }

            log.info("STATEMENT_TRANSLATION_SEED_SUMMARY: inserted=$inserted, noFather=$skippedNoFather, noStatement=$skippedNoStatement, total=${entries.size}")
            phaseTracker.markSuccess("patristic_translate_statements", inserted)
            return inserted
        } catch (e: Exception) {
            phaseTracker.markFailed("patristic_translate_statements", e.message ?: "Unknown error")
            throw e
        }
    }

    private fun runTranslateBiographies(force: Boolean): Int {
        val targetLocales = listOf("pt", "es")
        val fathersWithBio = filterEntries(ChurchFathersSeedData.entries) { normalize(it.displayName) }
            .filter { !it.biographyOriginal.isNullOrBlank() }
        phaseTracker.markRunning("patristic_translate_biographies", total = 0, runBy = "system")
        try {
            var enqueued = 0
            var skippedExisting = 0
            var skippedReviewed = 0
            var skippedNoBio = 0

            for (entry in fathersWithBio) {
                val normalizedName = normalize(entry.displayName)
                val father = repository.findByNormalizedName(normalizedName)
                if (father == null) { skippedNoBio++; continue }

                for (locale in targetLocales) {
                    val meta = repository.findTranslationMeta(father.id, locale)
                    if (!force && meta != null && !meta.biographyOriginal.isNullOrBlank()) { skippedExisting++; continue }
                    if (meta != null && meta.translationSource == "reviewed") { skippedReviewed++; continue }

                    summarizationService.enqueueBioTranslate(
                        fatherId = father.id,
                        text = entry.biographyOriginal!!,
                        targetLocale = locale,
                        fatherName = entry.displayName
                    )
                    enqueued++
                }
            }

            log.info("BIO_TRANSLATE_ENQUEUE: enqueued=$enqueued, skippedExisting=$skippedExisting, skippedReviewed=$skippedReviewed, skippedNoBio=$skippedNoBio")
            phaseTracker.markSuccess("patristic_translate_biographies", enqueued)
            return enqueued
        } catch (e: Exception) {
            phaseTracker.markFailed("patristic_translate_biographies", e.message ?: "Unknown error")
            throw e
        }
    }

    private fun runEnrichDating(): Int {
        if (!datingEnrichmentService.isEnabled()) {
            log.info("PATRISTIC_ENRICH_DATING: dating enrichment disabled — skipping")
            phaseTracker.markRunning("patristic_enrich_dating", total = 0, runBy = "system")
            phaseTracker.markSuccess("patristic_enrich_dating", 0)
            return 0
        }

        phaseTracker.markRunning("patristic_enrich_dating", total = 0, runBy = "system")
        try {
            val enqueued = datingEnrichmentService.enqueueFatherDating(limit = 100)
            log.info("PATRISTIC_ENRICH_DATING: enqueued $enqueued fathers for LLM queue processing")
            phaseTracker.markSuccess("patristic_enrich_dating", enqueued)
            return enqueued
        } catch (e: Exception) {
            phaseTracker.markFailed("patristic_enrich_dating", e.message ?: "Unknown error")
            throw e
        }
    }

    private fun findStatementId(fatherId: Int, sourceReference: String): Int? {
        val statements = statementRepository.findByFather(fatherId)
        return statements.firstOrNull { it.sourceReference == sourceReference }?.id
    }

    fun deleteAll(): Int {
        val count = repository.deleteAll()
        log.info("PATRISTIC_RESET: deleted $count church fathers (statements + translations cascade-deleted)")
        return count
    }

}
