# Backend — Kotlin / Ktor / Exposed

## Conventions

- **Classes:** PascalCase | **Functions:** camelCase | **Tables:** PascalCase plural | **Enums:** UPPER_SNAKE_CASE
- **Serialization:** `kotlinx.serialization` (`@Serializable`) — never Jackson
- **Transactions:** always via `transaction {}` in Repository, never in Service
- **Async:** Kotlin Coroutines (`suspend fun`, `async`, `withContext`)
- **DI:** Constructor injection, no frameworks
- **Logging:** `LoggerFactory.getLogger()`
- **Route ordering:** Literal routes (`/fathers/statements`) BEFORE dynamic (`/fathers/{id}`)
- **Repository pattern:** Returns DTOs, never ResultRows. Custom SQL via `object : Op<Boolean>()`
- **External calls:** Retry with exponential backoff (NTVMR, LLM APIs)

## Repositories (27)

| Repository | Domain | Key Operations |
|------------|--------|----------------|
| ManuscriptRepository | Manuscripts | findAll (type/century/year filters), findByGaId |
| VerseRepository | Manuscripts | findByBookChapterVerse, insertBook, insertVerses |
| CoverageRepository | Manuscripts | coverageByCentury, aggregatedStats, missingVerses |
| ChapterCoverageRepository | Manuscripts | chapterLevel coverage, covered/missing verse lists |
| MetricsRepository | Manuscripts | fragmentation, stabilization, growth rates |
| StatsRepository | Manuscripts | totalManuscripts, byType, byCentury, byBook |
| ChurchFatherRepository | Patristic | findAll (century/tradition/year), search (pg_trgm), dating |
| FatherTextualStatementRepository | Patristic | findByFather, findByTopic, searchKeyword, countByTopic |
| CouncilRepository | Councils | findAll (paginated/filtered), findBySlug, mapPoints, updateConflictResolution |
| CouncilSourceClaimRepository | Councils | consensus claims (weight, score) |
| CouncilCanonRepository | Councils | canons by council |
| CouncilHereticParticipantRepository | Councils | heretic participants |
| HeresyRepository | Councils | findAll, findBySlug, councilsForHeresy |
| SourceRepository | Councils | source metadata (Schaff, Hefele, etc.) |
| ApologeticTopicRepository | Apologetics | CRUD topics |
| ApologeticResponseRepository | Apologetics | CRUD responses |
| IngestionMetadataRepository | Ingestion | singleton status row |
| IngestionPhaseRepository | Ingestion | phase tracking by domain (status, progress, timing) |
| UserRepository | Auth | CRUD, findByEmail, updateRole |
| VisitorRepository | Observatory | sessions, pageviews, fingerprinting, analytics |
| LlmUsageLogRepository | LLM | request logging (provider, model, tokens, cost) |
| LlmQueueRepository | LLM Queue | enqueue/dequeue prompts, status tracking, batch ops, stats |
| BibleVersionRepository | Bible | version metadata (KJV, AA, ACF, ARC69) |
| BibleBookRepository | Bible | books, chapters, abbreviations |
| BibleVerseRepository | Bible | verses, verse texts by version |
| InterlinearRepository | Bible | Greek/Hebrew interlinear words, word alignments |
| LexiconRepository | Bible | Strong's Greek/Hebrew lexicon entries + translations |

## Services (30)

| Service | Purpose |
|---------|---------|
| IngestionOrchestrator | Master orchestrator — launches/resets all pipelines |
| IngestionService | Manuscript ingestion from NTVMR (4 phases) |
| PatristicIngestionService | Church fathers + statements from seed (6 phases) |
| CouncilIngestionService | Council extraction from 6 sources + consensus (14 phases) |
| BibleIngestionService | Bible text + interlinear + lexicon import (25 phases) |
| IngestionPhaseTracker | Phase status persistence, stuck phase recovery on startup |
| CoverageService | Coverage metrics, timelines, missing verses |
| ManuscriptService | Manuscript queries, detail views |
| ChurchFatherService | Father queries, search, dating |
| CouncilService | Council queries, relationships (fathers, canons, heresies, sources) |
| StatsService | Global statistics aggregation |
| MetricsService | Academic metrics (fragmentation, growth, stabilization) |
| BibleService | Bible reading, comparison, interlinear, search |
| ApologeticsService | LLM-powered apologetic Q&A (direct OpenAI, real-time) |
| BiographySummarizationService | LLM-powered biography generation |
| DatingEnrichmentService | LLM-powered date inference (yearMin/Max/Best + confidence) |
| ConflictResolutionService | LLM-based consensus resolution (direct or queue mode) |
| WordAlignmentService | LLM-powered Greek/Hebrew → translation alignment |
| LexiconEnrichmentService | Lexicon scraping enrichment (BibleHub) |
| LlmResponseProcessor | Applies completed LLM queue items back to original tables |
| KafkaProducerService | Publishes LLM results-ready notifications to Kafka |
| LlmResultsConsumer | Kafka consumer that auto-triggers LlmResponseProcessor |
| SourceConsensusEngine | Weighted multi-source truth reconstruction |
| LlmUsageService | LLM request logging and dashboard metrics |
| BibleReferenceParser | Parses bible references (e.g., "John 3:16") |
| VerseExpander | Expands verse ranges to individual verses |
| VerseRangeCompressor | Compresses verse lists to ranges |
| RetentionScheduler | Scheduled cleanup of old visitor data |
| VisitorService | Session/pageview tracking, analytics |
| UserService | User management (CRUD, roles) |

## LLM Package — Multi-Provider Abstraction

```
LlmOrchestrator (direct mode — real-time, for Apologetics)
  ├── OpenAiProvider (gpt-5.4 / gpt-4.1-mini, 3-tier)
  ├── LlmRateLimiter (per-provider: tokens + requests)
  └── LlmConfig (env vars: OPENAI_API_KEY, tier models)

LlmPromptQueue (async mode — batch ingestion, uses Claude Code)
  ├── LlmQueueRepository (enqueue/dequeue/status tracking)
  ├── LlmResponseProcessor (applies completed responses to DB)
  ├── KafkaProducerService → Kafka topic "llm-results-ready"
  └── LlmResultsConsumer ← Kafka consumer → auto-applies results

Direct flow (ApologeticsService only):
  1. Service calls llmOrchestrator.execute()
  2. OpenAI processes in real-time
  3. Response parsed and saved immediately

Queue flow (ingestion services):
  1. _prepare phase: Service calls llmQueueRepository.enqueue()
  2. Claude Code /run-llm: Reads pending, generates responses, POSTs complete
  3. _apply phase: LlmResponseProcessor applies to original tables
  4. Kafka auto-notify or manual POST /admin/llm/queue/apply/{phase}

Used by (direct): ApologeticsService
Used by (queue): ConflictResolutionService, BibleIngestionService (9 phases),
                 WordAlignmentService (4 phases), DatingEnrichmentService,
                 BiographySummarizationService (bios + council/heresy translations)
Pending migration: AcademicTextExtractor
```

## Scraper Package (15 files)

| Scraper | Source | Data |
|---------|--------|------|
| NtvmrClient | NTVMR API (INTF Munster) | TEI/XML manuscript transcriptions |
| NtvmrListClient | NTVMR list endpoint | Available manuscripts catalog |
| NtvmrVerseParser | TEI/XML | Verse ranges per manuscript |
| SchaffExtractor | Schaff's Church History | Council historical data |
| HefeleExtractor | Hefele's Council History | Council academic data |
| CatholicEncyclopediaExtractor | Catholic Encyclopedia | Council articles |
| FordhamExtractor | Fordham Medieval Sourcebook | Council documents |
| WikidataSparqlClient | Wikidata SPARQL | Structured council data |
| CouncilWikipediaScraper | Wikipedia | Council articles (HTML) |
| WikipediaScraper | Wikipedia | General Wikipedia scraping |
| CouncilNameNormalizer | — | Council name normalization utility |
| AcademicTextExtractor | Various (LLM-powered) | Structured extraction from HTML |
| BibleOnlineScraper | BibleHub / APIs | Bible text, interlinear, lexicon |
| BibleHubLexiconScraper | BibleHub | Greek/Hebrew lexicon enrichment |

All scrapers use `SourceFileCache` for disk caching and retry with backoff.

## Seed Package (17 files)

Pre-loaded reference data for all domains:

- **CanonicalVerses** — 27 NT books, 7,956 verses (chapter/verse structure)
- **ChurchFathersSeedData** — Church fathers across 4 traditions (Greek, Latin, Syriac, Coptic)
- **TextualStatementsSeedData** — Patristic statements on 8 NT topics
- **CouncilsSeedData** — Councils (Nicaea I, Constantinople I, etc.)
- **HeresiesSeedData** — Heresy definitions (Arianism, Nestorianism, etc.)
- **BibleVersionsSeedData** — Bible versions (KJV, AA, ACF, ARC69)
- **BibleBooksSeedData** — 66 books (NT + OT)
- **BibleAbbreviationsSeedData** — Multi-locale book abbreviations
- **Translation seed files** — pt/en/es translations for fathers, statements, councils, heresies, books

## Config Package

| Config | Purpose |
|--------|---------|
| DatabaseConfig | PostgreSQL connection (HikariCP pooling) |
| BibleDatabaseConfig | Separate Bible database instance |
| FlywayConfig | Migrations + Exposed fallback (schema creation) |
| BibleFlywayConfig | Bible database migrations |
| IngestionConfig | Feature flags: ENABLE_INGESTION, SKIP_IF_POPULATED, ENABLE_DATING_ENRICHMENT |
| KafkaConfig | Kafka producer/consumer properties, topic names, consumer group |
| LocaleConfig | Supported locales: pt, en, es |
| RateLimiter | Sliding window rate limiting |
| SourceFileCache | Disk cache for scraped source documents |

## Application.kt — Wiring

Startup sequence:
1. Install CORS, content negotiation (kotlinx JSON), status pages
2. Initialize databases (main + bible) with Flyway/Exposed fallback
3. Configure JWT authentication (Google SSO + internal tokens)
4. Instantiate all repositories (22)
5. Instantiate LLM providers → LlmOrchestrator
6. Instantiate LLM Queue → LlmResponseProcessor → KafkaProducer → LlmResultsConsumer
7. Instantiate all services with DI (constructor injection)
8. Instantiate scrapers with SourceFileCache
9. Register all routes (10 route files)
10. Recover stuck ingestion phases
11. Launch ingestion if enabled (ENABLE_INGESTION=true)
12. Start Kafka producer + LLM results consumer (ApplicationStarted)
13. Register graceful shutdown hooks (close Kafka, cancel scopes, close DBs)
