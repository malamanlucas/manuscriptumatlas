package com.ntcoverage.routes

import com.ntcoverage.config.IngestionConfig
import com.ntcoverage.repository.IngestionMetadataRepository
import com.ntcoverage.service.DatingEnrichmentService
import com.ntcoverage.service.IngestionOrchestrator
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(val message: String)

fun Route.adminRoutes(
    orchestrator: IngestionOrchestrator,
    metadataRepository: IngestionMetadataRepository,
    ingestionScope: CoroutineScope,
    datingEnrichmentService: DatingEnrichmentService
) {
    get("/admin/ingestion/status") {
        val status = metadataRepository.getStatus()
        val enriched = status.copy(
            isRunning = orchestrator.isCurrentlyRunning(),
            enableIngestion = IngestionConfig.enableIngestion
        )
        call.respond(enriched)
    }

    post("/admin/ingestion/run") {
        if (!IngestionConfig.enableIngestion) {
            call.respond(HttpStatusCode.Forbidden, MessageResponse("Ingestion is disabled via ENABLE_INGESTION=false"))
            return@post
        }

        try {
            orchestrator.triggerManual(ingestionScope)
            call.respond(HttpStatusCode.Accepted, MessageResponse("Ingestion started"))
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, MessageResponse(e.message ?: "Ingestion already running"))
        }
    }

    post("/admin/ingestion/reset") {
        if (!IngestionConfig.enableIngestion) {
            call.respond(HttpStatusCode.Forbidden, MessageResponse("Ingestion is disabled via ENABLE_INGESTION=false"))
            return@post
        }

        try {
            orchestrator.resetAndReIngest(ingestionScope)
            call.respond(HttpStatusCode.Accepted, MessageResponse("Database reset and re-ingestion started"))
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, MessageResponse(e.message ?: "Ingestion already running"))
        }
    }

    post("/admin/enrich-dating") {
        if (!datingEnrichmentService.isEnabled()) {
            call.respond(HttpStatusCode.Forbidden, MessageResponse(
                "Dating enrichment is disabled. Set ENABLE_DATING_ENRICHMENT=true and provide OPENAI_API_KEY."
            ))
            return@post
        }

        val domain = call.request.queryParameters["domain"] ?: "all"
        if (domain !in listOf("manuscripts", "fathers", "all")) {
            call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid domain. Use: manuscripts, fathers, all"))
            return@post
        }

        val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 10).coerceIn(1, 50)

        ingestionScope.launch {
            datingEnrichmentService.enrichAll(domain, limit)
        }

        call.respond(HttpStatusCode.Accepted, MessageResponse(
            "Dating enrichment started for domain=$domain, limit=$limit"
        ))
    }
}
