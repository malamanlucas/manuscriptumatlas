package com.ntcoverage.repository

import com.ntcoverage.config.BibleDatabaseConfig
import com.ntcoverage.model.BibleLayer4ApplicationDTO
import com.ntcoverage.model.BibleLayer4Applications
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class BibleLayer4ApplicationRepository {

    private val db get() = BibleDatabaseConfig.database

    fun insert(phase: String, bookName: String?, chapter: Int?, verse: Int?): Int = transaction(db) {
        BibleLayer4Applications.insertAndGetId {
            it[BibleLayer4Applications.phaseName] = phase
            it[BibleLayer4Applications.bookName] = bookName
            it[BibleLayer4Applications.chapter] = chapter
            it[BibleLayer4Applications.verse] = verse
            it[BibleLayer4Applications.status] = "running"
        }.value
    }

    fun markSuccess(id: Int, itemsProcessed: Int, enqueued: Int) = transaction(db) {
        BibleLayer4Applications.update({ BibleLayer4Applications.id eq id }) {
            it[BibleLayer4Applications.status] = "success"
            it[BibleLayer4Applications.itemsProcessed] = itemsProcessed
            it[BibleLayer4Applications.enqueuedCount] = enqueued
            it[BibleLayer4Applications.finishedAt] = Instant.now()
        }
    }

    fun markFailed(id: Int, error: String) = transaction(db) {
        BibleLayer4Applications.update({ BibleLayer4Applications.id eq id }) {
            it[BibleLayer4Applications.status] = "failed"
            it[BibleLayer4Applications.errorMessage] = error.take(1000)
            it[BibleLayer4Applications.finishedAt] = Instant.now()
        }
    }

    fun list(book: String?, chapter: Int?, verse: Int?, limit: Int): List<BibleLayer4ApplicationDTO> = transaction(db) {
        val query = BibleLayer4Applications.selectAll()
        if (book != null) query.andWhere { BibleLayer4Applications.bookName eq book }
        if (chapter != null) query.andWhere { BibleLayer4Applications.chapter eq chapter }
        if (verse != null) query.andWhere { BibleLayer4Applications.verse eq verse }
        query.orderBy(BibleLayer4Applications.requestedAt to SortOrder.DESC)
            .limit(limit)
            .map { row ->
                BibleLayer4ApplicationDTO(
                    id = row[BibleLayer4Applications.id].value,
                    phaseName = row[BibleLayer4Applications.phaseName],
                    bookName = row[BibleLayer4Applications.bookName],
                    chapter = row[BibleLayer4Applications.chapter],
                    verse = row[BibleLayer4Applications.verse],
                    status = row[BibleLayer4Applications.status],
                    itemsProcessed = row[BibleLayer4Applications.itemsProcessed],
                    enqueuedCount = row[BibleLayer4Applications.enqueuedCount],
                    errorMessage = row[BibleLayer4Applications.errorMessage],
                    requestedAt = row[BibleLayer4Applications.requestedAt].toString(),
                    finishedAt = row[BibleLayer4Applications.finishedAt]?.toString()
                )
            }
    }
}
