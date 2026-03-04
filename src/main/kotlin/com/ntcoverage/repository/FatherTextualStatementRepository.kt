package com.ntcoverage.repository

import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

class FatherTextualStatementRepository {

    private val baseJoin
        get() = FatherTextualStatements.innerJoin(
            ChurchFathers,
            { fatherId },
            { ChurchFathers.id }
        )

    private fun translatedJoin(locale: String) =
        baseJoin.join(FatherStatementTranslations, JoinType.LEFT,
            additionalConstraint = {
                (FatherTextualStatements.id eq FatherStatementTranslations.statementId) and
                (FatherStatementTranslations.locale eq locale)
            })
        .join(ChurchFatherTranslations, JoinType.LEFT,
            additionalConstraint = {
                (ChurchFathers.id eq ChurchFatherTranslations.fatherId) and
                (ChurchFatherTranslations.locale eq locale)
            })

    private fun sourceFor(locale: String) =
        if (locale == "en") baseJoin else translatedJoin(locale)

    fun findByFather(fatherId: Int, locale: String = "en"): List<TextualStatementDTO> = transaction {
        sourceFor(locale).selectAll()
            .where { FatherTextualStatements.fatherId eq fatherId }
            .orderBy(
                FatherTextualStatements.approximateYear to SortOrder.ASC_NULLS_LAST,
                FatherTextualStatements.id to SortOrder.ASC
            )
            .map { it.toDTO(locale) }
    }

    fun findAll(
        topic: String? = null,
        century: Int? = null,
        tradition: String? = null,
        yearMin: Int? = null,
        yearMax: Int? = null,
        page: Int = 1,
        limit: Int = 20,
        locale: String = "en"
    ): List<TextualStatementDTO> = transaction {
        val query = sourceFor(locale).selectAll()
        applyFilters(query, topic, century, tradition, yearMin, yearMax)

        query
            .orderBy(
                FatherTextualStatements.approximateYear to SortOrder.ASC_NULLS_LAST,
                FatherTextualStatements.id to SortOrder.ASC
            )
            .limit(limit)
            .offset(((page - 1) * limit).toLong())
            .map { it.toDTO(locale) }
    }

    fun countAll(
        topic: String? = null,
        century: Int? = null,
        tradition: String? = null,
        yearMin: Int? = null,
        yearMax: Int? = null
    ): Int = transaction {
        val query = baseJoin.selectAll()
        applyFilters(query, topic, century, tradition, yearMin, yearMax)
        query.count().toInt()
    }

    fun searchByKeyword(q: String, limit: Int = 20, locale: String = "en"): List<TextualStatementDTO> = transaction {
        val similarityExpr = object : Expression<Float>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                queryBuilder.append("similarity(")
                queryBuilder.append(FatherTextualStatements.statementText)
                queryBuilder.append(", ")
                queryBuilder.registerArgument(VarCharColumnType(200), q)
                queryBuilder.append(")")
            }
        }

        val matchExpr = object : Op<Boolean>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                queryBuilder.append(FatherTextualStatements.statementText)
                queryBuilder.append(" % ")
                queryBuilder.registerArgument(VarCharColumnType(200), q)
            }
        }

        val ilikeFallback = object : Op<Boolean>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                queryBuilder.append("LOWER(")
                queryBuilder.append(FatherTextualStatements.statementText)
                queryBuilder.append(") LIKE ")
                queryBuilder.registerArgument(VarCharColumnType(200), "%${q.lowercase()}%")
            }
        }

        if (locale == "en") {
            baseJoin.selectAll()
                .where { matchExpr or ilikeFallback }
                .orderBy(similarityExpr to SortOrder.DESC)
                .limit(limit)
                .map { it.toDTO("en") }
        } else {
            val translatedMatch = object : Op<Boolean>() {
                override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                    queryBuilder.append(FatherStatementTranslations.statementText)
                    queryBuilder.append(" % ")
                    queryBuilder.registerArgument(VarCharColumnType(200), q)
                }
            }
            val translatedIlike = object : Op<Boolean>() {
                override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                    queryBuilder.append("LOWER(")
                    queryBuilder.append(FatherStatementTranslations.statementText)
                    queryBuilder.append(") LIKE ")
                    queryBuilder.registerArgument(VarCharColumnType(200), "%${q.lowercase()}%")
                }
            }

            translatedJoin(locale).selectAll()
                .where { matchExpr or ilikeFallback or translatedMatch or translatedIlike }
                .orderBy(similarityExpr to SortOrder.DESC)
                .limit(limit)
                .map { it.toDTO(locale) }
        }
    }

    fun countByTopic(): List<TopicSummaryDTO> = transaction {
        FatherTextualStatements
            .select(FatherTextualStatements.topic, FatherTextualStatements.topic.count())
            .groupBy(FatherTextualStatements.topic)
            .orderBy(FatherTextualStatements.topic.count() to SortOrder.DESC)
            .map {
                TopicSummaryDTO(
                    topic = it[FatherTextualStatements.topic],
                    count = it[FatherTextualStatements.topic.count()].toInt()
                )
            }
    }

    fun insertIfNotExists(
        fatherId: Int,
        topic: String,
        statementText: String,
        originalLanguage: String? = null,
        originalText: String? = null,
        sourceWork: String? = null,
        sourceReference: String? = null,
        approximateYear: Int? = null
    ): Int? = transaction {
        val existing = FatherTextualStatements.selectAll().where {
            (FatherTextualStatements.fatherId eq fatherId) and
            (FatherTextualStatements.sourceWork eq (sourceWork ?: "")) and
            (FatherTextualStatements.sourceReference eq (sourceReference ?: ""))
        }.singleOrNull()

        if (existing != null) return@transaction null

        FatherTextualStatements.insertAndGetId {
            it[FatherTextualStatements.fatherId] = fatherId
            it[FatherTextualStatements.topic] = topic
            it[FatherTextualStatements.statementText] = statementText
            it[FatherTextualStatements.originalLanguage] = originalLanguage
            it[FatherTextualStatements.originalText] = originalText
            it[FatherTextualStatements.sourceWork] = sourceWork
            it[FatherTextualStatements.sourceReference] = sourceReference
            it[FatherTextualStatements.approximateYear] = approximateYear
            it[createdAt] = OffsetDateTime.now()
        }.value
    }

    fun insertTranslation(
        statementId: Int,
        locale: String,
        statementText: String
    ): Unit = transaction {
        val exists = FatherStatementTranslations.selectAll().where {
            (FatherStatementTranslations.statementId eq statementId) and
            (FatherStatementTranslations.locale eq locale)
        }.count() > 0

        if (!exists) {
            FatherStatementTranslations.insert {
                it[FatherStatementTranslations.statementId] = statementId
                it[FatherStatementTranslations.locale] = locale
                it[FatherStatementTranslations.statementText] = statementText
            }
        }
    }

    private fun applyFilters(
        query: Query,
        topic: String?,
        century: Int?,
        tradition: String?,
        yearMin: Int? = null,
        yearMax: Int? = null
    ) {
        if (topic != null) {
            query.andWhere { FatherTextualStatements.topic eq topic }
        }
        if (yearMin != null || yearMax != null) {
            query.andWhere { ChurchFathers.yearMin.isNotNull() }
            if (yearMin != null) {
                query.andWhere { ChurchFathers.yearMax greaterEq yearMin }
            }
            if (yearMax != null) {
                query.andWhere { ChurchFathers.yearMin lessEq yearMax }
            }
        } else if (century != null) {
            query.andWhere {
                (ChurchFathers.centuryMin lessEq century) and
                (ChurchFathers.centuryMax greaterEq century)
            }
        }
        if (tradition != null) {
            query.andWhere { ChurchFathers.tradition eq tradition }
        }
    }

    private fun ResultRow.toDTO(locale: String = "en") = TextualStatementDTO(
        id = this[FatherTextualStatements.id].value,
        fatherId = this[FatherTextualStatements.fatherId],
        fatherName = if (locale == "en") this[ChurchFathers.displayName]
            else this.getOrNull(ChurchFatherTranslations.displayName) ?: this[ChurchFathers.displayName],
        topic = this[FatherTextualStatements.topic],
        statementText = if (locale == "en") this[FatherTextualStatements.statementText]
            else this.getOrNull(FatherStatementTranslations.statementText) ?: this[FatherTextualStatements.statementText],
        originalLanguage = this[FatherTextualStatements.originalLanguage],
        originalText = this[FatherTextualStatements.originalText],
        sourceWork = this[FatherTextualStatements.sourceWork],
        sourceReference = this[FatherTextualStatements.sourceReference],
        approximateYear = this[FatherTextualStatements.approximateYear]
    )
}
