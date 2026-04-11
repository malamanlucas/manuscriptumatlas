# Database — PostgreSQL 16 / Exposed ORM / Flyway

## Overview

Two PostgreSQL 16 databases:
- **nt_coverage** — Main database (33 tables): manuscripts, patristic, councils, apologetics, observatory, auth, LLM
- **bible_db** — Bible module (12 tables): versions, verses, interlinear, lexicon, translations

Extensions: `pg_trgm` (fuzzy search), `unaccent` (accent-insensitive search).

## Critical Convention

**Flyway does NOT execute migrations at runtime.** The fallback in `FlywayConfig.kt` creates the schema via Exposed's `SchemaUtils.createMissingTablesAndColumns()`. Migration `.sql` files serve as documentation only.

**Every new normal table MUST be registered in SchemaUtils** in `FlywayConfig.kt`. Forgetting this causes `PSQLException: relation "xxx" does not exist` at runtime.

**Partitioned tables** (VisitorSessions, PageViews) must NOT be registered in SchemaUtils — they are created via raw SQL `exec()`.

## Tables — Main Database (33)

### Core (Manuscripts)

| Table | Columns | Notes |
|-------|---------|-------|
| Books | name, abbreviation, totalChapters, totalVerses, bookOrder | 27 NT books (+ 39 OT via seed) |
| Verses | bookId (FK), chapter, verse | ~31k verses (NT 7,956 + OT 23,145) |
| Manuscripts | gaId (UNIQUE), name, centuryMin/Max, manuscriptType, yearMin/Max/Best, datingConfidence/Source | Gregory-Aland catalog |
| ManuscriptSources | manuscriptId (FK, UNIQUE), sourceName, ntvmrUrl, historicalNotes | 1:1 metadata |
| ManuscriptVerses | manuscriptId (FK), verseId (FK) | Junction table |
| CoverageByCentury | century, bookId (FK), coveredVerses, totalVerses, coveragePercent | Materialized metrics |
| BookTranslations | bookId (FK), locale, name, abbreviation | i18n (pt, en, es) |

### Patristic

| Table | Columns | Notes |
|-------|---------|-------|
| ChurchFathers | displayName, normalizedName (UNIQUE), centuryMin/Max, tradition, yearMin/Max/Best, datingConfidence, biographySummary | ~40 fathers |
| FatherTextualStatements | fatherId, topic (8 types), statementText, sourceWork, sourceReference, approximateYear | NT textual testimony |
| ChurchFatherTranslations | fatherId (FK), locale, displayName, shortDescription, biographySummary | i18n |
| FatherStatementTranslations | statementId (FK), locale, statementText | i18n |

### Councils & Heresies

| Table | Columns | Notes |
|-------|---------|-------|
| Councils | displayName, normalizedName, slug (UNIQUE), year, yearEnd, century, councilType, location, lat/lon, summary, consensusConfidence, dataConfidence, sourceCount | ~147 councils |
| CouncilTranslations | councilId (FK), locale, displayName, shortDescription, summary | i18n |
| CouncilFathers | councilId (FK), fatherId (FK), role | N:N junction |
| Sources | name (UNIQUE), displayName, sourceLevel, baseWeight, reliabilityScore | Schaff, Hefele, etc. |
| CouncilSourceClaims | councilId (FK), sourceId (FK), claimedYear, claimedLocation, rawText | Per-source claims |
| Heresies | name, normalizedName (UNIQUE), slug (UNIQUE), centuryOrigin, yearOrigin, keyFigure | Arianism, etc. |
| HeresyTranslations | heresyId (FK), locale, name, description | i18n |
| CouncilHeresies | councilId (FK), heresyId (FK), action | Junction (condemned/addressed) |
| CouncilCanons | councilId (FK), canonNumber, title, canonText, topic | Disciplinary rules |
| CouncilHereticParticipants | councilId (FK), displayName, role, description | Heretics at councils |

### Infrastructure

| Table | Columns | Notes |
|-------|---------|-------|
| IngestionMetadata | status, startedAt, finishedAt, durationMs, manuscriptsIngested | Singleton row |
| CouncilIngestionPhases | phaseName (UNIQUE), status, startedAt, completedAt, itemsProcessed/Total, errorMessage | Phase tracking |
| Users | email (UNIQUE), displayName, pictureUrl, role (ADMIN/MEMBER) | Auth |
| LlmUsageLogs | provider, model, label, success, inputTokens, outputTokens, estimatedCostUsd, latencyMs | LLM audit |
| LlmPromptQueue | phaseName, label, systemPrompt, userContent, temperature, maxTokens, tier, status, responseContent, callbackContext | Async LLM queue |

### Apologetics

| Table | Columns | Notes |
|-------|---------|-------|
| ApologeticTopics | slug (UNIQUE), category, difficulty, status | Apologetic Q&A topics |
| ApologeticTopicTranslations | topicId (FK), locale, title, question | i18n |
| ApologeticResponses | topicId (FK), responseType, status, modelUsed | AI-generated responses |
| ApologeticResponseTranslations | responseId (FK), locale, content, summary | i18n |

### Observatory

| Table | Columns | Notes |
|-------|---------|-------|
| VisitorSessions | 36 columns (fingerprinting, browser, OS, device, screen, network, GPU, canvas) | Partitioned monthly |
| PageViews | sessionId, visitorId, path, referrerPath, durationMs | Partitioned monthly |
| VisitorDailyStats | statDate, totalSessions, totalPageviews, uniqueVisitors, topBrowsers (JSONB) | Aggregated |

## Tables — Bible Database (12)

| Table | Columns | Notes |
|-------|---------|-------|
| BibleVersions | code (UNIQUE), name, language, isPrimary, testamentScope | KJV, AA, ACF, ARC69 |
| BibleBooks | name, abbreviation, totalChapters, totalVerses, bookOrder, testament | 66 books |
| BibleBookAbbreviations | bookId (FK), locale, abbreviation | Multi-locale (Mt, Jo, Gn) |
| BibleChapters | bookId (FK), chapterNumber, totalVerses | Chapter metadata |
| BibleVerses | chapterId (FK), verseNumber | Verse identity |
| BibleVerseTexts | verseId (FK), versionId (FK), text | Actual text per translation |
| InterlinearWords | verseId (FK), wordPosition, originalWord, transliteration, lemma, morphology, strongsNumber, englishGloss, language | Greek/Hebrew word-by-word |
| WordAlignments | interlinearWordId (FK), versionId (FK), alignedText, confidence | LLM-powered word alignment |
| GreekLexicon | strongsNumber (UNIQUE), lemma, transliteration, pronunciation, shortDefinition, fullDefinition, partOfSpeech | ~5,600 entries |
| GreekLexiconTranslations | lexiconId (FK), locale, shortDefinition, fullDefinition | Translated lexicon (pt, es) |
| HebrewLexicon | strongsNumber (UNIQUE), lemma, transliteration, pronunciation, shortDefinition, fullDefinition, partOfSpeech | ~8,700 entries |
| HebrewLexiconTranslations | lexiconId (FK), locale, shortDefinition, fullDefinition | Translated lexicon (pt, es) |

## Migrations (V1–V21)

| Migration | Description |
|-----------|-------------|
| V1 | Core tables: books, verses, manuscripts, manuscript_verses, coverage_by_century |
| V2 | Index: idx_manuscripts_type |
| V3 | Table: manuscript_sources (metadata 1:1) |
| V4 | Table: ingestion_metadata (singleton status) |
| V5 | Table: book_translations (i18n seed: 27 books x 3 locales) |
| V6 | Table: church_fathers (with pg_trgm index) |
| V7 | Table: father_textual_statements (8 topic types) |
| V8 | Table: church_father_translations |
| V9 | ALTER: church_fathers + dating fields (yearMin/Max/Best, confidence) |
| V10 | ALTER: church_father_translations + translationSource |
| V11 | Partitioned tables: visitor_sessions, page_views + PL/pgSQL auto-partition function |
| V12 | Table: users (email, role, Google SSO) |
| V13 | ALTER: manuscripts + church_fathers dating fields |
| V14 | Tables: councils, council_translations, council_fathers, sources, council_source_claims |
| V15 | Tables: heresies, heresy_translations, council_heresies, council_canons |
| V16 | ALTER: heresy_translations + translationSource |
| V17 | Table: council_heretic_participants |
| V18 | Table: llm_usage_logs |
| V19 | Documentation: bible_db schema (created by BibleFlywayConfig.kt) |
| V20 | Tables: apologetic_topics, apologetic_topic_translations, apologetic_responses, apologetic_response_translations |
| V21 | Table: llm_prompt_queue (async LLM processing) |

**Next migration: V22**

## Indexes

### Full-Text Search (tsvector GIN)
- `councils.display_name` — FTS for council name search

### Trigram (pg_trgm GIN)
- `church_fathers.display_name` — Fuzzy father name search
- `father_textual_statements.statement_text` — Fuzzy statement search
- `councils.display_name` — Fuzzy council name search
- `heresies.name` — Fuzzy heresy name search

### B-tree (key lookups)
- `manuscripts`: gaId, manuscriptType, effectiveCentury, yearBest, (yearMin, yearMax)
- `church_fathers`: normalizedName, centuryMin, tradition, yearBest, (yearMin, yearMax)
- `councils`: slug, century, year, councilType, consensusConfidence, normalizedName
- `visitor_sessions`: created_at DESC, session_id, visitor_id, ip, browser, os, device, language, timezone
- `page_views`: created_at, session_id, visitor_id, path
- `llm_usage_logs`: created_at DESC, provider

## Partitioning Strategy

Tables `visitor_sessions` and `page_views` use **monthly RANGE partitioning** on `created_at`.

```sql
-- Auto-partition function (runs on startup)
CREATE OR REPLACE FUNCTION create_monthly_partitions(months_ahead INT DEFAULT 2)
  -- Creates partitions for current month + N months ahead
  -- Naming: visitor_sessions_2026_03, page_views_2026_03
  -- Idempotent (checks pg_class before creating)
```

## Checklist — New Normal Table

1. Migration SQL: `V{N}__description.sql`
2. Table object in `Tables.kt`
3. **Register in `SchemaUtils.createMissingTablesAndColumns()` in `FlywayConfig.kt`** (most common mistake)
4. Extra indexes in `applyExtraIndexesAndConstraints()`

## Checklist — New Partitioned Table

1. Migration SQL: `V{N}__description.sql`
2. Table object in `Tables.kt` (points to parent table)
3. **Do NOT register** in SchemaUtils
4. Create via `exec(SQL)` in `FlywayConfig.kt` (check existence with `pg_class`)
5. Create initial partitions (current month + 2)
6. Create auto-partition function + indexes via `exec()`
