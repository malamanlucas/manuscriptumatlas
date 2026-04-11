# Relatório de Integridade — LLM Queue

> Gerado em 10/04/2026 ~12:30 BRT (15:30 UTC)
> Baseado na análise cruzada: screenshot do admin + queries diretas no banco

---

## Estado Atual da Fila

| Status | Quantidade | % |
|---|---|---|
| Pendentes | 27.202 | 93,5% |
| Processando | 567 | 1,9% |
| Aplicados | 1.311 | 4,5% |
| Completados | 0 | 0% |
| Falharam | 0 | 0% |
| **Total** | **29.080** | **100%** |

**Integridade de IDs:** Min=29.272, Max=58.351, Count=29.080. **Zero gaps, zero delecoes.** Todos os itens originais existem.

---

## PROBLEMAS ENCONTRADOS

### 1. 540 itens ORFAOS em "processing" (SEM resposta)

Itens claimed ha ~12 horas por sessoes anteriores do `/run-llm` que morreram sem completar. **Nao foram processados — nao tem `response_content` nem `processed_at`.**

| Fase | Qtd orfaos | Idade |
|---|---|---|
| `bible_translate_enrichment_greek` | 227 | ~12h |
| `bible_translate_enrichment_hebrew` | 116 | ~12h |
| `bible_translate_glosses` | 48 | ~12h |
| `bible_translate_hebrew_lexicon` | 71 | ~12h |
| `bible_translate_lexicon` | 79 | ~12h |
| **Total** | **540** | ~12h |

**Impacto:** Estes 540 itens estao bloqueados — nao voltam para a fila sozinhos e nenhum worker novo vai pegá-los.

**Correcao:**
```sql
UPDATE llm_prompt_queue 
SET status = 'pending' 
WHERE status = 'processing' AND processed_at IS NULL AND response_content IS NULL;
-- Esperado: 540 rows affected
```

### 2. 27 itens em "processing" COM resposta (processados mas NAO aplicados)

Estes itens foram processados pela IA (tem `response_content`), mas nunca foram marcados como `completed` nem `applied`. **Dados prontos mas perdidos no limbo.**

| Fase | Qtd | Resposta |
|---|---|---|
| `bible_translate_enrichment_greek` | 26 | 139-142 bytes cada |
| `bible_translate_enrichment_hebrew` | 1 | 471 bytes |
| **Total** | **27** | — |

**Impacto:** As respostas da IA existem mas nunca foram salvas nas tabelas destino. Trabalho desperdicado.

**Correcao:**
```sql
-- Marcar como completed para que o apply os pegue
UPDATE llm_prompt_queue 
SET status = 'completed', processed_at = NOW()
WHERE status = 'processing' AND response_content IS NOT NULL;
-- Esperado: 27 rows affected

-- Depois aplicar:
-- curl -X POST http://localhost:8080/admin/llm/queue/apply/bible_translate_enrichment_greek
-- curl -X POST http://localhost:8080/admin/llm/queue/apply/bible_translate_enrichment_hebrew
```

### 3. 6 itens APLICADOS com resposta vazia/corrompida

Estes itens passaram pelo apply mas com conteudo vazio — os dados destino para esses Strong's numbers estao **ausentes ou corrompidos**.

| ID | Label | Conteudo | Tamanho |
|---|---|---|---|
| 41076 | ENRICHMENT_TRANSLATE_**H0001**_pt | `{}` | 2 bytes |
| 41077 | ENRICHMENT_TRANSLATE_**H0001**_es | `{}` | 2 bytes |
| 41208 | ENRICHMENT_TRANSLATE_**H0204**_pt | *(vazio)* | 0 bytes |
| 41209 | ENRICHMENT_TRANSLATE_**H0204**_es | *(vazio)* | 0 bytes |
| 41283 | ENRICHMENT_TRANSLATE_**H0351**_es | *(vazio)* | 0 bytes |
| 41284 | ENRICHMENT_TRANSLATE_**H0353**_pt | *(vazio)* | 0 bytes |

**Strong's afetados:** H0001 (av/pai), H0204 (On), H0351 (onde?), H0353 (força/poder)

**Impacto:** 4 entradas hebraicas tem enrichment corrompido nas tabelas destino.

**Correcao:**
```sql
-- Resetar para reprocessar
UPDATE llm_prompt_queue 
SET status = 'pending', response_content = NULL, processed_at = NULL, model_used = NULL
WHERE id IN (41076, 41077, 41208, 41209, 41283, 41284);
-- Esperado: 6 rows affected
```

### 4. Locale `es` ausente em greek_lexicon_translations

| Tabela | pt | es |
|---|---|---|
| `greek_lexicon_translations` | 7.116 | **0** |
| `hebrew_lexicon_translations` | 6.334 | 160 |

**Impacto:** Nenhuma traducao espanhola do lexico grego chegou na tabela destino, apesar de existirem itens `_es` na fila de enrichment grego que ja foram applied.

**Possivel causa:** O `LlmResponseProcessor` para enrichment grego pode nao estar gravando o locale `es`, ou o formato da resposta para greek `es` nao esta sendo parseado corretamente.

**Investigacao necessaria:** Verificar o codigo do `LlmResponseProcessor.processCompleted()` para a fase `bible_translate_enrichment_greek` e confirmar se ele grava ambos locales.

---

## RESUMO DE DADOS PERDIDOS/EM RISCO

| Problema | Qtd itens | Dados perdidos? | Recuperavel? |
|---|---|---|---|
| Orfaos sem resposta | 540 | Nao (nunca processados) | SIM — resetar para pending |
| Processados nao aplicados | 27 | Parcial (resposta existe, nao aplicada) | SIM — marcar completed + apply |
| Applied com resposta vazia | 6 | SIM (dados corrompidos no destino) | SIM — resetar + reprocessar |
| Greek es no destino | ? | SIM (locale inteiro ausente) | INVESTIGAR — pode ser bug no processor |
| **Total em risco** | **573** | — | **Todos recuperaveis** |

---

## ACOES RECOMENDADAS (em ordem)

### Acao 1 — Resetar 540 orfaos (URGENTE)
```sql
UPDATE llm_prompt_queue 
SET status = 'pending' 
WHERE status = 'processing' AND processed_at IS NULL AND response_content IS NULL;
```

### Acao 2 — Recuperar 27 processados nao aplicados
```sql
UPDATE llm_prompt_queue 
SET status = 'completed', processed_at = NOW()
WHERE status = 'processing' AND response_content IS NOT NULL;
```
Depois:
```bash
curl -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/llm/queue/apply/bible_translate_enrichment_greek
curl -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/llm/queue/apply/bible_translate_enrichment_hebrew
```

### Acao 3 — Reprocessar 6 corrompidos
```sql
UPDATE llm_prompt_queue 
SET status = 'pending', response_content = NULL, processed_at = NULL, model_used = NULL
WHERE id IN (41076, 41077, 41208, 41209, 41283, 41284);
```

### Acao 4 — Investigar greek es ausente
Verificar `LlmResponseProcessor` para a fase `bible_translate_enrichment_greek` — confirmar se grava locale `es` na tabela `greek_lexicon_translations`.

---

## NOTA POSITIVA

- **Zero itens deletados** — todos 29.080 itens originais existem
- **Zero falhas** — nenhum item com status `failed`
- **Worker ativo novamente** — últimos applies em 12:23 BRT (15:23 UTC)
- **Todos os problemas sao recuperaveis** — nenhum dado permanentemente perdido
