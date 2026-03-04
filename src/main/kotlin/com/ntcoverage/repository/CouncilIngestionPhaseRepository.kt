package com.ntcoverage.repository

import com.ntcoverage.model.CouncilIngestionPhases
import com.ntcoverage.model.PhaseStatusDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime

class CouncilIngestionPhaseRepository {

    fun markRunning(phaseName: String, itemsTotal: Int = 0, lastRunBy: String = "manual"): PhaseStatusDTO = transaction {
        val existing = findRow(phaseName)
        if (existing == null) {
            CouncilIngestionPhases.insert {
                it[CouncilIngestionPhases.phaseName] = phaseName
                it[status] = "running"
                it[startedAt] = OffsetDateTime.now()
                it[completedAt] = null
                it[itemsProcessed] = 0
                it[CouncilIngestionPhases.itemsTotal] = itemsTotal
                it[errorMessage] = null
                it[CouncilIngestionPhases.lastRunBy] = lastRunBy
            }
        } else {
            CouncilIngestionPhases.update({ CouncilIngestionPhases.id eq existing[CouncilIngestionPhases.id].value }) {
                it[status] = "running"
                it[startedAt] = OffsetDateTime.now()
                it[completedAt] = null
                it[itemsProcessed] = 0
                it[CouncilIngestionPhases.itemsTotal] = itemsTotal
                it[errorMessage] = null
                it[CouncilIngestionPhases.lastRunBy] = lastRunBy
            }
        }
        getByPhase(phaseName) ?: PhaseStatusDTO(phaseName = phaseName, status = "running")
    }

    fun markProgress(phaseName: String, processed: Int, total: Int? = null): Boolean = transaction {
        CouncilIngestionPhases.update({ CouncilIngestionPhases.phaseName eq phaseName }) {
            it[itemsProcessed] = processed
            if (total != null) it[itemsTotal] = total
        } > 0
    }

    fun markSuccess(phaseName: String, processed: Int): Boolean = transaction {
        CouncilIngestionPhases.update({ CouncilIngestionPhases.phaseName eq phaseName }) {
            it[status] = "success"
            it[itemsProcessed] = processed
            it[completedAt] = OffsetDateTime.now()
            it[errorMessage] = null
        } > 0
    }

    fun markFailed(phaseName: String, error: String): Boolean = transaction {
        CouncilIngestionPhases.update({ CouncilIngestionPhases.phaseName eq phaseName }) {
            it[status] = "failed"
            it[completedAt] = OffsetDateTime.now()
            it[errorMessage] = error.take(5000)
        } > 0
    }

    fun getByPhase(phaseName: String): PhaseStatusDTO? = transaction {
        CouncilIngestionPhases.selectAll()
            .where { CouncilIngestionPhases.phaseName eq phaseName }
            .singleOrNull()
            ?.toDto()
    }

    fun getAll(): List<PhaseStatusDTO> = transaction {
        CouncilIngestionPhases.selectAll()
            .orderBy(CouncilIngestionPhases.phaseName to SortOrder.ASC)
            .map { it.toDto() }
    }

    fun isAnyRunning(): Boolean = transaction {
        CouncilIngestionPhases.selectAll()
            .where { CouncilIngestionPhases.status eq "running" }
            .count() > 0
    }

    private fun findRow(phaseName: String): ResultRow? =
        CouncilIngestionPhases.selectAll()
            .where { CouncilIngestionPhases.phaseName eq phaseName }
            .singleOrNull()

    private fun ResultRow.toDto() = PhaseStatusDTO(
        phaseName = this[CouncilIngestionPhases.phaseName],
        status = this[CouncilIngestionPhases.status],
        startedAt = this[CouncilIngestionPhases.startedAt]?.toString(),
        completedAt = this[CouncilIngestionPhases.completedAt]?.toString(),
        itemsProcessed = this[CouncilIngestionPhases.itemsProcessed],
        itemsTotal = this[CouncilIngestionPhases.itemsTotal],
        errorMessage = this[CouncilIngestionPhases.errorMessage],
        lastRunBy = this[CouncilIngestionPhases.lastRunBy]
    )
}
