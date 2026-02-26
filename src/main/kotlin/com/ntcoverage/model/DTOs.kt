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
