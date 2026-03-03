package com.ntcoverage.routes

import com.ntcoverage.config.SimpleRateLimiter
import com.ntcoverage.model.*
import com.ntcoverage.service.VisitorService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Route.visitorRoutes(
    visitorService: VisitorService,
    sessionRateLimiter: SimpleRateLimiter,
    heartbeatRateLimiter: SimpleRateLimiter,
    pageviewRateLimiter: SimpleRateLimiter
) {
    route("/visitor") {

        // ── Tracking endpoints ──

        post("/session") {
            val ip = (call.request.header("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                ?: call.request.local.remoteHost).take(100)
            if (!sessionRateLimiter.allow(ip)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Rate limited"))
                return@post
            }
            val req = call.receive<VisitorSessionRequest>()
            val id = visitorService.createSession(req, ip)
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }

        post("/pageview") {
            val req = call.receive<PageViewRequest>()
            if (!pageviewRateLimiter.allow(req.sessionId)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Rate limited"))
                return@post
            }
            val id = visitorService.recordPageView(req)
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }

        post("/heartbeat") {
            val req = call.receive<HeartbeatRequest>()
            if (!heartbeatRateLimiter.allow(req.sessionId)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Rate limited"))
                return@post
            }
            visitorService.heartbeat(req.sessionId)
            call.respond(HttpStatusCode.OK, mapOf("ok" to true))
        }

        // ── Analytics endpoints ──

        route("/analytics") {

            get("/overview") {
                val (from, to) = call.parseTimeRange()
                call.respond(visitorService.getOverview(from, to))
            }

            get("/live") {
                call.respond(visitorService.getLiveVisitors())
            }

            get("/filters/values") {
                call.respond(visitorService.getFilterValues())
            }

            // ── Explorer ──

            get("/sessions") {
                val (from, to) = call.parseTimeRange()
                val p = call.parameters
                val view = p["view"] ?: "compact"
                val browser = p["browser"]
                val os = p["os"]
                val deviceType = p["deviceType"]
                val language = p["language"]
                val timezone = p["timezone"]
                val ip = p["ip"]
                val visitorId = p["visitorId"]
                val fingerprint = p["fingerprint"]
                val referrer = p["referrer"]
                val minLoadTime = p["minLoadTime"]?.toIntOrNull()
                val maxLoadTime = p["maxLoadTime"]?.toIntOrNull()
                val sort = p["sort"] ?: "created_at"
                val order = p["order"] ?: "desc"
                val page = p["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val limit = p["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50

                if (view == "complete") {
                    call.respond(visitorService.getSessionsComplete(
                        from, to, browser, os, deviceType, language, timezone, ip,
                        visitorId, fingerprint, referrer, minLoadTime, maxLoadTime,
                        sort, order, page, limit
                    ))
                } else {
                    call.respond(visitorService.getSessionsCompact(
                        from, to, browser, os, deviceType, language, timezone, ip,
                        visitorId, fingerprint, referrer, minLoadTime, maxLoadTime,
                        sort, order, page, limit
                    ))
                }
            }

            get("/sessions/{sessionId}") {
                val sessionId = call.parameters["sessionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val session = visitorService.getSessionDetail(sessionId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Session not found"))
                call.respond(session)
            }

            get("/sessions/{sessionId}/pageviews") {
                val sessionId = call.parameters["sessionId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                call.respond(visitorService.getSessionPageViews(sessionId))
            }

            // ── Timeline ──

            get("/timeline/sessions") {
                val (from, to) = call.parseTimeRange()
                val granularity = call.parameters["granularity"]
                val breakdown = call.parameters["breakdown"]
                call.respond(visitorService.getTimeline("sessions", from, to, granularity, breakdown))
            }

            get("/timeline/pageviews") {
                val (from, to) = call.parseTimeRange()
                val granularity = call.parameters["granularity"]
                val breakdown = call.parameters["breakdown"]
                call.respond(visitorService.getTimeline("pageviews", from, to, granularity, breakdown))
            }

            get("/timeline/heatmap") {
                val (from, to) = call.parseTimeRange()
                call.respond(visitorService.getHeatmap(from, to))
            }

            // ── Visitors ──

            get("/visitors") {
                val (from, to) = call.parseTimeRange()
                val p = call.parameters
                val returning = p["returning"]?.toBooleanStrictOrNull()
                val sort = p["sort"] ?: "last_seen"
                val order = p["order"] ?: "desc"
                val page = p["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val limit = p["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
                call.respond(visitorService.getVisitorsList(from, to, returning, sort, order, page, limit))
            }

            get("/visitors/{visitorId}") {
                val visitorId = call.parameters["visitorId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val profile = visitorService.getVisitorProfile(visitorId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Visitor not found"))
                call.respond(profile)
            }

            get("/visitors/{visitorId}/sessions") {
                val visitorId = call.parameters["visitorId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val page = call.parameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
                call.respond(visitorService.getVisitorSessions(visitorId, page, limit))
            }

            // ── Top / Distribution ──

            get("/top/pages") {
                val (from, to) = call.parseTimeRange()
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 10
                call.respond(visitorService.getTopPages(from, to, limit))
            }

            get("/top/referrers") {
                val (from, to) = call.parseTimeRange()
                val limit = call.parameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 10
                call.respond(visitorService.getTopReferrers(from, to, limit))
            }

            get("/distribution") {
                val (from, to) = call.parseTimeRange()
                val field = call.parameters["field"] ?: "browser"
                call.respond(visitorService.getDistribution(field, from, to))
            }

            // ── Trends ──

            get("/trends") {
                val days = call.parameters["days"]?.toIntOrNull()?.coerceIn(1, 90) ?: 30
                call.respond(visitorService.getTrends(days))
            }
        }
    }
}

private fun ApplicationCall.parseTimeRange(): Pair<Instant, Instant> {
    val qp = request.queryParameters
    val fromParam = qp["from"]
    val toParam = qp["to"]
    val daysParam = qp["days"]?.toIntOrNull()

    val to = if (toParam != null) Instant.parse(toParam) else Instant.now()
    val from = when {
        fromParam != null -> Instant.parse(fromParam)
        daysParam != null -> Instant.now().minus(daysParam.toLong(), ChronoUnit.DAYS)
        else -> Instant.now().minus(7, ChronoUnit.DAYS)
    }
    return Pair(from, to)
}
