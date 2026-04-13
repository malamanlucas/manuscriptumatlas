# Estimativa de Conclusão — Fila LLM
**Data:** 2026-04-13 05:50 UTC | **Sessões ativas:** 1

---

## Estado Atual da Fila

| Status | Itens |
|--------|-------|
| `processing` | **320** (em andamento agora) |
| `completed` (não aplicados) | **130** |
| `pending LOW` | **2.174** |
| `pending MEDIUM` | **28.818** |
| **Total restante** | **31.312** |

---

## Throughput Medido

Baseado nos últimos 60 minutos de execução real:

| Métrica | Valor |
|---------|-------|
| Itens concluídos no período | 130 |
| Janela observada | ~30.5 minutos |
| **Taxa atual** | **~4,27 itens/min** |
| **Taxa/hora** | **~256 itens/hora** |
| Modelo em uso | `claude-haiku-4-5` |
| Paralelos por batch | 5 Agents simultâneos |
| Sessões rodando | **1** |

Padrão observado: **5 itens a cada ~1 minuto** de forma consistente.

---

## Estimativa por Fase (1 sessão — estado atual)

| Fase | Tier | Modelo | Pendentes | Estimativa |
|------|------|--------|-----------|------------|
| `bible_translate_glosses` | LOW | Haiku | 2.494 (+ 320 em proc.) | **~9,7h** |
| `bible_translate_enrichment_greek` | MEDIUM | Haiku | 11.046 | **~43h** |
| `bible_translate_enrichment_hebrew` | MEDIUM | Haiku | 17.294 | **~67,5h** |
| `bible_translate_lexicon` | MEDIUM | Sonnet | 272 | **~1,1h** |
| `bible_translate_hebrew_lexicon` | MEDIUM | Sonnet | 206 | **~0,8h** |

> As fases `enrichment_*` usam **Haiku** (mesma velocidade que LOW).  
> As fases `lexicon` usam **Sonnet** (mais lento, ~2–3 itens/min estimado).

### Ordem de execução (prioridade do `/run-llm`)
```
LOW (glosses) → MEDIUM enrichment (greek + hebrew) → MEDIUM lexicon
```

---

## Estimativa Total

### Cenário atual — 1 sessão

| Etapa | Itens | Tempo |
|-------|-------|-------|
| Finalizar glosses | ~2.494 | ~9,7h |
| Enrichment grego + hebraico | 28.340 | ~110,4h |
| Lexicon grego + hebraico | 478 | ~3,2h |
| **TOTAL** | **31.312** | **~123h ≈ 5,1 dias** |

> **Conclusão estimada com 1 sessão: ~2026-04-18 06:00 UTC**

---

## Como Acelerar

O endpoint `/admin/llm/queue/claim` usa `SELECT FOR UPDATE SKIP LOCKED` — **múltiplas sessões podem rodar em paralelo sem conflito**.

### Tabela de aceleração

| Sessões paralelas | Taxa total | Tempo enrichment | Tempo total | Conclusão estimada |
|-------------------|-----------|-----------------|-------------|-------------------|
| 1 (atual) | 4,27/min | ~110h | **~123h (5,1 dias)** | ~18 abr |
| 2 | 8,5/min | ~55h | **~62h (2,6 dias)** | ~16 abr |
| **3 (recomendado)** | **12,8/min** | **~37h** | **~41h (1,7 dias)** | **~15 abr** |
| 4 | 17/min | ~28h | **~31h (1,3 dias)** | ~15 abr |

> **Sweet spot: 3 sessões** — reduz de 5 dias para ~1,7 dias com risco mínimo de rate limit.

### Como abrir sessões adicionais

Abra **novos terminais Claude Code** no projeto e execute:
```
# Terminal 1 (já rodando)
/run-llm

# Terminal 2 (nova aba/janela)
/run-llm

# Terminal 3 (nova aba/janela)
/run-llm
```

Cada sessão vai:
1. Autenticar
2. Fazer claim atômico de 50 itens (`SKIP LOCKED` = sem conflito)
3. Processar 5 em paralelo por batch
4. Completar, aplicar e repetir

### Estratégia por fase (3 sessões)

| Fase | Volume | Sessões dedicadas |
|------|--------|-------------------|
| `bible_translate_enrichment_hebrew` | 17.294 | 2 sessões |
| `bible_translate_enrichment_greek` | 11.046 | 1 sessão |
| `bible_translate_glosses` (LOW) | 2.174 | pega junto com enrichment |
| `bible_translate_lexicon` (Sonnet) | 478 | processa por último |

---

## Itens Completed Não Aplicados ⚠️

Há **130 itens `completed`** da fase `bible_translate_glosses` aguardando ser gravados na tabela `interlinear_words`. Para aplicá-los agora:

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/dev-login?email=dev@manuscriptum.local" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
curl -s -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/llm/queue/apply/bible_translate_glosses
```

---

## Resumo Executivo

| Item | Valor |
|------|-------|
| Taxa atual | 4,27 itens/min (1 sessão) |
| Total restante | 31.312 itens |
| **Tempo com 1 sessão** | **~5,1 dias** |
| **Tempo com 3 sessões** | **~1,7 dias** |
| Ação imediata recomendada | Abrir 2 terminais adicionais com `/run-llm` |
| Gargalo principal | `bible_translate_enrichment_hebrew` (17.294 itens) |

---

*Throughput baseado em medição real dos últimos 60 minutos — deploy-postgres-1 — 2026-04-13*
