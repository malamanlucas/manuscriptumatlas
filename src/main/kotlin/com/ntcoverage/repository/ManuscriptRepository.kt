package com.ntcoverage.repository

import com.ntcoverage.model.Books
import com.ntcoverage.model.Manuscripts
import com.ntcoverage.model.ManuscriptVerses
import com.ntcoverage.model.Verses
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

data class ManuscriptRow(
    val id: Int,
    val gaId: String,
    val name: String?,
    val centuryMin: Int,
    val centuryMax: Int,
    val manuscriptType: String?,
    val historicalNotes: String?,
    val ntvmrUrl: String?
)

data class ManuscriptVerseInfo(val bookName: String, val chapter: Int, val verse: Int)

class ManuscriptRepository {

    fun findByGaId(gaId: String): ManuscriptRow? = transaction {
        Manuscripts.selectAll()
            .where { Manuscripts.gaId eq gaId }
            .singleOrNull()
            ?.let { row ->
                ManuscriptRow(
                    id = row[Manuscripts.id].value,
                    gaId = row[Manuscripts.gaId],
                    name = row[Manuscripts.name],
                    centuryMin = row[Manuscripts.centuryMin],
                    centuryMax = row[Manuscripts.centuryMax],
                    manuscriptType = row[Manuscripts.manuscriptType],
                    historicalNotes = row[Manuscripts.historicalNotes],
                    ntvmrUrl = row[Manuscripts.ntvmrUrl]
                )
            }
    }

    fun findAll(
        type: String? = null,
        century: Int? = null,
        page: Int = 1,
        limit: Int = 50
    ): List<ManuscriptRow> = transaction {
        val query = when {
            type != null && century != null -> Manuscripts.selectAll()
                .where { (Manuscripts.manuscriptType eq type) and (Manuscripts.effectiveCentury lessEq century) }
            type != null -> Manuscripts.selectAll().where { Manuscripts.manuscriptType eq type }
            century != null -> Manuscripts.selectAll().where { Manuscripts.effectiveCentury lessEq century }
            else -> Manuscripts.selectAll()
        }
        query
            .orderBy(Manuscripts.gaId to SortOrder.ASC)
            .limit(limit)
            .offset(((page - 1) * limit).toLong())
            .map { row ->
                ManuscriptRow(
                    id = row[Manuscripts.id].value,
                    gaId = row[Manuscripts.gaId],
                    name = row[Manuscripts.name],
                    centuryMin = row[Manuscripts.centuryMin],
                    centuryMax = row[Manuscripts.centuryMax],
                    manuscriptType = row[Manuscripts.manuscriptType],
                    historicalNotes = row[Manuscripts.historicalNotes],
                    ntvmrUrl = row[Manuscripts.ntvmrUrl]
                )
            }
    }

    fun getBooksAndVersesForManuscript(manuscriptId: Int): List<ManuscriptVerseInfo> = transaction {
        ManuscriptVerses
            .join(Verses, JoinType.INNER, ManuscriptVerses.verseId, Verses.id)
            .join(Books, JoinType.INNER, Verses.bookId, Books.id)
            .select(Books.name, Verses.chapter, Verses.verse)
            .where { ManuscriptVerses.manuscriptId eq manuscriptId }
            .orderBy(Books.bookOrder to SortOrder.ASC, Verses.chapter to SortOrder.ASC, Verses.verse to SortOrder.ASC)
            .map { row ->
                ManuscriptVerseInfo(
                    bookName = row[Books.name],
                    chapter = row[Verses.chapter],
                    verse = row[Verses.verse]
                )
            }
    }

    fun getHistoricalNotesFromSource(manuscriptId: Int): String? = transaction {
        com.ntcoverage.model.ManuscriptSources
            .selectAll()
            .where { com.ntcoverage.model.ManuscriptSources.manuscriptId eq manuscriptId }
            .singleOrNull()
            ?.get(com.ntcoverage.model.ManuscriptSources.historicalNotes)
    }

    fun insertIfNotExists(
        gaId: String,
        name: String?,
        centuryMin: Int,
        centuryMax: Int,
        manuscriptType: String?
    ): Int = transaction {
        val existing = Manuscripts.selectAll()
            .where { Manuscripts.gaId eq gaId }
            .singleOrNull()

        if (existing != null) {
            return@transaction existing[Manuscripts.id].value
        }

        Manuscripts.insertIgnore {
            it[Manuscripts.gaId] = gaId
            it[Manuscripts.name] = name
            it[Manuscripts.centuryMin] = centuryMin
            it[Manuscripts.centuryMax] = centuryMax
            it[Manuscripts.effectiveCentury] = centuryMin
            it[Manuscripts.manuscriptType] = manuscriptType
        }

        Manuscripts.selectAll()
            .where { Manuscripts.gaId eq gaId }
            .single()[Manuscripts.id].value
    }
}
