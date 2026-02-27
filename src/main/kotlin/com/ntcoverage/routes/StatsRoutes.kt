package com.ntcoverage.routes

import com.ntcoverage.service.StatsService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.statsRoutes(statsService: StatsService) {
    get("/stats/overview") {
        val result = statsService.getOverview()
        call.response.headers.append(HttpHeaders.CacheControl, "max-age=300")
        call.respond(result)
    }

    get("/stats/manuscripts-count") {
        val result = statsService.getManuscriptsCount()
        call.response.headers.append(HttpHeaders.CacheControl, "max-age=300")
        call.respond(result)
    }
}
