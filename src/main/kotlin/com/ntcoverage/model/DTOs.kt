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

@Serializable
data class ChurchFatherSummary(
    val id: Int,
    val displayName: String,
    val normalizedName: String,
    val centuryMin: Int,
    val centuryMax: Int,
    val tradition: String,
    val primaryLocation: String? = null
)

@Serializable
data class ChurchFatherDetail(
    val id: Int,
    val displayName: String,
    val normalizedName: String,
    val centuryMin: Int,
    val centuryMax: Int,
    val shortDescription: String? = null,
    val primaryLocation: String? = null,
    val tradition: String,
    val source: String,
    val mannerOfDeath: String? = null,
    val biographySummary: String? = null,
    val biographyOriginal: String? = null,
    val biographyIsLong: Boolean = false
)

@Serializable
data class ChurchFathersListResponse(
    val total: Int,
    val fathers: List<ChurchFatherSummary>
)

@Serializable
enum class TextualTopic {
    MANUSCRIPTS, AUTOGRAPHS, APOCRYPHA, CANON,
    TEXTUAL_VARIANTS, TRANSLATION, CORRUPTION, SCRIPTURE_AUTHORITY
}

@Serializable
data class TextualStatementDTO(
    val id: Int,
    val fatherId: Int,
    val fatherName: String,
    val topic: String,
    val statementText: String,
    val originalLanguage: String? = null,
    val originalText: String? = null,
    val sourceWork: String? = null,
    val sourceReference: String? = null,
    val approximateYear: Int? = null
)

@Serializable
data class TextualStatementsListResponse(
    val total: Int,
    val statements: List<TextualStatementDTO>
)

@Serializable
data class TopicSummaryDTO(
    val topic: String,
    val count: Int
)

@Serializable
data class TopicsSummaryResponse(
    val topics: List<TopicSummaryDTO>
)

// ── Visitor Tracking DTOs ──────────────────────────────────────

@Serializable
data class VisitorSessionRequest(
    val visitorId: String,
    val sessionId: String,
    val userAgent: String,
    val screenWidth: Int? = null,
    val screenHeight: Int? = null,
    val viewportWidth: Int? = null,
    val viewportHeight: Int? = null,
    val language: String? = null,
    val languages: List<String>? = null,
    val timezone: String? = null,
    val platform: String? = null,
    val networkInfo: NetworkInfoDTO? = null,
    val deviceMemory: Int? = null,
    val hardwareConcurrency: Int? = null,
    val colorDepth: Int? = null,
    val pixelRatio: Double? = null,
    val touchPoints: Int? = null,
    val cookieEnabled: Boolean? = null,
    val doNotTrack: Boolean? = null,
    val webglRenderer: String? = null,
    val webglVendor: String? = null,
    val canvasFingerprint: String? = null,
    val referrer: String? = null,
    val pageLoadTimeMs: Int? = null
)

@Serializable
data class NetworkInfoDTO(
    val effectiveType: String? = null,
    val downlink: Double? = null,
    val rtt: Int? = null
)

@Serializable
data class PageViewRequest(
    val sessionId: String,
    val visitorId: String,
    val path: String,
    val referrerPath: String? = null,
    val durationMs: Int? = null
)

@Serializable
data class HeartbeatRequest(
    val sessionId: String
)

@Serializable
data class VisitorSessionCompact(
    val id: Long,
    val visitorId: String,
    val sessionId: String,
    val ipAddress: String,
    val browserName: String? = null,
    val browserVersion: String? = null,
    val osName: String? = null,
    val deviceType: String? = null,
    val language: String? = null,
    val timezone: String? = null,
    val referrer: String? = null,
    val pageLoadTimeMs: Int? = null,
    val createdAt: String,
    val lastActivityAt: String
)

@Serializable
data class VisitorSessionComplete(
    val id: Long,
    val visitorId: String,
    val sessionId: String,
    val ipAddress: String,
    val userAgent: String,
    val browserName: String? = null,
    val browserVersion: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceType: String? = null,
    val screenWidth: Int? = null,
    val screenHeight: Int? = null,
    val viewportWidth: Int? = null,
    val viewportHeight: Int? = null,
    val language: String? = null,
    val languages: String? = null,
    val timezone: String? = null,
    val platform: String? = null,
    val networkInfo: String? = null,
    val deviceMemory: Int? = null,
    val hardwareConcurrency: Int? = null,
    val colorDepth: Int? = null,
    val pixelRatio: Double? = null,
    val touchPoints: Int? = null,
    val cookieEnabled: Boolean? = null,
    val doNotTrack: Boolean? = null,
    val webglRenderer: String? = null,
    val webglVendor: String? = null,
    val canvasFingerprint: String? = null,
    val referrer: String? = null,
    val pageLoadTimeMs: Int? = null,
    val createdAt: String,
    val lastActivityAt: String
)

@Serializable
data class PageViewDTO(
    val id: Long,
    val sessionId: String,
    val visitorId: String,
    val path: String,
    val referrerPath: String? = null,
    val durationMs: Int? = null,
    val createdAt: String
)

@Serializable
data class AnalyticsOverview(
    val activeNow: Int,
    val sessionsInRange: Int,
    val uniqueVisitorsInRange: Int,
    val pageviewsInRange: Int,
    val avgLoadTimeMs: Int?
)

@Serializable
data class SessionsPageResponse(
    val total: Long,
    val page: Int,
    val limit: Int,
    val sessions: List<VisitorSessionCompact>
)

@Serializable
data class SessionsPageCompleteResponse(
    val total: Long,
    val page: Int,
    val limit: Int,
    val sessions: List<VisitorSessionComplete>
)

@Serializable
data class LiveVisitorDTO(
    val sessionId: String,
    val visitorId: String,
    val ipAddress: String,
    val browserName: String? = null,
    val osName: String? = null,
    val deviceType: String? = null,
    val currentPage: String? = null,
    val sessionStarted: String,
    val lastActivity: String
)

@Serializable
data class FilterValuesResponse(
    val browsers: List<String>,
    val operatingSystems: List<String>,
    val deviceTypes: List<String>,
    val languages: List<String>,
    val timezones: List<String>,
    val connectionTypes: List<String>,
    val paths: List<String>
)

@Serializable
data class TimelineBucket(
    val bucket: String,
    val count: Int,
    val series: Map<String, Int>? = null
)

@Serializable
data class TimelineResponse2(
    val granularity: String,
    val breakdown: String,
    val buckets: List<TimelineBucket>
)

@Serializable
data class HeatmapCell(
    val dayOfWeek: Int,
    val hourOfDay: Int,
    val count: Int
)

@Serializable
data class HeatmapResponse(
    val cells: List<HeatmapCell>
)

@Serializable
data class VisitorSummaryDTO(
    val visitorId: String,
    val sessionCount: Int,
    val totalPageviews: Int,
    val firstSeenAt: String,
    val lastSeenAt: String,
    val lastBrowser: String? = null,
    val lastOs: String? = null,
    val lastDeviceType: String? = null,
    val lastIp: String? = null
)

@Serializable
data class VisitorsListResponse(
    val total: Long,
    val page: Int,
    val limit: Int,
    val visitors: List<VisitorSummaryDTO>
)

@Serializable
data class DistributionItem(
    val value: String,
    val count: Int,
    val percent: Double
)

@Serializable
data class DistributionResponse(
    val field: String,
    val total: Int,
    val items: List<DistributionItem>
)

@Serializable
data class TopPageDTO(
    val path: String,
    val count: Int,
    val avgDurationMs: Int? = null
)

@Serializable
data class TopReferrerDTO(
    val referrer: String,
    val count: Int
)

@Serializable
data class DailyStatDTO(
    val date: String,
    val totalSessions: Int,
    val totalPageviews: Int,
    val uniqueVisitors: Int,
    val avgSessionDurationMs: Int? = null,
    val topBrowsers: String? = null,
    val topOs: String? = null,
    val topDevices: String? = null,
    val topPages: String? = null
)

@Serializable
data class TrendsResponse(
    val days: List<DailyStatDTO>
)
