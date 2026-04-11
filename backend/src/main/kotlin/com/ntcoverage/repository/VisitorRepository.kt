package com.ntcoverage.repository

import com.ntcoverage.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class VisitorRepository {

    private val isoFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    private fun Instant.toOdt(): OffsetDateTime = this.atOffset(ZoneOffset.UTC)
    private fun OffsetDateTime.fmt(): String = isoFmt.format(this)

    fun insertSession(
        req: VisitorSessionRequest,
        ipAddress: String,
        browserName: String?,
        browserVersion: String?,
        osName: String?,
        osVersion: String?,
        deviceType: String?
    ): Long = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        VisitorSessions.insert {
            it[visitorId] = req.visitorId
            it[sessionId] = req.sessionId
            it[this.ipAddress] = ipAddress
            it[userAgent] = req.userAgent
            it[this.browserName] = browserName
            it[this.browserVersion] = browserVersion
            it[this.osName] = osName
            it[this.osVersion] = osVersion
            it[this.deviceType] = deviceType
            it[screenWidth] = req.screenWidth?.toShort()
            it[screenHeight] = req.screenHeight?.toShort()
            it[viewportWidth] = req.viewportWidth?.toShort()
            it[viewportHeight] = req.viewportHeight?.toShort()
            it[language] = req.language
            it[languages] = req.languages?.joinToString(",")
            it[timezone] = req.timezone
            it[platform] = req.platform
            it[networkInfo] = req.networkInfo?.let {
                ni -> """{"effectiveType":"${ni.effectiveType ?: ""}","downlink":${ni.downlink ?: 0},"rtt":${ni.rtt ?: 0}}"""
            }
            it[deviceMemory] = req.deviceMemory?.toShort()
            it[hardwareConcurrency] = req.hardwareConcurrency?.toShort()
            it[colorDepth] = req.colorDepth?.toShort()
            it[pixelRatio] = req.pixelRatio?.let { p -> BigDecimal.valueOf(p) }
            it[touchPoints] = req.touchPoints?.toShort()
            it[cookieEnabled] = req.cookieEnabled
            it[doNotTrack] = req.doNotTrack
            it[webglRenderer] = req.webglRenderer?.take(200)
            it[webglVendor] = req.webglVendor?.take(200)
            it[canvasFingerprint] = req.canvasFingerprint?.take(64)
            it[referrer] = req.referrer
            it[pageLoadTimeMs] = req.pageLoadTimeMs
            it[createdAt] = now
            it[lastActivityAt] = now
        }[VisitorSessions.id]
    }

    fun insertPageView(req: PageViewRequest): Long = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        PageViews.insert {
            it[sessionId] = req.sessionId
            it[visitorId] = req.visitorId
            it[path] = req.path.take(500)
            it[referrerPath] = req.referrerPath?.take(500)
            it[durationMs] = req.durationMs
            it[createdAt] = now
        }[PageViews.id]
    }

    fun updateHeartbeat(sessionId: String): Boolean = transaction {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        VisitorSessions.update({ VisitorSessions.sessionId eq sessionId }) {
            it[lastActivityAt] = now
        } > 0
    }

    fun countActiveNow(minutesThreshold: Int = 5): Int = transaction {
        val cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(minutesThreshold.toLong())
        VisitorSessions.selectAll()
            .where { VisitorSessions.lastActivityAt greaterEq cutoff }
            .count().toInt()
    }

    fun countSessionsInRange(from: Instant, to: Instant): Int = transaction {
        VisitorSessions.selectAll()
            .where { (VisitorSessions.createdAt greaterEq from.toOdt()) and (VisitorSessions.createdAt lessEq to.toOdt()) }
            .count().toInt()
    }

    fun countUniqueVisitorsInRange(from: Instant, to: Instant): Int = transaction {
        VisitorSessions.select(VisitorSessions.visitorId)
            .where { (VisitorSessions.createdAt greaterEq from.toOdt()) and (VisitorSessions.createdAt lessEq to.toOdt()) }
            .withDistinct()
            .count().toInt()
    }

    fun countPageviewsInRange(from: Instant, to: Instant): Int = transaction {
        PageViews.selectAll()
            .where { (PageViews.createdAt greaterEq from.toOdt()) and (PageViews.createdAt lessEq to.toOdt()) }
            .count().toInt()
    }

    fun avgLoadTimeInRange(from: Instant, to: Instant): Int? = transaction {
        VisitorSessions
            .select(VisitorSessions.pageLoadTimeMs.avg())
            .where {
                (VisitorSessions.createdAt greaterEq from.toOdt()) and
                (VisitorSessions.createdAt lessEq to.toOdt()) and
                (VisitorSessions.pageLoadTimeMs.isNotNull())
            }
            .firstOrNull()?.let { row ->
                row[VisitorSessions.pageLoadTimeMs.avg()]?.toInt()
            }
    }

    fun getLiveVisitors(minutesThreshold: Int = 5): List<LiveVisitorDTO> = transaction {
        val cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(minutesThreshold.toLong())
        VisitorSessions.selectAll()
            .where { VisitorSessions.lastActivityAt greaterEq cutoff }
            .orderBy(VisitorSessions.lastActivityAt, SortOrder.DESC)
            .map { row ->
                val sid = row[VisitorSessions.sessionId]
                val latestPage = PageViews.selectAll()
                    .where { PageViews.sessionId eq sid }
                    .orderBy(PageViews.createdAt, SortOrder.DESC)
                    .limit(1)
                    .firstOrNull()?.get(PageViews.path)
                LiveVisitorDTO(
                    sessionId = sid,
                    visitorId = row[VisitorSessions.visitorId],
                    ipAddress = row[VisitorSessions.ipAddress],
                    browserName = row[VisitorSessions.browserName],
                    osName = row[VisitorSessions.osName],
                    deviceType = row[VisitorSessions.deviceType],
                    currentPage = latestPage,
                    sessionStarted = row[VisitorSessions.createdAt].fmt(),
                    lastActivity = row[VisitorSessions.lastActivityAt].fmt()
                )
            }
    }

    fun getSessionsCompact(
        from: Instant, to: Instant,
        browser: String?, os: String?, deviceType: String?,
        language: String?, timezone: String?, ip: String?,
        visitorId: String?, fingerprint: String?, referrer: String?,
        minLoadTime: Int?, maxLoadTime: Int?,
        sort: String, order: String,
        page: Int, limit: Int
    ): Pair<Long, List<VisitorSessionCompact>> = transaction {
        val query = VisitorSessions.selectAll().where {
            buildConditions(from, to, browser, os, deviceType, language, timezone, ip, visitorId, fingerprint, referrer, minLoadTime, maxLoadTime)
        }
        val total = query.count()
        val sortCol = resolveSortColumn(sort)
        val sortOrder = if (order.lowercase() == "asc") SortOrder.ASC else SortOrder.DESC
        val rows = query.orderBy(sortCol, sortOrder)
            .limit(limit).offset(((page - 1) * limit).toLong())
            .map { it.toCompact() }
        Pair(total, rows)
    }

    fun getSessionsComplete(
        from: Instant, to: Instant,
        browser: String?, os: String?, deviceType: String?,
        language: String?, timezone: String?, ip: String?,
        visitorId: String?, fingerprint: String?, referrer: String?,
        minLoadTime: Int?, maxLoadTime: Int?,
        sort: String, order: String,
        page: Int, limit: Int
    ): Pair<Long, List<VisitorSessionComplete>> = transaction {
        val query = VisitorSessions.selectAll().where {
            buildConditions(from, to, browser, os, deviceType, language, timezone, ip, visitorId, fingerprint, referrer, minLoadTime, maxLoadTime)
        }
        val total = query.count()
        val sortCol = resolveSortColumn(sort)
        val sortOrder = if (order.lowercase() == "asc") SortOrder.ASC else SortOrder.DESC
        val rows = query.orderBy(sortCol, sortOrder)
            .limit(limit).offset(((page - 1) * limit).toLong())
            .map { it.toComplete() }
        Pair(total, rows)
    }

    fun getSessionBySessionId(sessionId: String): VisitorSessionComplete? = transaction {
        VisitorSessions.selectAll()
            .where { VisitorSessions.sessionId eq sessionId }
            .firstOrNull()?.toComplete()
    }

    fun getPageViewsForSession(sessionId: String): List<PageViewDTO> = transaction {
        PageViews.selectAll()
            .where { PageViews.sessionId eq sessionId }
            .orderBy(PageViews.createdAt, SortOrder.ASC)
            .map { it.toPageViewDTO() }
    }

    fun getDistinctValues(field: String, from: Instant, to: Instant): List<String> = transaction {
        val col = when (field) {
            "browser" -> VisitorSessions.browserName
            "os" -> VisitorSessions.osName
            "device" -> VisitorSessions.deviceType
            "language" -> VisitorSessions.language
            "timezone" -> VisitorSessions.timezone
            else -> VisitorSessions.browserName
        }
        VisitorSessions.select(col)
            .where { (VisitorSessions.createdAt greaterEq from.toOdt()) and (VisitorSessions.createdAt lessEq to.toOdt()) and (col.isNotNull()) }
            .withDistinct()
            .mapNotNull { it[col] }
            .sorted()
    }

    fun getDistinctConnectionTypes(from: Instant, to: Instant): List<String> = transaction {
        VisitorSessions.select(VisitorSessions.networkInfo)
            .where { (VisitorSessions.createdAt greaterEq from.toOdt()) and (VisitorSessions.createdAt lessEq to.toOdt()) and (VisitorSessions.networkInfo.isNotNull()) }
            .withDistinct()
            .mapNotNull { row ->
                val json = row[VisitorSessions.networkInfo] ?: return@mapNotNull null
                val match = Regex(""""effectiveType"\s*:\s*"([^"]+)"""").find(json)
                match?.groupValues?.get(1)
            }
            .distinct()
            .sorted()
    }

    fun getDistinctPaths(from: Instant, to: Instant): List<String> = transaction {
        PageViews.select(PageViews.path)
            .where { (PageViews.createdAt greaterEq from.toOdt()) and (PageViews.createdAt lessEq to.toOdt()) }
            .withDistinct()
            .map { it[PageViews.path] }
            .sorted()
    }

    fun getDistribution(field: String, from: Instant, to: Instant, limit: Int = 20): List<Pair<String, Int>> = transaction {
        val col = when (field) {
            "browser" -> VisitorSessions.browserName
            "os" -> VisitorSessions.osName
            "device" -> VisitorSessions.deviceType
            "language" -> VisitorSessions.language
            "timezone" -> VisitorSessions.timezone
            else -> VisitorSessions.browserName
        }
        val cnt = VisitorSessions.id.count()
        VisitorSessions.select(col, cnt)
            .where { (VisitorSessions.createdAt greaterEq from.toOdt()) and (VisitorSessions.createdAt lessEq to.toOdt()) and (col.isNotNull()) }
            .groupBy(col)
            .orderBy(cnt, SortOrder.DESC)
            .limit(limit)
            .map { Pair(it[col]!!, it[cnt].toInt()) }
    }

    fun getTopPages(from: Instant, to: Instant, limit: Int = 10): List<TopPageDTO> = transaction {
        val cnt = PageViews.id.count()
        val avgDur = PageViews.durationMs.avg()
        PageViews.select(PageViews.path, cnt, avgDur)
            .where { (PageViews.createdAt greaterEq from.toOdt()) and (PageViews.createdAt lessEq to.toOdt()) }
            .groupBy(PageViews.path)
            .orderBy(cnt, SortOrder.DESC)
            .limit(limit)
            .map {
                TopPageDTO(
                    path = it[PageViews.path],
                    count = it[cnt].toInt(),
                    avgDurationMs = it[avgDur]?.toInt()
                )
            }
    }

    fun getTopReferrers(from: Instant, to: Instant, limit: Int = 10): List<TopReferrerDTO> = transaction {
        val cnt = VisitorSessions.id.count()
        VisitorSessions.select(VisitorSessions.referrer, cnt)
            .where {
                (VisitorSessions.createdAt greaterEq from.toOdt()) and
                (VisitorSessions.createdAt lessEq to.toOdt()) and
                (VisitorSessions.referrer.isNotNull()) and
                (VisitorSessions.referrer neq "")
            }
            .groupBy(VisitorSessions.referrer)
            .orderBy(cnt, SortOrder.DESC)
            .limit(limit)
            .map { TopReferrerDTO(referrer = it[VisitorSessions.referrer]!!, count = it[cnt].toInt()) }
    }

    fun getTimelineBucketed(
        table: String,
        from: Instant, to: Instant,
        granularity: String,
        breakdown: String?
    ): List<TimelineBucket> = transaction {
        val truncFn = when (granularity) {
            "minute" -> "minute"
            "hour" -> "hour"
            else -> "day"
        }
        val tbl = if (table == "pageviews") "page_views" else "visitor_sessions"
        val breakdownCol = when (breakdown) {
            "browser" -> "browser_name"
            "os" -> "os_name"
            "device" -> "device_type"
            "path" -> if (table == "pageviews") "path" else null
            else -> null
        }

        val fromStr = from.toOdt().fmt()
        val toStr = to.toOdt().fmt()

        if (breakdownCol != null) {
            val sql = """
                SELECT date_trunc('$truncFn', created_at) AS bucket, $breakdownCol AS bk, COUNT(*) AS cnt
                FROM $tbl
                WHERE created_at >= '$fromStr' AND created_at <= '$toStr'
                GROUP BY bucket, bk ORDER BY bucket ASC
            """.trimIndent()
            val results = mutableMapOf<String, MutableMap<String, Int>>()
            exec(sql) { rs ->
                while (rs.next()) {
                    val bucket = rs.getTimestamp("bucket").toInstant().atOffset(ZoneOffset.UTC).fmt()
                    val bk = rs.getString("bk") ?: "unknown"
                    val cnt = rs.getInt("cnt")
                    results.getOrPut(bucket) { mutableMapOf() }[bk] = cnt
                }
            }
            results.map { (bucket, series) ->
                TimelineBucket(bucket = bucket, count = series.values.sum(), series = series)
            }
        } else {
            val sql = """
                SELECT date_trunc('$truncFn', created_at) AS bucket, COUNT(*) AS cnt
                FROM $tbl
                WHERE created_at >= '$fromStr' AND created_at <= '$toStr'
                GROUP BY bucket ORDER BY bucket ASC
            """.trimIndent()
            val results = mutableListOf<TimelineBucket>()
            exec(sql) { rs ->
                while (rs.next()) {
                    results.add(
                        TimelineBucket(
                            bucket = rs.getTimestamp("bucket").toInstant().atOffset(ZoneOffset.UTC).fmt(),
                            count = rs.getInt("cnt")
                        )
                    )
                }
            }
            results
        }
    }

    fun getHeatmap(from: Instant, to: Instant): List<HeatmapCell> = transaction {
        val fromStr = from.toOdt().fmt()
        val toStr = to.toOdt().fmt()
        val sql = """
            SELECT EXTRACT(DOW FROM created_at)::int AS dow, EXTRACT(HOUR FROM created_at)::int AS hod, COUNT(*)::int AS cnt
            FROM visitor_sessions
            WHERE created_at >= '$fromStr' AND created_at <= '$toStr'
            GROUP BY dow, hod
        """.trimIndent()
        val results = mutableListOf<HeatmapCell>()
        exec(sql) { rs ->
            while (rs.next()) {
                results.add(HeatmapCell(dayOfWeek = rs.getInt("dow"), hourOfDay = rs.getInt("hod"), count = rs.getInt("cnt")))
            }
        }
        results
    }

    fun getVisitorsList(
        from: Instant, to: Instant,
        returning: Boolean?,
        sort: String, order: String,
        page: Int, limit: Int
    ): Pair<Long, List<VisitorSummaryDTO>> = transaction {
        val fromStr = from.toOdt().fmt()
        val toStr = to.toOdt().fmt()
        val orderDir = if (order.lowercase() == "asc") "ASC" else "DESC"
        val sortField = when (sort) {
            "sessions" -> "session_count"
            "pageviews" -> "total_pv"
            "first_seen" -> "first_seen_at"
            else -> "last_seen_at"
        }

        val havingClause = when (returning) {
            true -> "HAVING COUNT(DISTINCT session_id) > 1"
            false -> "HAVING COUNT(DISTINCT session_id) = 1"
            null -> ""
        }

        val countSql = """
            SELECT COUNT(*) FROM (
                SELECT visitor_id FROM visitor_sessions
                WHERE created_at >= '$fromStr' AND created_at <= '$toStr'
                GROUP BY visitor_id $havingClause
            ) sub
        """.trimIndent()

        var total = 0L
        exec(countSql) { rs -> if (rs.next()) total = rs.getLong(1) }

        val offset = (page - 1) * limit
        val sql = """
            SELECT
                vs.visitor_id,
                COUNT(DISTINCT vs.session_id) AS session_count,
                MIN(vs.created_at) AS first_seen_at,
                MAX(vs.created_at) AS last_seen_at,
                (SELECT COUNT(*) FROM page_views pv WHERE pv.visitor_id = vs.visitor_id AND pv.created_at >= '$fromStr' AND pv.created_at <= '$toStr') AS total_pv
            FROM visitor_sessions vs
            WHERE vs.created_at >= '$fromStr' AND vs.created_at <= '$toStr'
            GROUP BY vs.visitor_id $havingClause
            ORDER BY $sortField $orderDir
            LIMIT $limit OFFSET $offset
        """.trimIndent()

        val visitors = mutableListOf<VisitorSummaryDTO>()
        exec(sql) { rs ->
            while (rs.next()) {
                val vid = rs.getString("visitor_id")
                val lastSession = VisitorSessions.selectAll()
                    .where { VisitorSessions.visitorId eq vid }
                    .orderBy(VisitorSessions.createdAt, SortOrder.DESC)
                    .limit(1)
                    .firstOrNull()

                visitors.add(
                    VisitorSummaryDTO(
                        visitorId = vid,
                        sessionCount = rs.getInt("session_count"),
                        totalPageviews = rs.getInt("total_pv"),
                        firstSeenAt = rs.getTimestamp("first_seen_at").toInstant().atOffset(ZoneOffset.UTC).fmt(),
                        lastSeenAt = rs.getTimestamp("last_seen_at").toInstant().atOffset(ZoneOffset.UTC).fmt(),
                        lastBrowser = lastSession?.get(VisitorSessions.browserName),
                        lastOs = lastSession?.get(VisitorSessions.osName),
                        lastDeviceType = lastSession?.get(VisitorSessions.deviceType),
                        lastIp = lastSession?.get(VisitorSessions.ipAddress)
                    )
                )
            }
        }
        Pair(total, visitors)
    }

    fun getVisitorProfile(visitorId: String): VisitorSummaryDTO? = transaction {
        val sessions = VisitorSessions.selectAll()
            .where { VisitorSessions.visitorId eq visitorId }
            .orderBy(VisitorSessions.createdAt, SortOrder.DESC)
            .toList()
        if (sessions.isEmpty()) return@transaction null
        val pvCount = PageViews.selectAll()
            .where { PageViews.visitorId eq visitorId }
            .count().toInt()
        val last = sessions.first()
        val first = sessions.last()
        VisitorSummaryDTO(
            visitorId = visitorId,
            sessionCount = sessions.size,
            totalPageviews = pvCount,
            firstSeenAt = first[VisitorSessions.createdAt].fmt(),
            lastSeenAt = last[VisitorSessions.createdAt].fmt(),
            lastBrowser = last[VisitorSessions.browserName],
            lastOs = last[VisitorSessions.osName],
            lastDeviceType = last[VisitorSessions.deviceType],
            lastIp = last[VisitorSessions.ipAddress]
        )
    }

    fun getVisitorSessions(visitorId: String, page: Int, limit: Int): Pair<Long, List<VisitorSessionCompact>> = transaction {
        val query = VisitorSessions.selectAll()
            .where { VisitorSessions.visitorId eq visitorId }
        val total = query.count()
        val rows = query.orderBy(VisitorSessions.createdAt, SortOrder.DESC)
            .limit(limit).offset(((page - 1) * limit).toLong())
            .map { it.toCompact() }
        Pair(total, rows)
    }

    fun getDailyStats(days: Int): List<DailyStatDTO> = transaction {
        VisitorDailyStats.selectAll()
            .orderBy(VisitorDailyStats.statDate, SortOrder.DESC)
            .limit(days)
            .map {
                DailyStatDTO(
                    date = it[VisitorDailyStats.statDate].toString(),
                    totalSessions = it[VisitorDailyStats.totalSessions],
                    totalPageviews = it[VisitorDailyStats.totalPageviews],
                    uniqueVisitors = it[VisitorDailyStats.uniqueVisitors],
                    avgSessionDurationMs = it[VisitorDailyStats.avgSessionDurationMs],
                    topBrowsers = it[VisitorDailyStats.topBrowsers],
                    topOs = it[VisitorDailyStats.topOs],
                    topDevices = it[VisitorDailyStats.topDevices],
                    topPages = it[VisitorDailyStats.topPages]
                )
            }
            .reversed()
    }

    // ── private helpers ──

    private fun buildConditions(
        from: Instant, to: Instant,
        browser: String?, os: String?, deviceType: String?,
        language: String?, timezone: String?, ip: String?,
        visitorId: String?, fingerprint: String?, referrer: String?,
        minLoadTime: Int?, maxLoadTime: Int?
    ): Op<Boolean> {
        var cond: Op<Boolean> = (VisitorSessions.createdAt greaterEq from.toOdt()) and (VisitorSessions.createdAt lessEq to.toOdt())
        browser?.let { cond = cond and (VisitorSessions.browserName eq it) }
        os?.let { cond = cond and (VisitorSessions.osName eq it) }
        deviceType?.let { cond = cond and (VisitorSessions.deviceType eq it) }
        language?.let { cond = cond and (VisitorSessions.language eq it) }
        timezone?.let { cond = cond and (VisitorSessions.timezone eq it) }
        ip?.let { cond = cond and (VisitorSessions.ipAddress eq it) }
        visitorId?.let { cond = cond and (VisitorSessions.visitorId eq it) }
        fingerprint?.let { cond = cond and (VisitorSessions.canvasFingerprint eq it) }
        referrer?.let { cond = cond and (VisitorSessions.referrer like "%$it%") }
        minLoadTime?.let { cond = cond and (VisitorSessions.pageLoadTimeMs greaterEq it) }
        maxLoadTime?.let { cond = cond and (VisitorSessions.pageLoadTimeMs lessEq it) }
        return cond
    }

    private fun resolveSortColumn(sort: String): Column<*> = when (sort) {
        "last_activity_at" -> VisitorSessions.lastActivityAt
        "page_load_time_ms" -> VisitorSessions.pageLoadTimeMs
        "browser_name" -> VisitorSessions.browserName
        "os_name" -> VisitorSessions.osName
        "ip_address" -> VisitorSessions.ipAddress
        else -> VisitorSessions.createdAt
    }

    private fun ResultRow.toCompact() = VisitorSessionCompact(
        id = this[VisitorSessions.id],
        visitorId = this[VisitorSessions.visitorId],
        sessionId = this[VisitorSessions.sessionId],
        ipAddress = this[VisitorSessions.ipAddress],
        browserName = this[VisitorSessions.browserName],
        browserVersion = this[VisitorSessions.browserVersion],
        osName = this[VisitorSessions.osName],
        deviceType = this[VisitorSessions.deviceType],
        language = this[VisitorSessions.language],
        timezone = this[VisitorSessions.timezone],
        referrer = this[VisitorSessions.referrer],
        pageLoadTimeMs = this[VisitorSessions.pageLoadTimeMs],
        createdAt = this[VisitorSessions.createdAt].fmt(),
        lastActivityAt = this[VisitorSessions.lastActivityAt].fmt()
    )

    private fun ResultRow.toComplete() = VisitorSessionComplete(
        id = this[VisitorSessions.id],
        visitorId = this[VisitorSessions.visitorId],
        sessionId = this[VisitorSessions.sessionId],
        ipAddress = this[VisitorSessions.ipAddress],
        userAgent = this[VisitorSessions.userAgent],
        browserName = this[VisitorSessions.browserName],
        browserVersion = this[VisitorSessions.browserVersion],
        osName = this[VisitorSessions.osName],
        osVersion = this[VisitorSessions.osVersion],
        deviceType = this[VisitorSessions.deviceType],
        screenWidth = this[VisitorSessions.screenWidth]?.toInt(),
        screenHeight = this[VisitorSessions.screenHeight]?.toInt(),
        viewportWidth = this[VisitorSessions.viewportWidth]?.toInt(),
        viewportHeight = this[VisitorSessions.viewportHeight]?.toInt(),
        language = this[VisitorSessions.language],
        languages = this[VisitorSessions.languages],
        timezone = this[VisitorSessions.timezone],
        platform = this[VisitorSessions.platform],
        networkInfo = this[VisitorSessions.networkInfo],
        deviceMemory = this[VisitorSessions.deviceMemory]?.toInt(),
        hardwareConcurrency = this[VisitorSessions.hardwareConcurrency]?.toInt(),
        colorDepth = this[VisitorSessions.colorDepth]?.toInt(),
        pixelRatio = this[VisitorSessions.pixelRatio]?.toDouble(),
        touchPoints = this[VisitorSessions.touchPoints]?.toInt(),
        cookieEnabled = this[VisitorSessions.cookieEnabled],
        doNotTrack = this[VisitorSessions.doNotTrack],
        webglRenderer = this[VisitorSessions.webglRenderer],
        webglVendor = this[VisitorSessions.webglVendor],
        canvasFingerprint = this[VisitorSessions.canvasFingerprint],
        referrer = this[VisitorSessions.referrer],
        pageLoadTimeMs = this[VisitorSessions.pageLoadTimeMs],
        createdAt = this[VisitorSessions.createdAt].fmt(),
        lastActivityAt = this[VisitorSessions.lastActivityAt].fmt()
    )

    private fun ResultRow.toPageViewDTO() = PageViewDTO(
        id = this[PageViews.id],
        sessionId = this[PageViews.sessionId],
        visitorId = this[PageViews.visitorId],
        path = this[PageViews.path],
        referrerPath = this[PageViews.referrerPath],
        durationMs = this[PageViews.durationMs],
        createdAt = this[PageViews.createdAt].fmt()
    )
}
