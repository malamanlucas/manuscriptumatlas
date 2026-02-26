package com.ntcoverage.service

import com.ntcoverage.model.StatsOverviewResponse
import com.ntcoverage.repository.CoverageRepository
import com.ntcoverage.repository.StatsRepository
import java.math.BigDecimal
import java.math.RoundingMode

class StatsService(
    private val statsRepository: StatsRepository,
    private val coverageRepository: CoverageRepository
) {

    fun getOverview(): StatsOverviewResponse {
        val totalManuscripts = statsRepository.getTotalManuscripts()
        val byType = statsRepository.getManuscriptsByType()
        val byCentury = statsRepository.getManuscriptsByCentury()
        val byBook = statsRepository.getManuscriptCountByBook()
        val avgBooks = statsRepository.getAvgBooksPerManuscript()

        val totalVerses = coverageRepository.getTotalNtVerses()
        val (coveredVerses, _) = statsRepository.getCoverageAtCentury(10)
            ?: (0L to totalVerses)

        val overallPercent = if (totalVerses > 0) {
            BigDecimal(coveredVerses).multiply(BigDecimal(100))
                .divide(BigDecimal(totalVerses), 2, RoundingMode.HALF_UP).toDouble()
        } else 0.0

        return StatsOverviewResponse(
            totalManuscripts = totalManuscripts,
            byType = byType.ifEmpty { mapOf("papyrus" to 0, "uncial" to 0) },
            byCentury = byCentury,
            byBook = byBook,
            avgBooksPerManuscript = BigDecimal(avgBooks).setScale(2, RoundingMode.HALF_UP).toDouble(),
            totalVerses = totalVerses,
            coveredVerses = coveredVerses,
            overallCoveragePercent = overallPercent
        )
    }
}
