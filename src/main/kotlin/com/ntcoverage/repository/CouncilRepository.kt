package com.ntcoverage.repository

import com.ntcoverage.model.*
import com.ntcoverage.scraper.CouncilNameNormalizer
import com.ntcoverage.seed.CouncilSeedEntry
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

class CouncilRepository {
    private fun translatedJoin(locale: String) =
        Councils.join(CouncilTranslations, JoinType.LEFT,
            additionalConstraint = {
                (Councils.id eq CouncilTranslations.councilId) and
                    (CouncilTranslations.locale eq locale)
            })

    private fun sourceFor(locale: String) =
        if (locale == "en") Councils else translatedJoin(locale)

    fun findAll(
        century: Int? = null,
        type: String? = null,
        yearMin: Int? = null,
        yearMax: Int? = null,
        page: Int = 1,
        limit: Int = 20,
        locale: String = "en"
    ): List<CouncilSummaryDTO> = transaction {
        val query = sourceFor(locale).selectAll()
        applyFilters(query, century, type, yearMin, yearMax)
        query
            .orderBy(Councils.year to SortOrder.ASC, Councils.id to SortOrder.ASC)
            .limit(limit)
            .offset(((page - 1) * limit).toLong())
            .map { it.toSummary(locale) }
    }

    fun countAll(
        century: Int? = null,
        type: String? = null,
        yearMin: Int? = null,
        yearMax: Int? = null
    ): Int = transaction {
        val query = Councils.selectAll()
        applyFilters(query, century, type, yearMin, yearMax)
        query.count().toInt()
    }

    fun findBySlug(slug: String, locale: String = "en"): CouncilDetailDTO? = transaction {
        sourceFor(locale).selectAll()
            .where { Councils.slug eq slug }
            .singleOrNull()
            ?.toDetail(locale)
    }

    fun findIdBySlug(slug: String): Int? = transaction {
        Councils.select(Councils.id)
            .where { Councils.slug eq slug }
            .singleOrNull()
            ?.get(Councils.id)
            ?.value
    }

    fun findById(id: Int, locale: String = "en"): CouncilDetailDTO? = transaction {
        sourceFor(locale).selectAll()
            .where { Councils.id eq id }
            .singleOrNull()
            ?.toDetail(locale)
    }

    fun findIdByNameAndYear(name: String, year: Int): Int? = transaction {
        val normalized = CouncilNameNormalizer.normalize(name)
        Councils.select(Councils.id)
            .where { (Councils.normalizedName eq normalized) and (Councils.year eq year) }
            .singleOrNull()
            ?.get(Councils.id)
            ?.value
    }

    fun findIdByMatchKey(matchKey: String): Int? {
        val year = matchKey.substringAfterLast("-", "").toIntOrNull() ?: return null
        val normalized = matchKey.substringBeforeLast("-", "")
        return transaction {
            Councils.select(Councils.id)
                .where { (Councils.normalizedName eq normalized) and (Councils.year eq year) }
                .singleOrNull()
                ?.get(Councils.id)
                ?.value
        }
    }

    fun search(query: String, limit: Int = 20, locale: String = "en"): List<CouncilSummaryDTO> = transaction {
        val q = query.trim()
        if (q.isBlank()) return@transaction emptyList()
        val escaped = q.replace("'", "''")
        val tsQuery = q.split("\\s+".toRegex()).joinToString(" & ")

        val tablePrefix = if (locale == "en") "councils" else "c"
        val fromSql = if (locale == "en") {
            "FROM councils"
        } else {
            """
            FROM councils c
            LEFT JOIN council_translations ct
              ON ct.council_id = c.id AND ct.locale = '$locale'
            """.trimIndent()
        }

        val selectSql = if (locale == "en") {
            """
            SELECT councils.id
            $fromSql
            WHERE to_tsvector('english',
                coalesce(councils.display_name,'') || ' ' ||
                coalesce(councils.summary,'') || ' ' ||
                coalesce(councils.main_topics,'')
            ) @@ to_tsquery('english', '$tsQuery')
            OR councils.display_name % '$escaped'
            OR lower(councils.display_name) LIKE lower('%$escaped%')
            ORDER BY councils.year ASC, councils.id ASC
            LIMIT $limit
            """.trimIndent()
        } else {
            """
            SELECT c.id
            $fromSql
            WHERE to_tsvector('english',
                coalesce(c.display_name,'') || ' ' ||
                coalesce(c.summary,'') || ' ' ||
                coalesce(c.main_topics,'') || ' ' ||
                coalesce(ct.display_name,'') || ' ' ||
                coalesce(ct.summary,'') || ' ' ||
                coalesce(ct.main_topics,'')
            ) @@ to_tsquery('english', '$tsQuery')
            OR c.display_name % '$escaped'
            OR lower(c.display_name) LIKE lower('%$escaped%')
            OR lower(coalesce(ct.display_name,'')) LIKE lower('%$escaped%')
            ORDER BY c.year ASC, c.id ASC
            LIMIT $limit
            """.trimIndent()
        }

        val ids = mutableListOf<Int>()
        exec(selectSql) { rs ->
            while (rs.next()) ids += rs.getInt(1)
        }

        if (ids.isEmpty()) return@transaction emptyList()

        sourceFor(locale).selectAll()
            .where { Councils.id inList ids }
            .orderBy(Councils.year to SortOrder.ASC, Councils.id to SortOrder.ASC)
            .map { it.toSummary(locale) }
    }

    fun listMapPoints(): List<CouncilMapPointDTO> = transaction {
        Councils.selectAll()
            .where { Councils.latitude.isNotNull() and Councils.longitude.isNotNull() }
            .orderBy(Councils.year to SortOrder.ASC)
            .map {
                CouncilMapPointDTO(
                    id = it[Councils.id].value,
                    slug = it[Councils.slug],
                    displayName = it[Councils.displayName],
                    year = it[Councils.year],
                    councilType = it[Councils.councilType],
                    latitude = it[Councils.latitude] ?: 0.0,
                    longitude = it[Councils.longitude] ?: 0.0
                )
            }
    }

    fun listTypeSummary(): List<CouncilTypeSummaryDTO> = transaction {
        Councils
            .select(Councils.councilType, Councils.id.count())
            .groupBy(Councils.councilType)
            .orderBy(Councils.id.count() to SortOrder.DESC)
            .map {
                CouncilTypeSummaryDTO(
                    councilType = it[Councils.councilType],
                    count = it[Councils.id.count()].toInt()
                )
            }
    }

    fun auditAll(maxYear: Int? = null, onlyMissing: Boolean = false): List<CouncilAuditDTO> = transaction {
        var query = Councils.selectAll()
        if (maxYear != null) {
            query = query.andWhere { Councils.year lessEq maxYear }
        }
        query = query.orderBy(Councils.year to SortOrder.ASC, Councils.id to SortOrder.ASC)

        query.map { row ->
            val id = row[Councils.id].value
            val shortDesc = row[Councils.shortDescription]
            val origText = row[Councils.originalText]
            val summary = row[Councils.summary]
            val wikiUrl = row[Councils.wikipediaUrl]

            val hasShortDescription = !shortDesc.isNullOrBlank()
            val hasOriginalText = !origText.isNullOrBlank()
            val hasSummary = !summary.isNullOrBlank()
            val hasWikipediaUrl = !wikiUrl.isNullOrBlank()

            if (onlyMissing && (hasShortDescription || hasOriginalText || hasSummary)) {
                return@map null
            }

            val canonCount = CouncilCanons.selectAll().where { CouncilCanons.councilId eq id }.count()
            val fatherCount = CouncilFathers.selectAll().where { CouncilFathers.councilId eq id }.count()
            val hereticCount = CouncilHereticParticipants.selectAll().where { CouncilHereticParticipants.councilId eq id }.count()
            val sourceClaimCount = CouncilSourceClaims.selectAll().where { CouncilSourceClaims.councilId eq id }.count()

            CouncilAuditDTO(
                id = id,
                displayName = row[Councils.displayName],
                slug = row[Councils.slug],
                year = row[Councils.year],
                councilType = row[Councils.councilType],
                hasShortDescription = hasShortDescription,
                hasOriginalText = hasOriginalText,
                hasSummary = hasSummary,
                hasCanons = canonCount > 0,
                hasFathers = fatherCount > 0,
                hasHeretics = hereticCount > 0,
                hasWikipediaUrl = hasWikipediaUrl,
                sourceCount = sourceClaimCount.toInt()
            )
        }.filterNotNull()
    }

    fun insertOrUpdate(entry: CouncilSeedEntry): Int = transaction {
        val normalized = CouncilNameNormalizer.normalize(entry.displayName)
        val slug = "${normalized}-${entry.year}"
        val existing = Councils.selectAll().where {
            (Councils.normalizedName eq normalized) and (Councils.year eq entry.year)
        }.singleOrNull()

        if (existing != null) {
            val id = existing[Councils.id].value
            Councils.update({ Councils.id eq id }) {
                it[displayName] = entry.displayName
                it[yearEnd] = entry.yearEnd
                it[century] = entry.century
                it[councilType] = entry.councilType
                it[location] = entry.location
                it[latitude] = entry.latitude
                it[longitude] = entry.longitude
                it[shortDescription] = entry.shortDescription
                it[mainTopics] = entry.mainTopics
                it[keyParticipants] = entry.keyParticipants
                it[numberOfParticipants] = entry.numberOfParticipants
                it[wikipediaUrl] = entry.wikipediaUrl
                it[wikidataId] = entry.wikidataId
                it[updatedAt] = OffsetDateTime.now()
            }
            return@transaction id
        }

        Councils.insertAndGetId {
            it[displayName] = entry.displayName
            it[normalizedName] = normalized
            it[Councils.slug] = slug
            it[year] = entry.year
            it[yearEnd] = entry.yearEnd
            it[century] = entry.century
            it[councilType] = entry.councilType
            it[location] = entry.location
            it[latitude] = entry.latitude
            it[longitude] = entry.longitude
            it[shortDescription] = entry.shortDescription
            it[mainTopics] = entry.mainTopics
            it[keyParticipants] = entry.keyParticipants
            it[numberOfParticipants] = entry.numberOfParticipants
            it[wikipediaUrl] = entry.wikipediaUrl
            it[wikidataId] = entry.wikidataId
            it[dataSource] = "seed"
            it[createdAt] = OffsetDateTime.now()
            it[updatedAt] = OffsetDateTime.now()
        }.value
    }

    fun updateOriginalText(councilId: Int, originalText: String): Boolean = transaction {
        Councils.update({ Councils.id eq councilId }) {
            it[Councils.originalText] = originalText
            it[updatedAt] = OffsetDateTime.now()
        } > 0
    }

    fun updateSummary(councilId: Int, summary: String, reviewed: Boolean = false): Boolean = transaction {
        Councils.update({ Councils.id eq councilId }) {
            it[Councils.summary] = summary
            it[summaryReviewed] = reviewed
            it[updatedAt] = OffsetDateTime.now()
        } > 0
    }

    data class TranslationMeta(val translationSource: String?, val hasDisplayName: Boolean, val hasSummary: Boolean)

    fun findTranslationMeta(councilId: Int, locale: String): TranslationMeta? = transaction {
        CouncilTranslations.selectAll().where {
            (CouncilTranslations.councilId eq councilId) and (CouncilTranslations.locale eq locale)
        }.singleOrNull()?.let { row ->
            TranslationMeta(
                translationSource = row[CouncilTranslations.translationSource],
                hasDisplayName = !row[CouncilTranslations.displayName].isNullOrBlank(),
                hasSummary = !row[CouncilTranslations.summary].isNullOrBlank()
            )
        }
    }

    fun insertOrUpdateTranslation(
        councilId: Int,
        locale: String,
        displayName: String,
        shortDescription: String? = null,
        location: String? = null,
        mainTopics: String? = null,
        summary: String? = null,
        translationSource: String = "seed"
    ): Boolean = transaction {
        val existing = CouncilTranslations.selectAll().where {
            (CouncilTranslations.councilId eq councilId) and (CouncilTranslations.locale eq locale)
        }.singleOrNull()

        if (existing == null) {
            CouncilTranslations.insert {
                it[CouncilTranslations.councilId] = councilId
                it[CouncilTranslations.locale] = locale
                it[CouncilTranslations.displayName] = displayName
                it[CouncilTranslations.shortDescription] = shortDescription
                it[CouncilTranslations.location] = location
                it[CouncilTranslations.mainTopics] = mainTopics
                it[CouncilTranslations.summary] = summary
                it[CouncilTranslations.translationSource] = translationSource
            }
        } else {
            CouncilTranslations.update({ CouncilTranslations.id eq existing[CouncilTranslations.id].value }) {
                it[CouncilTranslations.displayName] = displayName
                it[CouncilTranslations.shortDescription] = shortDescription
                it[CouncilTranslations.location] = location
                it[CouncilTranslations.mainTopics] = mainTopics
                if (!summary.isNullOrBlank()) {
                    it[CouncilTranslations.summary] = summary
                }
                it[CouncilTranslations.translationSource] = translationSource
            }
        }
        true
    }

    fun updateConsensus(
        councilId: Int,
        year: Int?,
        yearEnd: Int?,
        location: String?,
        participants: Int?,
        confidence: Double,
        dataConfidence: String,
        sourceCount: Int,
        conflictResolution: String?
    ): Boolean = transaction {
        Councils.update({ Councils.id eq councilId }) {
            if (year != null) it[Councils.year] = year
            it[Councils.yearEnd] = yearEnd
            if (!location.isNullOrBlank()) it[Councils.location] = location
            it[Councils.numberOfParticipants] = participants
            it[consensusConfidence] = confidence
            it[Councils.dataConfidence] = dataConfidence
            it[Councils.sourceCount] = sourceCount
            it[Councils.conflictResolution] = conflictResolution
            it[updatedAt] = OffsetDateTime.now()
        } > 0
    }

    private fun applyFilters(query: Query, century: Int?, type: String?, yearMin: Int?, yearMax: Int?) {
        if (century != null) query.andWhere { Councils.century eq century }
        if (!type.isNullOrBlank()) query.andWhere { Councils.councilType eq type }
        if (yearMin != null) query.andWhere { Councils.year greaterEq yearMin }
        if (yearMax != null) query.andWhere { Councils.year lessEq yearMax }
    }

    private fun ResultRow.toSummary(locale: String): CouncilSummaryDTO {
        val translatedName = if (locale == "en") null else this.getOrNull(CouncilTranslations.displayName)
        return CouncilSummaryDTO(
            id = this[Councils.id].value,
            displayName = translatedName ?: this[Councils.displayName],
            slug = this[Councils.slug],
            year = this[Councils.year],
            yearEnd = this[Councils.yearEnd],
            century = this[Councils.century],
            councilType = this[Councils.councilType],
            location = if (locale == "en") this[Councils.location]
                else this.getOrNull(CouncilTranslations.location) ?: this[Councils.location],
            latitude = this[Councils.latitude],
            longitude = this[Councils.longitude],
            numberOfParticipants = this[Councils.numberOfParticipants],
            consensusConfidence = this[Councils.consensusConfidence],
            dataConfidence = this[Councils.dataConfidence],
            sourceCount = this[Councils.sourceCount]
        )
    }

    private fun ResultRow.toDetail(locale: String): CouncilDetailDTO {
        val translatedName = if (locale == "en") null else this.getOrNull(CouncilTranslations.displayName)
        return CouncilDetailDTO(
            id = this[Councils.id].value,
            displayName = translatedName ?: this[Councils.displayName],
            slug = this[Councils.slug],
            year = this[Councils.year],
            yearEnd = this[Councils.yearEnd],
            century = this[Councils.century],
            councilType = this[Councils.councilType],
            location = if (locale == "en") this[Councils.location]
                else this.getOrNull(CouncilTranslations.location) ?: this[Councils.location],
            latitude = this[Councils.latitude],
            longitude = this[Councils.longitude],
            shortDescription = if (locale == "en") this[Councils.shortDescription]
                else this.getOrNull(CouncilTranslations.shortDescription) ?: this[Councils.shortDescription],
            mainTopics = if (locale == "en") this[Councils.mainTopics]
                else this.getOrNull(CouncilTranslations.mainTopics) ?: this[Councils.mainTopics],
            keyParticipants = this[Councils.keyParticipants],
            numberOfParticipants = this[Councils.numberOfParticipants],
            originalText = this[Councils.originalText],
            summary = if (locale == "en") this[Councils.summary]
                else this.getOrNull(CouncilTranslations.summary) ?: this[Councils.summary],
            summaryReviewed = this[Councils.summaryReviewed],
            wikipediaUrl = this[Councils.wikipediaUrl],
            consensusConfidence = this[Councils.consensusConfidence],
            dataConfidence = this[Councils.dataConfidence],
            sourceCount = this[Councils.sourceCount],
            conflictResolution = this[Councils.conflictResolution]
        )
    }
}
