package com.ntcoverage

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.ntcoverage.config.BibleDatabaseConfig
import com.ntcoverage.config.BibleFlywayConfig
import com.ntcoverage.config.DatabaseConfig
import com.ntcoverage.config.FlywayConfig
import com.ntcoverage.config.SourceFileCache
import com.ntcoverage.config.SimpleRateLimiter
import com.ntcoverage.repository.*
import com.ntcoverage.routes.*
import com.ntcoverage.scraper.*
import com.ntcoverage.scraper.NtvmrListClient
import com.ntcoverage.seed.UserSeedData
import com.ntcoverage.service.*
import com.ntcoverage.llm.LlmConfig
import com.ntcoverage.util.JwtUtil
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.metrics.micrometer.*
import io.micrometer.core.instrument.binder.jvm.*
import io.micrometer.core.instrument.binder.system.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import org.slf4j.event.Level
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

    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(MicrometerMetrics) {
        registry = prometheusMeterRegistry
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            JvmThreadMetrics(),
            ClassLoaderMetrics(),
            ProcessorMetrics(),
            UptimeMetrics()
        )
    }

    install(CallLogging) {
        level = Level.INFO
        mdc("method") { it.request.httpMethod.value }
        mdc("path") { it.request.path() }
        mdc("status") { it.response.status()?.value?.toString() }
        filter { call ->
            !call.request.path().startsWith("/prometheus") &&
            !call.request.path().startsWith("/health")
        }
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

    BibleDatabaseConfig.init()
    BibleFlywayConfig.migrate()

    val userRepository = UserRepository()
    val userService = UserService(userRepository)

    val googleClientId = System.getenv("GOOGLE_CLIENT_ID") ?: ""
    if (googleClientId.isBlank()) {
        log.warn("GOOGLE_CLIENT_ID is not set — JWT authentication will reject all requests.")
    }

    val jwtSecret = System.getenv("JWT_SECRET") ?: "manuscriptum-dev-secret-change-in-production"
    JwtUtil.init(jwtSecret)

    val jwkProvider = JwkProviderBuilder(URL("https://www.googleapis.com/oauth2/v3/certs"))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {
        jwt("internal-jwt") {
            verifier(
                JWT.require(JwtUtil.getAlgorithm())
                    .withIssuer(JwtUtil.getIssuer())
                    .acceptLeeway(5)
                    .build()
            )
            validate { credential ->
                val email = credential.payload.getClaim("email")?.asString()
                if (email.isNullOrBlank()) {
                    log.warn("AUTH: rejected_missing_email_claim")
                    return@validate null
                }
                JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Authentication required"))
            }
        }
    }

    val phaseRepository = IngestionPhaseRepository()
    val phaseTracker = IngestionPhaseTracker(phaseRepository)
    phaseTracker.recoverStuckPhases()

    val verseRepository = VerseRepository()
    val manuscriptRepository = ManuscriptRepository()
    val coverageRepository = CoverageRepository()
    val chapterCoverageRepository = ChapterCoverageRepository()
    val statsRepository = StatsRepository()
    val verseExpander = VerseExpander()
    val ingestionMetadataRepository = IngestionMetadataRepository()

    val ntvmrListClient = NtvmrListClient()
    val coverageService = CoverageService(coverageRepository, chapterCoverageRepository)
    val statsService = StatsService(statsRepository, coverageRepository)
    val manuscriptService = ManuscriptService(manuscriptRepository)
    val metricsRepository = MetricsRepository(coverageRepository)
    val metricsService = MetricsService(metricsRepository)
    val churchFatherRepository = ChurchFatherRepository()
    val statementRepository = FatherTextualStatementRepository()
    val llmConfig = LlmConfig()
    val llmQueueRepository = LlmQueueRepository()
    val biographySummarizationService = BiographySummarizationService(llmQueueRepository = llmQueueRepository)
    val datingEnrichmentService = DatingEnrichmentService(manuscriptRepository, churchFatherRepository, llmQueueRepository = llmQueueRepository)
    val ingestionService = IngestionService(verseRepository, manuscriptRepository, coverageRepository, verseExpander, ntvmrListClient, phaseTracker, datingEnrichmentService)
    val patristicIngestionService = PatristicIngestionService(churchFatherRepository, statementRepository, biographySummarizationService, phaseTracker, datingEnrichmentService)
    val churchFatherService = ChurchFatherService(churchFatherRepository, statementRepository)

    val councilRepository = CouncilRepository()
    val sourceRepository = SourceRepository()
    val claimRepository = CouncilSourceClaimRepository()
    val heresyRepository = HeresyRepository()
    val canonRepository = CouncilCanonRepository()
    val hereticParticipantRepository = CouncilHereticParticipantRepository()
    val sourceFileCache = SourceFileCache()
    val academicTextExtractor = AcademicTextExtractor()

    val councilExtractors = listOf<CouncilSourceExtractor>(
        SchaffExtractor(sourceFileCache, academicTextExtractor),
        HefeleExtractor(sourceFileCache, academicTextExtractor),
        CatholicEncyclopediaExtractor(sourceFileCache, academicTextExtractor),
        FordhamExtractor(sourceFileCache, academicTextExtractor),
        WikidataSparqlClient(),
        CouncilWikipediaScraper(sourceFileCache)
    )
    val interlinearRepository = InterlinearRepository()
    val lexiconRepository = LexiconRepository()
    val apologeticTopicRepository = ApologeticTopicRepository()
    val apologeticResponseRepository = ApologeticResponseRepository()
    val glossAuditRepository = GlossAuditRepository()
    val llmResponseProcessor = LlmResponseProcessor(
        queueRepository = llmQueueRepository,
        councilRepository = councilRepository,
        lexiconRepository = lexiconRepository,
        interlinearRepository = interlinearRepository,
        manuscriptRepository = manuscriptRepository,
        churchFatherRepository = churchFatherRepository,
        heresyRepository = heresyRepository,
        apologeticTopicRepository = apologeticTopicRepository,
        apologeticResponseRepository = apologeticResponseRepository,
        glossAuditRepository = glossAuditRepository
    )
    val kafkaProducer = KafkaProducerService()
    val llmResultsConsumer = LlmResultsConsumer(llmResponseProcessor)

    val conflictResolutionService = ConflictResolutionService(llmQueueRepository = llmQueueRepository)
    val consensusEngine = SourceConsensusEngine(sourceRepository, claimRepository, conflictResolutionService)
    val councilService = CouncilService(
        councilRepository = councilRepository,
        sourceRepository = sourceRepository,
        claimRepository = claimRepository,
        heresyRepository = heresyRepository,
        canonRepository = canonRepository,
        hereticParticipantRepository = hereticParticipantRepository
    )
    val councilIngestionService = CouncilIngestionService(
        councilRepository = councilRepository,
        heresyRepository = heresyRepository,
        canonRepository = canonRepository,
        hereticParticipantRepository = hereticParticipantRepository,
        sourceRepository = sourceRepository,
        claimRepository = claimRepository,
        churchFatherRepository = churchFatherRepository,
        extractors = councilExtractors,
        consensusEngine = consensusEngine,
        summarizationService = biographySummarizationService,
        phaseTracker = phaseTracker,
        fileCache = sourceFileCache,
        llmResponseProcessor = llmResponseProcessor
    )

    val orchestrator = IngestionOrchestrator(
        ingestionService,
        patristicIngestionService,
        councilIngestionService,
        ingestionMetadataRepository,
        statsRepository,
        phaseTracker,
        llmQueueRepository
    )

    // Bible module (bible_db)
    val bibleVersionRepository = BibleVersionRepository()
    val bibleBookRepository = BibleBookRepository()
    val bibleVerseRepository = BibleVerseRepository()
    val bibleReferenceParser = BibleReferenceParser(bibleBookRepository)
    val bibleService = BibleService(bibleVersionRepository, bibleBookRepository, bibleVerseRepository, bibleReferenceParser, interlinearRepository, lexiconRepository)
    val bibleOnlineScraper = BibleOnlineScraper(sourceFileCache)
    val wordAlignmentService = WordAlignmentService(interlinearRepository, bibleVerseRepository, bibleVersionRepository, bibleBookRepository)
    val bibleHubLexiconScraper = com.ntcoverage.scraper.BibleHubLexiconScraper(sourceFileCache)
    val lexiconEnrichmentService = LexiconEnrichmentService(lexiconRepository, bibleHubLexiconScraper)
    val bibleTokenRepository = com.ntcoverage.repository.BibleTokenRepository()
    val bibleTokenizationService = BibleTokenizationService(bibleTokenRepository, bibleVerseRepository, bibleVersionRepository, bibleBookRepository)
    val bibleLayer4ApplicationRepository = com.ntcoverage.repository.BibleLayer4ApplicationRepository()
    val bibleLayer4CoverageService = com.ntcoverage.service.BibleLayer4CoverageService()
    val bibleIngestionService = BibleIngestionService(bibleVersionRepository, bibleBookRepository, bibleVerseRepository, interlinearRepository, lexiconRepository, phaseTracker, bibleOnlineScraper = bibleOnlineScraper, wordAlignmentService = wordAlignmentService, lexiconEnrichmentService = lexiconEnrichmentService, llmConfig = llmConfig, llmQueueRepository = llmQueueRepository, tokenizationService = bibleTokenizationService, applicationRepository = bibleLayer4ApplicationRepository)

    // Apologetics module (repositories created above, before LlmResponseProcessor)
    val apologeticsService = ApologeticsService(
        topicRepository = apologeticTopicRepository,
        responseRepository = apologeticResponseRepository,
        llmQueueRepository = llmQueueRepository
    )

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
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
        }
        get("/prometheus") {
            call.respond(prometheusMeterRegistry.scrape())
        }

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
                    "GET /admin/councils/audit?maxYear=&onlyMissing= - Council audit (gaps)",
                    "POST /admin/enrich-dating?domain=&limit= - Manual dating enrichment via OpenAI",
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
        councilRoutes(councilService)
        visitorTrackingRoutes(visitorService, sessionRateLimiter, heartbeatRateLimiter, pageviewRateLimiter)
        bibleRoutes(bibleService)
        apologeticsRoutes(apologeticsService)

        authLoginRoute(userRepository, jwkProvider, googleClientId)

        authenticate("internal-jwt") {
            authRoutes(userRepository, userService)
            visitorAnalyticsRoutes(visitorService)
            apologeticsAdminRoutes(apologeticsService)
            adminRoutes(
                orchestrator,
                ingestionMetadataRepository,
                ingestionScope,
                datingEnrichmentService,
                councilIngestionService,
                councilService,
                phaseTracker,
                ingestionService,
                patristicIngestionService,
                bibleIngestionService,
                llmQueueRepository,
                kafkaProducer,
                llmResponseProcessor,
                bibleLayer4ApplicationRepository,
                bibleLayer4CoverageService,
                glossAuditRepository
            )
        }
    }

    monitor.subscribe(ApplicationStarted) {
        retentionScheduler.start(ingestionScope)
        kafkaProducer.start()
        llmResultsConsumer.start(ingestionScope)
    }

    monitor.subscribe(ApplicationStopped) {
        llmResultsConsumer.close()
        kafkaProducer.close()
        ingestionScope.cancel()
        BibleDatabaseConfig.close()
        DatabaseConfig.close()
    }
}
