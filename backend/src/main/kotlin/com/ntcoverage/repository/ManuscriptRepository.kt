package com.ntcoverage.repository

import com.ntcoverage.model.Books
import com.ntcoverage.model.Manuscripts
import com.ntcoverage.model.ManuscriptVerses
import com.ntcoverage.model.Verses
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction

data class ManuscriptRow(
    val id: Int,
    val gaId: String,
    val name: String?,
    val centuryMin: Int,
    val centuryMax: Int,
    val manuscriptType: String?,
    val historicalNotes: String?,
    val ntvmrUrl: String?,
    val yearMin: Int? = null,
    val yearMax: Int? = null,
    val yearBest: Int? = null,
    val datingSource: String? = null,
    val datingReference: String? = null,
    val datingConfidence: String? = null
)

data class ManuscriptVerseInfo(val bookName: String, val chapter: Int, val verse: Int)

class ManuscriptRepository {

    fun findByGaId(gaId: String): ManuscriptRow? = transaction {
        Manuscripts.selectAll()
            .where { Manuscripts.gaId eq gaId }
            .singleOrNull()
            ?.toRow()
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toRow() = ManuscriptRow(
        id = this[Manuscripts.id].value,
        gaId = this[Manuscripts.gaId],
        name = this[Manuscripts.name],
        centuryMin = this[Manuscripts.centuryMin],
        centuryMax = this[Manuscripts.centuryMax],
        manuscriptType = this[Manuscripts.manuscriptType],
        historicalNotes = this[Manuscripts.historicalNotes],
        ntvmrUrl = this[Manuscripts.ntvmrUrl],
        yearMin = this[Manuscripts.yearMin],
        yearMax = this[Manuscripts.yearMax],
        yearBest = this[Manuscripts.yearBest],
        datingSource = this[Manuscripts.datingSource],
        datingReference = this[Manuscripts.datingReference],
        datingConfidence = this[Manuscripts.datingConfidence]
    )

    fun findAll(
        type: String? = null,
        century: Int? = null,
        yearMin: Int? = null,
        yearMax: Int? = null,
        page: Int = 1,
        limit: Int = 50
    ): List<ManuscriptRow> = transaction {
        val query = Manuscripts.selectAll()

        if (type != null) {
            query.andWhere { Manuscripts.manuscriptType eq type }
        }

        if (yearMin != null || yearMax != null) {
            query.andWhere { Manuscripts.yearMin.isNotNull() }
            if (yearMin != null) {
                query.andWhere { Manuscripts.yearMax.isNotNull() and (Manuscripts.yearMax greaterEq yearMin) }
            }
            if (yearMax != null) {
                query.andWhere { Manuscripts.yearMin lessEq yearMax }
            }
        } else if (century != null) {
            query.andWhere { Manuscripts.effectiveCentury lessEq century }
        }

        query
            .orderBy(Manuscripts.gaId to SortOrder.ASC)
            .limit(limit)
            .offset(((page - 1) * limit).toLong())
            .map { it.toRow() }
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
        manuscriptType: String?,
        yearMin: Int? = null,
        yearMax: Int? = null
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
            if (yearMin != null) it[Manuscripts.yearMin] = yearMin
            if (yearMax != null) it[Manuscripts.yearMax] = yearMax
        }

        Manuscripts.selectAll()
            .where { Manuscripts.gaId eq gaId }
            .single()[Manuscripts.id].value
    }

    fun updateDating(
        gaId: String,
        yearMin: Int,
        yearMax: Int,
        yearBest: Int?,
        datingSource: String,
        datingReference: String?,
        datingConfidence: String
    ): Boolean = transaction {
        val updated = Manuscripts.update({ Manuscripts.gaId eq gaId }) {
            it[Manuscripts.yearMin] = yearMin
            it[Manuscripts.yearMax] = yearMax
            it[Manuscripts.yearBest] = yearBest
            it[Manuscripts.datingSource] = datingSource
            it[Manuscripts.datingReference] = datingReference
            it[Manuscripts.datingConfidence] = datingConfidence
        }
        updated > 0
    }

    fun findAllWithoutDating(limit: Int = 50): List<ManuscriptRow> = transaction {
        Manuscripts.selectAll()
            .where { Manuscripts.yearMin.isNull() }
            .orderBy(Manuscripts.gaId to SortOrder.ASC)
            .limit(limit)
            .map { it.toRow() }
    }
}
