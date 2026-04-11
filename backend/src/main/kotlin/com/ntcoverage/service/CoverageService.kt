package com.ntcoverage.service

import com.ntcoverage.model.*
import com.ntcoverage.repository.ChapterCoverageRepository
import com.ntcoverage.repository.CoverageRepository
import java.math.BigDecimal
import java.math.RoundingMode

class CoverageService(
    private val coverageRepository: CoverageRepository,
    private val chapterCoverageRepository: ChapterCoverageRepository
) {
    val maxCentury = 10

    private val gospelNames = listOf("Matthew", "Mark", "Luke", "John")

    fun getFullCoverage(types: List<String>? = null): FullCoverageResponse {
        val centuries = (1..maxCentury).map { getCoverageByCentury(it, types) }
        val latest = centuries.last()
        return FullCoverageResponse(
            maxCentury = maxCentury,
            summary = latest.summary,
            byCentury = centuries
        )
    }

    fun getCoverageByBook(bookName: String, types: List<String>? = null): CenturyCoverageResponse {
        val allCoverage = getCoverageByCentury(maxCentury, types)
        val normalizedName = bookName.trim()

        val bookCov = allCoverage.books.find {
            it.bookName.equals(normalizedName, ignoreCase = true)
        } ?: throw NoSuchElementException("Book not found: $bookName")

        return CenturyCoverageResponse(
            century = maxCentury,
            summary = CoverageSummary(
                totalNtVerses = bookCov.totalVerses,
                coveredVerses = bookCov.coveredVerses,
                overallCoveragePercent = bookCov.coveragePercent
            ),
            books = listOf(bookCov),
            fullyAttested = if (bookCov.coveragePercent >= 100.0) listOf(bookCov.bookName) else emptyList(),
            notFullyAttested = if (bookCov.coveragePercent < 100.0) listOf(bookCov) else emptyList()
        )
    }

    fun getCoverageByCentury(century: Int, types: List<String>? = null): CenturyCoverageResponse {
        require(century in 1..maxCentury) { "Century must be between 1 and $maxCentury" }

        val bookCoverages = if (types.isNullOrEmpty()) {
            coverageRepository.getCachedCoverage(century)
                ?: coverageRepository.calculateCoverage(century)
        } else {
            coverageRepository.calculateCoverage(century, types)
        }

        val totalNtVerses = coverageRepository.getTotalNtVerses()
        val totalCovered = bookCoverages.sumOf { it.coveredVerses }
        val overallPct = if (totalNtVerses > 0)
            BigDecimal(totalCovered).multiply(BigDecimal(100))
                .divide(BigDecimal(totalNtVerses), 2, RoundingMode.HALF_UP).toDouble()
        else 0.0

        val fullyAttested = bookCoverages.filter { it.coveragePercent >= 100.0 }.map { it.bookName }
        val notFullyAttested = bookCoverages.filter { it.coveragePercent < 100.0 }

        return CenturyCoverageResponse(
            century = century,
            summary = CoverageSummary(
                totalNtVerses = totalNtVerses,
                coveredVerses = totalCovered,
                overallCoveragePercent = overallPct
            ),
            books = bookCoverages,
            fullyAttested = fullyAttested,
            notFullyAttested = notFullyAttested
        )
    }

    fun getChapterCoverage(
        bookName: String,
        century: Int,
        types: List<String>? = null
    ): BookChapterCoverageResponse {
        require(century in 1..maxCentury) { "Century must be between 1 and $maxCentury" }
        val bookIds = coverageRepository.getAllBookIds()
        val bookId = bookIds[bookName]
            ?: bookIds.entries.find { it.key.equals(bookName, ignoreCase = true) }?.value
            ?: throw NoSuchElementException("Book not found: $bookName")
        val resolvedName = bookIds.entries.find { it.value == bookId }?.key ?: bookName

        return chapterCoverageRepository.getChapterCoverage(bookId, resolvedName, century, types)
    }

    fun getGospelCoverage(century: Int, types: List<String>? = null): GospelCoverageResponse {
        require(century in 1..maxCentury) { "Century must be between 1 and $maxCentury" }

        val bookIds = coverageRepository.getBookIdsByNames(gospelNames)
        val gospelBookIds = gospelNames.mapNotNull { bookIds[it] }

        val individual = coverageRepository.calculateCoverageForBooks(gospelBookIds, century, types)

        val totalVerses = coverageRepository.getTotalVersesForBooks(gospelBookIds)
        val totalCovered = individual.sumOf { it.coveredVerses }
        val overallPct = if (totalVerses > 0)
            BigDecimal(totalCovered).multiply(BigDecimal(100))
                .divide(BigDecimal(totalVerses), 2, RoundingMode.HALF_UP).toDouble()
        else 0.0

        val allMissing = gospelNames.flatMap { name ->
            val bid = bookIds[name] ?: return@flatMap emptyList()
            chapterCoverageRepository.getMissingVerses(bid, name, century, types).missingVerses
        }

        return GospelCoverageResponse(
            century = century,
            individual = individual,
            aggregated = CoverageSummary(
                totalNtVerses = totalVerses,
                coveredVerses = totalCovered,
                overallCoveragePercent = overallPct
            ),
            missingVerses = allMissing
        )
    }

    fun getTimeline(
        bookName: String? = null,
        types: List<String>? = null
    ): TimelineResponse {
        val bookId = if (bookName != null) {
            val bookIds = coverageRepository.getAllBookIds()
            bookIds[bookName]
                ?: bookIds.entries.find { it.key.equals(bookName, ignoreCase = true) }?.value
                ?: throw NoSuchElementException("Book not found: $bookName")
        } else null

        val totalVerses = if (bookId != null) {
            coverageRepository.getTotalVersesForBook(bookId)
        } else {
            coverageRepository.getTotalNtVerses()
        }

        var previousCovered = 0L
        val entries = (1..maxCentury).map { century ->
            val covered = coverageRepository.countDistinctCoveredVerses(century, bookId, types)
            val delta = covered - previousCovered
            val cumulativePct = if (totalVerses > 0)
                BigDecimal(covered).multiply(BigDecimal(100))
                    .divide(BigDecimal(totalVerses), 2, RoundingMode.HALF_UP).toDouble()
            else 0.0
            val growthPct = if (totalVerses > 0)
                BigDecimal(delta).multiply(BigDecimal(100))
                    .divide(BigDecimal(totalVerses), 2, RoundingMode.HALF_UP).toDouble()
            else 0.0

            previousCovered = covered

            TimelineEntry(
                century = century,
                cumulativePercent = cumulativePct,
                coveredVerses = covered,
                newVersesCount = delta,
                missingVersesCount = totalVerses.toLong() - covered,
                growthPercent = growthPct
            )
        }

        return TimelineResponse(
            book = bookName,
            type = types?.joinToString(","),
            totalVerses = totalVerses,
            entries = entries
        )
    }

    fun getMissingVerses(
        bookName: String,
        century: Int,
        types: List<String>? = null
    ): MissingVersesResponse {
        require(century in 1..maxCentury) { "Century must be between 1 and $maxCentury" }
        val bookIds = coverageRepository.getAllBookIds()
        val bookId = bookIds[bookName]
            ?: bookIds.entries.find { it.key.equals(bookName, ignoreCase = true) }?.value
            ?: throw NoSuchElementException("Book not found: $bookName")
        val resolvedName = bookIds.entries.find { it.value == bookId }?.key ?: bookName

        return chapterCoverageRepository.getMissingVerses(bookId, resolvedName, century, types)
    }
}
