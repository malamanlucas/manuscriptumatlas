package com.ntcoverage.routes

import com.ntcoverage.ErrorResponse
import com.ntcoverage.config.LocaleConfig
import com.ntcoverage.service.CouncilService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.councilRoutes(service: CouncilService) {
    get("/councils") {
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val century = call.request.queryParameters["century"]?.toIntOrNull()
        val type = call.request.queryParameters["type"]?.uppercase()
        val yearMin = call.request.queryParameters["yearMin"]?.toIntOrNull()
        val yearMax = call.request.queryParameters["yearMax"]?.toIntOrNull()
        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

        val response = service.listCouncils(
            century = century,
            type = type,
            yearMin = yearMin,
            yearMax = yearMax,
            page = page,
            limit = limit,
            locale = locale
        )
        call.respond(response)
    }

    get("/councils/search") {
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val query = call.request.queryParameters["q"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Query parameter 'q' is required"))
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
        call.respond(service.searchCouncils(query, limit, locale))
    }

    get("/councils/types/summary") {
        call.respond(service.getCouncilTypeSummary())
    }

    get("/councils/map") {
        call.respond(service.getCouncilMapPoints())
    }

    get("/councils/{slug}") {
        val slug = call.parameters["slug"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing council slug"))
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val detail = service.getCouncilDetail(slug, locale)
            ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Council not found: $slug"))
        call.respond(detail)
    }

    get("/councils/{slug}/fathers") {
        val slug = call.parameters["slug"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing council slug"))
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        call.respond(service.getCouncilFathers(slug, locale))
    }

    get("/councils/{slug}/canons") {
        val slug = call.parameters["slug"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing council slug"))
        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 200) ?: 50
        call.respond(service.getCouncilCanons(slug, page, limit))
    }

    get("/councils/{slug}/heresies") {
        val slug = call.parameters["slug"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing council slug"))
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        call.respond(service.getCouncilHeresies(slug, locale))
    }

    get("/councils/{slug}/sources") {
        val slug = call.parameters["slug"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing council slug"))
        call.respond(service.getCouncilSources(slug))
    }

    get("/heresies") {
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
        call.respond(service.listHeresies(page, limit, locale))
    }

    get("/heresies/{slug}") {
        val slug = call.parameters["slug"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing heresy slug"))
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val detail = service.getHeresyDetail(slug, locale)
            ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Heresy not found: $slug"))
        call.respond(detail)
    }

    get("/heresies/{slug}/councils") {
        val slug = call.parameters["slug"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing heresy slug"))
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        call.respond(service.getHeresyCouncils(slug, locale))
    }

    get("/sources") {
        call.respond(service.listSources())
    }

    get("/fathers/{id}/councils") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid numeric ID required"))
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        call.respond(service.getCouncilsByFather(id, locale))
    }
}
