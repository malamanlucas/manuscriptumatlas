# Análise: Paralelismo do /run-llm

> Data: 2026-04-10

## Situação Atual

| Métrica | Valor |
|---------|-------|
| Itens pendentes | ~28.716 |
| Taxa de processamento | ~14 itens/min (1 sessão) |
| Tempo estimado | ~34 horas |
| Sessões em paralelo | 1 |

### Gargalo identificado

O `/run-llm` opera com **1 sessão Claude Code sequencial**. Mesmo com subagents (Haiku/Sonnet), o throughput é limitado porque:

1. **Overhead por item**: mark processing → gerar resposta → salvar resultado → aplicar = ~4 chamadas HTTP por item
2. **Sem claim atômico**: `getPending` + `markProcessing` eram 2 chamadas separadas, criando race condition se rodar em paralelo
3. **Lotes pequenos**: processamento 1-a-1 dentro de cada batch

## Solução Implementada

### 1. Endpoint atômico `POST /admin/llm/queue/claim`

```sql
UPDATE llm_prompt_queue SET status = 'processing'
WHERE id IN (
    SELECT id FROM llm_prompt_queue
    WHERE status = 'pending' [AND tier = ?] [AND phase_name = ?]
    ORDER BY id ASC
    LIMIT ?
    FOR UPDATE SKIP LOCKED  -- ← chave: pula itens já lockados por outra sessão
)
RETURNING *
```

**O que faz:**
- Seleciona N itens pendentes
- Marca como `processing` atomicamente
- `SKIP LOCKED` garante que outra sessão concorrente pega itens **diferentes**
- Retorna os itens já com status `processing` — não precisa chamar `/processing` separado

**Parâmetros:**
- `limit` — quantidade de itens (default 50, max 200)
- `tier` — filtrar por tier (LOW/MEDIUM/HIGH)
- `phase` — filtrar por fase

### 2. Múltiplas sessões Claude Code

Com o claim atômico, é seguro rodar **múltiplas sessões `/run-llm` em paralelo**:

```
Terminal 1: /run-llm  →  claim 50 itens  →  processa  →  claim mais 50  →  ...
Terminal 2: /run-llm  →  claim 50 itens  →  processa  →  claim mais 50  →  ...
Terminal 3: /run-llm  →  claim 50 itens  →  processa  →  claim mais 50  →  ...
```

Cada sessão recebe itens únicos. Sem desperdício, sem conflito.

## Estimativas de Throughput

| Sessões | Taxa estimada | Tempo total | Redução |
|---------|--------------|-------------|---------|
| 1 | ~14 itens/min | ~34h | baseline |
| 2 | ~28 itens/min | ~17h | 50% |
| 3 | ~42 itens/min | ~11h | 68% |
| 4 | ~56 itens/min | ~8.5h | 75% |

**Limitantes:**
- CPU e rede do host (cada sessão Claude Code consome recursos)
- Rate limits da API Claude (improvável com Haiku/Sonnet)
- Conexões PostgreSQL (cada sessão usa 1 conexão por claim)

**Recomendação:** 2-3 sessões simultâneas é o sweet spot para a maioria dos cenários.

## Estratégia Ótima por Fase

| Fase | Volume | Modelo | Sessões recomendadas |
|------|--------|--------|---------------------|
| bible_translate_enrichment_hebrew | 17.252 | Haiku | 2-3 (maior volume) |
| bible_translate_enrichment_greek | 10.991 | Haiku | 1-2 |
| bible_translate_lexicon | 231 | Sonnet | 1 (volume baixo) |
| bible_translate_hebrew_lexicon | 191 | Sonnet | 1 (volume baixo) |
| bible_translate_glosses | 51 | Haiku | 1 (quase acabou) |

**Abordagem:**
1. Dedicar 2 sessões para enrichment (maior volume, Haiku = rápido)
2. 1 sessão para lexicon + glosses restantes
3. Apply automático via Kafka ou manual após cada batch

## Como usar

```bash
# Terminal 1
/run-llm

# Terminal 2 (nova janela)
/run-llm

# Terminal 3 (nova janela)
/run-llm
```

Cada sessão vai:
1. Autenticar
2. Verificar stats
3. Usar `POST /admin/llm/queue/claim?tier=LOW&limit=50` (atômico)
4. Processar itens claimed
5. Salvar resultados
6. Aplicar ao banco
7. Repetir até fila vazia
