package com.ntcoverage.model

import kotlinx.serialization.Serializable

@Serializable
data class BookCoverage(
    val bookName: String,
    val coveredVerses: Long,
    val totalVerses: Int,
    val coveragePercent: Double
)

@Serializable
data class CoverageSummary(
    val totalNtVerses: Int,
    val coveredVerses: Long,
    val overallCoveragePercent: Double
)

@Serializable
data class CenturyCoverageResponse(
    val century: Int,
    val summary: CoverageSummary,
    val books: List<BookCoverage>,
    val fullyAttested: List<String>,
    val notFullyAttested: List<BookCoverage>
)

@Serializable
data class FullCoverageResponse(
    val maxCentury: Int,
    val summary: CoverageSummary,
    val byCentury: List<CenturyCoverageResponse>
)

@Serializable
data class ManuscriptSeed(
    val gaId: String,
    val name: String? = null,
    val centuryMin: Int,
    val centuryMax: Int,
    val type: String,
    val content: List<BookContent>
)

@Serializable
data class BookContent(
    val book: String,
    val ranges: List<String>
)

@Serializable
data class ChapterCoverage(
    val chapter: Int,
    val coveredVerses: Int,
    val totalVerses: Int,
    val coveragePercent: Double,
    val coveredList: List<Int>,
    val missingList: List<Int>
)

@Serializable
data class BookChapterCoverageResponse(
    val book: String,
    val century: Int,
    val chapters: List<ChapterCoverage>
)

@Serializable
data class GospelCoverageResponse(
    val century: Int,
    val individual: List<BookCoverage>,
    val aggregated: CoverageSummary,
    val missingVerses: List<MissingVerse>
)

@Serializable
data class TimelineEntry(
    val century: Int,
    val cumulativePercent: Double,
    val coveredVerses: Long,
    val newVersesCount: Long,
    val missingVersesCount: Long,
    val growthPercent: Double
)

@Serializable
data class TimelineResponse(
    val book: String? = null,
    val type: String? = null,
    val totalVerses: Int,
    val entries: List<TimelineEntry>
)

@Serializable
data class MissingVerse(
    val book: String,
    val chapter: Int,
    val verse: Int
)

@Serializable
data class MissingVersesResponse(
    val book: String,
    val century: Int,
    val totalMissing: Int,
    val missingVerses: List<MissingVerse>
)

@Serializable
data class StatsOverviewResponse(
    val totalManuscripts: Int,
    val byType: Map<String, Int>,
    val byCentury: List<CenturyCount>,
    val byBook: List<BookManuscriptCount>,
    val avgBooksPerManuscript: Double,
    val totalVerses: Int,
    val coveredVerses: Long,
    val overallCoveragePercent: Double
)

@Serializable
data class CenturyCount(
    val century: Int,
    val count: Int
)

@Serializable
data class BookManuscriptCount(
    val bookName: String,
    val manuscriptCount: Int
)

@Serializable
data class ManuscriptSummary(
    val gaId: String,
    val name: String?,
    val centuryMin: Int,
    val centuryMax: Int,
    val manuscriptType: String?,
    val bookCount: Int,
    val verseCount: Int
)

@Serializable
data class ManuscriptDetailResponse(
    val gaId: String,
    val name: String?,
    val centuryMin: Int,
    val centuryMax: Int,
    val manuscriptType: String?,
    val booksPreserved: List<BookRanges>,
    val intervals: List<BookInterval>,
    val dataSource: String,
    val ntvmrUrl: String,
    val historicalNotes: String? = null
)

@Serializable
data class BookRanges(
    val book: String,
    val ranges: List<String>
)

@Serializable
data class BookInterval(
    val book: String,
    val chapterMin: Int,
    val chapterMax: Int,
    val verseCount: Int
)

@Serializable
data class CenturyGrowthRate(
    val century: Int,
    val rate: Double
)

@Serializable
data class BookMetricsResponse(
    val bookName: String,
    val centuryGrowthRates: List<CenturyGrowthRate>,
    val stabilizationCentury: Int?,
    val fragmentationIndex: Double,
    val coverageDensity: Double,
    val manuscriptConcentrationScore: Double,
    val coverageByCentury: List<CenturyCoveragePercent>
)

@Serializable
data class CenturyCoveragePercent(
    val century: Int,
    val percent: Double
)

@Serializable
data class NtMetricsResponse(
    val books: List<BookMetricsResponse>,
    val overallStabilizationCentury: Int?,
    val overallCoverageByCentury: List<CenturyCoveragePercent>
)

@Serializable
data class ManuscriptsCountResponse(
    val total: Int,
    val papyrus: Int,
    val uncial: Int,
    val minuscule: Int,
    val lectionary: Int
)

@Serializable
data class IngestionStatusResponse(
    val status: String,
    val startedAt: String? = null,
    val finishedAt: String? = null,
    val durationMs: Long? = null,
    val manuscriptsIngested: Int = 0,
    val versesLinked: Int = 0,
    val errorMessage: String? = null,
    val isRunning: Boolean = false,
    val enableIngestion: Boolean = true
)

@Serializable
data class IngestionResult(
    val manuscriptsIngested: Int,
    val versesLinked: Int
)

@Serializable
data class VerseManuscriptItem(
    val gaId: String,
    val name: String? = null,
    val centuryMin: Int,
    val centuryMax: Int,
    val type: String? = null,
    val ntvmrUrl: String? = null
)

@Serializable
data class VerseManuscriptsResponse(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val manuscripts: List<VerseManuscriptItem>
)

@Serializable
data class BookTranslationItem(
    val canonicalName: String,
    val localizedName: String,
    val abbreviation: String,
    val order: Int
)
