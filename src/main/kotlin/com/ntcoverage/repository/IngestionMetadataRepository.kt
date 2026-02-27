package com.ntcoverage.repository

import com.ntcoverage.model.IngestionMetadata
import com.ntcoverage.model.IngestionStatusResponse
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class IngestionMetadataRepository {

    private val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun getStatus(): IngestionStatusResponse = transaction {
        val row = IngestionMetadata.selectAll().singleOrNull()
            ?: return@transaction IngestionStatusResponse(status = "idle")

        IngestionStatusResponse(
            status = row[IngestionMetadata.status],
            startedAt = row[IngestionMetadata.startedAt]?.format(fmt),
            finishedAt = row[IngestionMetadata.finishedAt]?.format(fmt),
            durationMs = row[IngestionMetadata.durationMs],
            manuscriptsIngested = row[IngestionMetadata.manuscriptsIngested] ?: 0,
            versesLinked = row[IngestionMetadata.versesLinked] ?: 0,
            errorMessage = row[IngestionMetadata.errorMessage]
        )
    }

    fun markRunning() = transaction {
        IngestionMetadata.update({ IngestionMetadata.id eq 1 }) {
            it[status] = "running"
            it[startedAt] = OffsetDateTime.now()
            it[finishedAt] = null
            it[durationMs] = null
            it[errorMessage] = null
            it[updatedAt] = OffsetDateTime.now()
        }
    }

    fun markSuccess(durationMs: Long, manuscriptsIngested: Int, versesLinked: Int) = transaction {
        IngestionMetadata.update({ IngestionMetadata.id eq 1 }) {
            it[status] = "success"
            it[finishedAt] = OffsetDateTime.now()
            it[IngestionMetadata.durationMs] = durationMs
            it[IngestionMetadata.manuscriptsIngested] = manuscriptsIngested
            it[IngestionMetadata.versesLinked] = versesLinked
            it[errorMessage] = null
            it[updatedAt] = OffsetDateTime.now()
        }
    }

    fun markFailed(durationMs: Long, error: String) = transaction {
        IngestionMetadata.update({ IngestionMetadata.id eq 1 }) {
            it[status] = "failed"
            it[finishedAt] = OffsetDateTime.now()
            it[IngestionMetadata.durationMs] = durationMs
            it[errorMessage] = error
            it[updatedAt] = OffsetDateTime.now()
        }
    }
}
