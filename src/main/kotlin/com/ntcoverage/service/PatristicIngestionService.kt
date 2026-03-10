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
    private val summarizationService: BiographySummarizationService
) {
    private val log = LoggerFactory.getLogger(PatristicIngestionService::class.java)

    suspend fun ingestFromSeed(): Int {
        val totalStart = System.currentTimeMillis()
        log.info("PATRISTIC_INGESTION_START: beginning full patristic seed ingestion")

        val inserted = seedOnly()

        val statementsStart = System.currentTimeMillis()
        log.info("TEXTUAL_STATEMENTS_SEED_START: processing ${TextualStatementsSeedData.entries.size} statements")
        val statementsInserted = seedTextualStatements()
        val statementsDuration = System.currentTimeMillis() - statementsStart
        log.info("TEXTUAL_STATEMENTS_SEED_DONE: $statementsInserted new statements inserted, durationMs=$statementsDuration")

        val translationResults = translateOnly()

        val totalDuration = System.currentTimeMillis() - totalStart
        log.info("PATRISTIC_INGESTION_DONE: fathers=$inserted, statements=$statementsInserted, fatherTranslations=${translationResults.fatherTranslations}, statementTranslations=${translationResults.statementTranslations}, bioTranslations=${translationResults.bioTranslations}, totalDurationMs=$totalDuration")

        return inserted
    }

    suspend fun seedOnly(): Int {
        val entries = ChurchFathersSeedData.entries
        var inserted = 0
        var skipped = 0

        log.info("PATRISTIC_SEED_START: processing ${entries.size} church fathers")
        val seedStart = System.currentTimeMillis()

        for ((index, entry) in entries.withIndex()) {
            val normalizedName = normalize(entry.displayName)
            val existing = repository.findByNormalizedName(normalizedName)

            val biographySummary = if (existing?.biographySummary != null) {
                existing.biographySummary
            } else {
                summarizationService.summarizeIfNeeded(entry.biographyOriginal)
            }

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
        }

        val seedDuration = System.currentTimeMillis() - seedStart
        log.info("PATRISTIC_SEED_DONE: $inserted inserted, $skipped already existed (total ${entries.size}) durationMs=$seedDuration")

        val statementsStart = System.currentTimeMillis()
        log.info("TEXTUAL_STATEMENTS_SEED_START: processing ${TextualStatementsSeedData.entries.size} statements")
        val statementsInserted = seedTextualStatements()
        val statementsDuration = System.currentTimeMillis() - statementsStart
        log.info("TEXTUAL_STATEMENTS_SEED_DONE: $statementsInserted new statements inserted, durationMs=$statementsDuration")

        return inserted
    }

    data class TranslationResult(val fatherTranslations: Int, val statementTranslations: Int, val bioTranslations: Int)

    suspend fun translateOnly(force: Boolean = false): TranslationResult {
        val translationsStart = System.currentTimeMillis()
        log.info("TRANSLATIONS_SEED_START: seeding father and statement translations")
        val fatherTranslations = seedFatherTranslations()
        val statementTranslations = seedStatementTranslations()
        val translationsDuration = System.currentTimeMillis() - translationsStart
        log.info("TRANSLATIONS_SEED_DONE: $fatherTranslations father translations, $statementTranslations statement translations inserted, durationMs=$translationsDuration")

        val bioStart = System.currentTimeMillis()
        log.info("BIO_TRANSLATIONS_START: translating biographies to pt/es (force=$force)")
        val bioTranslations = translateBiographies(force)
        val bioDuration = System.currentTimeMillis() - bioStart
        log.info("BIO_TRANSLATIONS_DONE: $bioTranslations biography translations generated, durationMs=$bioDuration")

        return TranslationResult(fatherTranslations, statementTranslations, bioTranslations)
    }

    private fun seedTextualStatements(): Int {
        val entries = TextualStatementsSeedData.entries
        var inserted = 0
        var skippedExisting = 0
        var skippedNoFather = 0

        for ((index, entry) in entries.withIndex()) {
            val father = repository.findByNormalizedName(entry.fatherNormalizedName)
            if (father == null) {
                skippedNoFather++
                log.warn("STATEMENT_SEED: [${index + 1}/${entries.size}] father not found for '${entry.fatherNormalizedName}', skipping")
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
                log.info("STATEMENT_SEED: [${index + 1}/${entries.size}] inserted statement for '${entry.fatherNormalizedName}' ref='${entry.sourceReference}'")
            } else {
                skippedExisting++
                log.debug("STATEMENT_SEED: [${index + 1}/${entries.size}] already exists for '${entry.fatherNormalizedName}' ref='${entry.sourceReference}'")
            }
        }

        log.info("STATEMENT_SEED_SUMMARY: inserted=$inserted, alreadyExisted=$skippedExisting, noFather=$skippedNoFather, total=${entries.size}")
        return inserted
    }

    private fun seedFatherTranslations(): Int {
        val entries = ChurchFatherTranslationsSeedData.entries
        var inserted = 0
        var skippedNoFather = 0

        for ((index, entry) in entries.withIndex()) {
            val father = repository.findByNormalizedName(entry.normalizedName)
            if (father == null) {
                skippedNoFather++
                log.warn("FATHER_TRANSLATION_SEED: [${index + 1}/${entries.size}] father not found for '${entry.normalizedName}', skipping")
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
            log.debug("FATHER_TRANSLATION_SEED: [${index + 1}/${entries.size}] inserted locale=${entry.locale} for '${entry.normalizedName}'")
        }

        log.info("FATHER_TRANSLATION_SEED_SUMMARY: inserted=$inserted, noFather=$skippedNoFather, total=${entries.size}")
        return inserted
    }

    private fun seedStatementTranslations(): Int {
        val entries = TextualStatementTranslationsSeedData.entries
        var inserted = 0
        var skippedNoFather = 0
        var skippedNoStatement = 0

        for ((index, entry) in entries.withIndex()) {
            val father = repository.findByNormalizedName(entry.fatherNormalizedName)
            if (father == null) {
                skippedNoFather++
                log.warn("STATEMENT_TRANSLATION_SEED: [${index + 1}/${entries.size}] father not found for '${entry.fatherNormalizedName}', skipping")
                continue
            }

            val statementId = transaction {
                findStatementId(father.id, entry.sourceReference)
            }

            if (statementId == null) {
                skippedNoStatement++
                log.warn("STATEMENT_TRANSLATION_SEED: [${index + 1}/${entries.size}] statement not found for father='${entry.fatherNormalizedName}', ref='${entry.sourceReference}', skipping")
                continue
            }

            statementRepository.insertTranslation(
                statementId = statementId,
                locale = entry.locale,
                statementText = entry.statementText
            )
            inserted++
            log.debug("STATEMENT_TRANSLATION_SEED: [${index + 1}/${entries.size}] inserted locale=${entry.locale} for '${entry.fatherNormalizedName}' ref='${entry.sourceReference}'")
        }

        log.info("STATEMENT_TRANSLATION_SEED_SUMMARY: inserted=$inserted, noFather=$skippedNoFather, noStatement=$skippedNoStatement, total=${entries.size}")
        return inserted
    }

    private fun findStatementId(fatherId: Int, sourceReference: String): Int? {
        val statements = statementRepository.findByFather(fatherId)
        return statements.firstOrNull { it.sourceReference == sourceReference }?.id
    }

    private suspend fun translateBiographies(force: Boolean = false): Int {
        val targetLocales = listOf("pt", "es")
        var translated = 0
        var skippedExisting = 0
        var skippedReviewed = 0
        var skippedNoBio = 0
        var failed = 0
        val fathersWithBio = ChurchFathersSeedData.entries.filter { !it.biographyOriginal.isNullOrBlank() }

        log.info("BIO_TRANSLATE_START: ${fathersWithBio.size} fathers with biographies, ${targetLocales.size} target locales (${fathersWithBio.size * targetLocales.size} potential translations), force=$force")

        for ((fatherIndex, entry) in fathersWithBio.withIndex()) {
            val normalizedName = normalize(entry.displayName)
            val father = repository.findByNormalizedName(normalizedName)
            if (father == null) {
                skippedNoBio++
                continue
            }

            for (locale in targetLocales) {
                val meta = repository.findTranslationMeta(father.id, locale)

                if (!force && meta != null && !meta.biographyOriginal.isNullOrBlank()) {
                    skippedExisting++
                    log.debug("BIO_TRANSLATE: [${fatherIndex + 1}/${fathersWithBio.size}] skipping '${entry.displayName}' locale=$locale — already translated")
                    continue
                }

                if (meta != null && meta.translationSource == "reviewed") {
                    skippedReviewed++
                    log.info("BIO_TRANSLATE: [${fatherIndex + 1}/${fathersWithBio.size}] skipping '${entry.displayName}' locale=$locale — human-reviewed")
                    continue
                }

                log.info("BIO_TRANSLATE: [${fatherIndex + 1}/${fathersWithBio.size}] translating '${entry.displayName}' to $locale")
                val translatedOriginal = summarizationService.translateBiography(
                    text = entry.biographyOriginal!!,
                    targetLocale = locale,
                    fatherName = entry.displayName
                )

                if (translatedOriginal == null) {
                    failed++
                    log.warn("BIO_TRANSLATE: [${fatherIndex + 1}/${fathersWithBio.size}] translation failed for '${entry.displayName}' locale=$locale")
                    continue
                }

                val translatedSummary = summarizationService.summarizeIfNeeded(translatedOriginal)

                repository.insertTranslation(
                    fatherId = father.id,
                    locale = locale,
                    displayName = meta?.let { entry.displayName } ?: entry.displayName,
                    shortDescription = null,
                    primaryLocation = null,
                    biographyOriginal = translatedOriginal,
                    biographySummary = translatedSummary,
                    translationSource = "machine"
                )
                translated++
                log.info("BIO_TRANSLATE: [${fatherIndex + 1}/${fathersWithBio.size}] completed '${entry.displayName}' locale=$locale")
            }
        }

        log.info("BIO_TRANSLATE_SUMMARY: translated=$translated, alreadyExisted=$skippedExisting, humanReviewed=$skippedReviewed, failed=$failed")
        return translated
    }

    fun deleteAll(): Int {
        val count = repository.deleteAll()
        log.info("PATRISTIC_RESET: deleted $count church fathers (statements + translations cascade-deleted)")
        return count
    }

    companion object {
        fun normalize(displayName: String): String =
            displayName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    }
}
