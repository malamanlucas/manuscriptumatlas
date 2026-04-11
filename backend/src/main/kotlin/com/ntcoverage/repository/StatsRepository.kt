package com.ntcoverage.repository

import com.ntcoverage.model.BookManuscriptCount
import com.ntcoverage.model.Books
import com.ntcoverage.model.CenturyCount
import com.ntcoverage.model.CoverageByCentury
import com.ntcoverage.model.Manuscripts
import com.ntcoverage.model.ManuscriptVerses
import com.ntcoverage.model.Verses
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction

class StatsRepository {

    fun getTotalManuscripts(): Int = transaction {
        Manuscripts.selectAll().count().toInt()
    }

    fun getManuscriptsByType(): Map<String, Int> = transaction {
        Manuscripts
            .select(Manuscripts.manuscriptType, Manuscripts.id.count())
            .where { Manuscripts.manuscriptType.isNotNull() }
            .groupBy(Manuscripts.manuscriptType)
            .associate { row ->
                val type = row[Manuscripts.manuscriptType] ?: "unknown"
                type to row[Manuscripts.id.count()].toInt()
            }
    }

    fun getManuscriptsByCentury(): List<CenturyCount> = transaction {
        Manuscripts
            .select(Manuscripts.effectiveCentury, Manuscripts.id.count())
            .groupBy(Manuscripts.effectiveCentury)
            .orderBy(Manuscripts.effectiveCentury)
            .map { row ->
                CenturyCount(
                    century = row[Manuscripts.effectiveCentury],
                    count = row[Manuscripts.id.count()].toInt()
                )
            }
    }

    fun getManuscriptCountByBook(upToCentury: Int = 10): List<BookManuscriptCount> = transaction {
        val manuscriptCount = Manuscripts.id.countDistinct()
        ManuscriptVerses
            .join(Manuscripts, JoinType.INNER, ManuscriptVerses.manuscriptId, Manuscripts.id)
            .join(Verses, JoinType.INNER, ManuscriptVerses.verseId, Verses.id)
            .join(Books, JoinType.INNER, Verses.bookId, Books.id)
            .select(Books.name, manuscriptCount)
            .where { Manuscripts.effectiveCentury lessEq upToCentury }
            .groupBy(Books.name, Books.bookOrder)
            .orderBy(Books.bookOrder)
            .map { row ->
                BookManuscriptCount(
                    bookName = row[Books.name],
                    manuscriptCount = row[manuscriptCount].toInt()
                )
            }
    }

    fun getAvgBooksPerManuscript(upToCentury: Int = 10): Double = transaction {
        val counts = ManuscriptVerses
            .join(Verses, JoinType.INNER, ManuscriptVerses.verseId, Verses.id)
            .join(Manuscripts, JoinType.INNER, ManuscriptVerses.manuscriptId, Manuscripts.id)
            .select(ManuscriptVerses.manuscriptId, Verses.bookId.countDistinct())
            .where { Manuscripts.effectiveCentury lessEq upToCentury }
            .groupBy(ManuscriptVerses.manuscriptId)
            .map { it[Verses.bookId.countDistinct()] }

        if (counts.isEmpty()) return@transaction 0.0
        counts.average()
    }

    fun getCoverageAtCentury(century: Int): Pair<Long, Int>? = transaction {
        val rows = CoverageByCentury
            .selectAll()
            .where { CoverageByCentury.century eq century }
            .toList()

        if (rows.isEmpty()) return@transaction null

        val covered = rows.sumOf { it[CoverageByCentury.coveredVerses].toLong() }
        val total = rows.sumOf { it[CoverageByCentury.totalVerses] }
        covered to total
    }
}
