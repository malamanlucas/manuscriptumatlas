package com.ntcoverage.repository

import com.ntcoverage.model.Books
import com.ntcoverage.model.ManuscriptVerses
import com.ntcoverage.model.Verses
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class VerseRepository {

    fun insertBook(
        name: String,
        abbreviation: String,
        order: Int,
        totalChapters: Int,
        totalVerses: Int
    ): Int = transaction {
        val existing = Books.selectAll()
            .where { Books.name eq name }
            .singleOrNull()
        if (existing != null) return@transaction existing[Books.id].value

        Books.insertIgnore {
            it[Books.name] = name
            it[Books.abbreviation] = abbreviation
            it[Books.bookOrder] = order
            it[Books.totalChapters] = totalChapters
            it[Books.totalVerses] = totalVerses
        }

        Books.selectAll()
            .where { Books.name eq name }
            .single()[Books.id].value
    }

    fun insertVerses(bookId: Int, versesData: List<Pair<Int, Int>>): Unit = transaction {
        Verses.batchInsert(versesData, ignore = true) { (chapter, verse) ->
            this[Verses.bookId] = bookId
            this[Verses.chapter] = chapter
            this[Verses.verse] = verse
        }
    }

    fun findVerseId(bookId: Int, chapter: Int, verse: Int): Int? = transaction {
        Verses.selectAll()
            .where { (Verses.bookId eq bookId) and (Verses.chapter eq chapter) and (Verses.verse eq verse) }
            .singleOrNull()
            ?.get(Verses.id)?.value
    }

    fun findBookIdByName(name: String): Int? = transaction {
        Books.selectAll()
            .where { Books.name eq name }
            .singleOrNull()
            ?.get(Books.id)?.value
    }

    fun insertManuscriptVerses(manuscriptId: Int, verseIds: List<Int>): Unit = transaction {
        ManuscriptVerses.batchInsert(verseIds, ignore = true) { verseId ->
            this[ManuscriptVerses.manuscriptId] = manuscriptId
            this[ManuscriptVerses.verseId] = verseId
        }
    }

    data class VerseIdLookup(val bookId: Int, val chapter: Int, val verse: Int, val verseId: Int)

    fun loadAllVersesForBook(bookId: Int): List<VerseIdLookup> = transaction {
        Verses.selectAll()
            .where { Verses.bookId eq bookId }
            .map {
                VerseIdLookup(
                    bookId = it[Verses.bookId].value,
                    chapter = it[Verses.chapter],
                    verse = it[Verses.verse],
                    verseId = it[Verses.id].value
                )
            }
    }
}
