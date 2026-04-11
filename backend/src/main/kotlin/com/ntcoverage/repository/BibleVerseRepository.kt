package com.ntcoverage.repository

import com.ntcoverage.config.BibleDatabaseConfig
import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction

class BibleVerseRepository {

    private val db get() = BibleDatabaseConfig.database

    fun upsertVerse(bookId: Int, chapter: Int, verseNumber: Int): Int = transaction(db) {
        val existing = BibleVerses.selectAll()
            .where { (BibleVerses.bookId eq bookId) and (BibleVerses.chapter eq chapter) and (BibleVerses.verseNumber eq verseNumber) }
            .firstOrNull()
        if (existing != null) {
            existing[BibleVerses.id].value
        } else {
            BibleVerses.insertAndGetId {
                it[BibleVerses.bookId] = bookId
                it[BibleVerses.chapter] = chapter
                it[BibleVerses.verseNumber] = verseNumber
            }.value
        }
    }

    fun getVerseId(bookId: Int, chapter: Int, verseNumber: Int): Int? = transaction(db) {
        BibleVerses.selectAll()
            .where { (BibleVerses.bookId eq bookId) and (BibleVerses.chapter eq chapter) and (BibleVerses.verseNumber eq verseNumber) }
            .firstOrNull()?.get(BibleVerses.id)?.value
    }

    fun getVerseIdsForChapter(bookId: Int, chapter: Int): Map<Int, Int> = transaction(db) {
        BibleVerses.selectAll()
            .where { (BibleVerses.bookId eq bookId) and (BibleVerses.chapter eq chapter) }
            .associate { it[BibleVerses.verseNumber] to it[BibleVerses.id].value }
    }

    fun upsertVerseText(versionId: Int, verseId: Int, text: String) = transaction(db) {
        val existing = BibleVerseTexts.selectAll()
            .where { (BibleVerseTexts.versionId eq versionId) and (BibleVerseTexts.verseId eq verseId) }
            .firstOrNull()
        if (existing == null) {
            BibleVerseTexts.insert {
                it[BibleVerseTexts.versionId] = versionId
                it[BibleVerseTexts.verseId] = verseId
                it[BibleVerseTexts.text] = text
            }
        }
    }

    fun getChapterTexts(versionId: Int, bookId: Int, chapter: Int): List<BibleVerseTextDTO> = transaction(db) {
        (BibleVerseTexts innerJoin BibleVerses)
            .selectAll()
            .where {
                (BibleVerseTexts.versionId eq versionId) and
                (BibleVerses.bookId eq bookId) and
                (BibleVerses.chapter eq chapter)
            }
            .orderBy(BibleVerses.verseNumber to SortOrder.ASC)
            .map {
                BibleVerseTextDTO(
                    verseNumber = it[BibleVerses.verseNumber],
                    text = it[BibleVerseTexts.text]
                )
            }
    }

    fun getVerseText(versionId: Int, bookId: Int, chapter: Int, verseNumber: Int): BibleVerseTextDTO? = transaction(db) {
        (BibleVerseTexts innerJoin BibleVerses)
            .selectAll()
            .where {
                (BibleVerseTexts.versionId eq versionId) and
                (BibleVerses.bookId eq bookId) and
                (BibleVerses.chapter eq chapter) and
                (BibleVerses.verseNumber eq verseNumber)
            }
            .firstOrNull()
            ?.let {
                BibleVerseTextDTO(
                    verseNumber = it[BibleVerses.verseNumber],
                    text = it[BibleVerseTexts.text]
                )
            }
    }

    fun countVerseTexts(versionId: Int): Long = transaction(db) {
        BibleVerseTexts.selectAll()
            .where { BibleVerseTexts.versionId eq versionId }
            .count()
    }

    fun countVerses(): Long = transaction(db) {
        BibleVerses.selectAll().count()
    }

    fun compareChapter(
        bookId: Int, chapter: Int, versionIds: List<Int>
    ): List<BibleCompareRow> = transaction(db) {
        val rows = (BibleVerseTexts innerJoin BibleVerses innerJoin BibleVersions)
            .select(BibleVerses.verseNumber, BibleVersions.code, BibleVerseTexts.text)
            .where {
                (BibleVerses.bookId eq bookId) and
                (BibleVerses.chapter eq chapter) and
                (BibleVerseTexts.versionId inList versionIds)
            }
            .orderBy(BibleVerses.verseNumber to SortOrder.ASC)

        rows.groupBy { it[BibleVerses.verseNumber] }
            .map { (verseNum, group) ->
                BibleCompareRow(
                    verseNumber = verseNum,
                    texts = group.associate { it[BibleVersions.code] to it[BibleVerseTexts.text] }
                )
            }
            .sortedBy { it.verseNumber }
    }

    fun searchText(
        query: String,
        versionId: Int? = null,
        bookId: Int? = null,
        testament: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Pair<List<BibleSearchResultDTO>, Int> = transaction(db) {
        val offset = ((page - 1) * limit).toLong()

        // Use ILIKE for simple text search (FTS requires the generated column which may not exist yet)
        val baseJoin = BibleVerseTexts innerJoin BibleVerses innerJoin BibleBooks innerJoin BibleVersions
        val conditions = mutableListOf<Op<Boolean>>()

        conditions.add(BibleVerseTexts.text.lowerCase() like "%${query.lowercase()}%")

        if (versionId != null) {
            conditions.add(BibleVerseTexts.versionId eq versionId)
        }
        if (bookId != null) {
            conditions.add(BibleVerses.bookId eq bookId)
        }
        if (testament != null) {
            conditions.add(BibleBooks.testament eq testament.uppercase())
        }

        val where = conditions.reduce { acc, op -> acc and op }

        val total = baseJoin.selectAll().where { where }.count().toInt()

        val results = baseJoin
            .select(
                BibleBooks.name, BibleVerses.chapter, BibleVerses.verseNumber,
                BibleVersions.code, BibleVerseTexts.text
            )
            .where { where }
            .orderBy(BibleBooks.bookOrder to SortOrder.ASC, BibleVerses.chapter to SortOrder.ASC, BibleVerses.verseNumber to SortOrder.ASC)
            .limit(limit).offset(offset)
            .map {
                val fullText = it[BibleVerseTexts.text]
                val idx = fullText.lowercase().indexOf(query.lowercase())
                val snippetStart = (idx - 40).coerceAtLeast(0)
                val snippetEnd = (idx + query.length + 40).coerceAtMost(fullText.length)
                val snippet = (if (snippetStart > 0) "..." else "") +
                    fullText.substring(snippetStart, snippetEnd) +
                    (if (snippetEnd < fullText.length) "..." else "")

                BibleSearchResultDTO(
                    book = it[BibleBooks.name],
                    chapter = it[BibleVerses.chapter],
                    verseNumber = it[BibleVerses.verseNumber],
                    version = it[BibleVersions.code],
                    text = fullText,
                    snippet = snippet
                )
            }

        Pair(results, total)
    }
}
