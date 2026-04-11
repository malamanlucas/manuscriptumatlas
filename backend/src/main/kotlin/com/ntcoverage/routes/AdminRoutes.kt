package com.ntcoverage.routes

import com.ntcoverage.config.IngestionConfig
import com.ntcoverage.ErrorResponse
import com.ntcoverage.model.QueueCompleteRequest
import com.ntcoverage.model.RunPhasesRequest
import com.ntcoverage.repository.IngestionMetadataRepository
import com.ntcoverage.repository.LlmQueueRepository
import com.ntcoverage.service.CouncilIngestionService
import com.ntcoverage.service.IngestionPhaseTracker
import com.ntcoverage.service.CouncilService
import com.ntcoverage.service.DatingEnrichmentService
import com.ntcoverage.service.IngestionOrchestrator
import com.ntcoverage.service.IngestionService
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
    phaseTracker: IngestionPhaseTracker,
    ingestionService: IngestionService,
    patristicIngestionService: PatristicIngestionService,
    bibleIngestionService: com.ntcoverage.service.BibleIngestionService,
    llmQueueRepository: LlmQueueRepository,
    kafkaProducer: com.ntcoverage.service.KafkaProducerService,
    llmResponseProcessor: com.ntcoverage.service.LlmResponseProcessor
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

    post("/admin/reset/{domain}") {
        val domain = call.parameters["domain"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing domain"))
        if (domain !in listOf("manuscripts", "patristic", "councils", "bible", "bible-layer1", "bible-layer2", "bible-layer3", "bible-layer4")) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid domain: $domain"))
        }
        if (orchestrator.isCurrentlyRunning()) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("Ingestion is running"))
        }

        val deleted = when (domain) {
            "manuscripts" -> orchestrator.resetManuscripts()
            "patristic" -> orchestrator.resetPatristic()
            "councils" -> orchestrator.resetCouncils()
            "bible" -> orchestrator.resetBible()
            "bible-layer1" -> orchestrator.resetBibleLayer1()
            "bible-layer2" -> orchestrator.resetBibleLayer2()
            "bible-layer3" -> orchestrator.resetBibleLayer3()
            "bible-layer4" -> orchestrator.resetBibleLayer4()
            else -> 0
        }
        call.respond(MessageResponse("Cleared $domain: $deleted rows deleted"))
    }

    post("/admin/enrich-dating") {
        if (!datingEnrichmentService.isEnabled()) {
            call.respond(HttpStatusCode.Forbidden, MessageResponse(
                "Dating enrichment is disabled. Set ENABLE_DATING_ENRICHMENT=true and provide ANTHROPIC_API_KEY or OPENAI_API_KEY."
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
            when (domain) {
                "manuscripts" -> datingEnrichmentService.enqueueManuscriptDating(limit)
                "fathers" -> datingEnrichmentService.enqueueFatherDating(limit)
                "all" -> {
                    datingEnrichmentService.enqueueManuscriptDating(limit)
                    datingEnrichmentService.enqueueFatherDating(limit)
                }
            }
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
        val filter = call.request.queryParameters["filter"]?.split(",")?.map { it.trim() }?.toSet()
        ingestionScope.launch {
            patristicIngestionService.withFilter(filter).seedOnly()
        }
        val msg = if (filter != null) "Patristic seed started (filter=${filter.joinToString(",")})" else "Patristic seed started"
        call.respond(HttpStatusCode.Accepted, MessageResponse(msg))
    }

    post("/admin/patristic/translate") {
        if (!IngestionConfig.enableIngestion) {
            call.respond(HttpStatusCode.Forbidden, MessageResponse("Ingestion is disabled via ENABLE_INGESTION=false"))
            return@post
        }
        val force = call.request.queryParameters["force"]?.toBooleanStrictOrNull() ?: false
        val filter = call.request.queryParameters["filter"]?.split(",")?.map { it.trim() }?.toSet()
        ingestionScope.launch {
            patristicIngestionService.withFilter(filter).translateOnly(force)
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
        call.respond(phaseTracker.getPhasesByPrefix("council_").plus(phaseTracker.getPhasesByPrefix("heresy_")))
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

    // Manuscript ingestion phases
    get("/admin/manuscripts/ingestion/phases") {
        call.respond(phaseTracker.getPhasesByPrefix("manuscript_"))
    }

    post("/admin/manuscripts/ingestion/run/{phase}") {
        val phase = call.parameters["phase"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing phase"))
        if (phase !in IngestionService.ALL_PHASES) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid phase: $phase"))
        }
        if (phaseTracker.isAnyRunningByPrefix("manuscript_")) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("A manuscript ingestion phase is already running"))
        }
        ingestionScope.launch {
            ingestionService.runPhase(phase)
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse("Phase started: $phase"))
    }

    post("/admin/manuscripts/ingestion/run-all") {
        if (phaseTracker.isAnyRunningByPrefix("manuscript_")) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("A manuscript ingestion phase is already running"))
        }
        ingestionScope.launch {
            ingestionService.runAllPhases()
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse("Full manuscript ingestion started"))
    }

    // Patristic ingestion phases
    get("/admin/patristic/ingestion/phases") {
        call.respond(phaseTracker.getPhasesByPrefix("patristic_"))
    }

    post("/admin/patristic/ingestion/run/{phase}") {
        val phase = call.parameters["phase"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing phase"))
        if (phase !in PatristicIngestionService.ALL_PHASES) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid phase: $phase"))
        }
        if (phaseTracker.isAnyRunningByPrefix("patristic_")) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("A patristic ingestion phase is already running"))
        }
        val filter = call.request.queryParameters["filter"]?.split(",")?.map { it.trim() }?.toSet()
        ingestionScope.launch {
            patristicIngestionService.withFilter(filter).runPhases(listOf(phase))
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse("Phase started: $phase"))
    }

    post("/admin/patristic/ingestion/run") {
        val request = call.receive<RunPhasesRequest>()
        val phases = request.phases.distinct()
        if (phases.isEmpty()) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("At least one phase is required"))
        }
        val invalid = phases.filter { it !in PatristicIngestionService.ALL_PHASES }
        if (invalid.isNotEmpty()) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid phases: $invalid"))
        }
        if (phaseTracker.isAnyRunningByPrefix("patristic_")) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("A patristic ingestion phase is already running"))
        }
        val filter = call.request.queryParameters["filter"]?.split(",")?.map { it.trim() }?.toSet()
        ingestionScope.launch {
            patristicIngestionService.withFilter(filter).runPhases(phases)
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse("Phases started: ${phases.joinToString(", ")}"))
    }

    post("/admin/patristic/ingestion/run-all") {
        if (phaseTracker.isAnyRunningByPrefix("patristic_")) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("A patristic ingestion phase is already running"))
        }
        val filter = call.request.queryParameters["filter"]?.split(",")?.map { it.trim() }?.toSet()
        ingestionScope.launch {
            patristicIngestionService.withFilter(filter).fullIngestion()
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse(
            if (filter != null) "Patristic ingestion started (filter=${filter.joinToString(",")})" else "Full patristic ingestion started"
        ))
    }

    // Bible ingestion phases
    get("/admin/bible/ingestion/phases") {
        call.respond(phaseTracker.getPhasesByPrefix("bible_"))
    }

    post("/admin/bible/ingestion/run/{phase}") {
        val phase = call.parameters["phase"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing phase"))
        if (phase !in com.ntcoverage.service.BibleIngestionService.ALL_PHASES) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid phase: $phase"))
        }
        if (phaseTracker.isPhaseRunning(phase)) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("Phase already running: $phase"))
        }
        ingestionScope.launch {
            bibleIngestionService.runPhase(phase)
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse("Phase started: $phase"))
    }

    post("/admin/bible/ingestion/run") {
        val request = call.receive<RunPhasesRequest>()
        val phases = request.phases.distinct()
        if (phases.isEmpty()) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("At least one phase is required"))
        }
        val invalid = phases.filter { it !in com.ntcoverage.service.BibleIngestionService.ALL_PHASES }
        if (invalid.isNotEmpty()) {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid phases: $invalid"))
        }
        val alreadyRunning = phases.filter { phaseTracker.isPhaseRunning(it) }
        if (alreadyRunning.isNotEmpty()) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("Phases already running: ${alreadyRunning.joinToString(", ")}"))
        }
        ingestionScope.launch {
            bibleIngestionService.runPhases(phases)
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse("Phases started: ${phases.joinToString(", ")}"))
    }

    post("/admin/bible/glosses/clear") {
        val cleared = bibleIngestionService.clearGlosses()
        val alignmentsDeleted = orchestrator.resetBibleLayer4()
        call.respond(MessageResponse("Cleared $cleared glosses, reset bible_translate_glosses phase, reset layer4 alignments ($alignmentsDeleted rows)"))
    }

    post("/admin/bible/glosses/fix-corrupted") {
        val cleared = bibleIngestionService.fixCorruptedPortugueseGlosses()
        call.respond(MessageResponse("Fixed $cleared corrupted Portuguese glosses (set to NULL for re-translation)"))
    }

    post("/admin/bible/ingestion/run-all") {
        if (phaseTracker.isAnyRunningByPrefix("bible_")) {
            return@post call.respond(HttpStatusCode.Conflict, MessageResponse("A bible ingestion phase is already running"))
        }
        ingestionScope.launch {
            bibleIngestionService.fullIngestion()
        }
        call.respond(HttpStatusCode.Accepted, MessageResponse("Full bible ingestion started"))
    }

    // ── LLM Prompt Queue ────────────────────────────────────

    get("/admin/llm/queue/stats") {
        call.respond(llmQueueRepository.getStats())
    }

    get("/admin/llm/queue/pending") {
        val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 100).coerceIn(1, 500)
        val tier = call.request.queryParameters["tier"]
        val phase = call.request.queryParameters["phase"]
        val items = if (phase != null) llmQueueRepository.getPendingByPhase(phase, limit)
                    else llmQueueRepository.getPending(limit, tier)
        call.respond(items)
    }

    get("/admin/llm/queue/completed/{phase}") {
        val phase = call.parameters["phase"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing phase"))
        call.respond(llmQueueRepository.getCompletedByPhase(phase))
    }

    post("/admin/llm/queue/{id}/complete") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
        val body = call.receive<QueueCompleteRequest>()
        llmQueueRepository.markCompleted(id, body.responseContent, body.modelUsed, body.inputTokens, body.outputTokens)
        call.respond(MessageResponse("Completed"))
    }

    post("/admin/llm/queue/{id}/processing") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
        llmQueueRepository.markProcessing(id)
        call.respond(MessageResponse("Marked as processing"))
    }

    post("/admin/llm/queue/claim") {
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
        val tier = call.request.queryParameters["tier"]
        val phase = call.request.queryParameters["phase"]
        val claimed = llmQueueRepository.claimPending(limit.coerceIn(1, 200), tier, phase)
        call.respond(claimed)
    }

    post("/admin/llm/queue/{id}/fail") {
        val id = call.parameters["id"]?.toIntOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid id"))
        val body = call.receive<MessageResponse>()
        llmQueueRepository.markFailed(id, body.message)
        call.respond(MessageResponse("Marked as failed"))
    }

    post("/admin/llm/queue/batch-complete") {
        val items = call.receive<List<QueueCompleteRequest>>()
        var completed = 0
        for (item in items) {
            if (llmQueueRepository.markCompleted(item.id, item.responseContent, item.modelUsed, item.inputTokens, item.outputTokens)) {
                completed++
            }
        }
        call.respond(MessageResponse("$completed/${items.size} completed"))
    }

    delete("/admin/llm/queue/{phase}") {
        val phase = call.parameters["phase"]
            ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing phase"))
        val deleted = llmQueueRepository.clearByPhase(phase)
        call.respond(MessageResponse("Cleared $deleted items from phase: $phase"))
    }

    post("/admin/llm/queue/notify-ready/{phase}") {
        val phase = call.parameters["phase"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing phase"))
        kafkaProducer.notifyResultsReady(phase)
        call.respond(MessageResponse("Published to Kafka: $phase"))
    }

    post("/admin/llm/queue/retry") {
        val phase = call.request.queryParameters["phase"]
        val retried = llmQueueRepository.retryFailed(phase)
        call.respond(MessageResponse("$retried items reset to pending"))
    }

    post("/admin/llm/queue/unstick") {
        val phase = call.request.queryParameters["phase"]
        val unstuck = llmQueueRepository.unstickProcessing(phase)
        call.respond(MessageResponse("$unstuck processing items reset to pending"))
    }

    post("/admin/llm/queue/apply/{phase}") {
        val phase = call.parameters["phase"]
            ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing phase"))
        val result = llmResponseProcessor.processCompleted(phase)
        call.respond(MessageResponse("Applied ${result.applied} items, ${result.errors} errors for phase: $phase"))
    }

    // ── Queue-based prepare endpoints ──

    post("/admin/llm/prepare/dating") {
        val domain = call.request.queryParameters["domain"] ?: "all"
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 500
        var totalEnqueued = 0
        if (domain == "manuscripts" || domain == "all") {
            totalEnqueued += datingEnrichmentService.enqueueManuscriptDating(limit)
        }
        if (domain == "fathers" || domain == "all") {
            totalEnqueued += datingEnrichmentService.enqueueFatherDating(limit)
        }
        call.respond(MessageResponse("Enqueued $totalEnqueued dating items for domain=$domain"))
    }
}
