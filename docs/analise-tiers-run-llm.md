# Analise: /run-llm nao respeita sistema de tiers

Data: 2026-04-10

---

## 1. Problema

O skill `/run-llm` usa **claude-opus-4-6 para todos os itens da fila**, independente do tier (LOW/MEDIUM/HIGH) definido no backend. Isso significa que traducoes triviais de glosses (tier LOW) sao processadas pelo modelo mais pesado e lento disponivel.

---

## 2. Evidencia

### Tiers na fila (dados do banco)

```sql
SELECT tier, phase_name, COUNT(*) FROM llm_prompt_queue GROUP BY tier, phase_name;
```

| Fase | Tier | Itens | Complexidade |
|------|------|-------|-------------|
| bible_translate_glosses | **LOW** | 260 | Traducoes 1:1 simples |
| bible_translate_lexicon | **MEDIUM** | 272 | Batch de 80 entries lexico |
| bible_translate_hebrew_lexicon | **MEDIUM** | 206 | Batch de 80 entries lexico |
| bible_translate_enrichment_greek | **MEDIUM** | 11.046 | Enriquecimento multi-campo |
| bible_translate_enrichment_hebrew | **MEDIUM** | 17.294 | Enriquecimento multi-campo |
| **TOTAL** | | **29.078** | |

### Modelo usado nos itens aplicados

```sql
SELECT tier, model_used, COUNT(*) FROM llm_prompt_queue WHERE status = 'applied' GROUP BY tier, model_used;
```

| Tier | Modelo usado | Itens |
|------|-------------|-------|
| MEDIUM | claude-opus-4-6 | 3 |

**100% dos itens processados usaram Opus**, independente do tier.

### Causa raiz no SKILL.md

Arquivo: `.claude/skills/run-llm/SKILL.md`

**Linha 70** — nao diferencia modelo por tier:
```
Voce E o LLM — use seu proprio conhecimento para gerar a resposta
```

**Linha 76** — modelo hardcoded:
```json
"modelUsed":"claude-opus-4-6"
```

**Linha 107-108** — menciona tiers mas sem acao concreta:
```
Respeite o tier do item — items HIGH sao mais complexos
```

Nao ha nenhuma instrucao para usar modelos diferentes por tier.

---

## 3. Mapeamento de tiers: OpenAI → Claude

O sistema original (modo direto) usa tiers OpenAI:

| Tier | OpenAI original | Concurrency | Batch | Timeout | Uso |
|------|----------------|-------------|-------|---------|-----|
| LOW | gpt-4.1-mini | 80 | 150 | 30s | Glosses, traducoes simples 1:1 |
| MEDIUM | gpt-4.1 | 40 | 80 | 60s | Batches lexico, enrichment, bios, traducoes |
| HIGH | gpt-5.4 | 15 | 20 | 120s | Dating, alignment interlinear, apologetica |

O mapeamento equivalente para Claude:

| Tier | OpenAI | Claude equivalente | Model ID |
|------|--------|-------------------|----------|
| LOW | gpt-4.1-mini | **Haiku 4.5** | claude-haiku-4-5 |
| MEDIUM | gpt-4.1 | **Sonnet 4.6** | claude-sonnet-4-6 |
| HIGH | gpt-5.4 | **Opus 4.6** | claude-opus-4-6 |

---

## 4. Impacto

### Velocidade

| Modelo | Velocidade relativa | Output tokens/s (estimativa) |
|--------|-------------------|------------------------------|
| Haiku 4.5 | **5-10x mais rapido** que Opus | ~150-200 tok/s |
| Sonnet 4.6 | **2-3x mais rapido** que Opus | ~80-120 tok/s |
| Opus 4.6 | baseline | ~30-50 tok/s |

### Throughput atual vs potencial

Usando Opus para tudo (atual):
- ~3 itens completados por ciclo de 2min
- ~1.5 batches/min
- ETA para 29.078 itens: **~320 horas**

Usando modelo correto por tier (proposto):
- LOW (Haiku): ~10-15 itens/min → 260 glosses em **~20min**
- MEDIUM (Sonnet): ~5-8 itens/min → 28.818 itens em **~60-96 horas**
- HIGH (Opus): manter para tarefas complexas

### Overkill

| Fase | Tier | O que faz | Por que Opus e desnecessario |
|------|------|-----------|------------------------------|
| bible_translate_glosses | LOW | Traduz "love" → "amor" | Traducao 1:1 trivial, Haiku resolve |
| bible_translate_lexicon | MEDIUM | Traduz batch de 80 definicoes | Traducao estruturada, Sonnet resolve |
| bible_translate_enrichment_* | MEDIUM | Traduz campos de enriquecimento | Traducao multi-campo, Sonnet resolve |

---

## 5. Correcao implementada

### SKILL.md atualizado (`.claude/skills/run-llm/SKILL.md`)

Mapeamento tier + phaseName → modelo + effort:

| Tier | Fases | Modelo | Effort | Justificativa |
|------|-------|--------|--------|---------------|
| LOW | `bible_translate_glosses` | **Haiku** | Minimo | Traducao 1:1 trivial (love→amor) |
| MEDIUM | `bible_translate_enrichment_*` | **Haiku** | Minimo | Sys prompt 103 chars, "translate KEY: VALUE" — LOW disfarçado |
| MEDIUM | `bible_translate_lexicon`, `*_hebrew_lexicon` | **Sonnet** | Medio | Definicoes longas estruturadas, batch 80 entries |
| MEDIUM | `council_*`, `heresy_*`, `bio_*` | **Sonnet** | Medio | Traducao academica com terminologia eclesiastica |
| HIGH | `bible_align_*` | **Opus** | Alto | 255+ linhas de regras, scoring posicional, morfologia |
| HIGH | `dating_*` | **Opus** | Alto | Domain expertise paleografia/patristica |

### Ordem de processamento

1. LOW (Haiku) — mais rapido, resolve primeiro
2. MEDIUM enrichment (Haiku) — volume alto (28k), rapido
3. MEDIUM lexicon/outros (Sonnet) — medio
4. HIGH (Opus) — complexo, por ultimo

### modelUsed agora reporta o modelo real

- `claude-haiku-4-5` para LOW + enrichment
- `claude-sonnet-4-6` para MEDIUM lexicon/bios/councils
- `claude-opus-4-6` para HIGH

---

## 6. Resumo

| Aspecto | Antes | Depois |
|---------|-------|--------|
| Modelo LOW | claude-opus-4-6 | **claude-haiku-4-5** |
| Modelo MEDIUM enrichment | claude-opus-4-6 | **claude-haiku-4-5** |
| Modelo MEDIUM lexicon | claude-opus-4-6 | **claude-sonnet-4-6** |
| Modelo HIGH | claude-opus-4-6 | claude-opus-4-6 (mantido) |
| Throughput LOW | ~1.5/min | **~10-15/min** (6-10x) |
| Throughput MEDIUM enrichment | ~1.5/min | **~10-15/min** (6-10x) |
| Throughput MEDIUM lexicon | ~1.5/min | **~5-8/min** (3-5x) |
| modelUsed reportado | hardcoded "opus" | modelo real usado |
| ETA 29k itens | ~320h | **~30-50h** |
