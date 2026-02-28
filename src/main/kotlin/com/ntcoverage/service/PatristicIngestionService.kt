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
        val entries = ChurchFathersSeedData.entries
        var inserted = 0
        var skipped = 0

        for (entry in entries) {
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
            } else {
                skipped++
            }
        }

        log.info("PATRISTIC_SEED: $inserted inserted, $skipped already existed (total ${entries.size})")

        val statementsInserted = seedTextualStatements()
        log.info("TEXTUAL_STATEMENTS_SEED: $statementsInserted new statements inserted")

        val fatherTranslations = seedFatherTranslations()
        val statementTranslations = seedStatementTranslations()
        log.info("TRANSLATIONS_SEED: $fatherTranslations father translations, $statementTranslations statement translations inserted")

        val bioTranslations = translateBiographies()
        log.info("BIO_TRANSLATIONS: $bioTranslations biography translations generated")

        return inserted
    }

    private fun seedTextualStatements(): Int {
        val entries = TextualStatementsSeedData.entries
        var inserted = 0

        for (entry in entries) {
            val father = repository.findByNormalizedName(entry.fatherNormalizedName)
            if (father == null) {
                log.warn("STATEMENT_SEED: father not found for '${entry.fatherNormalizedName}', skipping")
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

            if (result != null) inserted++
        }

        return inserted
    }

    private fun seedFatherTranslations(): Int {
        val entries = ChurchFatherTranslationsSeedData.entries
        var inserted = 0

        for (entry in entries) {
            val father = repository.findByNormalizedName(entry.normalizedName)
            if (father == null) {
                log.warn("FATHER_TRANSLATION_SEED: father not found for '${entry.normalizedName}', skipping")
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
        }

        return inserted
    }

    private fun seedStatementTranslations(): Int {
        val entries = TextualStatementTranslationsSeedData.entries
        var inserted = 0

        for (entry in entries) {
            val father = repository.findByNormalizedName(entry.fatherNormalizedName)
            if (father == null) {
                log.warn("STATEMENT_TRANSLATION_SEED: father not found for '${entry.fatherNormalizedName}', skipping")
                continue
            }

            val statementId = transaction {
                findStatementId(father.id, entry.sourceReference)
            }

            if (statementId == null) {
                log.warn("STATEMENT_TRANSLATION_SEED: statement not found for father='${entry.fatherNormalizedName}', ref='${entry.sourceReference}', skipping")
                continue
            }

            statementRepository.insertTranslation(
                statementId = statementId,
                locale = entry.locale,
                statementText = entry.statementText
            )
            inserted++
        }

        return inserted
    }

    private fun findStatementId(fatherId: Int, sourceReference: String): Int? {
        val statements = statementRepository.findByFather(fatherId)
        return statements.firstOrNull { it.sourceReference == sourceReference }?.id
    }

    private suspend fun translateBiographies(): Int {
        val targetLocales = listOf("pt", "es")
        var translated = 0

        for (entry in ChurchFathersSeedData.entries) {
            if (entry.biographyOriginal.isNullOrBlank()) continue

            val normalizedName = normalize(entry.displayName)
            val father = repository.findByNormalizedName(normalizedName) ?: continue

            for (locale in targetLocales) {
                val meta = repository.findTranslationMeta(father.id, locale)

                if (meta != null && !meta.biographyOriginal.isNullOrBlank()) {
                    log.debug("BIO_TRANSLATE: skipping father=${entry.displayName} locale=$locale — already translated")
                    continue
                }

                if (meta != null && meta.translationSource == "reviewed") {
                    log.info("BIO_TRANSLATE: skipping father=${entry.displayName} locale=$locale — human-reviewed")
                    continue
                }

                val translatedOriginal = summarizationService.translateBiography(
                    text = entry.biographyOriginal,
                    targetLocale = locale,
                    fatherName = entry.displayName
                ) ?: continue

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
            }
        }

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
