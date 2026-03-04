---
name: ingestion-pipeline
description: Multi-phase ingestion pipelines for manuscripts, patristic data, and councils. Use when working on ingestão, ingestion, scraper, pipeline, fases, or enriquecimento.
---

# Pipeline de Ingestão

## Ingestão de manuscritos

`IngestionOrchestrator` — NTVMR API → parse TEI/XML → persist. Retry com backoff para chamadas externas.

## Ingestão patrística

Seed curado + enrichment: biografias (OpenAI), datação (NTVMR/seed/OpenAI), traduções (machine). `PatristicIngestionService.translateBiographies()`.

## Ingestão de concílios — 11 fases

`ALL_PHASES` em `CouncilIngestionService` define a ordem:

1. council_seed — seed curado
2. council_extract_schaff, hefele, catholic_enc, fordham — extractors primários
3. council_extract_wikidata — enriquecimento estruturado
4. council_extract_wikipedia — agregador
5. council_consensus — SourceConsensusEngine
6. council_summaries — resumos IA
7. council_translate_all — tradução concílios (pt/es)
8. heresy_translate_all — tradução heresias (pt/es)

## Controle manual

Admin: `POST /admin/councils/ingestion/run/{phase}` ou run all / run selected. `CouncilPhaseTracker` registra progresso por fase.

## SourceConsensusEngine

Pesos por tipo de fonte: PRIMARY 1.0 (Schaff, Hefele, Fordham), ACADEMIC 0.8 (Catholic Enc, Seed), STRUCTURED 0.7 (Wikidata), AGGREGATOR 0.5 (Wikipedia).
