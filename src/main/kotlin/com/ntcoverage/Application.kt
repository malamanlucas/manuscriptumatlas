package com.ntcoverage

import com.ntcoverage.config.DatabaseConfig
import com.ntcoverage.config.FlywayConfig
import com.ntcoverage.repository.ChapterCoverageRepository
import com.ntcoverage.repository.CoverageRepository
import com.ntcoverage.repository.IngestionMetadataRepository
import com.ntcoverage.repository.ManuscriptRepository
import com.ntcoverage.repository.MetricsRepository
import com.ntcoverage.repository.StatsRepository
import com.ntcoverage.repository.VerseRepository
import com.ntcoverage.repository.ChurchFatherRepository
import com.ntcoverage.repository.FatherTextualStatementRepository
import com.ntcoverage.routes.adminRoutes
import com.ntcoverage.routes.churchFatherRoutes
import com.ntcoverage.routes.coverageRoutes
import com.ntcoverage.routes.manuscriptRoutes
import com.ntcoverage.routes.metricsRoutes
import com.ntcoverage.routes.statsRoutes
import com.ntcoverage.routes.verseRoutes
import com.ntcoverage.scraper.NtvmrListClient
import com.ntcoverage.service.BiographySummarizationService
import com.ntcoverage.service.ChurchFatherService
import com.ntcoverage.service.CoverageService
import com.ntcoverage.service.IngestionOrchestrator
import com.ntcoverage.service.IngestionService
import com.ntcoverage.service.ManuscriptService
import com.ntcoverage.service.MetricsService
import com.ntcoverage.service.PatristicIngestionService
import com.ntcoverage.service.StatsService
import com.ntcoverage.service.VerseExpander
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Serializable
data class ServiceInfo(
    val service: String,
    val version: String,
    val endpoints: List<String>
)

@Serializable
data class ErrorResponse(val error: String)

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val log = LoggerFactory.getLogger("Application")

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
    }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        })
    }

    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Bad request"))
        }
        exception<NoSuchElementException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Not found"))
        }
        exception<Throwable> { call, cause ->
            log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }

    DatabaseConfig.init(environment)
    FlywayConfig.migrate(DatabaseConfig.getDataSource())

    val verseRepository = VerseRepository()
    val manuscriptRepository = ManuscriptRepository()
    val coverageRepository = CoverageRepository()
    val chapterCoverageRepository = ChapterCoverageRepository()
    val statsRepository = StatsRepository()
    val verseExpander = VerseExpander()
    val ingestionMetadataRepository = IngestionMetadataRepository()

    val ntvmrListClient = NtvmrListClient()
    val ingestionService = IngestionService(verseRepository, manuscriptRepository, coverageRepository, verseExpander, ntvmrListClient)
    val coverageService = CoverageService(coverageRepository, chapterCoverageRepository)
    val statsService = StatsService(statsRepository, coverageRepository)
    val manuscriptService = ManuscriptService(manuscriptRepository)
    val metricsRepository = MetricsRepository(coverageRepository)
    val metricsService = MetricsService(metricsRepository)
    val churchFatherRepository = ChurchFatherRepository()
    val statementRepository = FatherTextualStatementRepository()
    val biographySummarizationService = BiographySummarizationService()
    val patristicIngestionService = PatristicIngestionService(churchFatherRepository, statementRepository, biographySummarizationService)
    val churchFatherService = ChurchFatherService(churchFatherRepository, statementRepository)
    val orchestrator = IngestionOrchestrator(ingestionService, patristicIngestionService, ingestionMetadataRepository, statsRepository)

    ingestionService.seedBooksAndVerses()
    log.info("Canonical seed complete. API is ready.")

    val ingestionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        get("/") {
            call.respond(ServiceInfo(
                service = "NT Manuscript Coverage API",
                version = "2.0.0",
                endpoints = listOf(
                    "GET /coverage - Full coverage report (all centuries I-X)",
                    "GET /coverage?type=papyrus&century=4 - Coverage filtered by type and century",
                    "GET /coverage/{book} - Coverage for a specific book",
                    "GET /coverage/{book}/chapters/{century} - Chapter-level coverage",
                    "GET /coverage/gospels/{century} - Gospel coverage aggregate",
                    "GET /century/{number} - Cumulative coverage up to century N",
                    "GET /timeline?book=John&type=papyrus - Evolutionary timeline",
                    "GET /timeline/full - Full NT timeline",
                    "GET /missing/{book}/{century} - Missing verses for a book up to century",
                    "GET /stats/overview - Global statistics overview",
                    "GET /stats/manuscripts-count - Manuscript count by type",
                    "GET /manuscripts?type=papyrus&century=3 - Manuscript explorer",
                    "GET /manuscripts/{gaId} - Manuscript detail",
                    "GET /verses/manuscripts?book=&chapter=&verse= - Manuscripts that contain a verse",
                    "GET /metrics/nt - NT-wide academic metrics",
                    "GET /metrics/{book} - Book-level metrics",
                    "GET /admin/ingestion/status - Ingestion status",
                    "POST /admin/ingestion/run - Trigger manual ingestion",
                    "GET /fathers - Church fathers list (filterable by century, tradition)",
                    "GET /fathers/search?q= - Search church fathers by name",
                    "GET /fathers/{id} - Church father detail",
                    "GET /fathers/statements - Textual statements (filterable by topic, century, tradition)",
                    "GET /fathers/statements/search?q= - Search statements by keyword",
                    "GET /fathers/statements/topics/summary - Statement count by topic",
                    "GET /fathers/{id}/statements - Statements by a specific father",
                    "GET /swagger - Swagger UI documentation"
                )
            ))
        }
        coverageRoutes(coverageService)
        statsRoutes(statsService)
        manuscriptRoutes(manuscriptService)
        metricsRoutes(metricsService)
        verseRoutes(verseRepository)
        churchFatherRoutes(churchFatherService)
        adminRoutes(orchestrator, ingestionMetadataRepository, ingestionScope)
    }

    monitor.subscribe(ApplicationStarted) {
        ingestionScope.launch {
            orchestrator.launchIfEnabled()
        }
    }

    monitor.subscribe(ApplicationStopped) {
        ingestionScope.cancel()
        DatabaseConfig.close()
    }
}
