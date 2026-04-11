# Analise: Execucao /run-llm e Comportamento da Fila LLM

Data: 2026-04-09

---

## 1. Validacao da execucao

### Resultado: SUCESSO

O `/run-llm` processou 10 itens da fase `bible_translate_lexicon` com sucesso:

| Metrica | Valor |
|---------|-------|
| Itens processados | 10 |
| Itens aplicados | 10/10 |
| Erros | 0 |
| Duracao | ~4s |
| Entradas de lexico traduzidas | ~800 (G0798-G1562, Portugues) |

**Evidencia de sucesso:**
- Terminal mostra 10 entries (G0798-G0464 ate G1449-G1460) todas com status "Completed"
- Mensagem: `Apply bible_translate_lexicon: Applied 10 items, 0 errors`
- Contador da fase caiu de **272 → 252** pendentes (20 itens processados em 2 batches de 10)

### Novas fases na fila

Apos a preparacao das fases de enrichment, a fila cresceu significativamente:

| Fase | Antes | Depois |
|------|-------|--------|
| bible_translate_lexicon | 272 | 252 (-20) |
| bible_translate_hebrew_lexicon | 206 | 206 (inalterado) |
| bible_translate_glosses | 260 | 260 (inalterado) |
| bible_translate_enrichment_greek | — | 11.046 (NOVO) |
| bible_translate_enrichment_hebrew | — | 17.294 (NOVO) |
| **Total** | **738** | **29.058** |

---

## 2. Por que "Concluidos" mostra 0?

### Resposta curta: exclusao logica — itens aplicados ficam invisiveis para as stats.

### Fluxo completo de um item na fila

```
pending → processing → completed → applied
                                      ↓
                              (invisivel no dashboard)
```

### O que acontece em cada estado

| Estado | Significado | Visivel no dashboard? |
|--------|-------------|----------------------|
| `pending` | Aguardando processamento LLM | Sim (card "Pendentes") |
| `processing` | LLM esta gerando a resposta | Sim (card "Processando") |
| `completed` | Resposta gerada, aguardando apply | Sim (card "Concluidos") |
| `failed` | Erro no processamento ou apply | Sim (card "Falharam") |
| `applied` | Resposta aplicada ao banco original | **NAO** |

### Causa raiz no codigo

**`LlmQueueRepository.getStats()`** retorna apenas 4 contadores:

```kotlin
QueuePhaseStatsDTO(
    pending   = statuses["pending"] ?: 0,
    processing = statuses["processing"] ?: 0,
    completed = statuses["completed"] ?: 0,
    failed    = statuses["failed"] ?: 0
    // "applied" NAO esta aqui
)
```

A query faz `GROUP BY phaseName, status` e monta o mapa, mas o DTO so extrai 4 chaves. Itens com `status = "applied"` existem no banco mas nao aparecem em nenhum contador.

### Por que "Concluidos" nunca aparece com valor > 0?

O `/run-llm` executa **processamento + apply na mesma execucao**:

1. Marca items como `processing`
2. Gera resposta com Claude
3. Marca como `completed`
4. **Imediatamente** chama `POST /admin/llm/queue/apply/{phase}`
5. Items passam para `applied`

O estado `completed` e **transitorio** — dura apenas os milissegundos entre o salvamento da resposta e a chamada do apply. O dashboard nunca captura esse momento.

---

## 3. E exclusao logica?

### Sim — itens aplicados permanecem no banco, mas sao excluidos das consultas.

| Aspecto | Comportamento |
|---------|---------------|
| **Registro no banco** | Permanece com `status = "applied"` |
| **Dados preservados** | Prompt, resposta LLM, timestamps — tudo mantido |
| **Visibilidade stats** | ~~Excluido~~ → CORRIGIDO: `getStats()` agora conta status "applied" |
| **Visibilidade dashboard** | ~~Excluido~~ → CORRIGIDO: 5o card "Aplicados" (teal) no dashboard |
| **Limpeza automatica** | ~~Nao existe~~ → CORRIGIDO: `RetentionScheduler` limpa applied > 7 dias |
| **Limpeza manual** | `DELETE /admin/llm/queue/{phase}` — remove TODOS de uma fase |

### Impacto

- **Positivo:** historico completo de processamento fica no banco para auditoria
- **Negativo:** tabela `llm_prompt_queue` cresce indefinidamente com itens applied que nunca sao limpos
- **Dashboard:** o usuario nao tem visibilidade dos itens ja processados com sucesso

---

## 4. Melhorias implementadas

### 4.1 Dashboard — contador "Aplicados" (IMPLEMENTADO)

5o card "Aplicados" (cor teal) no `LlmQueuePanel` + coluna na tabela por fase:

```
Pendentes | Processando | Concluidos | Aplicados | Falharam
  29.058  |      0      |     0      |    20     |    0
```

**Arquivos alterados:**
- `backend/.../model/DTOs.kt` — `applied` em `QueuePhaseStatsDTO`, `totalApplied` em `QueueStatsDTO`
- `backend/.../repository/LlmQueueRepository.kt` — `getStats()` agora conta status "applied"
- `frontend/types/llm.ts` — tipos TypeScript atualizados
- `frontend/components/ingestion/LlmQueuePanel.tsx` — card + coluna + grid 5 colunas
- `frontend/messages/{pt,en,es}.json` — traducao "Aplicados"/"Applied"/"Aplicados"

### 4.2 Limpeza automatica de itens antigos (IMPLEMENTADO)

`RetentionScheduler.cleanAppliedQueueItems(7)` roda diariamente no ciclo de manutencao:

```sql
DELETE FROM llm_prompt_queue
WHERE status = 'applied'
  AND processed_at < NOW() - INTERVAL '7 days';
```

**Arquivo alterado:** `backend/.../service/RetentionScheduler.kt`

### 4.3 Mostrar totais acumulados no /run-llm

O skill ja mostra "Total acumulado: ~49 itens processados". Com a melhoria 4.1, o dashboard agora reflete esses totais no card "Aplicados".

---

## 5. Resumo

| Pergunta | Resposta |
|----------|---------|
| A execucao deu sucesso? | Sim — 10 itens processados e aplicados sem erros |
| O lexico foi traduzido? | Sim — 800 entradas (G0798-G1562) traduzidas para portugues |
| Por que Concluidos = 0? | Estado transitorio: /run-llm aplica imediatamente apos completar |
| E exclusao logica? | Sim — itens com status "applied" ficam no banco (agora visiveis no dashboard) |
| Os dados sao perdidos? | Nao — permanecem por 7 dias para auditoria, depois sao limpos automaticamente |
| Existe cleanup? | Sim — `RetentionScheduler` limpa applied > 7 dias + manual via `DELETE /admin/llm/queue/{phase}` |
