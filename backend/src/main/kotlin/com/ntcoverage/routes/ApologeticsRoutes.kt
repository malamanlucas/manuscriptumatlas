package com.ntcoverage.routes

import com.ntcoverage.ErrorResponse
import com.ntcoverage.config.LocaleConfig
import com.ntcoverage.model.CreateApologeticResponseRequest
import com.ntcoverage.model.CreateApologeticTopicRequest
import com.ntcoverage.model.UpdateApologeticResponseRequest
import com.ntcoverage.model.UpdateApologeticTopicRequest
import com.ntcoverage.service.ApologeticsService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("ApologeticsRoutes")

fun Route.apologeticsRoutes(service: ApologeticsService) {
    get("/apologetics") {
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val status = call.request.queryParameters["status"]?.uppercase()
        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
        log.debug("GET /apologetics: locale=$locale, status=$status, page=$page, limit=$limit")
        call.respond(service.listTopics(page, limit, locale, status))
    }

    get("/apologetics/search") {
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        val query = call.request.queryParameters["q"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Query parameter 'q' is required"))
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
        log.debug("GET /apologetics/search: q='$query', limit=$limit, locale=$locale")
        call.respond(service.searchTopics(query, limit, locale))
    }

    get("/apologetics/{slug}") {
        val slug = call.parameters["slug"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing slug"))
        val locale = LocaleConfig.sanitize(call.request.queryParameters["locale"])
        log.debug("GET /apologetics/$slug: locale=$locale")
        val detail = service.getTopicDetail(slug, locale)
            ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Topic not found: $slug"))
        call.respond(detail)
    }
}

fun Route.apologeticsAdminRoutes(service: ApologeticsService) {
    post("/apologetics") {
        val email = call.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()
        val req = call.receive<CreateApologeticTopicRequest>()
        if (req.prompt.isBlank()) {
            log.warn("POST /apologetics: rejected empty prompt, by=$email")
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Prompt cannot be empty"))
        }
        log.info("POST /apologetics: creating topic, promptLength=${req.prompt.length}, by=$email")
        val startMs = System.currentTimeMillis()
        val created = service.createTopic(req.prompt, email)
        val durationMs = System.currentTimeMillis() - startMs
        log.info("POST /apologetics: topic created, id=${created.id}, slug=${created.slug}, durationMs=$durationMs, by=$email")
        call.respond(HttpStatusCode.Created, created)
    }

    patch("/apologetics/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid numeric ID required"))
        val email = call.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()
        val req = call.receive<UpdateApologeticTopicRequest>()
        log.info("PATCH /apologetics/$id: updating topic, by=$email")
        val updated = service.updateTopic(id, req)
        if (!updated) {
            log.warn("PATCH /apologetics/$id: topic not found")
            return@patch call.respond(HttpStatusCode.NotFound, ErrorResponse("Topic not found: $id"))
        }
        log.info("PATCH /apologetics/$id: topic updated, by=$email")
        call.respond(mapOf("ok" to true))
    }

    post("/apologetics/{id}/responses") {
        val topicId = call.parameters["id"]?.toIntOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid numeric ID required"))
        val email = call.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()
        val req = call.receive<CreateApologeticResponseRequest>()
        if (req.body.isBlank()) {
            log.warn("POST /apologetics/$topicId/responses: rejected empty body, by=$email")
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Response body cannot be empty"))
        }
        log.info("POST /apologetics/$topicId/responses: creating response, bodyLength=${req.body.length}, useAi=${req.useAi}, by=$email")
        val startMs = System.currentTimeMillis()
        val created = service.createResponse(topicId, req.body, req.useAi, email)
        val durationMs = System.currentTimeMillis() - startMs
        log.info("POST /apologetics/$topicId/responses: response created, id=${created.id}, durationMs=$durationMs, by=$email")
        call.respond(HttpStatusCode.Created, created)
    }

    patch("/apologetics/responses/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid numeric ID required"))
        val email = call.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()
        val req = call.receive<UpdateApologeticResponseRequest>()
        log.info("PATCH /apologetics/responses/$id: updating response, useAi=${req.useAi}, by=$email")
        val startMs = System.currentTimeMillis()
        val updated = service.updateResponse(id, req)
        val durationMs = System.currentTimeMillis() - startMs
        if (!updated) {
            log.warn("PATCH /apologetics/responses/$id: response not found")
            return@patch call.respond(HttpStatusCode.NotFound, ErrorResponse("Response not found: $id"))
        }
        log.info("PATCH /apologetics/responses/$id: response updated, durationMs=$durationMs, by=$email")
        call.respond(mapOf("ok" to true))
    }

    delete("/apologetics/responses/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Valid numeric ID required"))
        val email = call.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()
        log.info("DELETE /apologetics/responses/$id: by=$email")
        val deleted = service.deleteResponse(id)
        if (!deleted) {
            log.warn("DELETE /apologetics/responses/$id: response not found")
            return@delete call.respond(HttpStatusCode.NotFound, ErrorResponse("Response not found: $id"))
        }
        log.info("DELETE /apologetics/responses/$id: deleted successfully, by=$email")
        call.respond(mapOf("ok" to true))
    }
}
