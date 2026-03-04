package com.ntcoverage.repository

import com.ntcoverage.model.*
import com.ntcoverage.seed.HeresySeedEntry
import com.ntcoverage.seed.HeresyTranslationSeedEntry
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

class HeresyRepository {
    private fun translatedJoin(locale: String) =
        Heresies.join(HeresyTranslations, JoinType.LEFT,
            additionalConstraint = {
                (Heresies.id eq HeresyTranslations.heresyId) and
                    (HeresyTranslations.locale eq locale)
            })

    fun findAll(page: Int = 1, limit: Int = 20, locale: String = "en"): List<HeresySummaryDTO> = transaction {
        val query = if (locale == "en") Heresies.selectAll() else translatedJoin(locale).selectAll()
        query
            .orderBy(Heresies.centuryOrigin to SortOrder.ASC_NULLS_LAST, Heresies.name to SortOrder.ASC)
            .limit(limit)
            .offset(((page - 1) * limit).toLong())
            .map { it.toSummary(locale) }
    }

    fun countAll(): Int = transaction { Heresies.selectAll().count().toInt() }

    fun findBySlug(slug: String, locale: String = "en"): HeresyDetailDTO? = transaction {
        val row = (if (locale == "en") Heresies.selectAll() else translatedJoin(locale).selectAll())
            .where { Heresies.slug eq slug }
            .singleOrNull()
            ?: return@transaction null

        val heresyId = row[Heresies.id].value
        val councils = listCouncilsByHeresyId(heresyId, locale)
        row.toDetail(locale, councils)
    }

    fun findByNormalizedName(normalizedName: String): Int? = transaction {
        Heresies.select(Heresies.id)
            .where { Heresies.normalizedName eq normalizedName }
            .singleOrNull()
            ?.get(Heresies.id)
            ?.value
    }

    fun insertOrUpdate(entry: HeresySeedEntry): Int = transaction {
        val existing = Heresies.selectAll().where { Heresies.normalizedName eq entry.normalizedName }.singleOrNull()
        if (existing != null) {
            val id = existing[Heresies.id].value
            Heresies.update({ Heresies.id eq id }) {
                it[name] = entry.name
                it[slug] = entry.slug
                it[description] = entry.description
                it[centuryOrigin] = entry.centuryOrigin
                it[yearOrigin] = entry.yearOrigin
                it[keyFigure] = entry.keyFigure
                it[wikipediaUrl] = entry.wikipediaUrl
            }
            return@transaction id
        }

        Heresies.insertAndGetId {
            it[name] = entry.name
            it[normalizedName] = entry.normalizedName
            it[slug] = entry.slug
            it[description] = entry.description
            it[centuryOrigin] = entry.centuryOrigin
            it[yearOrigin] = entry.yearOrigin
            it[keyFigure] = entry.keyFigure
            it[wikipediaUrl] = entry.wikipediaUrl
            it[createdAt] = OffsetDateTime.now()
        }.value
    }

    data class TranslationMeta(val translationSource: String?, val hasName: Boolean, val hasDescription: Boolean)

    fun findTranslationMeta(heresyId: Int, locale: String): TranslationMeta? = transaction {
        HeresyTranslations.selectAll().where {
            (HeresyTranslations.heresyId eq heresyId) and (HeresyTranslations.locale eq locale)
        }.singleOrNull()?.let { row ->
            TranslationMeta(
                translationSource = row.getOrNull(HeresyTranslations.translationSource),
                hasName = !row[HeresyTranslations.name].isNullOrBlank(),
                hasDescription = !row[HeresyTranslations.description].isNullOrBlank()
            )
        }
    }

    fun insertOrUpdateTranslation(entry: HeresyTranslationSeedEntry): Boolean = transaction {
        val heresyId = findByNormalizedName(entry.normalizedName) ?: return@transaction false
        insertOrUpdateTranslation(heresyId, entry.locale, entry.name, entry.description, "seed")
    }

    fun insertOrUpdateTranslation(
        heresyId: Int,
        locale: String,
        name: String,
        description: String? = null,
        translationSource: String = "seed"
    ): Boolean = transaction {
        val existing = HeresyTranslations.selectAll().where {
            (HeresyTranslations.heresyId eq heresyId) and (HeresyTranslations.locale eq locale)
        }.singleOrNull()

        if (existing == null) {
            HeresyTranslations.insert {
                it[HeresyTranslations.heresyId] = heresyId
                it[HeresyTranslations.locale] = locale
                it[HeresyTranslations.name] = name
                it[HeresyTranslations.description] = description
                it[HeresyTranslations.translationSource] = translationSource
            }
        } else {
            HeresyTranslations.update({ HeresyTranslations.id eq existing[HeresyTranslations.id].value }) {
                it[HeresyTranslations.name] = name
                it[HeresyTranslations.description] = description
                it[HeresyTranslations.translationSource] = translationSource
            }
        }
        true
    }

    fun linkCouncilHeresy(councilId: Int, heresyId: Int, action: String = "condemned"): Boolean = transaction {
        val exists = CouncilHeresies.selectAll().where {
            (CouncilHeresies.councilId eq councilId) and (CouncilHeresies.heresyId eq heresyId)
        }.count() > 0

        if (!exists) {
            CouncilHeresies.insert {
                it[CouncilHeresies.councilId] = councilId
                it[CouncilHeresies.heresyId] = heresyId
                it[CouncilHeresies.action] = action
            }
        }
        true
    }

    fun listCouncilsByHeresySlug(slug: String, locale: String = "en"): List<CouncilSummaryDTO> = transaction {
        val heresyId = Heresies.select(Heresies.id).where { Heresies.slug eq slug }.singleOrNull()?.get(Heresies.id)?.value
            ?: return@transaction emptyList()
        listCouncilsByHeresyId(heresyId, locale)
    }

    private fun listCouncilsByHeresyId(heresyId: Int, locale: String): List<CouncilSummaryDTO> {
        val join = CouncilHeresies
            .join(Councils, JoinType.INNER, additionalConstraint = { CouncilHeresies.councilId eq Councils.id })
            .join(CouncilTranslations, JoinType.LEFT, additionalConstraint = {
                (Councils.id eq CouncilTranslations.councilId) and (CouncilTranslations.locale eq locale)
            })

        return join.selectAll()
            .where { CouncilHeresies.heresyId eq heresyId }
            .orderBy(Councils.year to SortOrder.ASC)
            .map {
                CouncilSummaryDTO(
                    id = it[Councils.id].value,
                    displayName = if (locale == "en") it[Councils.displayName]
                    else it.getOrNull(CouncilTranslations.displayName) ?: it[Councils.displayName],
                    slug = it[Councils.slug],
                    year = it[Councils.year],
                    yearEnd = it[Councils.yearEnd],
                    century = it[Councils.century],
                    councilType = it[Councils.councilType],
                    location = if (locale == "en") it[Councils.location]
                    else it.getOrNull(CouncilTranslations.location) ?: it[Councils.location],
                    latitude = it[Councils.latitude],
                    longitude = it[Councils.longitude],
                    numberOfParticipants = it[Councils.numberOfParticipants],
                    consensusConfidence = it[Councils.consensusConfidence],
                    dataConfidence = it[Councils.dataConfidence],
                    sourceCount = it[Councils.sourceCount]
                )
            }
    }

    private fun ResultRow.toSummary(locale: String): HeresySummaryDTO =
        HeresySummaryDTO(
            id = this[Heresies.id].value,
            name = if (locale == "en") this[Heresies.name]
            else this.getOrNull(HeresyTranslations.name) ?: this[Heresies.name],
            slug = this[Heresies.slug],
            centuryOrigin = this[Heresies.centuryOrigin],
            yearOrigin = this[Heresies.yearOrigin],
            keyFigure = this[Heresies.keyFigure]
        )

    private fun ResultRow.toDetail(locale: String, councils: List<CouncilSummaryDTO>): HeresyDetailDTO =
        HeresyDetailDTO(
            id = this[Heresies.id].value,
            name = if (locale == "en") this[Heresies.name]
            else this.getOrNull(HeresyTranslations.name) ?: this[Heresies.name],
            slug = this[Heresies.slug],
            description = if (locale == "en") this[Heresies.description]
            else this.getOrNull(HeresyTranslations.description) ?: this[Heresies.description],
            centuryOrigin = this[Heresies.centuryOrigin],
            yearOrigin = this[Heresies.yearOrigin],
            keyFigure = this[Heresies.keyFigure],
            wikipediaUrl = this[Heresies.wikipediaUrl],
            councils = councils
        )
}
