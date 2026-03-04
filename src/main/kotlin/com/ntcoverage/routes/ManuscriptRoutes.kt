package com.ntcoverage.routes

import com.ntcoverage.ErrorResponse
import com.ntcoverage.service.ManuscriptService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.manuscriptRoutes(manuscriptService: ManuscriptService) {
    get("/manuscripts") {
        val type = call.request.queryParameters["type"]?.takeIf { it in listOf("papyrus", "uncial") }
        val century = call.request.queryParameters["century"]?.toIntOrNull()?.takeIf { it in 1..10 }
        val yearMin = call.request.queryParameters["yearMin"]?.toIntOrNull()?.takeIf { it in 1..2000 }
        val yearMax = call.request.queryParameters["yearMax"]?.toIntOrNull()?.takeIf { it in 1..2000 }
        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50

        val result = manuscriptService.listManuscripts(type, century, yearMin, yearMax, page, limit)
        call.respond(result)
    }

    get("/manuscripts/{gaId}") {
        val gaId = call.parameters["gaId"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("GA ID required"))
        val result = manuscriptService.getManuscriptDetail(gaId)
            ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Manuscript not found: $gaId"))
        call.respond(result)
    }
}
