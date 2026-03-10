package com.ntcoverage.routes

import com.ntcoverage.ErrorResponse
import com.ntcoverage.config.LocaleConfig
import com.ntcoverage.model.TextualTopic
import com.ntcoverage.service.ChurchFatherService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.churchFatherRoutes(service: ChurchFatherService) {

    get("/fathers/statements") {
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val topic = call.request.queryParameters["topic"]?.uppercase()?.takeIf { t ->
            TextualTopic.entries.any { it.name == t }
        }
        val century = call.request.queryParameters["century"]?.toIntOrNull()?.takeIf { it in 1..10 }
        val yearMin = call.request.queryParameters["yearMin"]?.toIntOrNull()?.takeIf { it in 1..2000 }
        val yearMax = call.request.queryParameters["yearMax"]?.toIntOrNull()?.takeIf { it in 1..2000 }
        val tradition = call.request.queryParameters["tradition"]?.takeIf {
            it in listOf("greek", "latin", "syriac", "coptic")
        }
        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

        val result = service.listStatements(topic, century, tradition, yearMin, yearMax, page, limit, locale)
        call.respond(result)
    }

    get("/fathers/statements/search") {
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val q = call.request.queryParameters["q"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Query parameter 'q' is required"))
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

        val results = service.searchStatements(q, limit, locale)
        call.respond(results)
    }

    get("/fathers/statements/topics/summary") {
        val summary = service.getTopicsSummary()
        call.respond(summary)
    }

    get("/fathers") {
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val century = call.request.queryParameters["century"]?.toIntOrNull()?.takeIf { it in 1..10 }
        val yearMin = call.request.queryParameters["yearMin"]?.toIntOrNull()?.takeIf { it in 1..2000 }
        val yearMax = call.request.queryParameters["yearMax"]?.toIntOrNull()?.takeIf { it in 1..2000 }
        val yearMinFrom = call.request.queryParameters["yearMinFrom"]?.toIntOrNull()?.takeIf { it in 0..2000 }
        val yearMinTo = call.request.queryParameters["yearMinTo"]?.toIntOrNull()?.takeIf { it in 0..2000 }
        val tradition = call.request.queryParameters["tradition"]?.takeIf {
            it in listOf("greek", "latin", "syriac", "coptic")
        }
        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50

        val result = service.listFathers(century, tradition, yearMin, yearMax, yearMinFrom, yearMinTo, page, limit, locale)
        call.respond(result)
    }

    get("/fathers/search") {
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val q = call.request.queryParameters["q"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Query parameter 'q' is required"))
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

        val results = service.searchFathers(q, limit, locale)
        call.respond(results)
    }

    get("/fathers/{id}/statements") {
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid numeric ID required"))
        val statements = service.getStatementsByFather(id, locale)
        call.respond(statements)
    }

    get("/fathers/{id}") {
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid numeric ID required"))
        val detail = service.getFatherDetail(id, locale)
            ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Church father not found: $id"))
        call.respond(detail)
    }
}
