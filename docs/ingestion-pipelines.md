# Ingestion Pipelines

All pipelines use `IngestionPhaseTracker` for persistent phase tracking with status (idle/running/success/failed), progress counters, timing, and stuck phase recovery on server restart.

**"Run all" skips completed phases** — clicking "Executar todas" only runs phases not yet in `success` status. Individual phase buttons always execute regardless of status.

## Manuscripts (4 phases)

```
IngestionOrchestrator → IngestionService
  │
  ├── manuscript_seed_books          — Seed 27 NT books + 7,956 canonical verses
  ├── manuscript_ingest              — Fetch manuscripts from NTVMR API (TEI/XML)
  ├── manuscript_coverage            — Materialize coverage by century (cumulative)
  └── manuscript_enrich_dating       — LLM-powered date inference (yearMin/Max/Best)
```

**Data source:** NTVMR API (INTF Munster) — TEI/XML transcriptions with verse-level precision.
**Rate limiting:** 50ms delay between NTVMR requests (dev), 100ms (prod).

## Patristic (6 phases)

```
PatristicIngestionService
  │
  ├── patristic_seed_fathers         — Insert fathers from ChurchFathersSeedData
  ├── patristic_seed_statements      — Insert textual statements from seed
  ├── patristic_translate_fathers    — Insert pt/es translations from seed
  ├── patristic_translate_statements — Insert statement translations
  ├── patristic_translate_biographies — LLM-generated biography summaries (queue: BIO_SUMMARIZE_*, BIO_TRANSLATE_*)
  └── patristic_enrich_dating        — LLM dating enrichment (queue: DatingEnrichment:father)
```

**Optional filter:** `?filter=clement_of_rome,ignatius_of_antioch` — process only specific fathers.
**LLM usage:** Biography summarization + dating inference via LlmOrchestrator.

## Councils (14 phases)

```
CouncilIngestionService
  │
  ├── council_seed                   — Seed councils, heresies, canons, sources from curated data
  │
  │  Source Extraction (4 academic sources):
  ├── council_extract_schaff         — Schaff's Church History (weight 1.0, PRIMARY)
  ├── council_extract_hefele         — Hefele's Council History (weight 1.0, PRIMARY)
  ├── council_extract_catholic_enc   — Catholic Encyclopedia (weight 0.8, ACADEMIC)
  ├── council_extract_fordham        — Fordham Medieval Sourcebook (weight 1.0, PRIMARY)
  │
  │  Enrichment:
  ├── council_extract_wikidata       — SPARQL structured data (weight 0.7, STRUCTURED)
  ├── council_extract_wikipedia      — Wikipedia articles (weight 0.5, AGGREGATOR)
  │
  │  Consensus & AI (LLM Queue):
  ├── council_consensus              — SourceConsensusEngine resolves conflicts (direct LLM)
  ├── council_consensus_prepare      — Calculate consensus + enqueue conflicts to LLM queue
  ├── council_consensus_apply        — Apply LLM conflict resolutions from queue
  ├── council_summaries              — LLM-generated council summaries
  ├── council_overview_enrichment    — LLM overview enrichment (queue: COUNCIL_OVERVIEW_*)
  │
  │  Translation (LLM Queue):
  ├── council_translate_all          — Translate councils to pt/es (queue: COUNCIL_TRANSLATE_*)
  └── heresy_translate_all           — Translate heresies to pt/es (queue: HERESY_TRANSLATE_*)
```

**Queue-based consensus flow:**
1. Run `council_consensus_prepare` → calculates weighted consensus, enqueues conflicts to `llm_prompt_queue`
2. Run `/run-llm` in Claude Code → processes pending conflict resolution prompts
3. Run `council_consensus_apply` → applies LLM resolutions back to councils table

### Source Consensus Engine

When multiple sources disagree (e.g., council year, location, participant count):

```
Source weights:
  PRIMARY (1.0)    — Schaff, Hefele, Fordham
  ACADEMIC (0.8)   — Catholic Encyclopedia, Seed data
  STRUCTURED (0.7) — Wikidata
  AGGREGATOR (0.5) — Wikipedia

Process:
  1. Collect all claims per field (year, location, participants)
  2. Calculate weighted consensus
  3. If conflict persists → ConflictResolutionService (direct LLM or queue)
  4. Output: consensus value + confidence (0.0–1.0) + dataConfidence (HIGH/MEDIUM/LOW)
```

All source documents are cached via `SourceFileCache` to avoid re-downloading.

## Bible (25 phases)

```
BibleIngestionService
  │
  │  Setup:
  ├── bible_seed_versions            — Seed Bible version metadata (KJV, AA, ACF, ARC69)
  ├── bible_seed_books               — Seed 66 books (NT + OT)
  ├── bible_seed_abbreviations       — Multi-locale book abbreviations
  │
  │  Text Ingestion:
  ├── bible_ingest_text_kjv          — KJV text (66 books, public domain)
  ├── bible_ingest_text_aa           — Almeida Atualizada
  ├── bible_ingest_text_acf          — Almeida Corrigida Fiel
  ├── bible_ingest_text_arc69        — Almeida Revista e Corrigida (scraper)
  │
  │  Interlinear & Lexicon:
  ├── bible_ingest_nt_interlinear    — Greek interlinear (260 chapters)
  ├── bible_ingest_ot_interlinear    — Hebrew interlinear (929 chapters)
  ├── bible_ingest_greek_lexicon     — Strong's Greek lexicon (~5,600 entries)
  ├── bible_ingest_hebrew_lexicon    — Strong's Hebrew lexicon (~8,700 entries)
  ├── bible_fill_missing_hebrew      — Fill missing Hebrew entries from BibleHub
  │
  │  Lexicon Translation (LLM Queue):
  ├── bible_translate_lexicon        — Translate Greek lexicon to pt/es (queue: LEXICON_BATCH_Greek_*)
  ├── bible_translate_hebrew_lexicon — Translate Hebrew lexicon to pt/es (queue: LEXICON_BATCH_Hebrew_*)
  ├── bible_translate_glosses        — Translate interlinear glosses to pt/es (queue: GLOSS_TRANSLATE_*)
  │
  │  Word Alignment (LLM Queue):
  ├── bible_align_kjv                — Greek → KJV word alignment (queue: WORD_ALIGN_*)
  ├── bible_align_arc69              — Greek → ARC69 word alignment (queue: WORD_ALIGN_*)
  ├── bible_align_hebrew_kjv         — Hebrew → KJV word alignment (queue: WORD_ALIGN_*)
  ├── bible_align_hebrew_arc69       — Hebrew → ARC69 word alignment (queue: WORD_ALIGN_*)
  │
  │  Lexicon Enrichment (BibleHub scraping):
  ├── bible_enrich_greek_lexicon     — Enrich Greek lexicon from BibleHub
  ├── bible_enrich_hebrew_lexicon    — Enrich Hebrew lexicon from BibleHub
  ├── bible_reenrich_greek_lexicon   — Re-enrich Greek lexicon
  ├── bible_reenrich_hebrew_lexicon  — Re-enrich Hebrew lexicon
  │
  │  Enrichment Translation (LLM Queue):
  ├── bible_translate_enrichment_greek   — Translate Greek enrichment to pt/es (queue: ENRICHMENT_TRANSLATE_*)
  └── bible_translate_enrichment_hebrew  — Translate Hebrew enrichment to pt/es (queue: ENRICHMENT_TRANSLATE_*)
```

**Data sources:** BibleHub (interlinear/lexicon scraping), public APIs (bible-api.com, bolls.life), direct scraping for specific translations.

**Gloss translation:** `processGlossResponse()` usa estrategia JSON-first (key-based matching) com
fallback line-by-line apenas para respostas plain-text. Respostas JSON com chaves incompativeis
retornam mapa parcial — glosses faltantes ficam NULL e sao reprocessados no proximo run.

**Limpeza direcionada:** `POST /admin/bible/glosses/fix-corrupted` limpa apenas `portugueseGloss`
corrompidos (contendo fragmentos JSON `": "`), sem afetar `spanishGloss` ou `word_alignments`.

## Admin Control

### Per-phase execution
```bash
# Run single phase
POST /admin/{domain}/ingestion/run/{phase}

# Run selected phases
POST /admin/{domain}/ingestion/run
Body: { "phases": ["council_seed", "council_extract_schaff"] }

# Run all (skips completed)
POST /admin/{domain}/ingestion/run-all

# Reset domain (clears all data + phase statuses)
POST /admin/reset/{domain}
```

Where `{domain}` is `manuscripts`, `patristic`, `councils`, or `bible`.

### Dating enrichment (manual, LLM-powered)
```bash
POST /admin/enrich-dating?domain=fathers&limit=50
```
Uses LLM to infer `yearMin`, `yearMax`, `yearBest` with `datingConfidence` (HIGH/MEDIUM/LOW).

## Phase Status Lifecycle

```
idle → running → success
                → failed (error message stored)

On server restart:
  - Phases stuck in "running" are marked as "failed" with "Interrupted by server restart"
  - Can be re-executed individually or via "Run all"
```

## Queue-based Bible LLM flow

All 9 LLM phases da Bíblia usam o sistema de queue assíncrono:

1. Run a `_prepare` phase (e.g., `bible_translate_lexicon`) → builds prompts and enqueues to `llm_prompt_queue`
2. Run `/run-llm` in Claude Code → processes pending prompts from the queue
3. Responses are applied automatically via `LlmResponseProcessor` (Kafka auto-apply or manual `POST /admin/llm/queue/apply/{phase}`)

Phases using queue: `bible_translate_lexicon`, `bible_translate_hebrew_lexicon`, `bible_translate_glosses`, `bible_translate_enrichment_greek`, `bible_translate_enrichment_hebrew`, `bible_align_kjv`, `bible_align_arc69`, `bible_align_hebrew_kjv`, `bible_align_hebrew_arc69`.

## Frontend Integration

- **IngestionPhasePanel** — Generic component (status badge, progress bar, run/reset buttons)
- **ManuscriptIngestionPanel** / **PatristicIngestionPanel** / **CouncilIngestionPanel** / **BibleIngestionPanel** — Domain-specific wrappers
- **LlmQueuePanel** — LLM Queue tab: stats cards (pendentes/processando/concluídos/falharam), per-phase breakdown, delete/retry actions, auto-polls every 5s
- **useIngestionPhases** hook — Auto-refetches every 3s while any phase is running
- **Ingestion Status page** (`/ingestion-status`) — Tabbed view with 5 tabs: Manuscritos, Patrística, Concílios, Bíblia, Fila LLM
