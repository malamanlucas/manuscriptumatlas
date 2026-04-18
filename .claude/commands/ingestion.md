Você está trabalhando no pipeline de ingestão do Manuscriptum Atlas. Siga estas regras:

## Manuscritos
`IngestionOrchestrator` → NTVMR API → parse TEI/XML → persist. Retry com backoff.
Fases: `manuscript_load_catalog`, `manuscript_fetch_content`, `manuscript_link_verses`.

## Patrística (5 fases)
`patristic_seed_fathers` → `patristic_seed_statements` → `patristic_translate_fathers` → `patristic_translate_statements` → `patristic_translate_biographies`

Filtro opcional: `?filter=normalized_name` (ex: `?filter=clement_of_rome`)

## Bíblia — Layer 1..4 (via LLM Queue)
- **L1 tokenization** — `bible_tokenize_*`, tokens por versículo.
- **L2 lexicon** — `bible_translate_lexicon` (greek), `bible_translate_hebrew_lexicon`.
- **L3 glosses/enrichment** — `bible_translate_glosses` (LOW), `bible_translate_enrichment_*` (MEDIUM).
- **L4 alignment** — `bible_align_kjv` (en) e `bible_align_arc69` (pt) apenas — HIGH tier. Só essas duas versões recebem interlinear.

Services: `BibleIngestionService`, `BibleLayer4CoverageService`; repository `BibleLayer4ApplicationsRepository`.
Flow: enfileira em `llm_prompt_queue` → processado via `/run-llm` ou `/drain-queue` → `POST /admin/llm/queue/apply/{phase}` → coverage recalcula.

## Apologetics
- Fases `apologetics_*` (HIGH tier / Opus). Complementa respostas do usuário, não substitui (exceto quando pedido explicitamente).
- Migration `V20__create_apologetics.sql`.

## Concílios (11 fases)
1. `council_seed` — seed curado
2. `council_extract_schaff`, `council_extract_hefele`, `council_extract_catholic_enc`, `council_extract_fordham` — extractors primários
3. `council_extract_wikidata` — enriquecimento estruturado
4. `council_extract_wikipedia` — agregador
5. `council_consensus` — SourceConsensusEngine
6. `council_summaries` — resumos IA via LlmOrchestrator
7. `council_translate_all` — tradução concílios (pt/es)
8. `heresy_translate_all` — tradução heresias (pt/es)

## SourceConsensusEngine — Pesos
PRIMARY 1.0 (Schaff, Hefele, Fordham) > ACADEMIC 0.8 (Catholic Enc, Seed) > STRUCTURED 0.7 (Wikidata) > AGGREGATOR 0.5 (Wikipedia)

## LLM execution — 2 caminhos
1. **Assíncrono (padrão)**: `llm_prompt_queue` + `/run-llm` (Claude Code). Usado por bible/council/heresy/bio/patristic/dating/apologetics em massa.
2. **Síncrono (LlmOrchestrator legado)**: Anthropic → OpenAI → DeepSeek → OpenRouter. Endpoints request-scoped (ex: apologetics user-facing).

Ver `/run-llm` (tier → modelo) e `/drain-queue` (execução em massa).

## IngestionPhaseTracker
Rastreamento persistente via `IngestionPhaseRepository`. Recupera fases stuck no restart do servidor.

## Controle admin
- `POST /admin/{domain}/ingestion/run/{phase}` — fase individual
- `POST /admin/{domain}/ingestion/run` — fases selecionadas (JSON body `{phases:[]}`)
- `POST /admin/{domain}/ingestion/run-all` — todas
- `POST /admin/reset/{domain}` — limpar domínio
- `POST /admin/enrich-dating?domain=fathers&limit=50` — enriquecimento datas

## Frontend — Painéis de ingestão
`IngestionPhasePanel` (genérico) + `ManuscriptIngestionPanel`, `PatristicIngestionPanel`, `CouncilIngestionPanel` (específicos).
Hooks: `useIngestionPhases.ts`, `useIngestion.ts`

Analise o pedido do usuário: $ARGUMENTS
