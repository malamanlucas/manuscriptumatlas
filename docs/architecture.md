# Architecture

## What is Manuscriptum Atlas?

A scholarly platform for analyzing the textual coverage of the Greek New Testament through historical manuscripts, patristic testimonies, church councils, and heresies. It provides cumulative coverage metrics by century and book, interlinear Greek/Hebrew Bible study tools, and a multi-source consensus engine for historical data.

**Domains:**
- **Manuscripts** — Gregory-Aland catalog (papyri, uncials, minuscules, lectionaries) with verse-level coverage tracking across centuries I–X
- **Patristic** — Church fathers, their textual statements about NT topics (manuscripts, canon, autographs, etc.), biographies, and dating
- **Councils** — Ecumenical, regional, and local church councils up to 1000 AD, with heresies, canons, and multi-source consensus
- **Bible** — Multi-translation Bible reader with interlinear Greek/Hebrew, Strong's concordance, and full-text search
- **Observatory** — Visitor analytics with fingerprinting, session tracking, and real-time dashboards

## Stack

| Layer | Technology |
|-------|------------|
| Backend | Kotlin 2.1 / Ktor 3.1 / Exposed ORM / PostgreSQL 16 / Flyway |
| Frontend | Next.js 16 / React 19 / TypeScript 5.9 / Tailwind CSS 4 / Recharts / Leaflet / TanStack Query / next-intl |
| LLM | OpenAI GPT-5.4 / GPT-4.1-mini (3-tier) via LlmOrchestrator + LLM Queue (Claude Code) |
| Messaging | Apache Kafka (KRaft mode) — LLM results notification |
| Infrastructure | Docker Compose (postgres + kafka + init + app + frontend + prometheus + grafana + loki + promtail) |
| Monitoring | Prometheus + Grafana + Loki (metrics, dashboards, logs) |

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Frontend (Next.js 16)                │
│  Pages → Components → Hooks → API functions → /api/*    │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP (rewrite /api/* → backend)
┌──────────────────────────▼──────────────────────────────┐
│                    Backend (Ktor 3.1)                     │
│                                                          │
│  Routes → Services → Repositories → Database             │
│                 ↕                                         │
│           LlmOrchestrator (direct, for Apologetics)      │
│           LlmPromptQueue (async, for ingestion)          │
│                 ↕                                         │
│    Scrapers / Seed / SourceFileCache                     │
└──────────────────┬──────────────────┬───────────────────┘
                   │ JDBC             │ Kafka
┌──────────────────▼────────┐  ┌─────▼───────────────────┐
│  PostgreSQL 16 (2 DBs)    │  │  Kafka (KRaft mode)     │
│  nt_coverage: 33 tables   │  │  topic: llm-results-    │
│  bible_db: 9 tables       │  │         ready            │
└───────────────────────────┘  └─────────────────────────┘

Claude Code (/run-llm):
  1. GET /admin/llm/queue/pending → reads prompts
  2. Processes with Claude model
  3. POST /admin/llm/queue/{id}/complete → saves responses
  4. POST /admin/llm/queue/apply/{phase} → triggers apply
```
```

## Backend Package Structure

```
com.ntcoverage/
├── Application.kt          — Entry point, wiring, startup
├── config/                  — 9 configs (Database, Flyway, Kafka, ingestion flags, cache)
├── llm/                     — 6 files (LlmOrchestrator, OpenAiProvider, RateLimiter, Config)
├── model/
│   ├── Tables.kt            — 33 Exposed ORM table definitions (main DB)
│   ├── DTOs.kt              — ~108 response/request objects
│   ├── BibleTables.kt       — 12 Bible module tables (separate DB)
│   └── BibleDTOs.kt         — 18 Bible response objects
├── repository/              — 27 repositories (data access layer)
├── service/                 — 30 services (business logic, ingestion, LLM queue, Kafka)
├── routes/                  — 12 route files (60+ HTTP endpoints)
├── scraper/                 — 15 scrapers (NTVMR, BibleHub, Wikipedia, academic sources)
├── seed/                    — 17 seed data files (canonical verses, fathers, councils, bible)
└── util/                    — JWT, URL helpers
```

## Frontend Structure

```
frontend/
├── app/[locale]/(atlas)/    — 24 page directories (dashboard, manuscripts, fathers, councils, bible, admin)
├── components/              — 47+ components organized in 11 directories
│   ├── bible/               — Reader, navigation, interlinear, compare, search
│   ├── charts/              — Timeline, comparison, heatmap
│   ├── councils/            — Map, badges, provenance, summary
│   ├── coverage/            — Book cards, century slider, verse grid
│   ├── filters/             — Century/year range selectors
│   ├── ingestion/           — Phase panels (generic + domain-specific)
│   ├── layout/              — Sidebar, header, language selector, theme toggle
│   ├── llm/                 — Provider cards, rate limits, usage table
│   ├── observatory/         — Auth gate, analytics tabs, session drawer, visitor tracker
│   ├── stats/               — Statistics components
│   └── ui/                  — Confidence dots, dating badges
├── hooks/                   — 18 hook files (TanStack Query wrappers)
├── lib/api/                 — 16 API files (130+ fetch functions)
├── types/                   — 14 type files (75+ TypeScript interfaces)
├── messages/                — i18n (en.json, pt.json, es.json — 44 namespaces each)
└── i18n/                    — Routing, navigation, request config
```

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| ORM | Exposed (Kotlin-native) | Type-safe SQL, coroutine-friendly, no annotation magic |
| Serialization | kotlinx.serialization | Kotlin-native, compile-time, no reflection (never Jackson) |
| DI | Constructor injection | Simple, no framework overhead, explicit wiring |
| LLM strategy | Queue-based (Claude Code) + direct (OpenAI) | Async queue for all ingestion ($0), direct API only for Apologetics (~$2/mo) |
| Messaging | Apache Kafka (KRaft mode) | LLM results notification, restart-safe, consumer auto-catch-up |
| Database per domain | 2 databases (main + bible) | Bible module is self-contained, can scale independently |
| Partitioning | Monthly RANGE on visitor tables | High-volume analytics data, efficient pruning |
| Frontend data | TanStack Query (not RSC fetch) | Cache management, auto-refetch, mutation invalidation |
| i18n | next-intl (3 locales) | Mature, supports RSC, locale-aware routing |
| Consensus | Weighted multi-source engine | Academic rigor — Schaff/Hefele (1.0) > Wikidata (0.7) > Wikipedia (0.5) |

## Authentication & Authorization

```
Google SSO → Backend validates ID token → Creates/updates user → Issues internal JWT (8h)
                                                                        ↓
                                                          Claims: userId, email, role, displayName

Roles:
  ADMIN  → Full access (ingestion, analytics, user management)
  MEMBER → Public pages only

Dev shortcut:
  POST /auth/dev-login → Auto-creates admin JWT (localhost only)
```

## Data Flow — Coverage Calculation

```
NTVMR API → TEI/XML transcriptions
    ↓
Parse verse ranges per manuscript (NtvmrVerseParser)
    ↓
Link manuscripts to verses (ManuscriptVerses junction table)
    ↓
Materialize coverage by century (CoverageByCentury — cumulative)
    ↓
Century N coverage = all manuscripts from centuries 1..N
    ↓
API: GET /coverage?century=4&type=papyrus
Frontend: Dashboard charts, book cards, timeline
```

## Data Flow — Source Consensus Engine

```
Multiple academic sources provide claims about councils:
  Schaff (weight 1.0) → year, location, participants
  Hefele (weight 1.0) → year, location, participants
  Catholic Encyclopedia (weight 0.8) → year, location
  Fordham (weight 1.0) → year, location
  Wikidata (weight 0.7) → structured data
  Wikipedia (weight 0.5) → aggregated text

SourceConsensusEngine:
  1. Collect all claims per field (year, location, participants)
  2. Weight by source reliability
  3. If conflict → ConflictResolutionService (LLM-powered, direct or queue)
  4. Output: consensus value + confidence score (0.0–1.0)
```

## Data Flow — LLM Prompt Queue

```
Async LLM processing via Claude Code (eliminates OpenAI API costs for ingestion):

1. PREPARE phase (backend):
   Service builds prompts → LlmQueueRepository.enqueue() → llm_prompt_queue (status: pending)

2. PROCESS (Claude Code /run-llm skill):
   GET /admin/llm/queue/pending → Claude generates response →
   POST /admin/llm/queue/{id}/complete (status: completed)

3. APPLY phase (backend):
   POST /admin/llm/queue/apply/{phase} →
   LlmResponseProcessor.processCompleted() → saves to original tables (status: applied)
   OR: Kafka notification → LlmResultsConsumer → auto-apply

Statuses: pending → processing → completed → applied (or failed)

Migrated services:
  - ConflictResolutionService (council_consensus_prepare/_apply)
  - BibleIngestionService (9 phases: translate lexicon/hebrew_lexicon/glosses,
    enrichment greek/hebrew, align kjv/arc69/hebrew_kjv/hebrew_arc69)
  - WordAlignmentService (via bible_align_* phases)
  - DatingEnrichmentService (DatingEnrichment:manuscript/father)
  - BiographySummarizationService (BIO_*, COUNCIL_TRANSLATE_*, HERESY_TRANSLATE_*, COUNCIL_OVERVIEW_*)
Pending: AcademicTextExtractor
Exception: ApologeticsService keeps direct OpenAI (real-time, low volume)
```
