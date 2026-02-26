package com.ntcoverage.service

import com.ntcoverage.model.BookInterval
import com.ntcoverage.model.BookRanges
import com.ntcoverage.model.ManuscriptDetailResponse
import com.ntcoverage.model.ManuscriptSummary
import com.ntcoverage.repository.ManuscriptRepository
import com.ntcoverage.util.NtvmrUrl

class ManuscriptService(private val manuscriptRepository: ManuscriptRepository) {

    fun listManuscripts(
        type: String? = null,
        century: Int? = null,
        page: Int = 1,
        limit: Int = 50
    ): List<ManuscriptSummary> {
        val rows = manuscriptRepository.findAll(type, century, page, limit)
        return rows.map { row ->
            val verses = manuscriptRepository.getBooksAndVersesForManuscript(row.id)
            val bookCount = verses.map { it.bookName }.distinct().size
            ManuscriptSummary(
                gaId = row.gaId,
                name = row.name,
                centuryMin = row.centuryMin,
                centuryMax = row.centuryMax,
                manuscriptType = row.manuscriptType,
                bookCount = bookCount,
                verseCount = verses.size
            )
        }
    }

    fun getManuscriptDetail(gaId: String): ManuscriptDetailResponse? {
        val row = manuscriptRepository.findByGaId(gaId) ?: return null
        val verses = manuscriptRepository.getBooksAndVersesForManuscript(row.id)
        val historicalNotes = row.historicalNotes
            ?: manuscriptRepository.getHistoricalNotesFromSource(row.id)

        val booksPreserved = verses
            .groupBy { it.bookName }
            .map { (bookName, verseList) ->
                val pairs = verseList.map { it.chapter to it.verse }
                val ranges = VerseRangeCompressor.compress(bookName, pairs)
                BookRanges(book = bookName, ranges = ranges)
            }
            .sortedBy { com.ntcoverage.seed.CanonicalVerses.findBook(it.book)?.order ?: 999 }

        val intervals = verses
            .groupBy { it.bookName }
            .map { (bookName, verseList) ->
                val chapters = verseList.map { it.chapter }
                BookInterval(
                    book = bookName,
                    chapterMin = chapters.minOrNull() ?: 0,
                    chapterMax = chapters.maxOrNull() ?: 0,
                    verseCount = verseList.size
                )
            }
            .sortedBy { com.ntcoverage.seed.CanonicalVerses.findBook(it.book)?.order ?: 999 }

        val ntvmrUrl = row.ntvmrUrl ?: NtvmrUrl.buildUrl(gaId)
        val dataSource = "NTVMR"

        return ManuscriptDetailResponse(
            gaId = row.gaId,
            name = row.name,
            centuryMin = row.centuryMin,
            centuryMax = row.centuryMax,
            manuscriptType = row.manuscriptType,
            booksPreserved = booksPreserved,
            intervals = intervals,
            dataSource = dataSource,
            ntvmrUrl = ntvmrUrl,
            historicalNotes = historicalNotes
        )
    }
}
