Você está trabalhando no pipeline de ingestão do Manuscriptum Atlas. Siga estas regras:

## Manuscritos
`IngestionOrchestrator` → NTVMR API → parse TEI/XML → persist. Retry com backoff.
Fases: `manuscript_load_catalog`, `manuscript_fetch_content`, `manuscript_link_verses`.

## Patrística (5 fases)
`patristic_seed_fathers` → `patristic_seed_statements` → `patristic_translate_fathers` → `patristic_translate_statements` → `patristic_translate_biographies`

Filtro opcional: `?filter=normalized_name` (ex: `?filter=clement_of_rome`)

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

## LlmOrchestrator
Anthropic Claude Opus 4.6 (primário) → OpenAI GPT-5.4 (fallback). Rate limiting por provider. Configuração via env vars.

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
