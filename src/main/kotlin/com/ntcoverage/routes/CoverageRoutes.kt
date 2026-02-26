package com.ntcoverage.routes

import com.ntcoverage.ErrorResponse
import com.ntcoverage.service.CoverageService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun io.ktor.server.application.ApplicationCall.parseTypes(): List<String>? {
    return request.queryParameters["type"]
        ?.split(",")
        ?.map { it.trim().lowercase() }
        ?.filter { it in setOf("papyrus", "uncial") }
        ?.ifEmpty { null }
}

fun Route.coverageRoutes(coverageService: CoverageService) {

    route("/coverage") {
        get {
            val types = call.parseTypes()
            val century = call.request.queryParameters["century"]?.toIntOrNull()
            if (century != null) {
                val result = coverageService.getCoverageByCentury(century, types)
                call.respond(result)
            } else {
                val result = coverageService.getFullCoverage(types)
                call.respond(result)
            }
        }

        get("/gospels/{century}") {
            val century = call.parameters["century"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid century number required (1-10)"))
            val types = call.parseTypes()
            val result = coverageService.getGospelCoverage(century, types)
            call.respond(result)
        }

        get("/{book}/chapters/{century}") {
            val book = call.parameters["book"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Book parameter required"))
            val century = call.parameters["century"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid century number required (1-10)"))
            val types = call.parseTypes()
            val result = coverageService.getChapterCoverage(book, century, types)
            call.respond(result)
        }

        get("/{book}") {
            val book = call.parameters["book"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Book parameter required"))
            val types = call.parseTypes()
            try {
                val result = coverageService.getCoverageByBook(book, types)
                call.respond(result)
            } catch (e: NoSuchElementException) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse(e.message ?: "Not found"))
            }
        }
    }

    get("/century/{number}") {
        val century = call.parameters["number"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid century number required (1-10)"))
        val types = call.parseTypes()
        try {
            val result = coverageService.getCoverageByCentury(century, types)
            call.respond(result)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
        }
    }

    route("/timeline") {
        get {
            val book = call.request.queryParameters["book"]
            val types = call.parseTypes()
            val result = coverageService.getTimeline(book, types)
            call.respond(result)
        }

        get("/full") {
            val types = call.parseTypes()
            val result = coverageService.getTimeline(null, types)
            call.respond(result)
        }
    }

    get("/missing/{book}/{century}") {
        val book = call.parameters["book"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Book parameter required"))
        val century = call.parameters["century"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid century number required (1-10)"))
        val types = call.parseTypes()
        val result = coverageService.getMissingVerses(book, century, types)
        call.respond(result)
    }
}
