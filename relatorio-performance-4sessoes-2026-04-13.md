# Relatório de Performance — 4 Sessões `/run-llm` Paralelas
**Data:** 2026-04-13 ~05:25 UTC | **Pool:** 25 conexões (nt_coverage) | **Sessões:** 4

---

## Snapshot do Estado Atual

| Status | Fase | Itens |
|--------|------|-------|
| `processing` | bible_translate_glosses | **415** |
| `processing` | bible_translate_lexicon | **50** |
| `completed` (aguardando apply) | bible_translate_glosses | 10 |
| `pending` | bible_translate_glosses | 2.024 |
| `pending` | bible_translate_lexicon | 222 |
| `pending` | bible_translate_hebrew_lexicon | 206 |
| `pending` | bible_translate_enrichment_greek | 11.046 |
| `pending` | bible_translate_enrichment_hebrew | 17.294 |
| **Total restante** | | **30.792** |

---

## Throughput Medido

| Métrica | 1 sessão (antes) | 4 sessões (agora) | Ganho |
|---------|-----------------|-------------------|-------|
| Itens/min | ~4,3 | **~6** | +40% |
| Itens/hora | ~258 | **~360** | +40% |
| Completados após restart (8 min) | — | 48 | — |

> **Ganho real: ~40%** — bem abaixo do esperado **4× (400%)**. Ver causas abaixo.

---

## Infraestrutura — Sem Gargalos

| Recurso | Uso atual | Limite | Status |
|---------|-----------|--------|--------|
| CPU app (Ktor) | **0,57%** | 100% | ✅ ocioso |
| CPU PostgreSQL | **0,89%** | 100% | ✅ ocioso |
| RAM total | 556 MB / 7,6 GiB | — | ✅ ampla folga |
| Conexões PostgreSQL (nt_coverage) | **11** | 25 (pool) | ✅ sem contenção |
| Conexões esperadas pico (4×5 agents) | ~20 | 25 | ✅ dentro do limite |
| Locks de concorrência | **0** | — | ✅ zero deadlocks |

A infra **não é o gargalo**. Pool de 25 foi suficiente, sem nenhuma espera por conexão.

---

## ⚠️ Problema Identificado: Itens Orphaned em `processing`

### O que aconteceu

Quando o app foi **reiniciado** (para aplicar o novo pool size), a sessão original tinha **320 itens** em estado `processing`. O restart encerrou a sessão, mas os itens **permaneceram em `processing` no banco** — sem nenhuma sessão responsável por eles.

O endpoint `/claim` filtra `WHERE status = 'pending'`, portanto esses 320 itens **nunca serão reclamados novamente** automaticamente.

### Estado atual dos itens em processing

| Origem | Fase | Itens |
|--------|------|-------|
| Sessão antiga (orphaned) | glosses | **~320** (stuck) |
| 4 novas sessões (ativos) | glosses | ~95 |
| 4 novas sessões (ativos) | lexicon | 50 |
| **Total em processing** | | **465** |

Dos 465 em `processing`, **~320 nunca serão completados**.

### Fix — Resetar itens orphaned

Executar após as 4 sessões terminarem o ciclo atual (~10-15 min):

```sql
-- Contar orphaned (executar primeiro para confirmar)
SELECT COUNT(*) FROM llm_prompt_queue
WHERE status = 'processing'
  AND processed_at IS NULL;

-- Resetar para pending (só executar quando sessões estiverem entre ciclos)
UPDATE llm_prompt_queue
SET status = 'pending'
WHERE status = 'processing'
  AND processed_at IS NULL;
```

> **Cuidado:** executar apenas quando as sessões terminarem o batch atual e antes de fazerem novo `/claim`. Caso contrário, itens ativos ficam em pending mas os agents ainda terão responses para enviar — o backend aceitará o POST `/complete` e moverá de `pending` → `completed`, o que é inofensivo.

---

## Por Que o Ganho é Só 40% (Não 4×)?

### Causa 1 — Sobreposição de fases entre sessões

As 4 sessões seguem a mesma prioridade (`LOW → MEDIUM`):
- Todas tentam primeiro glosses (LOW)
- Todas tentam depois enrichment (MEDIUM)

Resultado: as sessões **competem pelos mesmos itens** em vez de dividir o trabalho. Como o pool de cada sessão é 5 agents, o throughput efetivo é próximo de 1-2 sessões dominando enquanto as outras esperam ou processam batches pequenos.

### Causa 2 — Latência da API Anthropic cresce com concorrência

Com 4 × 5 = **20 chamadas simultâneas ao Haiku**, a latência média por chamada sobe. Em vez de ~30s por resposta, pode estar chegando a 60-90s. A fórmula:

```
throughput = (sessions × agents_per_batch) / latency_seconds × 60
           = (4 × 5) / 90 × 60
           ≈ 13 items/min (teórico)
```

Com overhead (auth, stats, apply), chega-se ao ~6/min observado.

### Causa 3 — Overhead sequencial dentro de cada sessão

Cada sessão executa em sequência: claim → 5 agents → complete × 5 → apply → claim novamente. Esse ciclo tem ~30-60s de overhead (auth, HTTP calls) a cada 5 itens processados.

---

## Otimizações Recomendadas

### 1. Distribuir fases entre sessões (impacto: alto)

Em vez de todas as sessões seguirem a mesma prioridade, dedicar:

| Terminal | Fase alvo | Comando sugerido |
|----------|-----------|-----------------|
| 1 | glosses (LOW) | `/run-llm` padrão |
| 2 | enrichment_hebrew (MEDIUM) | `/run-llm` com tier=MEDIUM |
| 3 | enrichment_greek (MEDIUM) | `/run-llm` com tier=MEDIUM |
| 4 | lexicon (MEDIUM) | `/run-llm` com tier=MEDIUM |

> Isso requer que o SKILL.md suporte filtro de fase/tier por parâmetro, ou que o usuário adapte manualmente.

### 2. Aumentar agents por batch de 5 para 10 (impacto: médio)

O SKILL.md permite spawnar 3-5 agents em paralelo. Aumentar para 10 por sessão:
- 4 sessões × 10 agents = 40 chamadas simultâneas
- Risco: rate limit do Haiku (~1000 RPM → irrelevante) mas latência individual pode subir

### 3. Adicionar coluna `claimed_at` no schema (impacto: longo prazo)

```sql
ALTER TABLE llm_prompt_queue ADD COLUMN claimed_at TIMESTAMPTZ;
```

Permitiria detectar e resetar automaticamente itens stuck:
```sql
-- Reset automático de itens em processing há mais de 10 minutos
UPDATE llm_prompt_queue SET status = 'pending', claimed_at = NULL
WHERE status = 'processing'
  AND claimed_at < NOW() - INTERVAL '10 minutes';
```

---

## Estimativa Revisada com 4 Sessões

| Fase | Itens restantes | Taxa atual | Estimativa |
|------|----------------|-----------|------------|
| bible_translate_glosses | 2.024 + 415 stuck reset | 6/min | **~8h** |
| bible_translate_enrichment (greek + hebrew) | 28.340 | 6/min | **~79h** |
| bible_translate_lexicon + hebrew | 478 | ~3/min (Sonnet) | **~2,7h** |
| **Total** | **31.257** | | **~90h ≈ 3,7 dias** |

> Com reset dos orphaned e distribuição de fases por sessão (otimização 1):
> **estimativa cai para ~2-2,5 dias**

---

## Ações Imediatas Recomendadas

| Prioridade | Ação |
|-----------|------|
| 🔴 | Aguardar batch atual terminar (~10 min) → resetar `processing` orphaned para `pending` |
| 🟡 | Distribuir sessões por fase (1 sessão por fase) para eliminar competição |
| 🟡 | Aumentar agents por batch de 5 → 10 no SKILL.md |
| 🟢 | Adicionar coluna `claimed_at` para gestão automática de orphaned |

---

*Dados coletados via queries diretas no deploy-postgres-1 — 2026-04-13 05:25 UTC*
