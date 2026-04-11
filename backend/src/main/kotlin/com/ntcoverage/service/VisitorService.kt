package com.ntcoverage.service

import com.ntcoverage.model.*
import com.ntcoverage.repository.VisitorRepository
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicReference

class VisitorService(private val repository: VisitorRepository) {

    private val log = LoggerFactory.getLogger(VisitorService::class.java)

    // ── In-memory cache ──

    private data class Cached<T>(val data: T, val ts: Long)

    private val overviewCache = AtomicReference<Cached<AnalyticsOverview>?>()
    private val liveCache = AtomicReference<Cached<List<LiveVisitorDTO>>?>()
    private val filterValuesCache = AtomicReference<Cached<FilterValuesResponse>?>()

    private fun <T> getOrCompute(ref: AtomicReference<Cached<T>?>, ttlMs: Long, compute: () -> T): T {
        val cached = ref.get()
        val now = System.currentTimeMillis()
        if (cached != null && now - cached.ts < ttlMs) return cached.data
        val fresh = compute()
        ref.set(Cached(fresh, now))
        return fresh
    }

    // ── Tracking ──

    fun createSession(req: VisitorSessionRequest, ipAddress: String): Long {
        val parsed = parseUserAgent(req.userAgent)
        return repository.insertSession(
            req, ipAddress,
            browserName = parsed.browserName,
            browserVersion = parsed.browserVersion,
            osName = parsed.osName,
            osVersion = parsed.osVersion,
            deviceType = parsed.deviceType
        )
    }

    fun recordPageView(req: PageViewRequest): Long = repository.insertPageView(req)

    fun heartbeat(sessionId: String): Boolean = repository.updateHeartbeat(sessionId)

    // ── Analytics: Overview ──

    fun getOverview(from: Instant, to: Instant): AnalyticsOverview {
        val cacheKey = "$from-$to"
        return getOrCompute(overviewCache, 10_000L) {
            AnalyticsOverview(
                activeNow = repository.countActiveNow(),
                sessionsInRange = repository.countSessionsInRange(from, to),
                uniqueVisitorsInRange = repository.countUniqueVisitorsInRange(from, to),
                pageviewsInRange = repository.countPageviewsInRange(from, to),
                avgLoadTimeMs = repository.avgLoadTimeInRange(from, to)
            )
        }
    }

    fun getLiveVisitors(): List<LiveVisitorDTO> =
        getOrCompute(liveCache, 10_000L) { repository.getLiveVisitors() }

    // ── Analytics: Explorer ──

    fun getSessionsCompact(
        from: Instant, to: Instant,
        browser: String?, os: String?, deviceType: String?,
        language: String?, timezone: String?, ip: String?,
        visitorId: String?, fingerprint: String?, referrer: String?,
        minLoadTime: Int?, maxLoadTime: Int?,
        sort: String, order: String,
        page: Int, limit: Int
    ): SessionsPageResponse {
        val (total, sessions) = repository.getSessionsCompact(
            from, to, browser, os, deviceType, language, timezone, ip,
            visitorId, fingerprint, referrer, minLoadTime, maxLoadTime,
            sort, order, page, limit
        )
        return SessionsPageResponse(total = total, page = page, limit = limit, sessions = sessions)
    }

    fun getSessionsComplete(
        from: Instant, to: Instant,
        browser: String?, os: String?, deviceType: String?,
        language: String?, timezone: String?, ip: String?,
        visitorId: String?, fingerprint: String?, referrer: String?,
        minLoadTime: Int?, maxLoadTime: Int?,
        sort: String, order: String,
        page: Int, limit: Int
    ): SessionsPageCompleteResponse {
        val (total, sessions) = repository.getSessionsComplete(
            from, to, browser, os, deviceType, language, timezone, ip,
            visitorId, fingerprint, referrer, minLoadTime, maxLoadTime,
            sort, order, page, limit
        )
        return SessionsPageCompleteResponse(total = total, page = page, limit = limit, sessions = sessions)
    }

    fun getSessionDetail(sessionId: String): VisitorSessionComplete? =
        repository.getSessionBySessionId(sessionId)

    fun getSessionPageViews(sessionId: String): List<PageViewDTO> =
        repository.getPageViewsForSession(sessionId)

    // ── Analytics: Filters ──

    fun getFilterValues(): FilterValuesResponse =
        getOrCompute(filterValuesCache, 60_000L) {
            val from = Instant.now().minus(30, ChronoUnit.DAYS)
            val to = Instant.now()
            FilterValuesResponse(
                browsers = repository.getDistinctValues("browser", from, to),
                operatingSystems = repository.getDistinctValues("os", from, to),
                deviceTypes = repository.getDistinctValues("device", from, to),
                languages = repository.getDistinctValues("language", from, to),
                timezones = repository.getDistinctValues("timezone", from, to),
                connectionTypes = repository.getDistinctConnectionTypes(from, to),
                paths = repository.getDistinctPaths(from, to)
            )
        }

    // ── Analytics: Distribution ──

    fun getDistribution(field: String, from: Instant, to: Instant): DistributionResponse {
        val items = repository.getDistribution(field, from, to)
        val total = items.sumOf { it.second }
        return DistributionResponse(
            field = field,
            total = total,
            items = items.map { (value, count) ->
                DistributionItem(value, count, if (total > 0) count * 100.0 / total else 0.0)
            }
        )
    }

    fun getTopPages(from: Instant, to: Instant, limit: Int): List<TopPageDTO> =
        repository.getTopPages(from, to, limit)

    fun getTopReferrers(from: Instant, to: Instant, limit: Int): List<TopReferrerDTO> =
        repository.getTopReferrers(from, to, limit)

    // ── Analytics: Timeline ──

    fun getTimeline(
        table: String, from: Instant, to: Instant,
        granularity: String?, breakdown: String?
    ): TimelineResponse2 {
        val autoGranularity = granularity ?: autoGranularity(from, to)
        val buckets = repository.getTimelineBucketed(table, from, to, autoGranularity, breakdown)
        return TimelineResponse2(granularity = autoGranularity, breakdown = breakdown ?: "none", buckets = buckets)
    }

    fun getHeatmap(from: Instant, to: Instant): HeatmapResponse =
        HeatmapResponse(cells = repository.getHeatmap(from, to))

    // ── Analytics: Visitors ──

    fun getVisitorsList(
        from: Instant, to: Instant,
        returning: Boolean?,
        sort: String, order: String,
        page: Int, limit: Int
    ): VisitorsListResponse {
        val (total, visitors) = repository.getVisitorsList(from, to, returning, sort, order, page, limit)
        return VisitorsListResponse(total = total, page = page, limit = limit, visitors = visitors)
    }

    fun getVisitorProfile(visitorId: String): VisitorSummaryDTO? =
        repository.getVisitorProfile(visitorId)

    fun getVisitorSessions(visitorId: String, page: Int, limit: Int): SessionsPageResponse {
        val (total, sessions) = repository.getVisitorSessions(visitorId, page, limit)
        return SessionsPageResponse(total = total, page = page, limit = limit, sessions = sessions)
    }

    // ── Analytics: Trends ──

    fun getTrends(days: Int): TrendsResponse =
        TrendsResponse(days = repository.getDailyStats(days.coerceIn(1, 90)))

    // ── Helpers ──

    private fun autoGranularity(from: Instant, to: Instant): String {
        val hours = ChronoUnit.HOURS.between(from, to)
        return when {
            hours < 6 -> "minute"
            hours < 168 -> "hour"
            else -> "day"
        }
    }

    data class ParsedUA(
        val browserName: String?,
        val browserVersion: String?,
        val osName: String?,
        val osVersion: String?,
        val deviceType: String?
    )

    fun parseUserAgent(ua: String): ParsedUA {
        val browserName: String?
        val browserVersion: String?
        val osName: String?
        val osVersion: String?

        when {
            ua.contains("Edg/") -> {
                browserName = "Edge"
                browserVersion = Regex("Edg/([\\d.]+)").find(ua)?.groupValues?.get(1)
            }
            ua.contains("OPR/") || ua.contains("Opera") -> {
                browserName = "Opera"
                browserVersion = Regex("OPR/([\\d.]+)").find(ua)?.groupValues?.get(1)
            }
            ua.contains("Chrome/") && !ua.contains("Edg/") && !ua.contains("OPR/") -> {
                browserName = "Chrome"
                browserVersion = Regex("Chrome/([\\d.]+)").find(ua)?.groupValues?.get(1)
            }
            ua.contains("Safari/") && !ua.contains("Chrome") -> {
                browserName = "Safari"
                browserVersion = Regex("Version/([\\d.]+)").find(ua)?.groupValues?.get(1)
            }
            ua.contains("Firefox/") -> {
                browserName = "Firefox"
                browserVersion = Regex("Firefox/([\\d.]+)").find(ua)?.groupValues?.get(1)
            }
            else -> {
                browserName = null
                browserVersion = null
            }
        }

        when {
            ua.contains("Windows") -> {
                osName = "Windows"
                osVersion = Regex("Windows NT ([\\d.]+)").find(ua)?.groupValues?.get(1)
            }
            ua.contains("Mac OS X") -> {
                osName = "macOS"
                osVersion = Regex("Mac OS X ([\\d_]+)").find(ua)?.groupValues?.get(1)?.replace('_', '.')
            }
            ua.contains("Android") -> {
                osName = "Android"
                osVersion = Regex("Android ([\\d.]+)").find(ua)?.groupValues?.get(1)
            }
            ua.contains("iPhone") || ua.contains("iPad") -> {
                osName = "iOS"
                osVersion = Regex("OS ([\\d_]+)").find(ua)?.groupValues?.get(1)?.replace('_', '.')
            }
            ua.contains("Linux") -> {
                osName = "Linux"
                osVersion = null
            }
            ua.contains("CrOS") -> {
                osName = "Chrome OS"
                osVersion = null
            }
            else -> {
                osName = null
                osVersion = null
            }
        }

        val deviceType = when {
            ua.contains("Mobile") || ua.contains("Android") && !ua.contains("Tablet") -> "mobile"
            ua.contains("Tablet") || ua.contains("iPad") -> "tablet"
            else -> "desktop"
        }

        return ParsedUA(browserName, browserVersion?.take(30), osName, osVersion?.take(30), deviceType)
    }
}
