package com.ntcoverage.repository

import com.ntcoverage.config.BibleDatabaseConfig
import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class BibleBookRepository {

    private val db get() = BibleDatabaseConfig.database

    fun findAll(testament: String? = null, locale: String = "en"): List<BibleBookDTO> = transaction(db) {
        val query = BibleBooks.selectAll()
        if (testament != null) {
            query.andWhere { BibleBooks.testament eq testament }
        }
        val books = query.orderBy(BibleBooks.bookOrder to SortOrder.ASC).map { it.toDTO() }

        val translationByBook = if (locale == "en") emptyMap() else {
            BibleBookTranslations.selectAll()
                .where { BibleBookTranslations.locale eq locale }
                .associate { it[BibleBookTranslations.bookId].value to it[BibleBookTranslations.name] }
        }

        val abbreviations = BibleBookAbbreviations.selectAll()
            .groupBy { it[BibleBookAbbreviations.bookId].value }

        books.map { book ->
            val bookAbbrevs = abbreviations[book.id] ?: emptyList()
            val byLocale = bookAbbrevs.groupBy(
                { it[BibleBookAbbreviations.locale] },
                { it[BibleBookAbbreviations.abbreviation] }
            )
            book.copy(
                abbreviations = byLocale,
                localizedName = translationByBook[book.id] ?: book.canonicalName
            )
        }
    }

    fun findByName(name: String): BibleBookDTO? = transaction(db) {
        BibleBooks.selectAll().where { BibleBooks.name eq name }
            .firstOrNull()?.toDTO()
    }

    fun findByNameCaseInsensitive(name: String): BibleBookDTO? = transaction(db) {
        BibleBooks.selectAll().where { BibleBooks.name.lowerCase() eq name.lowercase() }
            .firstOrNull()?.toDTO()
    }

    fun findByAbbreviation(abbreviation: String, locale: String): BibleBookDTO? = transaction(db) {
        val bookId = BibleBookAbbreviations.selectAll()
            .where { (BibleBookAbbreviations.abbreviation.lowerCase() eq abbreviation.lowercase()) and
                     (BibleBookAbbreviations.locale eq locale) }
            .firstOrNull()
            ?.get(BibleBookAbbreviations.bookId)?.value
            ?: return@transaction null

        BibleBooks.selectAll().where { BibleBooks.id eq bookId }
            .firstOrNull()?.toDTO()
    }

    fun findByAbbreviationAnyLocale(abbreviation: String): BibleBookDTO? = transaction(db) {
        val bookId = BibleBookAbbreviations.selectAll()
            .where { BibleBookAbbreviations.abbreviation.lowerCase() eq abbreviation.lowercase() }
            .firstOrNull()
            ?.get(BibleBookAbbreviations.bookId)?.value
            ?: return@transaction null

        BibleBooks.selectAll().where { BibleBooks.id eq bookId }
            .firstOrNull()?.toDTO()
    }

    fun upsertBook(name: String, abbreviation: String, totalChapters: Int, totalVerses: Int, bookOrder: Int, testament: String): Int = transaction(db) {
        val existing = BibleBooks.selectAll().where { BibleBooks.name eq name }.firstOrNull()
        if (existing != null) {
            existing[BibleBooks.id].value
        } else {
            BibleBooks.insertAndGetId {
                it[BibleBooks.name] = name
                it[BibleBooks.abbreviation] = abbreviation
                it[BibleBooks.totalChapters] = totalChapters
                it[BibleBooks.totalVerses] = totalVerses
                it[BibleBooks.bookOrder] = bookOrder
                it[BibleBooks.testament] = testament
            }.value
        }
    }

    fun upsertChapter(bookId: Int, chapterNumber: Int, totalVerses: Int): Int = transaction(db) {
        val existing = BibleChapters.selectAll()
            .where { (BibleChapters.bookId eq bookId) and (BibleChapters.chapterNumber eq chapterNumber) }
            .firstOrNull()
        if (existing != null) {
            existing[BibleChapters.id].value
        } else {
            BibleChapters.insertAndGetId {
                it[BibleChapters.bookId] = bookId
                it[BibleChapters.chapterNumber] = chapterNumber
                it[BibleChapters.totalVerses] = totalVerses
            }.value
        }
    }

    fun upsertAbbreviation(bookId: Int, locale: String, abbreviation: String) = transaction(db) {
        val existing = BibleBookAbbreviations.selectAll()
            .where { (BibleBookAbbreviations.locale eq locale) and (BibleBookAbbreviations.abbreviation eq abbreviation) }
            .firstOrNull()
        if (existing == null) {
            BibleBookAbbreviations.insert {
                it[BibleBookAbbreviations.bookId] = bookId
                it[BibleBookAbbreviations.locale] = locale
                it[BibleBookAbbreviations.abbreviation] = abbreviation
            }
        }
    }

    fun upsertBookTranslation(bookId: Int, locale: String, name: String) = transaction(db) {
        val existing = BibleBookTranslations.selectAll()
            .where { (BibleBookTranslations.bookId eq bookId) and (BibleBookTranslations.locale eq locale) }
            .firstOrNull()
        if (existing != null) {
            BibleBookTranslations.update({
                (BibleBookTranslations.bookId eq bookId) and (BibleBookTranslations.locale eq locale)
            }) {
                it[BibleBookTranslations.name] = name
            }
        } else {
            BibleBookTranslations.insert {
                it[BibleBookTranslations.bookId] = bookId
                it[BibleBookTranslations.locale] = locale
                it[BibleBookTranslations.name] = name
            }
        }
    }

    private fun ResultRow.toDTO(): BibleBookDTO {
        val canonical = this[BibleBooks.name]
        return BibleBookDTO(
            id = this[BibleBooks.id].value,
            name = canonical,
            canonicalName = canonical,
            localizedName = canonical,
            abbreviation = this[BibleBooks.abbreviation],
            totalChapters = this[BibleBooks.totalChapters],
            totalVerses = this[BibleBooks.totalVerses],
            bookOrder = this[BibleBooks.bookOrder],
            testament = this[BibleBooks.testament]
        )
    }
}
