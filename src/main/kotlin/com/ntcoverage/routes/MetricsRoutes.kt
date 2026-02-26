package com.ntcoverage.routes

import com.ntcoverage.ErrorResponse
import com.ntcoverage.service.MetricsService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.metricsRoutes(metricsService: MetricsService) {
    get("/metrics/nt") {
        val result = metricsService.getNtMetrics()
        call.response.headers.append(HttpHeaders.CacheControl, "max-age=300")
        call.respond(result)
    }

    get("/metrics/{book}") {
        val book = call.parameters["book"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Book name required"))
        if (com.ntcoverage.seed.CanonicalVerses.findBook(book) == null) {
            return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Book not found: $book"))
        }
        val result = metricsService.getBookMetrics(book)
        call.response.headers.append(HttpHeaders.CacheControl, "max-age=300")
        call.respond(result)
    }
}
