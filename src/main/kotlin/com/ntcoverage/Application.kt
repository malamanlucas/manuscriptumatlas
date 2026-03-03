package com.ntcoverage

import com.auth0.jwk.JwkProviderBuilder
import com.ntcoverage.config.DatabaseConfig
import com.ntcoverage.config.FlywayConfig
import com.ntcoverage.config.SimpleRateLimiter
import com.ntcoverage.repository.*
import com.ntcoverage.routes.*
import com.ntcoverage.scraper.NtvmrListClient
import com.ntcoverage.seed.UserSeedData
import com.ntcoverage.service.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
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
import java.net.URL
import java.util.concurrent.TimeUnit

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
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
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
        exception<IllegalStateException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message ?: "Conflict"))
        }
        exception<Throwable> { call, cause ->
            log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
        }
    }

    DatabaseConfig.init(environment)
    FlywayConfig.migrate(DatabaseConfig.getDataSource())

    val userRepository = UserRepository()
    val userService = UserService(userRepository)

    val googleClientId = System.getenv("GOOGLE_CLIENT_ID") ?: ""
    if (googleClientId.isBlank()) {
        log.warn("GOOGLE_CLIENT_ID is not set — JWT authentication will reject all requests.")
    }

    val jwkProvider = JwkProviderBuilder(URL("https://www.googleapis.com/oauth2/v3/certs"))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt("google-jwt") {
            verifier(jwkProvider) {
                withClaimPresence("email")
                withAudience(googleClientId)
                acceptLeeway(5)
            }
            validate { credential ->
                val issuer = credential.payload.issuer
                if (issuer != "https://accounts.google.com" && issuer != "accounts.google.com") {
                    log.warn("AUTH: rejected_invalid_issuer | issuer=$issuer")
                    return@validate null
                }

                val emailVerified = credential.payload.getClaim("email_verified")?.asBoolean() ?: false
                if (!emailVerified) {
                    log.warn("AUTH: rejected_email_not_verified | email=${credential.payload.getClaim("email")?.asString()}")
                    return@validate null
                }

                val email = credential.payload.getClaim("email").asString()
                val user = userRepository.findByEmail(email)

                if (user == null) {
                    log.warn("AUTH: rejected_unknown_email | email=$email")
                    return@validate null
                }

                log.info("AUTH: login_success | email=$email | role=${user.role}")
                userRepository.updateLastLoginAndPicture(
                    email,
                    credential.payload.getClaim("picture")?.asString()
                )

                JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Authentication required"))
            }
        }
    }

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

    val visitorRepository = VisitorRepository()
    val visitorService = VisitorService(visitorRepository)
    val retentionScheduler = RetentionScheduler(DatabaseConfig.getDataSource())
    val sessionRateLimiter = SimpleRateLimiter(windowMs = 5_000, maxRequests = 1)
    val heartbeatRateLimiter = SimpleRateLimiter(windowMs = 15_000, maxRequests = 1)
    val pageviewRateLimiter = SimpleRateLimiter(windowMs = 10_000, maxRequests = 10)

    ingestionService.seedBooksAndVerses()
    UserSeedData.seedIfEmpty()
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
        visitorTrackingRoutes(visitorService, sessionRateLimiter, heartbeatRateLimiter, pageviewRateLimiter)

        authenticate("google-jwt") {
            authRoutes(userRepository, userService)
            visitorAnalyticsRoutes(visitorService)
        }
    }

    monitor.subscribe(ApplicationStarted) {
        ingestionScope.launch {
            orchestrator.launchIfEnabled()
        }
        retentionScheduler.start(ingestionScope)
    }

    monitor.subscribe(ApplicationStopped) {
        ingestionScope.cancel()
        DatabaseConfig.close()
    }
}
