package com.ntcoverage.repository

import com.ntcoverage.model.CouncilSourceClaims
import com.ntcoverage.model.Sources
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

data class SourceClaimWithMeta(
    val id: Int,
    val councilId: Int,
    val sourceId: Int,
    val sourceName: String,
    val sourceDisplayName: String,
    val sourceLevel: String,
    val sourceBaseWeight: Double,
    val claimedYear: Int? = null,
    val claimedYearEnd: Int? = null,
    val claimedLocation: String? = null,
    val claimedParticipants: Int? = null,
    val rawText: String? = null,
    val sourcePage: String? = null,
    val extractedAt: String
)

class CouncilSourceClaimRepository {
    private val join = CouncilSourceClaims
        .join(Sources, JoinType.INNER, additionalConstraint = { CouncilSourceClaims.sourceId eq Sources.id })

    fun findByCouncil(councilId: Int): List<SourceClaimWithMeta> = transaction {
        join.selectAll()
            .where { CouncilSourceClaims.councilId eq councilId }
            .orderBy(Sources.baseWeight to SortOrder.DESC, Sources.displayName to SortOrder.ASC)
            .map { it.toClaimWithMeta() }
    }

    fun findBySource(sourceId: Int): List<SourceClaimWithMeta> = transaction {
        join.selectAll()
            .where { CouncilSourceClaims.sourceId eq sourceId }
            .orderBy(CouncilSourceClaims.extractedAt to SortOrder.DESC)
            .map { it.toClaimWithMeta() }
    }

    fun upsertClaim(
        councilId: Int,
        sourceId: Int,
        claimedYear: Int? = null,
        claimedYearEnd: Int? = null,
        claimedLocation: String? = null,
        claimedParticipants: Int? = null,
        rawText: String? = null,
        sourcePage: String? = null
    ): Int = transaction {
        val existing = CouncilSourceClaims.selectAll().where {
            (CouncilSourceClaims.councilId eq councilId) and (CouncilSourceClaims.sourceId eq sourceId)
        }.singleOrNull()

        if (existing != null) {
            val id = existing[CouncilSourceClaims.id].value
            CouncilSourceClaims.update({ CouncilSourceClaims.id eq id }) {
                it[CouncilSourceClaims.claimedYear] = claimedYear
                it[CouncilSourceClaims.claimedYearEnd] = claimedYearEnd
                it[CouncilSourceClaims.claimedLocation] = claimedLocation
                it[CouncilSourceClaims.claimedParticipants] = claimedParticipants
                it[CouncilSourceClaims.rawText] = rawText
                it[CouncilSourceClaims.sourcePage] = sourcePage
                it[CouncilSourceClaims.extractedAt] = OffsetDateTime.now()
            }
            return@transaction id
        }

        CouncilSourceClaims.insertAndGetId {
            it[CouncilSourceClaims.councilId] = councilId
            it[CouncilSourceClaims.sourceId] = sourceId
            it[CouncilSourceClaims.claimedYear] = claimedYear
            it[CouncilSourceClaims.claimedYearEnd] = claimedYearEnd
            it[CouncilSourceClaims.claimedLocation] = claimedLocation
            it[CouncilSourceClaims.claimedParticipants] = claimedParticipants
            it[CouncilSourceClaims.rawText] = rawText
            it[CouncilSourceClaims.sourcePage] = sourcePage
            it[CouncilSourceClaims.extractedAt] = OffsetDateTime.now()
        }.value
    }

    private fun ResultRow.toClaimWithMeta() = SourceClaimWithMeta(
        id = this[CouncilSourceClaims.id].value,
        councilId = this[CouncilSourceClaims.councilId].value,
        sourceId = this[CouncilSourceClaims.sourceId].value,
        sourceName = this[Sources.name],
        sourceDisplayName = this[Sources.displayName],
        sourceLevel = this[Sources.sourceLevel],
        sourceBaseWeight = this[Sources.baseWeight],
        claimedYear = this[CouncilSourceClaims.claimedYear],
        claimedYearEnd = this[CouncilSourceClaims.claimedYearEnd],
        claimedLocation = this[CouncilSourceClaims.claimedLocation],
        claimedParticipants = this[CouncilSourceClaims.claimedParticipants],
        rawText = this[CouncilSourceClaims.rawText],
        sourcePage = this[CouncilSourceClaims.sourcePage],
        extractedAt = this[CouncilSourceClaims.extractedAt].toString()
    )
}
