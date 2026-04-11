package com.ntcoverage.repository

import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class ApologeticResponseRepository {

    private val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun findByTopicId(topicId: Int, locale: String = "en"): List<ApologeticResponseDTO> = transaction {
        val source = if (locale == "en") {
            ApologeticResponses
        } else {
            ApologeticResponses.join(ApologeticResponseTranslations, JoinType.LEFT,
                additionalConstraint = {
                    (ApologeticResponses.id eq ApologeticResponseTranslations.responseId) and
                        (ApologeticResponseTranslations.locale eq locale)
                })
        }

        source.selectAll()
            .where { ApologeticResponses.topicId eq topicId }
            .orderBy(ApologeticResponses.responseOrder to SortOrder.ASC, ApologeticResponses.id to SortOrder.ASC)
            .map { it.toDTO(locale) }
    }

    fun insert(topicId: Int, originalPrompt: String, body: String, createdByEmail: String?): Int = transaction {
        val nextOrder = ApologeticResponses.selectAll()
            .where { ApologeticResponses.topicId eq topicId }
            .count().toInt() + 1

        ApologeticResponses.insertAndGetId {
            it[ApologeticResponses.topicId] = topicId
            it[ApologeticResponses.originalPrompt] = originalPrompt
            it[ApologeticResponses.body] = body
            it[responseOrder] = nextOrder
            it[ApologeticResponses.createdByEmail] = createdByEmail
            it[createdAt] = OffsetDateTime.now()
            it[updatedAt] = OffsetDateTime.now()
        }.value
    }

    fun update(id: Int, body: String?, bodyReviewed: Boolean?): Boolean = transaction {
        ApologeticResponses.update({ ApologeticResponses.id eq id }) {
            if (body != null) it[ApologeticResponses.body] = body
            if (bodyReviewed != null) it[ApologeticResponses.bodyReviewed] = bodyReviewed
            it[updatedAt] = OffsetDateTime.now()
        } > 0
    }

    fun delete(id: Int): Boolean = transaction {
        ApologeticResponses.deleteWhere { ApologeticResponses.id eq id } > 0
    }

    fun countByTopicId(topicId: Int): Int = transaction {
        ApologeticResponses.selectAll()
            .where { ApologeticResponses.topicId eq topicId }
            .count().toInt()
    }

    private fun ResultRow.toDTO(locale: String): ApologeticResponseDTO {
        val translatedBody = if (locale == "en") null else this.getOrNull(ApologeticResponseTranslations.body)
        return ApologeticResponseDTO(
            id = this[ApologeticResponses.id].value,
            topicId = this[ApologeticResponses.topicId].value,
            originalPrompt = this[ApologeticResponses.originalPrompt],
            body = translatedBody ?: this[ApologeticResponses.body],
            bodyReviewed = this[ApologeticResponses.bodyReviewed],
            responseOrder = this[ApologeticResponses.responseOrder],
            createdAt = this[ApologeticResponses.createdAt].format(fmt),
            updatedAt = this[ApologeticResponses.updatedAt].format(fmt)
        )
    }
}
