# Plano de Correção — Gaps da LLM Queue (Ingestão Bíblica)

## Context

A verificação anterior (`~/.claude/plans/image-1-fa-a-uma-groovy-diffie.md`) confirmou que a arquitetura prepare → queue → /run-llm → apply está íntegra, mas identificou três gaps nas fases já executadas:

- **P1 — `bible_translate_hebrew_lexicon` (PT)**: 1.241 entries hebraicas com base preenchida mas sem `short_definition` em `hebrew_lexicon_translations.locale='pt'`.
- **P2 — `bible_translate_glosses`**: 59.090 palavras gregas sem `portuguese_gloss` e 59.572 sem `spanish_gloss` em `interlinear_words`.
- **P3 — Observabilidade**: todos os items `bible_*` têm `input_tokens = 0` e `output_tokens = 0`.

**Restrição crítica**: não perder nada do que já foi ingerido. Todas as correções são incrementais — o histórico (rows em `llm_prompt_queue`, translations já preenchidas, glosses já aplicados) permanece intacto.

---

## Root cause confirmado

### P1 — Truncamento do LLM em PT

Evidência empírica (amostra do gap, strongs `H0418`, id=68942):

```
batch id=62972  | LEXICON_BATCH_Hebrew_80  | locale=pt | resp_len=281 bytes
batch id=63075  | LEXICON_BATCH_Hebrew_80  | locale=es | resp_len=16659 bytes  OK
batch id=94421  | LEXICON_BATCH_Hebrew_10  | locale=pt | resp_len=300 bytes  (retry tambem truncado)
```

O batch PT de 80 entries retornou apenas 3 traduções (H0415, H0416, H0417) antes de ser cortado. Um retry em batch menor de 10 entries também truncou.

Distribuição dos truncamentos:

| Locale | Label | Batches | Truncados (<500B) |
|--------|-------|---------|-------------------|
| pt | LEXICON_BATCH_Hebrew_80 | 97 | 25 (26%) |
| pt | LEXICON_BATCH_Hebrew_10 | 236 | 50 (21%) |
| es | LEXICON_BATCH_Hebrew_80 | 5 | 0 |
| es | LEXICON_BATCH_Hebrew_10 | 778 | 0 |

**75 batches PT truncados** contra **1 ES**. O Haiku (`claude-haiku-4-5`) corta a geração em PT antes de completar as 80 entries — provavelmente porque PT usa mais tokens por definição biblica que ES (artigos, preposições, conjugações).

### P2 — Chave de agrupamento + max_tokens apertado

O `translateGlossesPrepare` (`BibleIngestionService.kt:1590-1659`) agrupa por capítulo e usa chaves `(transliteration, morphology, english_gloss, lemma)` com `chunked(80)` e `maxTokens = chunk.size * 20` (= 1.600 para chunk cheio).

Verificado:
- `hasTranslation` / `hasHebrewTranslation` e o filtro em `translateGlossesPrepare` já checam `short_definition IS NOT NULL` / `portugueseGloss.isNullOrBlank()` → **são idempotentes**.
- Gap disperso por todo NT (Matthew cap.10 = 4% cobertura PT, Mark cap.10 = 21%, etc.) — não é run parcial; é truncamento replicado por capítulo.
- `max_tokens = 1600` é apertado para chunks de 80 glosses em PT (glosses como "sacerdote principal", "filho do homem" consomem muitos tokens).

### P3 — Skill não propaga uso

O skill `/run-llm` chama `POST /admin/llm/queue/{id}/complete` com `QueueCompleteRequest` mas não popula `inputTokens`/`outputTokens`. Correção é no SKILL, não no schema.

---

## Estratégia geral (não-destrutiva)

1. **Preservar histórico**: nenhum `DELETE` na `llm_prompt_queue` nem em `hebrew_lexicon_translations` / `interlinear_words` / etc.
2. **Re-enfileirar apenas gaps**: rodar os `prepare` novamente — eles já são idempotentes (filtram o que falta).
3. **Mitigar truncamento**: reduzir chunk size e aumentar `maxTokens` antes de re-rodar as fases problemáticas.
4. **Aplicar em ordem**: P3 (código do skill) → P1 → P2, para que P1/P2 já gerem telemetria de tokens.
5. **Verificar antes e depois**: query de baseline salva, query pós-apply comparada.

---

## Passo a passo

### Passo 0 — Snapshot de baseline (antes de mexer)

```sql
-- Em nt_coverage
SELECT phase_name, status, COUNT(*)
FROM llm_prompt_queue
WHERE phase_name LIKE 'bible\_%' ESCAPE '\'
GROUP BY phase_name, status;

-- Em bible_db
SELECT
  (SELECT COUNT(*) FROM hebrew_lexicon_translations WHERE locale='pt' AND short_definition IS NOT NULL) AS hebrew_pt,
  (SELECT COUNT(*) FROM hebrew_lexicon_translations WHERE locale='es' AND short_definition IS NOT NULL) AS hebrew_es,
  (SELECT COUNT(*) FROM interlinear_words WHERE language='greek' AND portuguese_gloss IS NOT NULL) AS gloss_pt,
  (SELECT COUNT(*) FROM interlinear_words WHERE language='greek' AND spanish_gloss IS NOT NULL)    AS gloss_es;
```

Esperado (baseline): hebrew_pt=6.940 · hebrew_es=8.181 · gloss_pt=82.432 · gloss_es=81.950.

### Passo 1 — Fix P3 (tokens no skill)

Arquivo: `.claude/skills/run-llm/SKILL.md`

Garantir que o fluxo do skill capture `usage.input_tokens` e `usage.output_tokens` da resposta do LLM e envie no body do `POST /admin/llm/queue/{id}/complete`:

```json
{
  "responseContent": "...",
  "modelUsed": "claude-haiku-4-5",
  "inputTokens": 1234,
  "outputTokens": 567
}
```

O endpoint `POST /admin/llm/queue/{id}/complete` já aceita esses campos (ver `LlmQueueRepository.markCompleted(id, responseContent, modelUsed, inputTokens, outputTokens)`).

Efeito: todos os items re-enfileirados nos próximos passos terão uso real. Histórico antigo (33.478 items) continua com zero — OK, não perdemos nada, só não temos o dado retroativo.

### Passo 2 — Reduzir janela de truncamento (P1 + P2)

Antes de re-enfileirar, ajustar os prepares para evitar o mesmo truncamento.

**Arquivo**: `backend/src/main/kotlin/com/ntcoverage/service/BibleIngestionService.kt`

**P1 — `translateLexiconPrepare` (linhas 1317-1378)**:
- Atual: `llmConfig.mediumBatchSize` (10–80) + `maxTokens = 8000`, tier `MEDIUM`.
- Ajuste: reduzir `chunked(...)` para **máx 25 entries por batch** quando `locale='pt'` E `lexiconType='hebrew'`. Manter 80 para ES (sem problema histórico).
- Justificativa: 25 × (~320 tokens/entry PT média) ≈ 8.000 tokens, deixa margem.

**P2 — `translateGlossesPrepare` (linha 1615, 1648)**:
- Atual: `chunked(80)`, `maxTokens = chunk.size * 20`.
- Ajuste: `chunked(50)`, `maxTokens = chunk.size * 30`. Total 50 × 30 = 1.500 tokens — margem para glosses PT longas.

Essas mudanças são aditivas; nenhuma assinatura pública muda.

### Passo 3 — Rodar `bible_translate_hebrew_lexicon` (re-enfileirar 1.241 faltantes)

```bash
# Backend local, autenticado com JWT admin (ver memoria reference_backend_api_auth.md)
curl -X POST http://localhost:8080/admin/bible/ingestion/run/bible_translate_hebrew_lexicon \
  -H "Authorization: Bearer $TOKEN"
```

Efeito do prepare (idempotente):
- Filtra entries hebraicas com base (short/full_definition) **sem** tradução PT (via `hasHebrewTranslation` que já checa `shortDefinition IS NOT NULL`, linha 301-307 `LexiconRepository.kt`).
- ES não será re-enfileirado (já 100% traduzido).
- Enfileira ~1.241 entries em batches de 25 = ~50 novos items na fila.

Aguardar `/run-llm` processar (tier MEDIUM) → fila pending → processing → completed.

Aplicar:
```bash
curl -X POST http://localhost:8080/admin/llm/queue/apply/bible_translate_hebrew_lexicon \
  -H "Authorization: Bearer $TOKEN"
```

Verificação parcial:
```sql
-- Esperado: hebrew_pt ~= 8.181 (up de 6.940)
SELECT COUNT(*) FROM hebrew_lexicon_translations WHERE locale='pt' AND short_definition IS NOT NULL;
```

Se algum batch truncar de novo: reduzir para `chunked(10)` e repetir (é idempotente).

### Passo 4 — Rodar `bible_translate_glosses` (re-enfileirar 59k faltantes)

```bash
curl -X POST http://localhost:8080/admin/bible/ingestion/run/bible_translate_glosses \
  -H "Authorization: Bearer $TOKEN"
```

Efeito:
- Itera livros NT + capítulos; para cada, seleciona palavras com `portuguese_gloss IS NULL` (linha 1600) → enfileira chunks de 50 para ambos locales.
- Itens já traduzidos não são re-enfileirados.
- Volume esperado: ~59k palavras / ~17k transliterations únicas → algumas centenas de chunks por locale.

Aguardar processamento → aplicar:
```bash
curl -X POST http://localhost:8080/admin/llm/queue/apply/bible_translate_glosses \
  -H "Authorization: Bearer $TOKEN"
```

### Passo 5 — Verificação end-to-end

```sql
-- Em bible_db: cobertura final
SELECT
  'hebrew_lexicon_translations PT' AS item,
  COUNT(*) AS total,
  COUNT(*) FILTER (WHERE locale='pt' AND short_definition IS NOT NULL) AS filled
FROM hebrew_lexicon_translations
UNION ALL
SELECT
  'interlinear_words PT',
  COUNT(*),
  COUNT(portuguese_gloss)
FROM interlinear_words WHERE language='greek'
UNION ALL
SELECT
  'interlinear_words ES',
  COUNT(*),
  COUNT(spanish_gloss)
FROM interlinear_words WHERE language='greek';
```

Critério de sucesso:
- `hebrew_pt` ≥ 8.181 (era 6.940 — fecha o gap de 1.241)
- `gloss_pt` próximo de 141.522 (ideal > 140.000, tolerância para palavras duplicadas com tokenização diferente)
- `gloss_es` próximo de 141.522

```sql
-- Em nt_coverage: integridade da fila
SELECT phase_name, status, COUNT(*)
FROM llm_prompt_queue
WHERE phase_name IN ('bible_translate_hebrew_lexicon','bible_translate_glosses')
GROUP BY phase_name, status;

-- Também: confirmar tokens não-zero nos novos items
SELECT phase_name, MIN(input_tokens), AVG(input_tokens)::int, MAX(output_tokens)
FROM llm_prompt_queue
WHERE phase_name IN ('bible_translate_hebrew_lexicon','bible_translate_glosses')
  AND created_at > NOW() - INTERVAL '1 day';
```

---

## Rollback

Nenhum passo é destrutivo. Se algo sair errado:

- **Abortar enfileiramento**: `DELETE FROM llm_prompt_queue WHERE phase_name=X AND created_at > SNAPSHOT_TIME`. Só mata os items novos; histórico preservado.
- **Reverter aplicação PT hebraico**: não é necessário. As rows antigas estavam vazias. Se algum upsert preencher com conteúdo ruim, basta corrigir o LLM output e re-aplicar — `upsertHebrewTranslation` é idempotente.
- **Ajustes de código (Passo 2)**: reverter no git sem afetar dados.

---

## Pontos críticos no código

- `backend/src/main/kotlin/com/ntcoverage/service/BibleIngestionService.kt:1317-1378` — `translateLexiconPrepare` (P1, ajustar batch size PT)
- `backend/src/main/kotlin/com/ntcoverage/service/BibleIngestionService.kt:1590-1659` — `translateGlossesPrepare` (P2, ajustar chunk e maxTokens)
- `backend/src/main/kotlin/com/ntcoverage/repository/LexiconRepository.kt:201-207, 301-307` — `hasTranslation`/`hasHebrewTranslation` (já corretos, idempotência garantida)
- `backend/src/main/kotlin/com/ntcoverage/service/LlmResponseProcessor.kt:150-184` — `applyLexiconBatch` (não mexer, handler já funciona)
- `backend/src/main/kotlin/com/ntcoverage/service/LlmResponseProcessor.kt:187-207` — `applyGlossTranslation` (não mexer)
- `.claude/skills/run-llm/SKILL.md` — propagação de tokens (P3)

---

## Ordem de execução

1. Passo 0 — snapshot de baseline (SELECT)
2. Passo 1 — fix skill `/run-llm` para tokens
3. Passo 2 — ajustar `chunked(25)` para hebrew PT e `chunked(50)` + `*30` para glosses no backend
4. Passo 3 — rodar `bible_translate_hebrew_lexicon` + aplicar
5. Passo 4 — rodar `bible_translate_glosses` + aplicar
6. Passo 5 — verificação final
