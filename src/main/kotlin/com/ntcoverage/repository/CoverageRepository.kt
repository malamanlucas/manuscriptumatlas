package com.ntcoverage.repository

import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode

class CoverageRepository {

    fun calculateCoverage(upToCentury: Int, types: List<String>? = null): List<BookCoverage> = transaction {
        val coveredByBook = queryCoveredVerses(upToCentury, types)
        allBooksWithCoverage(coveredByBook)
    }

    fun calculateCoverageForBooks(
        bookIds: List<Int>,
        upToCentury: Int,
        types: List<String>? = null
    ): List<BookCoverage> = transaction {
        val coveredByBook = queryCoveredVerses(upToCentury, types, bookIds)
        allBooksWithCoverage(coveredByBook, bookIds)
    }

    private fun queryCoveredVerses(
        upToCentury: Int,
        types: List<String>? = null,
        filterBookIds: List<Int>? = null
    ): Map<String, Long> {
        val coveredCount = Verses.id.countDistinct()

        val baseJoin = Books
            .join(Verses, JoinType.INNER, Books.id, Verses.bookId)
            .join(ManuscriptVerses, JoinType.INNER, Verses.id, ManuscriptVerses.verseId)
            .join(Manuscripts, JoinType.INNER, ManuscriptVerses.manuscriptId, Manuscripts.id)

        var whereClause: Op<Boolean> = Manuscripts.effectiveCentury lessEq upToCentury
        if (!types.isNullOrEmpty()) {
            whereClause = whereClause and (Manuscripts.manuscriptType inList types)
        }
        if (!filterBookIds.isNullOrEmpty()) {
            whereClause = whereClause and (Books.id inList filterBookIds)
        }

        return baseJoin
            .select(Books.name, coveredCount)
            .where { whereClause }
            .groupBy(Books.name)
            .associate { row -> row[Books.name] to row[coveredCount] }
    }

    private fun allBooksWithCoverage(
        coveredByBook: Map<String, Long>,
        filterBookIds: List<Int>? = null
    ): List<BookCoverage> {
        val booksQuery = if (!filterBookIds.isNullOrEmpty()) {
            Books.selectAll().where { Books.id inList filterBookIds }
        } else {
            Books.selectAll()
        }

        return booksQuery
            .orderBy(Books.bookOrder)
            .map { row ->
                val bookName = row[Books.name]
                val total = row[Books.totalVerses]
                val covered = coveredByBook[bookName] ?: 0L
                val pct = if (total > 0)
                    BigDecimal(covered).multiply(BigDecimal(100))
                        .divide(BigDecimal(total), 2, RoundingMode.HALF_UP).toDouble()
                else 0.0
                BookCoverage(
                    bookName = bookName,
                    coveredVerses = covered,
                    totalVerses = total,
                    coveragePercent = pct
                )
            }
    }

    fun countDistinctCoveredVerses(
        upToCentury: Int,
        bookId: Int? = null,
        types: List<String>? = null
    ): Long = transaction {
        val baseJoin = ManuscriptVerses
            .join(Manuscripts, JoinType.INNER, ManuscriptVerses.manuscriptId, Manuscripts.id)
            .join(Verses, JoinType.INNER, ManuscriptVerses.verseId, Verses.id)

        var whereClause: Op<Boolean> = Manuscripts.effectiveCentury lessEq upToCentury
        if (bookId != null) {
            whereClause = whereClause and (Verses.bookId eq bookId)
        }
        if (!types.isNullOrEmpty()) {
            whereClause = whereClause and (Manuscripts.manuscriptType inList types)
        }

        val count = Verses.id.countDistinct()
        baseJoin
            .select(count)
            .where { whereClause }
            .single()[count]
    }

    fun clearCoverageCache(): Unit = transaction {
        CoverageByCentury.deleteAll()
    }

    fun materializeCoverage(century: Int, coverages: List<Pair<Int, BookCoverage>>): Unit = transaction {
        CoverageByCentury.deleteWhere { CoverageByCentury.century eq century }

        coverages.forEach { (bookId, cov) ->
            CoverageByCentury.insert {
                it[CoverageByCentury.century] = century
                it[CoverageByCentury.bookId] = bookId
                it[coveredVerses] = cov.coveredVerses.toInt()
                it[totalVerses] = cov.totalVerses
                it[coveragePercent] = BigDecimal.valueOf(cov.coveragePercent)
            }
        }
    }

    fun getCachedCoverage(century: Int): List<BookCoverage>? = transaction {
        val rows = CoverageByCentury
            .join(Books, JoinType.INNER, CoverageByCentury.bookId, Books.id)
            .selectAll()
            .where { CoverageByCentury.century eq century }
            .orderBy(Books.bookOrder)
            .toList()

        if (rows.isEmpty()) return@transaction null

        rows.map { row ->
            BookCoverage(
                bookName = row[Books.name],
                coveredVerses = row[CoverageByCentury.coveredVerses].toLong(),
                totalVerses = row[CoverageByCentury.totalVerses],
                coveragePercent = row[CoverageByCentury.coveragePercent].toDouble()
            )
        }
    }

    fun getAllBookIds(): Map<String, Int> = transaction {
        Books.selectAll().associate { it[Books.name] to it[Books.id].value }
    }

    fun getTotalNtVerses(): Int = transaction {
        Books.selectAll().sumOf { it[Books.totalVerses] }
    }

    fun getTotalVersesForBooks(bookIds: List<Int>): Int = transaction {
        Books.selectAll()
            .where { Books.id inList bookIds }
            .sumOf { it[Books.totalVerses] }
    }

    fun getBookIdsByNames(names: List<String>): Map<String, Int> = transaction {
        Books.selectAll()
            .where { Books.name inList names }
            .associate { it[Books.name] to it[Books.id].value }
    }

    fun getTotalVersesForBook(bookId: Int): Int = transaction {
        Books.selectAll()
            .where { Books.id eq bookId }
            .singleOrNull()
            ?.get(Books.totalVerses) ?: 0
    }
}
