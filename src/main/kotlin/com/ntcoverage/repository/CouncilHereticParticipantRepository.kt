package com.ntcoverage.repository

import com.ntcoverage.model.CouncilHereticParticipantDTO
import com.ntcoverage.model.CouncilHereticParticipants
import com.ntcoverage.model.Councils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class CouncilHereticParticipantRepository {

    fun findByCouncilId(councilId: Int): List<CouncilHereticParticipantDTO> = transaction {
        CouncilHereticParticipants.selectAll()
            .where { CouncilHereticParticipants.councilId eq councilId }
            .orderBy(CouncilHereticParticipants.displayName to SortOrder.ASC)
            .map {
                CouncilHereticParticipantDTO(
                    id = it[CouncilHereticParticipants.id].value,
                    displayName = it[CouncilHereticParticipants.displayName],
                    role = it[CouncilHereticParticipants.role],
                    description = it[CouncilHereticParticipants.description]
                )
            }
    }

    fun findByCouncilSlug(slug: String): List<CouncilHereticParticipantDTO> = transaction {
        val councilId = Councils.select(Councils.id)
            .where { Councils.slug eq slug }
            .singleOrNull()
            ?.get(Councils.id)
            ?.value
            ?: return@transaction emptyList()
        findByCouncilId(councilId)
    }

    fun insertIfNotExists(
        councilId: Int,
        displayName: String,
        normalizedName: String,
        role: String?,
        description: String?
    ): Boolean = transaction {
        val exists = CouncilHereticParticipants.selectAll().where {
            (CouncilHereticParticipants.councilId eq councilId) and (CouncilHereticParticipants.normalizedName eq normalizedName)
        }.count() > 0

        if (!exists) {
            CouncilHereticParticipants.insert {
                it[CouncilHereticParticipants.councilId] = councilId
                it[CouncilHereticParticipants.displayName] = displayName
                it[CouncilHereticParticipants.normalizedName] = normalizedName
                it[CouncilHereticParticipants.role] = role
                it[CouncilHereticParticipants.description] = description
            }
        }
        !exists
    }
}
