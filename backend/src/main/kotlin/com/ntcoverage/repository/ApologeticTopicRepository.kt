package com.ntcoverage.repository

import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ApologeticTopicRepository {

    private val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    private fun translatedJoin(locale: String) =
        ApologeticTopics.join(ApologeticTopicTranslations, JoinType.LEFT,
            additionalConstraint = {
                (ApologeticTopics.id eq ApologeticTopicTranslations.topicId) and
                    (ApologeticTopicTranslations.locale eq locale)
            })

    private fun sourceFor(locale: String) =
        if (locale == "en") ApologeticTopics else translatedJoin(locale)

    fun findAll(
        page: Int = 1,
        limit: Int = 20,
        locale: String = "en",
        status: String? = null
    ): List<ApologeticTopicSummaryDTO> = transaction {
        val query = sourceFor(locale).selectAll()
        if (!status.isNullOrBlank()) query.andWhere { ApologeticTopics.status eq status }
        query
            .orderBy(ApologeticTopics.createdAt to SortOrder.DESC, ApologeticTopics.id to SortOrder.DESC)
            .limit(limit)
            .offset(((page - 1) * limit).toLong())
            .map { it.toSummary(locale) }
    }

    fun countAll(status: String? = null): Int = transaction {
        val query = ApologeticTopics.selectAll()
        if (!status.isNullOrBlank()) query.andWhere { ApologeticTopics.status eq status }
        query.count().toInt()
    }

    fun findBySlug(slug: String, locale: String = "en"): ApologeticTopicDetailDTO? = transaction {
        sourceFor(locale).selectAll()
            .where { ApologeticTopics.slug eq slug }
            .singleOrNull()
            ?.toDetail(locale)
    }

    fun findById(id: Int, locale: String = "en"): ApologeticTopicDetailDTO? = transaction {
        sourceFor(locale).selectAll()
            .where { ApologeticTopics.id eq id }
            .singleOrNull()
            ?.toDetail(locale)
    }

    fun search(query: String, limit: Int = 20, locale: String = "en"): List<ApologeticTopicSummaryDTO> = transaction {
        val q = query.trim()
        if (q.isBlank()) return@transaction emptyList()
        // Sanitize: remove chars that break tsquery/SQL, then escape for SQL strings
        val sanitized = q.replace(Regex("['\";\\\\]"), "").trim()
        if (sanitized.isBlank()) return@transaction emptyList()
        val escaped = sanitized.replace("'", "''")
        val tsQuery = sanitized.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString(" & ")

        val selectSql = if (locale == "en") {
            """
            SELECT apologetic_topics.id
            FROM apologetic_topics
            WHERE to_tsvector('english',
                coalesce(apologetic_topics.title,'') || ' ' ||
                coalesce(apologetic_topics.body,'')
            ) @@ to_tsquery('english', '$tsQuery')
            OR unaccent(lower(apologetic_topics.title)) LIKE unaccent(lower('%$escaped%'))
            OR apologetic_topics.title % '$escaped'
            ORDER BY apologetic_topics.created_at DESC, apologetic_topics.id DESC
            LIMIT $limit
            """.trimIndent()
        } else {
            """
            SELECT at.id
            FROM apologetic_topics at
            LEFT JOIN apologetic_topic_translations att
              ON att.topic_id = at.id AND att.locale = '$locale'
            WHERE to_tsvector('english',
                coalesce(at.title,'') || ' ' ||
                coalesce(at.body,'') || ' ' ||
                coalesce(att.title,'') || ' ' ||
                coalesce(att.body,'')
            ) @@ to_tsquery('english', '$tsQuery')
            OR unaccent(lower(at.title)) LIKE unaccent(lower('%$escaped%'))
            OR unaccent(lower(coalesce(att.title,''))) LIKE unaccent(lower('%$escaped%'))
            OR at.title % '$escaped'
            ORDER BY at.created_at DESC, at.id DESC
            LIMIT $limit
            """.trimIndent()
        }

        val ids = mutableListOf<Int>()
        exec(selectSql) { rs -> while (rs.next()) ids += rs.getInt(1) }
        if (ids.isEmpty()) return@transaction emptyList()

        sourceFor(locale).selectAll()
            .where { ApologeticTopics.id inList ids }
            .orderBy(ApologeticTopics.createdAt to SortOrder.DESC, ApologeticTopics.id to SortOrder.DESC)
            .map { it.toSummary(locale) }
    }

    fun insert(title: String, slug: String, originalPrompt: String, body: String, createdByEmail: String?, status: String = "DRAFT"): Int = transaction {
        val finalSlug = ensureUniqueSlug(slug)
        ApologeticTopics.insertAndGetId {
            it[ApologeticTopics.title] = title
            it[ApologeticTopics.slug] = finalSlug
            it[ApologeticTopics.originalPrompt] = originalPrompt
            it[ApologeticTopics.body] = body
            it[ApologeticTopics.status] = status
            it[ApologeticTopics.createdByEmail] = createdByEmail
            it[createdAt] = OffsetDateTime.now()
            it[updatedAt] = OffsetDateTime.now()
        }.value
    }

    fun update(id: Int, title: String?, body: String?, status: String?, bodyReviewed: Boolean?): Boolean = transaction {
        ApologeticTopics.update({ ApologeticTopics.id eq id }) {
            if (title != null) it[ApologeticTopics.title] = title
            if (body != null) it[ApologeticTopics.body] = body
            if (status != null) it[ApologeticTopics.status] = status
            if (bodyReviewed != null) it[ApologeticTopics.bodyReviewed] = bodyReviewed
            it[updatedAt] = OffsetDateTime.now()
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        ApologeticTopics.deleteWhere { ApologeticTopics.id eq id } > 0
    }

    private fun ensureUniqueSlug(baseSlug: String): String {
        val exists = ApologeticTopics.selectAll()
            .where { ApologeticTopics.slug eq baseSlug }
            .count() > 0
        if (!exists) return baseSlug

        var suffix = 2
        while (true) {
            val candidate = "$baseSlug-$suffix"
            val taken = ApologeticTopics.selectAll()
                .where { ApologeticTopics.slug eq candidate }
                .count() > 0
            if (!taken) return candidate
            suffix++
        }
    }

    private fun ResultRow.toSummary(locale: String): ApologeticTopicSummaryDTO {
        val id = this[ApologeticTopics.id].value
        val translatedTitle = if (locale == "en") null else this.getOrNull(ApologeticTopicTranslations.title)
        val responseCount = ApologeticResponses.selectAll()
            .where { ApologeticResponses.topicId eq id }
            .count().toInt()

        return ApologeticTopicSummaryDTO(
            id = id,
            title = translatedTitle ?: this[ApologeticTopics.title],
            slug = this[ApologeticTopics.slug],
            status = this[ApologeticTopics.status],
            responseCount = responseCount,
            createdAt = this[ApologeticTopics.createdAt].format(fmt)
        )
    }

    private fun ResultRow.toDetail(locale: String): ApologeticTopicDetailDTO {
        val translatedTitle = if (locale == "en") null else this.getOrNull(ApologeticTopicTranslations.title)
        val translatedBody = if (locale == "en") null else this.getOrNull(ApologeticTopicTranslations.body)
        return ApologeticTopicDetailDTO(
            id = this[ApologeticTopics.id].value,
            title = translatedTitle ?: this[ApologeticTopics.title],
            slug = this[ApologeticTopics.slug],
            originalPrompt = this[ApologeticTopics.originalPrompt],
            body = translatedBody ?: this[ApologeticTopics.body],
            bodyReviewed = this[ApologeticTopics.bodyReviewed],
            status = this[ApologeticTopics.status],
            createdByEmail = this[ApologeticTopics.createdByEmail],
            createdAt = this[ApologeticTopics.createdAt].format(fmt),
            updatedAt = this[ApologeticTopics.updatedAt].format(fmt)
        )
    }
}
