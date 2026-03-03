package com.ntcoverage.service

import com.ntcoverage.model.PageViews
import com.ntcoverage.model.VisitorDailyStats
import com.ntcoverage.model.VisitorSessions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.sql.DataSource
import kotlin.time.Duration.Companion.hours

class RetentionScheduler(private val dataSource: DataSource) {

    private val log = LoggerFactory.getLogger(RetentionScheduler::class.java)

    fun start(scope: CoroutineScope) {
        scope.launch {
            delay(60_000)
            while (isActive) {
                try {
                    log.info("RETENTION_JOB starting daily maintenance")
                    ensureFuturePartitions()
                    dropExpiredPartitions(60)
                    cleanOldDailyStats(60)
                    aggregateDailyStats()
                    log.info("RETENTION_JOB completed")
                } catch (e: Exception) {
                    log.error("RETENTION_JOB failed", e)
                }
                delay(24.hours)
            }
        }
    }

    private fun ensureFuturePartitions() {
        transaction {
            exec("SELECT create_monthly_partitions(2)")
            log.info("RETENTION: ensured future partitions exist (+2 months)")
        }
    }

    private fun dropExpiredPartitions(retentionDays: Int) {
        val cutoff = LocalDate.now().minusDays(retentionDays.toLong())
        val cutoffYearMonth = cutoff.format(DateTimeFormatter.ofPattern("yyyy_MM"))

        transaction {
            val partitions = mutableListOf<Pair<String, String>>()
            exec("""
                SELECT child.relname AS partition_name, parent.relname AS parent_name
                FROM pg_inherits
                JOIN pg_class child ON pg_inherits.inhrelid = child.oid
                JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
                WHERE parent.relname IN ('visitor_sessions', 'page_views')
            """.trimIndent()) { rs ->
                while (rs.next()) {
                    partitions.add(Pair(rs.getString("partition_name"), rs.getString("parent_name")))
                }
            }

            for ((partName, _) in partitions) {
                val dateMatch = Regex("""(\d{4}_\d{2})$""").find(partName)
                val partDate = dateMatch?.groupValues?.get(1) ?: continue
                if (partDate < cutoffYearMonth) {
                    exec("DROP TABLE IF EXISTS $partName")
                    log.info("RETENTION: dropped expired partition $partName")
                }
            }
        }
    }

    private fun cleanOldDailyStats(retentionDays: Int) {
        val cutoff = LocalDate.now().minusDays(retentionDays.toLong())
        transaction {
            val deleted = VisitorDailyStats.deleteWhere { VisitorDailyStats.statDate less cutoff }
            if (deleted > 0) {
                log.info("RETENTION: deleted $deleted old daily stats rows (before $cutoff)")
            }
        }
    }

    fun aggregateDailyStats() {
        val yesterday = LocalDate.now().minusDays(1)
        val dayStart = yesterday.atStartOfDay().atOffset(ZoneOffset.UTC)
        val dayEnd = yesterday.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)

        transaction {
            val exists = VisitorDailyStats.selectAll()
                .where { VisitorDailyStats.statDate eq yesterday }
                .count() > 0
            if (exists) return@transaction

            var totalSessions = 0
            var totalPageviews = 0
            var uniqueVisitors = 0
            var topBrowsersJson: String? = null
            var topOsJson: String? = null
            var topDevicesJson: String? = null
            var topPagesJson: String? = null

            exec("""
                SELECT
                    COUNT(*) AS total_sessions,
                    COUNT(DISTINCT visitor_id) AS unique_visitors
                FROM visitor_sessions
                WHERE created_at >= '${dayStart}' AND created_at < '${dayEnd}'
            """.trimIndent()) { rs ->
                if (rs.next()) {
                    totalSessions = rs.getInt("total_sessions")
                    uniqueVisitors = rs.getInt("unique_visitors")
                }
            }

            exec("""
                SELECT COUNT(*) AS cnt FROM page_views
                WHERE created_at >= '${dayStart}' AND created_at < '${dayEnd}'
            """.trimIndent()) { rs ->
                if (rs.next()) totalPageviews = rs.getInt("cnt")
            }

            topBrowsersJson = aggregateTopField("browser_name", dayStart, dayEnd)
            topOsJson = aggregateTopField("os_name", dayStart, dayEnd)
            topDevicesJson = aggregateTopField("device_type", dayStart, dayEnd)
            topPagesJson = aggregateTopPages(dayStart, dayEnd)

            VisitorDailyStats.insert {
                it[statDate] = yesterday
                it[this.totalSessions] = totalSessions
                it[this.totalPageviews] = totalPageviews
                it[this.uniqueVisitors] = uniqueVisitors
                it[avgSessionDurationMs] = null
                it[topBrowsers] = topBrowsersJson
                it[topOs] = topOsJson
                it[topDevices] = topDevicesJson
                it[topPages] = topPagesJson
                it[topCountries] = null
                it[createdAt] = OffsetDateTime.now(ZoneOffset.UTC)
            }
            log.info("RETENTION: aggregated daily stats for $yesterday (sessions=$totalSessions, pv=$totalPageviews, unique=$uniqueVisitors)")
        }
    }

    private fun aggregateTopField(field: String, from: OffsetDateTime, to: OffsetDateTime): String? {
        val items = mutableListOf<String>()
        transaction {
            exec("""
                SELECT $field AS val, COUNT(*) AS cnt
                FROM visitor_sessions
                WHERE created_at >= '$from' AND created_at < '$to' AND $field IS NOT NULL
                GROUP BY $field ORDER BY cnt DESC LIMIT 10
            """.trimIndent()) { rs ->
                while (rs.next()) {
                    val v = rs.getString("val")
                    val c = rs.getInt("cnt")
                    items.add("""{"name":"$v","count":$c}""")
                }
            }
        }
        return if (items.isEmpty()) null else "[${items.joinToString(",")}]"
    }

    private fun aggregateTopPages(from: OffsetDateTime, to: OffsetDateTime): String? {
        val items = mutableListOf<String>()
        transaction {
            exec("""
                SELECT path AS val, COUNT(*) AS cnt
                FROM page_views
                WHERE created_at >= '$from' AND created_at < '$to'
                GROUP BY path ORDER BY cnt DESC LIMIT 10
            """.trimIndent()) { rs ->
                while (rs.next()) {
                    val v = rs.getString("val")
                    val c = rs.getInt("cnt")
                    items.add("""{"name":"$v","count":$c}""")
                }
            }
        }
        return if (items.isEmpty()) null else "[${items.joinToString(",")}]"
    }
}
