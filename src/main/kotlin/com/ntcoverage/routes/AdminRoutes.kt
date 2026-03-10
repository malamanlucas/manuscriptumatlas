package com.ntcoverage.routes

import com.ntcoverage.config.IngestionConfig
import com.ntcoverage.ErrorResponse
import com.ntcoverage.model.RunPhasesRequest
import com.ntcoverage.repository.IngestionMetadataRepository
import com.ntcoverage.service.CouncilIngestionService
import com.ntcoverage.service.CouncilPhaseTracker
import com.ntcoverage.service.CouncilService
import com.ntcoverage.service.DatingEnrichmentService
import com.ntcoverage.service.IngestionOrchestrator
import com.ntcoverage.service.PatristicIngestionService
import io.ktor.http.*
import io.ktor.server.request.*
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
    datingEnrichmentService: DatingEnrichmentService,
    councilIngestionService: CouncilIngestionService,
    councilService: CouncilService,
    phaseTracker: CouncilPhaseTracker,
    patristicIngestionService: PatristicIngestionService
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

    post("/admin/patristic/seed") {
        if (!IngestionConfig.enableIngestion) {
            call.respond(HttpStatusCode.Forbidden, MessageResponse("Ingestion is disabled via ENABLE_INGESTION=false"))
            return@post
        }
        ingestionScope.launch {
            patristicIngestionService.seedOnly()
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse("Patristic seed started"))
    }

    post("/admin/patristic/translate") {
        if (!IngestionConfig.enableIngestion) {
            call.respond(HttpStatusCode.Forbidden, MessageResponse("Ingestion is disabled via ENABLE_INGESTION=false"))
            return@post
        }
        val force = call.request.queryParameters["force"]?.toBooleanStrictOrNull() ?: false
        ingestionScope.launch {
            patristicIngestionService.translateOnly(force)
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse(
            if (force) "Patristic translation started (force re-translate)" else "Patristic translation started"
        ))
    }

    get("/admin/councils/audit") {
        val maxYear = call.request.queryParameters["maxYear"]?.toIntOrNull()
        val onlyMissing = call.request.queryParameters["onlyMissing"]?.toBooleanStrictOrNull() ?: false
        call.respond(councilService.auditCouncils(maxYear = maxYear, onlyMissing = onlyMissing))
    }

    get("/admin/councils/ingestion/phases") {
        call.respond(phaseTracker.getAllPhases())
    }

    post("/admin/councils/ingestion/run/{phase}") {
        val phase = call.parameters["phase"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing phase"))
        if (phase !in CouncilIngestionService.ALL_PHASES) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid phase: $phase"))
        }
        if (phaseTracker.isAnyRunning()) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("A council ingestion phase is already running"))
        }

        ingestionScope.launch {
            councilIngestionService.runPhases(listOf(phase))
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse("Phase started: $phase"))
    }

    post("/admin/councils/ingestion/run") {
        val request = call.receive<RunPhasesRequest>()
        val phases = request.phases.distinct()
        if (phases.isEmpty()) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("At least one phase is required"))
        }
        val invalid = phases.filter { it !in CouncilIngestionService.ALL_PHASES }
        if (invalid.isNotEmpty()) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid phases: $invalid"))
        }
        if (phaseTracker.isAnyRunning()) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("A council ingestion phase is already running"))
        }

        ingestionScope.launch {
            councilIngestionService.runPhases(phases)
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse("Phases started: ${phases.joinToString(", ")}"))
    }

    post("/admin/councils/ingestion/run-all") {
        if (phaseTracker.isAnyRunning()) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("A council ingestion phase is already running"))
        }
        ingestionScope.launch {
            councilIngestionService.fullIngestion()
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse("Full council ingestion started"))
    }

    get("/admin/councils/ingestion/cache") {
        call.respond(councilIngestionService.getCacheStats())
    }
}
