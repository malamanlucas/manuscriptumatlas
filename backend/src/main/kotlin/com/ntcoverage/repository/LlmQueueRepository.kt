package com.ntcoverage.repository

import com.ntcoverage.model.LlmPromptQueue
import com.ntcoverage.model.QueueItemDTO
import com.ntcoverage.model.QueuePhaseStatsDTO
import com.ntcoverage.model.QueueStatsDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset

class LlmQueueRepository {

    fun enqueue(
        phaseName: String,
        label: String,
        systemPrompt: String,
        userContent: String,
        temperature: Double = 0.3,
        maxTokens: Int = 1024,
        tier: String,
        callbackContext: String? = null,
        batchId: String? = null
    ): Int = transaction {
        LlmPromptQueue.insertAndGetId {
            it[LlmPromptQueue.phaseName] = phaseName
            it[LlmPromptQueue.label] = label
            it[LlmPromptQueue.systemPrompt] = systemPrompt
            it[LlmPromptQueue.userContent] = userContent
            it[LlmPromptQueue.temperature] = temperature
            it[LlmPromptQueue.maxTokens] = maxTokens
            it[LlmPromptQueue.tier] = tier
            it[LlmPromptQueue.callbackContext] = callbackContext
            it[LlmPromptQueue.batchId] = batchId
            it[LlmPromptQueue.createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
        }.value
    }

    fun enqueueBatch(items: List<EnqueueItem>, batchId: String): List<Int> = transaction {
        items.map { item ->
            LlmPromptQueue.insertAndGetId {
                it[phaseName] = item.phaseName
                it[label] = item.label
                it[systemPrompt] = item.systemPrompt
                it[userContent] = item.userContent
                it[temperature] = item.temperature
                it[maxTokens] = item.maxTokens
                it[tier] = item.tier
                it[callbackContext] = item.callbackContext
                it[LlmPromptQueue.batchId] = batchId
                it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }.value
        }
    }

    fun getPending(limit: Int = 100, tier: String? = null): List<QueueItemDTO> = transaction {
        val query = LlmPromptQueue.selectAll()
            .where { LlmPromptQueue.status eq "pending" }
        if (tier != null) {
            query.andWhere { LlmPromptQueue.tier eq tier }
        }
        query
            .orderBy(LlmPromptQueue.id to SortOrder.ASC)
            .limit(limit)
            .map { it.toDTO() }
    }

    fun getPendingByPhase(phaseName: String, limit: Int = 100): List<QueueItemDTO> = transaction {
        LlmPromptQueue.selectAll()
            .where { (LlmPromptQueue.status eq "pending") and (LlmPromptQueue.phaseName eq phaseName) }
            .orderBy(LlmPromptQueue.id to SortOrder.ASC)
            .limit(limit)
            .map { it.toDTO() }
    }

    fun markProcessing(id: Int): Boolean = transaction {
        LlmPromptQueue.update({ LlmPromptQueue.id eq id }) {
            it[status] = "processing"
        } > 0
    }

    /**
     * Atomically claims pending items using SELECT FOR UPDATE SKIP LOCKED.
     * Safe for concurrent consumers — each caller gets unique items.
     */
    fun claimPending(limit: Int = 50, tier: String? = null, phase: String? = null): List<QueueItemDTO> = transaction {
        // Step 1: SELECT ids with FOR UPDATE SKIP LOCKED (raw SQL for PG-specific locking)
        val tierFilter = if (tier != null) "AND tier = '${tier.replace("'", "''")}'" else ""
        val phaseFilter = if (phase != null) "AND phase_name = '${phase.replace("'", "''")}'" else ""
        val selectSql = """
            SELECT id FROM llm_prompt_queue
            WHERE status = 'pending' $tierFilter $phaseFilter
            ORDER BY id ASC
            LIMIT $limit
            FOR UPDATE SKIP LOCKED
        """.trimIndent()

        val ids = mutableListOf<Int>()
        exec(selectSql) { rs -> while (rs.next()) ids.add(rs.getInt("id")) }
        if (ids.isEmpty()) return@transaction emptyList()

        // Step 2: UPDATE those ids to processing
        LlmPromptQueue.update({ LlmPromptQueue.id inList ids }) {
            it[status] = "processing"
        }

        // Step 3: Return the full items
        LlmPromptQueue.selectAll()
            .where { LlmPromptQueue.id inList ids }
            .orderBy(LlmPromptQueue.id to SortOrder.ASC)
            .map { it.toDTO() }
    }

    fun markCompleted(
        id: Int,
        responseContent: String,
        modelUsed: String,
        inputTokens: Int,
        outputTokens: Int
    ): Boolean = transaction {
        LlmPromptQueue.update({ LlmPromptQueue.id eq id }) {
            it[status] = "completed"
            it[LlmPromptQueue.responseContent] = responseContent
            it[LlmPromptQueue.modelUsed] = modelUsed
            it[LlmPromptQueue.inputTokens] = inputTokens
            it[LlmPromptQueue.outputTokens] = outputTokens
            it[processedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        } > 0
    }

    fun markFailed(id: Int, errorMessage: String): Boolean = transaction {
        LlmPromptQueue.update({ LlmPromptQueue.id eq id }) {
            it[status] = "failed"
            it[LlmPromptQueue.errorMessage] = errorMessage
            it[processedAt] = OffsetDateTime.now(ZoneOffset.UTC)
        } > 0
    }

    fun getCompletedByPhase(phaseName: String): List<QueueItemDTO> = transaction {
        LlmPromptQueue.selectAll()
            .where { (LlmPromptQueue.status eq "completed") and (LlmPromptQueue.phaseName eq phaseName) }
            .orderBy(LlmPromptQueue.id to SortOrder.ASC)
            .map { it.toDTO() }
    }

    fun getStats(): QueueStatsDTO = transaction {
        val rows = LlmPromptQueue
            .select(LlmPromptQueue.phaseName, LlmPromptQueue.status, LlmPromptQueue.id.count())
            .groupBy(LlmPromptQueue.phaseName, LlmPromptQueue.status)
            .map { Triple(it[LlmPromptQueue.phaseName], it[LlmPromptQueue.status], it[LlmPromptQueue.id.count()]) }

        val byPhaseMap = mutableMapOf<String, MutableMap<String, Long>>()
        for ((phase, status, count) in rows) {
            byPhaseMap.getOrPut(phase) { mutableMapOf() }[status] = count
        }

        val byPhase = byPhaseMap.map { (phase, statuses) ->
            QueuePhaseStatsDTO(
                phaseName = phase,
                pending = statuses["pending"] ?: 0,
                processing = statuses["processing"] ?: 0,
                completed = statuses["completed"] ?: 0,
                applied = statuses["applied"] ?: 0,
                failed = statuses["failed"] ?: 0
            )
        }.sortedBy { it.phaseName }

        QueueStatsDTO(
            totalPending = byPhase.sumOf { it.pending },
            totalProcessing = byPhase.sumOf { it.processing },
            totalCompleted = byPhase.sumOf { it.completed },
            totalApplied = byPhase.sumOf { it.applied },
            totalFailed = byPhase.sumOf { it.failed },
            byPhase = byPhase
        )
    }

    fun markApplied(id: Int): Boolean = transaction {
        LlmPromptQueue.update({ LlmPromptQueue.id eq id }) {
            it[status] = "applied"
        } > 0
    }

    fun retryFailed(phaseName: String? = null): Int = transaction {
        val condition = if (phaseName != null) {
            (LlmPromptQueue.status eq "failed") and (LlmPromptQueue.phaseName eq phaseName)
        } else {
            LlmPromptQueue.status eq "failed"
        }
        LlmPromptQueue.update({ condition }) {
            it[status] = "pending"
            it[errorMessage] = null
            it[processedAt] = null
        }
    }

    fun unstickProcessing(phaseName: String? = null): Int = transaction {
        val condition = if (phaseName != null) {
            (LlmPromptQueue.status eq "processing") and (LlmPromptQueue.phaseName eq phaseName)
        } else {
            LlmPromptQueue.status eq "processing"
        }
        LlmPromptQueue.update({ condition }) {
            it[status] = "pending"
            it[processedAt] = null
            it[responseContent] = null
            it[modelUsed] = null
        }
    }

    fun clearByPhase(phaseName: String): Int = transaction {
        LlmPromptQueue.deleteWhere { LlmPromptQueue.phaseName eq phaseName }
    }

    fun clearByPrefix(prefix: String): Int = transaction {
        LlmPromptQueue.deleteWhere { LlmPromptQueue.phaseName like "$prefix%" }
    }

    private fun ResultRow.toDTO() = QueueItemDTO(
        id = this[LlmPromptQueue.id].value,
        phaseName = this[LlmPromptQueue.phaseName],
        label = this[LlmPromptQueue.label],
        systemPrompt = this[LlmPromptQueue.systemPrompt],
        userContent = this[LlmPromptQueue.userContent],
        temperature = this[LlmPromptQueue.temperature],
        maxTokens = this[LlmPromptQueue.maxTokens],
        tier = this[LlmPromptQueue.tier],
        status = this[LlmPromptQueue.status],
        responseContent = this[LlmPromptQueue.responseContent],
        modelUsed = this[LlmPromptQueue.modelUsed],
        inputTokens = this[LlmPromptQueue.inputTokens],
        outputTokens = this[LlmPromptQueue.outputTokens],
        errorMessage = this[LlmPromptQueue.errorMessage],
        callbackContext = this[LlmPromptQueue.callbackContext],
        createdAt = this[LlmPromptQueue.createdAt].toString(),
        processedAt = this[LlmPromptQueue.processedAt]?.toString(),
        batchId = this[LlmPromptQueue.batchId]
    )
}

data class EnqueueItem(
    val phaseName: String,
    val label: String,
    val systemPrompt: String,
    val userContent: String,
    val temperature: Double = 0.3,
    val maxTokens: Int = 1024,
    val tier: String,
    val callbackContext: String? = null
)
