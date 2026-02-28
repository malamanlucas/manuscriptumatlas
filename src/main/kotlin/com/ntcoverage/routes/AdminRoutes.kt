package com.ntcoverage.routes

import com.ntcoverage.config.IngestionConfig
import com.ntcoverage.repository.IngestionMetadataRepository
import com.ntcoverage.service.IngestionOrchestrator
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

@Serializable
data class MessageResponse(val message: String)

fun Route.adminRoutes(
    orchestrator: IngestionOrchestrator,
    metadataRepository: IngestionMetadataRepository,
    ingestionScope: CoroutineScope
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
}
