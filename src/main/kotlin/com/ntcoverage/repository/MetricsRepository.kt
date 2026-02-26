package com.ntcoverage.repository

import com.ntcoverage.model.Books
import com.ntcoverage.model.CoverageByCentury
import com.ntcoverage.model.Manuscripts
import com.ntcoverage.model.ManuscriptVerses
import com.ntcoverage.model.Verses
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class MetricsRepository(
    private val coverageRepository: CoverageRepository
) {

    fun getCoverageByCenturyForBook(bookName: String, upToCentury: Int = 10): List<Pair<Int, Double>> = transaction {
        val bookId = coverageRepository.getBookIdsByNames(listOf(bookName))[bookName] ?: return@transaction emptyList()
        CoverageByCentury
            .select(CoverageByCentury.century, CoverageByCentury.coveragePercent)
            .where { (CoverageByCentury.bookId eq bookId) and (CoverageByCentury.century lessEq upToCentury) }
            .orderBy(CoverageByCentury.century to SortOrder.ASC)
            .map { row ->
                row[CoverageByCentury.century] to row[CoverageByCentury.coveragePercent].toDouble()
            }
    }

    fun getCoverageByCenturyForNt(upToCentury: Int = 10): List<Pair<Int, Double>> = transaction {
        val bookIds = coverageRepository.getAllBookIds()
        val totalVerses = coverageRepository.getTotalNtVerses()
        (1..upToCentury).map { century ->
            val rows = CoverageByCentury.selectAll().where { CoverageByCentury.century eq century }.toList()
            val covered = rows.sumOf { row -> row[CoverageByCentury.coveredVerses].toLong() }
            val percent = if (totalVerses > 0) covered * 100.0 / totalVerses else 0.0
            century to percent
        }
    }

    fun getFragmentationIndexForBook(bookName: String, upToCentury: Int = 10): Double = transaction {
        val bookId = coverageRepository.getBookIdsByNames(listOf(bookName))[bookName] ?: return@transaction 0.0
        val verseCounts = ManuscriptVerses
            .join(Manuscripts, JoinType.INNER, ManuscriptVerses.manuscriptId, Manuscripts.id)
            .join(Verses, JoinType.INNER, ManuscriptVerses.verseId, Verses.id)
            .select(ManuscriptVerses.verseId, ManuscriptVerses.manuscriptId.count())
            .where { (Verses.bookId eq bookId) and (Manuscripts.effectiveCentury lessEq upToCentury) }
            .groupBy(ManuscriptVerses.verseId)
            .map { row -> row[ManuscriptVerses.manuscriptId.count()].toInt() }

        if (verseCounts.isEmpty()) return@transaction 0.0
        val avg = verseCounts.average()
        if (avg <= 0) return@transaction 0.0
        1.0 - (1.0 / avg)
    }

    fun getCoverageDensityForBook(bookName: String, upToCentury: Int = 10): Double = transaction {
        val bookId = coverageRepository.getBookIdsByNames(listOf(bookName))[bookName] ?: return@transaction 0.0
        val coveredVerses = coverageRepository.countDistinctCoveredVerses(upToCentury, bookId)
        val manuscriptCount = ManuscriptVerses
            .join(Manuscripts, JoinType.INNER, ManuscriptVerses.manuscriptId, Manuscripts.id)
            .join(Verses, JoinType.INNER, ManuscriptVerses.verseId, Verses.id)
            .select(ManuscriptVerses.manuscriptId)
            .where { (Verses.bookId eq bookId) and (Manuscripts.effectiveCentury lessEq upToCentury) }
            .map { row -> row[ManuscriptVerses.manuscriptId].value }
            .distinct()
            .size

        if (manuscriptCount == 0) return@transaction 0.0
        coveredVerses.toDouble() / manuscriptCount
    }

    fun getManuscriptConcentrationForBook(bookName: String, upToCentury: Int = 10): Double = transaction {
        val bookId = coverageRepository.getBookIdsByNames(listOf(bookName))[bookName] ?: return@transaction 0.0
        val totalManuscripts = Manuscripts.selectAll().where { Manuscripts.effectiveCentury lessEq upToCentury }.count().toInt()
        if (totalManuscripts == 0) return@transaction 0.0
        val manuscriptsWithBook = ManuscriptVerses
            .join(Manuscripts, JoinType.INNER, ManuscriptVerses.manuscriptId, Manuscripts.id)
            .join(Verses, JoinType.INNER, ManuscriptVerses.verseId, Verses.id)
            .select(ManuscriptVerses.manuscriptId)
            .where { (Verses.bookId eq bookId) and (Manuscripts.effectiveCentury lessEq upToCentury) }
            .map { row -> row[ManuscriptVerses.manuscriptId].value }
            .distinct()
            .size
        manuscriptsWithBook.toDouble() / totalManuscripts
    }

    fun getAllBookNames(): List<String> = transaction {
        Books.select(Books.name).orderBy(Books.bookOrder to SortOrder.ASC).map { it[Books.name] }
    }
}
