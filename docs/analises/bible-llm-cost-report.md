# Relatório de Chamadas LLM — Ingestão Bíblica

> Gerado em: 2026-04-16  
> Contexto: Após reset completo da Bíblia em 2026-04-11. Todas as tabelas bible_* estão zeradas.

---

## Preços de Referência (por 1M tokens)

| Modelo | Input | Output | Total médio | vs baseline |
|--------|-------|--------|-------------|-------------|
| GPT-4.1 mini | $0.40 | $1.60 | $2.00 | baseline |
| DeepSeek V3 | $0.14 | $0.28 | $0.42 | ~5x mais barato |
| Groq (LLaMA/Mixtral) | $0.20 | $0.50 | $0.70 | ~3x mais barato |

> **Nota:** O projeto usa `/run-llm` com assinatura Claude Code (custo marginal $0). Estes custos representam o **custo equivalente** caso as chamadas fossem feitas via API paga (OpenAI, DeepSeek ou Groq).

---

## As 4 Camadas de Ingestão Bíblica

```
CAMADA 1 — Fundação (0 chamadas LLM)
  bible_seed_versions, bible_seed_books, bible_seed_abbreviations
  bible_ingest_text_kjv/aa/acf/arc69

CAMADA 2 — Interlinear + Léxico (0 chamadas LLM)
  bible_ingest_nt_interlinear (260 capítulos TAGNT)
  bible_ingest_ot_interlinear (929 capítulos)
  bible_ingest_greek_lexicon (~5.600 entradas Strong's)
  bible_ingest_hebrew_lexicon (~8.700 entradas Strong's)
  bible_fill_missing_hebrew (BibleHub scraping)

CAMADA 3 — Tradução LLM: Glosses + Léxico  ← 738 chamadas API
  bible_translate_glosses        [LOW  → Haiku]
  bible_translate_lexicon        [MED  → Sonnet]
  bible_translate_hebrew_lexicon [MED  → Sonnet]

CAMADA 4 — Alinhamento de Palavras          ← 15.912 chamadas API (NT)
  bible_align_kjv                [HIGH → Opus]
  bible_align_arc69              [HIGH → Opus]
  bible_align_hebrew_kjv         [HIGH → Opus, OT opcional]
  bible_align_hebrew_arc69       [HIGH → Opus, OT opcional]

ENRICHMENT (paralelo à Camada 4)            ← 28.340 chamadas API
  bible_translate_enrichment_greek  [MED → Haiku]
  bible_translate_enrichment_hebrew [MED → Haiku]
```

---

## Dados de Volume (verificados no banco, pré-reset 2026-04-10)

| Fase | Tipo | Chamadas reais na fila | Tier | Modelo /run-llm |
|------|------|------------------------|------|-----------------|
| `bible_translate_glosses` | Batch (~80 glosses/call) | **260** | LOW | Haiku 4.5 |
| `bible_translate_lexicon` | Batch (~80 entries/call) | **272** | MEDIUM | Sonnet 4.6 |
| `bible_translate_hebrew_lexicon` | Batch (~80 entries/call) | **206** | MEDIUM | Sonnet 4.6 |
| `bible_translate_enrichment_greek` | Individual (1 entry/call) | **11.046** | MEDIUM | Haiku 4.5 |
| `bible_translate_enrichment_hebrew` | Individual (1 entry/call) | **17.294** | MEDIUM | Haiku 4.5 |
| `bible_align_kjv` | Individual (1 versículo/call) | **7.956** | HIGH | Opus 4.6 |
| `bible_align_arc69` | Individual (1 versículo/call) | **7.956** | HIGH | Opus 4.6 |
| `bible_align_hebrew_kjv` *(OT)* | Individual | **23.145** | HIGH | Opus 4.6 |
| `bible_align_hebrew_arc69` *(OT)* | Individual | **23.145** | HIGH | Opus 4.6 |

> **Fonte:** `analise-tiers-run-llm.md` (dados SQL da `llm_prompt_queue` em 2026-04-10).  
> Fases de alinhamento OT calculadas a partir de 23.145 versículos do AT.

---

## Estimativa de Tokens por Chamada

| Fase | Tokens Input | Tokens Output | Observação |
|------|-------------|---------------|------------|
| `translate_glosses` | ~500 | ~400 | System prompt ~180 + 80 glosses × 4 tok |
| `translate_lexicon` | ~900 | ~450 | System prompt ~200 + 80 defs × ~8 tok |
| `translate_hebrew_lexicon` | ~950 | ~450 | Hebraico tem definições mais longas |
| `enrichment_greek/hebrew` | ~200 | ~150 | Chamada individual "translate KEY: VALUE" |
| `align_kjv` | ~2.500 | ~500 | System prompt ~2.000 (255 linhas de regras) + versículo |
| `align_arc69` | ~2.600 | ~500 | Português requer análise de contrações |
| `align_hebrew_*` | ~2.400 | ~500 | OT hebraico |

---

---

# RELATÓRIO 1: O QUE FALTA (Estado Atual — 2026-04-16)

> **Contexto:** Reset completo em 2026-04-11. Todas as fases bible_* estão zeradas.  
> As camadas 1 e 2 (seeds + scraping) são rápidas e provavelmente já foram re-executadas.  
> **As 7 fases LLM abaixo estão 100% pendentes.**

## Chamadas pendentes

| # | Fase | Camada | Tier | Chamadas | Status |
|---|------|--------|------|----------|--------|
| 1 | `bible_translate_glosses` | 3 | LOW | **260** | Pendente |
| 2 | `bible_translate_lexicon` | 3 | MEDIUM | **272** | Pendente |
| 3 | `bible_translate_hebrew_lexicon` | 3 | MEDIUM | **206** | Pendente |
| 4 | `bible_translate_enrichment_greek` | Enrich | MEDIUM | **11.046** | Pendente |
| 5 | `bible_translate_enrichment_hebrew` | Enrich | MEDIUM | **17.294** | Pendente |
| 6 | `bible_align_kjv` | 4 | HIGH | **7.956** | Pendente |
| 7 | `bible_align_arc69` | 4 | HIGH | **7.956** | Pendente |
| — | **TOTAL NT (sem OT)** | | | **44.990** | |
| 8 | `bible_align_hebrew_kjv` *(OT)* | 4 | HIGH | **23.145** | Opcional |
| 9 | `bible_align_hebrew_arc69` *(OT)* | 4 | HIGH | **23.145** | Opcional |
| — | **TOTAL NT + OT** | | | **91.280** | |

## Tokens pendentes (NT, sem OT)

| Fase | Chamadas | Total Input | Total Output |
|------|----------|-------------|--------------|
| `translate_glosses` | 260 | 130.000 | 104.000 |
| `translate_lexicon` | 272 | 244.800 | 122.400 |
| `translate_hebrew_lexicon` | 206 | 195.700 | 92.700 |
| `enrichment_greek` | 11.046 | 2.209.200 | 1.656.900 |
| `enrichment_hebrew` | 17.294 | 3.458.800 | 2.594.100 |
| `align_kjv` | 7.956 | 19.890.000 | 3.978.000 |
| `align_arc69` | 7.956 | 20.685.600 | 3.978.000 |
| **TOTAL** | **44.990** | **46.814.100** | **12.526.100** |

> ~46,8M tokens de input / ~12,5M tokens de output

## Custo pendente por modelo (NT, sem OT)

| Fase | Chamadas | GPT-4.1 mini | DeepSeek V3 | Groq |
|------|----------|-------------|-------------|------|
| `translate_glosses` | 260 | $0.22 | $0.05 | $0.08 |
| `translate_lexicon` | 272 | $0.29 | $0.07 | $0.10 |
| `translate_hebrew_lexicon` | 206 | $0.23 | $0.05 | $0.08 |
| `enrichment_greek` | 11.046 | $3.53 | $0.77 | $1.27 |
| `enrichment_hebrew` | 17.294 | $5.53 | $1.21 | $1.99 |
| `align_kjv` | 7.956 | $14.12 | $3.67 | $5.97 |
| `align_arc69` | 7.956 | $14.69 | $3.81 | $6.21 |
| **TOTAL** | **44.990** | **$38.61** | **$9.63** | **$15.70** |

### Custo pendente com OT incluído

| Escopo | GPT-4.1 mini | DeepSeek V3 | Groq |
|--------|-------------|-------------|------|
| Faltando NT | $38.61 | $9.63 | $15.70 |
| Faltando OT (alignment) | +$55.49 | +$14.39 | +$23.43 |
| **Faltando NT + OT** | **$94.10** | **$24.02** | **$39.13** |

---

---

# RELATÓRIO 2: ESTADO ZERO (Tudo do Início, sem nada processado)

> **Contexto:** Como se a Bíblia jamais tivesse sido ingerida.  
> Inclui todas as chamadas das 7 fases LLM, do zero absoluto.

> **Nota:** Os valores são idênticos ao Relatório 1 porque o reset foi completo.  
> Este relatório serve como referência permanente para futuras re-ingestões.

## Visão geral por camada

| Camada | Fases LLM | Chamadas | Tipo de tarefa |
|--------|-----------|----------|----------------|
| Camada 1 (Fundação) | 0 | 0 | Seed + scraping |
| Camada 2 (Interlinear/Léxico) | 0 | 0 | Scraping |
| Camada 3 (Tradução Léxico/Glosses) | 3 | 738 | Batch translation |
| Camada 4 (Alinhamento NT) | 2 | 15.912 | Per-verse alignment |
| Camada 4 (Alinhamento OT, opcional) | 2 | 46.290 | Per-verse alignment |
| Enrichment (paralelo) | 2 | 28.340 | Individual translation |

## Detalhamento por fase — Estado Zero

### Camada 3: Tradução (738 chamadas)

| Fase | Tier | Batch | Chamadas | Input/call | Output/call |
|------|------|-------|----------|------------|-------------|
| `translate_glosses` | LOW (Haiku) | ~80/call | 260 | ~500 tok | ~400 tok |
| `translate_lexicon` | MED (Sonnet) | ~80/call | 272 | ~900 tok | ~450 tok |
| `translate_hebrew_lexicon` | MED (Sonnet) | ~80/call | 206 | ~950 tok | ~450 tok |
| **Subtotal Camada 3** | | | **738** | | |

### Camada 4: Alinhamento — NT (15.912 chamadas)

| Fase | Versículos | Chamadas | Input/call | Output/call |
|------|-----------|----------|------------|-------------|
| `align_kjv` | 7.956 NT | 7.956 | ~2.500 tok | ~500 tok |
| `align_arc69` | 7.956 NT | 7.956 | ~2.600 tok | ~500 tok |
| **Subtotal NT** | | **15.912** | | |

### Camada 4: Alinhamento — OT (46.290 chamadas, opcional)

| Fase | Versículos | Chamadas | Input/call | Output/call |
|------|-----------|----------|------------|-------------|
| `align_hebrew_kjv` | 23.145 OT | 23.145 | ~2.400 tok | ~500 tok |
| `align_hebrew_arc69` | 23.145 OT | 23.145 | ~2.400 tok | ~500 tok |
| **Subtotal OT** | | **46.290** | | |

### Enrichment: Tradução Individual (28.340 chamadas)

| Fase | Entradas | Chamadas | Input/call | Output/call |
|------|---------|----------|------------|-------------|
| `enrichment_greek` (PT+ES) | 5.523 × 2 | 11.046 | ~200 tok | ~150 tok |
| `enrichment_hebrew` (PT+ES) | 8.647 × 2 | 17.294 | ~200 tok | ~150 tok |
| **Subtotal Enrichment** | | **28.340** | | |

## Tokens — Estado Zero (NT completo + enrichment)

| Grupo | Chamadas | Total Input | Total Output |
|-------|----------|-------------|--------------|
| Camada 3 (tradução) | 738 | 570.500 | 318.100 |
| Camada 4 NT (alignment) | 15.912 | 40.575.600 | 7.956.000 |
| Enrichment | 28.340 | 5.668.000 | 4.251.000 |
| **TOTAL NT + enrichment** | **44.990** | **46.814.100** | **12.525.100** |

| Grupo | Chamadas | Total Input | Total Output |
|-------|----------|-------------|--------------|
| NT + enrichment (acima) | 44.990 | 46.814.100 | 12.525.100 |
| OT alignment (opcional) | 46.290 | 110.796.000 | 23.145.000 |
| **TOTAL NT + OT + enrichment** | **91.280** | **157.610.100** | **35.670.100** |

## Custo — Estado Zero (3 modelos, NT)

### Por fase

| Fase | Chamadas | GPT-4.1 mini | DeepSeek V3 | Groq |
|------|----------|-------------|-------------|------|
| `translate_glosses` | 260 | $0.22 | $0.05 | $0.08 |
| `translate_lexicon` | 272 | $0.29 | $0.07 | $0.10 |
| `translate_hebrew_lexicon` | 206 | $0.23 | $0.05 | $0.08 |
| `enrichment_greek` | 11.046 | $3.53 | $0.77 | $1.27 |
| `enrichment_hebrew` | 17.294 | $5.53 | $1.21 | $1.99 |
| `align_kjv` | 7.956 | $14.12 | $3.67 | $5.97 |
| `align_arc69` | 7.956 | $14.69 | $3.81 | $6.21 |
| **TOTAL NT** | **44.990** | **$38.61** | **$9.63** | **$15.70** |

### Resumo por escopo

| Escopo | Chamadas | GPT-4.1 mini | DeepSeek V3 | Groq | Economia vs mini |
|--------|----------|-------------|-------------|------|-----------------|
| Só Camada 3 (traduções) | 738 | $0.74 | $0.17 | $0.26 | — |
| Só Camada 4 NT (alignment) | 15.912 | $28.81 | $7.48 | $12.18 | — |
| Só Enrichment | 28.340 | $9.06 | $1.98 | $3.26 | — |
| **NT completo + enrichment** | **44.990** | **$38.61** | **$9.63** | **$15.70** | DS: ~4x / Groq: ~2.5x |
| **NT + OT + enrichment** | **91.280** | **$101.64** | **$26.14** | **$42.57** | DS: ~4x / Groq: ~2.4x |

---

## Detalhamento de cálculo

### Cálculo por fase (fórmula)

```
custo = chamadas × (input_tokens/1.000.000 × preço_input + output_tokens/1.000.000 × preço_output)
```

### translate_glosses (260 chamadas)
| Modelo | Input | Output | Total |
|--------|-------|--------|-------|
| GPT-4.1 mini | 260 × 0.500k × $0.40/M = $0.052 | 260 × 0.400k × $1.60/M = $0.166 | **$0.22** |
| DeepSeek V3 | 260 × 0.500k × $0.14/M = $0.018 | 260 × 0.400k × $0.28/M = $0.029 | **$0.05** |
| Groq | 260 × 0.500k × $0.20/M = $0.026 | 260 × 0.400k × $0.50/M = $0.052 | **$0.08** |

### align_kjv + align_arc69 (15.912 chamadas)
| Modelo | Input | Output | Total |
|--------|-------|--------|-------|
| GPT-4.1 mini | 15.912 × 2.550k avg × $0.40/M = $16.23 | 15.912 × 0.500k × $1.60/M = $12.73 | **$28.81** |
| DeepSeek V3 | 15.912 × 2.550k × $0.14/M = $5.68 | 15.912 × 0.500k × $0.28/M = $2.23 | **$7.48** |
| Groq | 15.912 × 2.550k × $0.20/M = $8.11 | 15.912 × 0.500k × $0.50/M = $3.98 | **$12.18** |

### enrichment_greek + hebrew (28.340 chamadas)
| Modelo | Input | Output | Total |
|--------|-------|--------|-------|
| GPT-4.1 mini | 28.340 × 0.200k × $0.40/M = $2.27 | 28.340 × 0.150k × $1.60/M = $6.80 | **$9.06** |
| DeepSeek V3 | 28.340 × 0.200k × $0.14/M = $0.79 | 28.340 × 0.150k × $0.28/M = $1.19 | **$1.98** |
| Groq | 28.340 × 0.200k × $0.20/M = $1.13 | 28.340 × 0.150k × $0.50/M = $2.13 | **$3.26** |

---

## Tempo estimado de execução (via /run-llm)

| Fase | Chamadas | Modelo | Throughput estimado | Tempo estimado |
|------|----------|--------|---------------------|----------------|
| `translate_glosses` | 260 | Haiku | ~10-15/min | ~18-26 min |
| `translate_lexicon` | 272 | Sonnet | ~5-8/min | ~34-54 min |
| `translate_hebrew_lexicon` | 206 | Sonnet | ~5-8/min | ~26-41 min |
| `enrichment_greek` | 11.046 | Haiku | ~10-15/min | ~12-18 horas |
| `enrichment_hebrew` | 17.294 | Haiku | ~10-15/min | ~19-29 horas |
| `align_kjv` | 7.956 | Opus | ~1-2/min | ~66-133 horas |
| `align_arc69` | 7.956 | Opus | ~1-2/min | ~66-133 horas |

> **Gargalo:** O alinhamento (Opus, HIGH tier) domina o tempo total.  
> Com `/run-llm` rodando continuamente, estimativa de **~6-12 dias** para NT completo.  
> O enrichment (28.340 chamadas Haiku) leva ~30-47 horas mas pode rodar em paralelo.

---

## Distribuição das chamadas por tier

```
TOTAL NT + enrichment: 44.990 chamadas

LOW  (Haiku)   — 260 chamadas    —  0.6%   [glosses]
MED  (Sonnet)  — 478 chamadas    —  1.1%   [lexicon grego + hebraico]
MED  (Haiku)   — 28.340 chamadas — 63.0%   [enrichment grego + hebraico]
HIGH (Opus)    — 15.912 chamadas — 35.4%   [alignment kjv + arc69]

Por custo (GPT-4.1 mini):
  Camada 3: $0.74  (  1.9%)
  Enrichment: $9.06  ( 23.5%)
  Camada 4: $28.81  ( 74.6%)   ← alinhamento domina o custo
```

---

## Conclusão

| | NT completo | NT + OT |
|---|-------------|---------|
| **Chamadas totais** | 44.990 | 91.280 |
| **Input total** | ~46,8M tokens | ~157,6M tokens |
| **Output total** | ~12,5M tokens | ~35,7M tokens |
| **GPT-4.1 mini** | **$38.61** | **$101.64** |
| **DeepSeek V3** | **$9.63** | **$26.14** |
| **Groq** | **$15.70** | **$42.57** |
| **Claude Code /run-llm** | **$0.00** | **$0.00** |

> O custo via `/run-llm` é zero (assinatura fixa Claude Code).  
> As estimativas acima são o **custo equivalente** caso se use API paga como alternativa.  
> **O alinhamento (Camada 4) representa ~75% do custo total** — é o único componente que justificaria considerar modelos alternativos.
