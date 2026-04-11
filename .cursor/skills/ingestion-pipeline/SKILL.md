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

## Ingestao biblica — 13 fases

`BibleIngestionService` com `ALL_PHASES`:

1. Seeds: `bible_seed_versions`, `bible_seed_ot_books`, `bible_seed_abbreviations`
2. Texto: `bible_ingest_text_kjv`, `bible_ingest_text_aa`, `bible_ingest_text_acf`, `bible_ingest_text_arc69`
3. Interlinear: `bible_ingest_interlinear_nt`, `bible_ingest_interlinear_ot`
4. Lexico: `bible_ingest_lexicon_greek`, `bible_ingest_lexicon_hebrew`
5. Traducao LLM: `bible_translate_lexicon`, `bible_translate_glosses`

### Traducao de glosses (`processGlossResponse`)

Estrategia JSON-first: `tryParseJsonGlosses()` faz key-based matching (exato → normalizado → sem pontuacao). Se JSON match parcial, retorna apenas os matched — glosses faltantes ficam NULL para retry. Se conteudo parece JSON mas nao matchou nada, retorna vazio (nao cai em line-by-line). Line-by-line fallback somente para respostas plain-text.

### Filtro por livro/capitulo

`resources/bible-ingestion-filter.txt` controla escopo: `John:1-3` (caps 1 a 3), `ALL` (tudo), vazio = ALL.

### Limpeza

- `POST /admin/bible/glosses/clear` — limpa PT + ES + alinhamentos
- `POST /admin/bible/glosses/fix-corrupted` — limpa apenas PT corrompidos (fragmentos JSON)

## Tiers LLM nas fases de ingestão

Toda chamada LLM deve usar `llmOrchestrator.execute(request, TaskComplexity.X)`:
- **LOW** (`gpt-4.1-mini`) — glosses, traduções simples 1:1
- **MEDIUM** (`gpt-5.4`) — batches de léxico, enrichment multi-campo
- **HIGH** (`gpt-5.4`) — biografias, dating, conflitos, extração, alignment

Referência completa com labels e configuração: `docs/llm-concepts.md` seção "Sistema de Tiers".

## Controle manual

Admin: `POST /admin/{domain}/ingestion/run/{phase}` ou run all / run selected. `IngestionPhaseTracker` registra progresso por fase. Dominio `bible` usa `BibleIngestionService`.

## SourceConsensusEngine

Pesos por tipo de fonte: PRIMARY 1.0 (Schaff, Hefele, Fordham), ACADEMIC 0.8 (Catholic Enc, Seed), STRUCTURED 0.7 (Wikidata), AGGREGATOR 0.5 (Wikipedia).
