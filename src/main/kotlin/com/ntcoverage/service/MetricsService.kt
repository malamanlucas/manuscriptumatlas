package com.ntcoverage.service

import com.ntcoverage.model.BookMetricsResponse
import com.ntcoverage.model.CenturyCoveragePercent
import com.ntcoverage.model.CenturyGrowthRate
import com.ntcoverage.model.NtMetricsResponse
import com.ntcoverage.repository.MetricsRepository

class MetricsService(private val metricsRepository: MetricsRepository) {

    fun getNtMetrics(): NtMetricsResponse {
        val bookNames = metricsRepository.getAllBookNames()
        val books = bookNames.map { getBookMetrics(it) }
        val overallCoverage = metricsRepository.getCoverageByCenturyForNt()
        val overallStabilization = overallCoverage.indexOfFirst { it.second >= 90.0 }.let {
            if (it >= 0) overallCoverage[it].first else null
        }
        return NtMetricsResponse(
            books = books,
            overallStabilizationCentury = overallStabilization,
            overallCoverageByCentury = overallCoverage.map { CenturyCoveragePercent(it.first, it.second) }
        )
    }

    fun getBookMetrics(bookName: String): BookMetricsResponse {
        val coverageByCentury = metricsRepository.getCoverageByCenturyForBook(bookName)
        val coverageList = coverageByCentury.map { CenturyCoveragePercent(it.first, it.second) }

        val growthRates = mutableListOf<CenturyGrowthRate>()
        for (i in 1 until coverageByCentury.size) {
            val (century, currPercent) = coverageByCentury[i]
            val prevPercent = coverageByCentury[i - 1].second
            val rate = if (prevPercent > 0) (currPercent - prevPercent) / prevPercent * 100 else 0.0
            growthRates.add(CenturyGrowthRate(century, rate))
        }

        val stabilizationCentury = coverageByCentury.firstOrNull { it.second >= 90.0 }?.first

        val fragmentationIndex = metricsRepository.getFragmentationIndexForBook(bookName)
        val coverageDensity = metricsRepository.getCoverageDensityForBook(bookName)
        val manuscriptConcentration = metricsRepository.getManuscriptConcentrationForBook(bookName)

        return BookMetricsResponse(
            bookName = bookName,
            centuryGrowthRates = growthRates,
            stabilizationCentury = stabilizationCentury,
            fragmentationIndex = fragmentationIndex,
            coverageDensity = coverageDensity,
            manuscriptConcentrationScore = manuscriptConcentration,
            coverageByCentury = coverageList
        )
    }
}
