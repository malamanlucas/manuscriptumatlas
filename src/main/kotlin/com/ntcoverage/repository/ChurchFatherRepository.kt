package com.ntcoverage.repository

import com.ntcoverage.model.ChurchFatherDetail
import com.ntcoverage.model.ChurchFatherSummary
import com.ntcoverage.model.ChurchFathers
import com.ntcoverage.model.ChurchFatherTranslations
import com.ntcoverage.service.BiographySummarizationService
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

class ChurchFatherRepository {

    fun findAll(
        century: Int? = null,
        tradition: String? = null,
        page: Int = 1,
        limit: Int = 50,
        locale: String = "en"
    ): List<ChurchFatherSummary> = transaction {
        val query = baseSelect(locale)

        if (century != null) {
            query.andWhere { (ChurchFathers.centuryMin lessEq century) and (ChurchFathers.centuryMax greaterEq century) }
        }
        if (tradition != null) {
            query.andWhere { ChurchFathers.tradition eq tradition }
        }

        query
            .orderBy(ChurchFathers.centuryMin to SortOrder.ASC, ChurchFathers.displayName to SortOrder.ASC)
            .limit(limit)
            .offset(((page - 1) * limit).toLong())
            .map { it.toSummary(locale) }
    }

    fun countAll(century: Int? = null, tradition: String? = null): Int = transaction {
        val query = ChurchFathers.selectAll()

        if (century != null) {
            query.andWhere { (ChurchFathers.centuryMin lessEq century) and (ChurchFathers.centuryMax greaterEq century) }
        }
        if (tradition != null) {
            query.andWhere { ChurchFathers.tradition eq tradition }
        }

        query.count().toInt()
    }

    fun findById(id: Int, locale: String = "en"): ChurchFatherDetail? = transaction {
        baseSelect(locale)
            .andWhere { ChurchFathers.id eq id }
            .singleOrNull()
            ?.toDetail(locale)
    }

    fun findByNormalizedName(name: String): ChurchFatherDetail? = transaction {
        ChurchFathers.selectAll()
            .where { ChurchFathers.normalizedName eq name }
            .singleOrNull()
            ?.toDetail("en")
    }

    fun search(query: String, limit: Int = 20, locale: String = "en"): List<ChurchFatherSummary> = transaction {
        val pattern = "%${query.lowercase()}%"

        if (locale == "en") {
            ChurchFathers.selectAll()
                .where {
                    (ChurchFathers.displayName.lowerCase() like pattern) or
                    (ChurchFathers.normalizedName like pattern)
                }
                .orderBy(ChurchFathers.displayName to SortOrder.ASC)
                .limit(limit)
                .map { it.toSummary("en") }
        } else {
            translatedJoin(locale).selectAll()
                .where {
                    (ChurchFathers.displayName.lowerCase() like pattern) or
                    (ChurchFathers.normalizedName like pattern) or
                    (ChurchFatherTranslations.displayName.lowerCase() like pattern)
                }
                .orderBy(ChurchFathers.displayName to SortOrder.ASC)
                .limit(limit)
                .map { it.toSummary(locale) }
        }
    }

    fun insertOrUpdate(
        displayName: String,
        normalizedName: String,
        centuryMin: Int,
        centuryMax: Int,
        shortDescription: String?,
        primaryLocation: String?,
        tradition: String,
        source: String = "seed",
        mannerOfDeath: String? = null,
        biographyOriginal: String? = null,
        biographySummary: String? = null
    ): Int = transaction {
        val existing = ChurchFathers.selectAll()
            .where { ChurchFathers.normalizedName eq normalizedName }
            .singleOrNull()

        if (existing != null) {
            val existingId = existing[ChurchFathers.id].value
            val existingDesc = existing[ChurchFathers.shortDescription]
            val existingSummary = existing[ChurchFathers.biographySummary]

            val needsUpdate = (existingDesc.isNullOrBlank() && !shortDescription.isNullOrBlank()) ||
                (existing[ChurchFathers.mannerOfDeath] == null && mannerOfDeath != null) ||
                (existing[ChurchFathers.biographyOriginal] == null && biographyOriginal != null) ||
                (existingSummary == null && biographySummary != null)

            if (needsUpdate) {
                ChurchFathers.update({ ChurchFathers.id eq existingId }) {
                    if (existingDesc.isNullOrBlank() && !shortDescription.isNullOrBlank()) {
                        it[ChurchFathers.shortDescription] = shortDescription
                    }
                    if (existing[ChurchFathers.mannerOfDeath] == null && mannerOfDeath != null) {
                        it[ChurchFathers.mannerOfDeath] = mannerOfDeath
                    }
                    if (existing[ChurchFathers.biographyOriginal] == null && biographyOriginal != null) {
                        it[ChurchFathers.biographyOriginal] = biographyOriginal
                    }
                    if (existingSummary == null && biographySummary != null) {
                        it[ChurchFathers.biographySummary] = biographySummary
                    }
                    it[updatedAt] = OffsetDateTime.now()
                }
            }

            return@transaction existingId
        }

        ChurchFathers.insertAndGetId {
            it[ChurchFathers.displayName] = displayName
            it[ChurchFathers.normalizedName] = normalizedName
            it[ChurchFathers.centuryMin] = centuryMin
            it[ChurchFathers.centuryMax] = centuryMax
            it[ChurchFathers.shortDescription] = shortDescription
            it[ChurchFathers.primaryLocation] = primaryLocation
            it[ChurchFathers.tradition] = tradition
            it[ChurchFathers.dataSource] = source
            it[ChurchFathers.mannerOfDeath] = mannerOfDeath
            it[ChurchFathers.biographyOriginal] = biographyOriginal
            it[ChurchFathers.biographySummary] = biographySummary
            it[createdAt] = OffsetDateTime.now()
            it[updatedAt] = OffsetDateTime.now()
        }.value
    }

    data class TranslationMeta(
        val biographyOriginal: String?,
        val biographySummary: String?,
        val translationSource: String
    )

    fun findTranslationMeta(fatherId: Int, locale: String): TranslationMeta? = transaction {
        ChurchFatherTranslations.selectAll().where {
            (ChurchFatherTranslations.fatherId eq fatherId) and
            (ChurchFatherTranslations.locale eq locale)
        }.singleOrNull()?.let {
            TranslationMeta(
                biographyOriginal = it[ChurchFatherTranslations.biographyOriginal],
                biographySummary = it[ChurchFatherTranslations.biographySummary],
                translationSource = it[ChurchFatherTranslations.translationSource]
            )
        }
    }

    fun insertTranslation(
        fatherId: Int,
        locale: String,
        displayName: String,
        shortDescription: String?,
        primaryLocation: String?,
        mannerOfDeath: String? = null,
        biographyOriginal: String? = null,
        biographySummary: String? = null,
        translationSource: String = "seed"
    ): Unit = transaction {
        val existing = ChurchFatherTranslations.selectAll().where {
            (ChurchFatherTranslations.fatherId eq fatherId) and
            (ChurchFatherTranslations.locale eq locale)
        }.singleOrNull()

        if (existing == null) {
            ChurchFatherTranslations.insert {
                it[ChurchFatherTranslations.fatherId] = fatherId
                it[ChurchFatherTranslations.locale] = locale
                it[ChurchFatherTranslations.displayName] = displayName
                it[ChurchFatherTranslations.shortDescription] = shortDescription
                it[ChurchFatherTranslations.primaryLocation] = primaryLocation
                it[ChurchFatherTranslations.mannerOfDeath] = mannerOfDeath
                it[ChurchFatherTranslations.biographyOriginal] = biographyOriginal
                it[ChurchFatherTranslations.biographySummary] = biographySummary
                it[ChurchFatherTranslations.translationSource] = translationSource
            }
        } else {
            val needsUpdate = (existing[ChurchFatherTranslations.mannerOfDeath] == null && mannerOfDeath != null) ||
                (existing[ChurchFatherTranslations.biographyOriginal] == null && biographyOriginal != null) ||
                (existing[ChurchFatherTranslations.biographySummary] == null && biographySummary != null)

            if (needsUpdate) {
                val existingId = existing[ChurchFatherTranslations.id].value
                ChurchFatherTranslations.update({ ChurchFatherTranslations.id eq existingId }) {
                    if (existing[ChurchFatherTranslations.mannerOfDeath] == null && mannerOfDeath != null) {
                        it[ChurchFatherTranslations.mannerOfDeath] = mannerOfDeath
                    }
                    if (existing[ChurchFatherTranslations.biographyOriginal] == null && biographyOriginal != null) {
                        it[ChurchFatherTranslations.biographyOriginal] = biographyOriginal
                        it[ChurchFatherTranslations.translationSource] = translationSource
                    }
                    if (existing[ChurchFatherTranslations.biographySummary] == null && biographySummary != null) {
                        it[ChurchFatherTranslations.biographySummary] = biographySummary
                    }
                }
            }
        }
    }

    fun deleteAll(): Int = transaction {
        ChurchFathers.deleteAll()
    }

    private fun translatedJoin(locale: String) =
        ChurchFathers.join(ChurchFatherTranslations, JoinType.LEFT,
            additionalConstraint = {
                (ChurchFathers.id eq ChurchFatherTranslations.fatherId) and
                (ChurchFatherTranslations.locale eq locale)
            })

    private fun baseSelect(locale: String): Query =
        if (locale == "en") ChurchFathers.selectAll()
        else translatedJoin(locale).selectAll()

    private fun ResultRow.toSummary(locale: String) = ChurchFatherSummary(
        id = this[ChurchFathers.id].value,
        displayName = if (locale == "en") this[ChurchFathers.displayName]
            else this.getOrNull(ChurchFatherTranslations.displayName) ?: this[ChurchFathers.displayName],
        normalizedName = this[ChurchFathers.normalizedName],
        centuryMin = this[ChurchFathers.centuryMin],
        centuryMax = this[ChurchFathers.centuryMax],
        tradition = this[ChurchFathers.tradition],
        primaryLocation = if (locale == "en") this[ChurchFathers.primaryLocation]
            else this.getOrNull(ChurchFatherTranslations.primaryLocation) ?: this[ChurchFathers.primaryLocation]
    )

    private fun ResultRow.toDetail(locale: String): ChurchFatherDetail {
        val bioOriginal = if (locale == "en") this[ChurchFathers.biographyOriginal]
            else this.getOrNull(ChurchFatherTranslations.biographyOriginal) ?: this[ChurchFathers.biographyOriginal]

        return ChurchFatherDetail(
            id = this[ChurchFathers.id].value,
            displayName = if (locale == "en") this[ChurchFathers.displayName]
                else this.getOrNull(ChurchFatherTranslations.displayName) ?: this[ChurchFathers.displayName],
            normalizedName = this[ChurchFathers.normalizedName],
            centuryMin = this[ChurchFathers.centuryMin],
            centuryMax = this[ChurchFathers.centuryMax],
            shortDescription = if (locale == "en") this[ChurchFathers.shortDescription]
                else this.getOrNull(ChurchFatherTranslations.shortDescription) ?: this[ChurchFathers.shortDescription],
            primaryLocation = if (locale == "en") this[ChurchFathers.primaryLocation]
                else this.getOrNull(ChurchFatherTranslations.primaryLocation) ?: this[ChurchFathers.primaryLocation],
            tradition = this[ChurchFathers.tradition],
            source = this[ChurchFathers.dataSource],
            mannerOfDeath = if (locale == "en") this[ChurchFathers.mannerOfDeath]
                else this.getOrNull(ChurchFatherTranslations.mannerOfDeath) ?: this[ChurchFathers.mannerOfDeath],
            biographySummary = if (locale == "en") this[ChurchFathers.biographySummary]
                else this.getOrNull(ChurchFatherTranslations.biographySummary) ?: this[ChurchFathers.biographySummary],
            biographyOriginal = bioOriginal,
            biographyIsLong = BiographySummarizationService.isLongBiography(bioOriginal)
        )
    }
}
