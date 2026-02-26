package com.ntcoverage.repository

import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode

class ChapterCoverageRepository {

    data class VerseCoverageRow(
        val chapter: Int,
        val verse: Int,
        val covered: Boolean
    )

    fun getVerseCoverage(
        bookId: Int,
        upToCentury: Int,
        types: List<String>? = null
    ): List<VerseCoverageRow> = transaction {
        val manuscriptFilter: Op<Boolean> = Manuscripts.effectiveCentury lessEq upToCentury
        val typeFilter = if (!types.isNullOrEmpty()) {
            manuscriptFilter and (Manuscripts.manuscriptType inList types)
        } else {
            manuscriptFilter
        }

        val coveredVerseIds = ManuscriptVerses
            .join(Manuscripts, JoinType.INNER, ManuscriptVerses.manuscriptId, Manuscripts.id)
            .select(ManuscriptVerses.verseId)
            .where { typeFilter }
            .map { it[ManuscriptVerses.verseId].value }
            .toSet()

        Verses.selectAll()
            .where { Verses.bookId eq bookId }
            .orderBy(Verses.chapter to SortOrder.ASC, Verses.verse to SortOrder.ASC)
            .map { row ->
                VerseCoverageRow(
                    chapter = row[Verses.chapter],
                    verse = row[Verses.verse],
                    covered = row[Verses.id].value in coveredVerseIds
                )
            }
    }

    fun getChapterCoverage(
        bookId: Int,
        bookName: String,
        upToCentury: Int,
        types: List<String>? = null
    ): BookChapterCoverageResponse {
        val rows = getVerseCoverage(bookId, upToCentury, types)

        val chapters = rows.groupBy { it.chapter }.map { (chapter, verses) ->
            val covered = verses.filter { it.covered }.map { it.verse }
            val missing = verses.filter { !it.covered }.map { it.verse }
            val total = verses.size
            val pct = if (total > 0)
                BigDecimal(covered.size).multiply(BigDecimal(100))
                    .divide(BigDecimal(total), 2, RoundingMode.HALF_UP).toDouble()
            else 0.0

            ChapterCoverage(
                chapter = chapter,
                coveredVerses = covered.size,
                totalVerses = total,
                coveragePercent = pct,
                coveredList = covered.sorted(),
                missingList = missing.sorted()
            )
        }.sortedBy { it.chapter }

        return BookChapterCoverageResponse(
            book = bookName,
            century = upToCentury,
            chapters = chapters
        )
    }

    fun getMissingVerses(
        bookId: Int,
        bookName: String,
        upToCentury: Int,
        types: List<String>? = null
    ): MissingVersesResponse {
        val rows = getVerseCoverage(bookId, upToCentury, types)
        val missing = rows.filter { !it.covered }.map {
            MissingVerse(book = bookName, chapter = it.chapter, verse = it.verse)
        }
        return MissingVersesResponse(
            book = bookName,
            century = upToCentury,
            totalMissing = missing.size,
            missingVerses = missing
        )
    }
}
