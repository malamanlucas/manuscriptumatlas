package com.ntcoverage

import com.ntcoverage.config.DatabaseConfig
import com.ntcoverage.config.FlywayConfig
import com.ntcoverage.repository.ChapterCoverageRepository
import com.ntcoverage.repository.CoverageRepository
import com.ntcoverage.repository.ManuscriptRepository
import com.ntcoverage.repository.VerseRepository
import com.ntcoverage.routes.coverageRoutes
import com.ntcoverage.service.CoverageService
import com.ntcoverage.service.IngestionService
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
    val verseExpander = VerseExpander()

    val ingestionService = IngestionService(verseRepository, manuscriptRepository, coverageRepository, verseExpander)
    val coverageService = CoverageService(coverageRepository, chapterCoverageRepository)

    ingestionService.run()
    log.info("Data ingestion completed. API is ready.")

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        get("/") {
            call.respond(ServiceInfo(
                service = "NT Manuscript Coverage API",
                version = "1.0.0",
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
                    "GET /swagger - Swagger UI documentation"
                )
            ))
        }
        coverageRoutes(coverageService)
    }

    monitor.subscribe(ApplicationStopped) {
        DatabaseConfig.close()
    }
}
