# Arquitetura de Tradução de Léxico via LLM

> **⚠️ Nota histórica:** Este documento foi escrito quando o projeto usava múltiplos providers (DeepSeek, OpenRouter, Anthropic). A arquitetura atual usa **provider único OpenAI** com sistema de 3 tiers (`TaskComplexity`). Para referência atualizada dos tiers e invocações, ver `docs/llm-concepts.md` seção "Sistema de Tiers". Os conceitos de concorrência (semaphore, batching, fallback) continuam válidos.

## Visão Geral

O sistema traduz definições de léxico (grego/hebraico) do inglês para português e espanhol
usando o LlmOrchestrator com tiers de complexidade e fallback automático.

---

## 1. Camadas da Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                    BibleIngestionService                        │
│              translateHebrewLexicon()                           │
│                                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       ┌──────────┐  │
│  │ Batch 1  │  │ Batch 2  │  │ Batch 3  │  ...  │ Batch N  │  │
│  │ 15 items │  │ 15 items │  │ 15 items │       │ 15 items │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘       └────┬─────┘  │
│       │             │             │                   │        │
│       └─────────────┴──────┬──────┴───────────────────┘        │
│                            │                                    │
│                   Semaphore (16 slots)                          │
│              16 batches em paralelo máximo                      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      LlmOrchestrator                            │
│                   executeRoundRobin()                           │
│                                                                 │
│  Counter atômico distribui chamadas entre providers:            │
│  chamada #0 → slot 0, chamada #1 → slot 1, ...                │
│                                                                 │
│  ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬───┐        │
│  │ DS  │ DS  │ DS  │ DS  │ DS  │ DS  │ DS  │ DS  │...│        │
│  │ #0  │ #1  │ #2  │ #3  │ #4  │ #5  │ #6  │ #7  │   │        │
│  ├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼───┤        │
│  │ OR  │ OR  │ OR  │ OR  │ OR  │ OR  │ OR  │ OR  │...│        │
│  │ #8  │ #9  │ #10 │ #11 │ #12 │ #13 │ #14 │ #15 │   │        │
│  └─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴───┘        │
│                                                                 │
│  DS = DeepSeek (direto)    OR = OpenRouter (11 modelos)        │
│  Total: ~23 slots no round-robin                                │
└────────────────────────────┬────────────────────────────────────┘
                             │
                   Se falhar → tenta próximo slot
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      LlmRateLimiter                             │
│              (1 por provider/modelo)                            │
│                                                                 │
│  ┌─────────────────────────────────────────┐                   │
│  │ 1. Min Delay (mutex)                    │                   │
│  │    DeepSeek: 100ms entre chamadas       │                   │
│  │    OpenRouter: 200ms entre chamadas     │                   │
│  ├─────────────────────────────────────────┤                   │
│  │ 2. Proactive Throttle                   │                   │
│  │    Se remaining-requests ≤ 2            │                   │
│  │    → pausa até reset time               │                   │
│  ├─────────────────────────────────────────┤                   │
│  │ 3. Retry com Backoff Exponencial        │                   │
│  │    429 → espera 5s → 10s → 20s → 40s   │                   │
│  │    + jitter aleatório (1.0-1.5x)        │                   │
│  │    Max: 2 retries (OpenRouter)          │                   │
│  │    Max: 5 retries (DeepSeek)            │                   │
│  └─────────────────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Providers Configurados

```
┌─────────────────────────────────────────────────────────────────┐
│                     PROVIDERS DIRETOS                            │
│              (conexão direta com a API)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   DeepSeek   │  │  Anthropic   │  │   OpenAI     │         │
│  │  deepseek-   │  │ claude-opus  │  │  (desativ.)  │         │
│  │    chat      │  │    4-6       │  │              │         │
│  │              │  │              │  │              │         │
│  │ $0.14/M in   │  │  $15/M in    │  │     ---      │         │
│  │ $0.28/M out  │  │  $75/M out   │  │              │         │
│  │              │  │              │  │              │         │
│  │ Prioridade 1 │  │ Prioridade 2 │  │ Prioridade 4 │         │
│  │  TRADUCAO    │  │   RESUMOS    │  │  (fallback)  │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                   PROVIDERS VIA OPENROUTER                      │
│          (1 API key, múltiplos modelos, round-robin)           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  PAGOS (rate limit alto, ~200 req/min)                         │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐  │
│  │ deepseek/  │ │ llama-3.3  │ │ gemma-3    │ │ qwen3-32b  │  │
│  │ deepseek-  │ │   -70b     │ │   -27b     │ │            │  │
│  │   chat     │ │            │ │            │ │            │  │
│  │ ~$0.14/M   │ │ ~$0.12/M   │ │ ~$0.10/M   │ │ ~$0.10/M   │  │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘  │
│  ┌────────────┐                                                │
│  │ mistral-   │                                                │
│  │ small-3.1  │                                                │
│  │ ~$0.10/M   │                                                │
│  └────────────┘                                                │
│                                                                 │
│  FREE (rate limit baixo, ~8 req/min)                           │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐  │
│  │ deepseek/  │ │ llama-3.3  │ │ gemma-3    │ │ qwen3-32b  │  │
│  │ r1-0528    │ │ -70b:free  │ │ -27b:free  │ │   :free    │  │
│  │   :free    │ │            │ │            │ │            │  │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘  │
│  ┌────────────┐ ┌────────────┐                                 │
│  │ mistral-   │ │ phi-4      │                                 │
│  │ small:free │ │ reasoning  │                                 │
│  │            │ │  :free     │                                 │
│  └────────────┘ └────────────┘                                 │
│                                                                 │
│  Total: 5 pagos + 6 free = 11 modelos OpenRouter               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Round-Robin: Distribuição de Carga

```
Com DeepSeek disponível + 11 modelos OpenRouter:

  deepSeekSlots = (11 + 1).coerceAtLeast(3) = 12 slots DeepSeek

  translationProviders (23 slots total):
  ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐
  │ DS │ DS │ DS │ DS │ DS │ DS │ DS │ DS │ DS │ DS │ DS │ DS │
  │ 0  │ 1  │ 2  │ 3  │ 4  │ 5  │ 6  │ 7  │ 8  │ 9  │ 10 │ 11 │
  └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘
  ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐
  │ OR │ OR │ OR │ OR │ OR │ OR │ OR │ OR │ OR │ OR │ OR │
  │ 12 │ 13 │ 14 │ 15 │ 16 │ 17 │ 18 │ 19 │ 20 │ 21 │ 22 │
  └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘

  Proporção: 52% DeepSeek / 48% OpenRouter

  Cada chamada incrementa um counter atômico:
    chamada #0  → slot 0  (DeepSeek)
    chamada #1  → slot 1  (DeepSeek)
    ...
    chamada #12 → slot 12 (OpenRouter deepseek-chat)
    chamada #13 → slot 13 (OpenRouter llama-3.3-70b)
    ...
    chamada #23 → slot 0  (volta pro DeepSeek)
```

---

## 4. Fluxo de uma Tradução (passo a passo)

```
8.600 entries hebraicas × 2 locales (pt, es) = 17.200 traduções
÷ 15 por batch = ~1.147 batches
÷ 2 locales processados em sequência = ~574 batches por locale

LOCALE "pt":
┌─────────────────────────────────────────────────┐
│  574 batches lançados como coroutines            │
│                                                  │
│  Semaphore(16) controla:                        │
│  máximo 16 batches executando ao mesmo tempo    │
│                                                  │
│  ┌─────┐ ┌─────┐ ┌─────┐     ┌─────┐          │
│  │ B1  │ │ B2  │ │ B3  │ ... │ B16 │  ← executando    │
│  └──┬──┘ └──┬──┘ └──┬──┘     └──┬──┘          │
│     │       │       │           │               │
│     ▼       ▼       ▼           ▼               │
│  ┌──────────────────────────────────┐           │
│  │      LlmOrchestrator            │           │
│  │      executeRoundRobin()        │           │
│  │                                  │           │
│  │  B1 → slot 0 (DeepSeek)        │           │
│  │  B2 → slot 1 (DeepSeek)        │           │
│  │  B3 → slot 2 (DeepSeek)        │           │
│  │  ...                            │           │
│  │  B13 → slot 12 (OR/deepseek)   │           │
│  │  B14 → slot 13 (OR/llama)      │           │
│  │  B15 → slot 14 (OR/gemma)      │           │
│  │  B16 → slot 15 (OR/qwen)       │           │
│  └──────────────────────────────────┘           │
│                                                  │
│  ┌─────┐ ┌─────┐ ┌─────┐                       │
│  │ B17 │ │ B18 │ │ B19 │ ...  ← aguardando     │
│  └─────┘ └─────┘ └─────┘        (na fila do    │
│                                   semaphore)    │
│                                                  │
│  Quando B1 termina → libera slot → B17 entra   │
└─────────────────────────────────────────────────┘

Depois repete para LOCALE "es"
```

---

## 5. Fallback em Cascata

```
Quando um provider falha, o round-robin tenta o próximo slot:

  Chamada #5 → slot 5 (DeepSeek)
                  │
                  ▼
            ┌───────────┐
            │ DeepSeek   │──── TIMEOUT (30s) ────┐
            └───────────┘                        │
                                                  ▼
                                          ┌───────────┐
                                          │ OR slot 6  │── 429 rate limit ──┐
                                          │ (DeepSeek) │                    │
                                          └───────────┘                    │
                                                                            ▼
                                                                    ┌───────────┐
                                                                    │ OR slot 7  │── SUCCESS ✓
                                                                    │  (llama)   │
                                                                    └───────────┘

  Percorre TODOS os 23 slots até encontrar um que funcione.
  Se todos falharem → batch vai para fallback individual
  (tenta traduzir 1 entry por vez em vez de 15)
```

---

## 6. Fallback de Batch → Individual

```
  ┌──────────────────────────────────────┐
  │  Batch de 15 entries                  │
  │  ┌─────────────────────────────────┐ │
  │  │ translateLexiconBatch()         │ │
  │  │ 1 chamada LLM com 15 entries   │ │
  │  └──────────────┬──────────────────┘ │
  │                 │                     │
  │          SUCESSO? ──── SIM ──→ batchUpsertHebrewTranslations()
  │                 │                     │
  │                NÃO                    │
  │                 │                     │
  │                 ▼                     │
  │  ┌─────────────────────────────────┐ │
  │  │ FALLBACK: traduz 1 por 1       │ │
  │  │                                 │ │
  │  │ for entry in batch:             │ │
  │  │   translateLexiconEntry(entry)  │ │
  │  │   upsertHebrewTranslation()    │ │
  │  └─────────────────────────────────┘ │
  └──────────────────────────────────────┘
```

---

## 7. Mecanismos de Resiliência

```
┌─────────────────────────────────────────────────────────┐
│                   LlmRateLimiter                         │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ① MIN DELAY (Mutex)                                    │
│     Garante intervalo mínimo entre chamadas ao          │
│     mesmo provider. Evita burst.                        │
│     DeepSeek: 100ms  |  OpenRouter: 200ms               │
│                                                          │
│  ② PROACTIVE THROTTLE                                   │
│     Lê header "x-ratelimit-remaining-requests"          │
│     Se ≤ 2 restantes → pausa até reset time             │
│     Previne 429 antes de acontecer                      │
│                                                          │
│  ③ EXPONENTIAL BACKOFF + JITTER                         │
│     429 recebido → espera com backoff crescente:        │
│     5s → 10s → 20s → 40s → 60s (cap)                   │
│     + fator aleatório (1.0x a 1.5x) para evitar         │
│     thundering herd                                      │
│                                                          │
│  ④ NON-RETRYABLE DETECTION                             │
│     Erros de billing/auth (402, 401) → falha imediata   │
│     Não desperdiça retries em erros permanentes         │
│                                                          │
│  ⑤ IDEMPOTÊNCIA                                        │
│     hasHebrewTranslation(id, locale) antes de traduzir  │
│     Entries já traduzidas são puladas                    │
│     Pode parar e retomar sem perder progresso            │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 8. Estimativa de Performance (com configuração atual)

```
ANTES (léxico grego — sem batching):
  10.847 entries × 2 locales = 21.694 chamadas LLM
  Semaphore: 8  |  Providers: 2 OpenRouter free
  Throughput: ~3-4 req/s
  Tempo: ~90 minutos

AGORA (léxico hebraico — com batching):
  8.600 entries × 2 locales = 17.200 traduções
  ÷ 15 por batch = ~1.147 chamadas LLM
  Semaphore: 16  |  Providers: DeepSeek + 11 OpenRouter
  Throughput estimado: ~10-15 req/s (com modelos pagos)
  Tempo estimado: ~2-5 minutos

GANHO:
  ┌──────────────┬───────────┬──────────────┐
  │   Melhoria   │   Antes   │    Agora     │
  ├──────────────┼───────────┼──────────────┤
  │ Batch size   │     1     │     15       │  ← 15x menos chamadas
  │ Semaphore    │     8     │     16       │  ← 2x mais paralelo
  │ Providers    │     2     │    12+       │  ← 6x mais capacidade
  │ Chamadas LLM │  21.694   │   ~1.147     │  ← 19x redução
  │ Tempo        │  ~90 min  │   ~2-5 min   │  ← 18-45x mais rápido
  └──────────────┴───────────┴──────────────┘

CUSTO (modelos pagos OpenRouter):
  ~2.3M tokens × $0.14/M = ~$0.32 (input)
  ~2.3M tokens × $0.28/M = ~$0.64 (output)
  Total: < $1.00
```

---

## 9. Fluxo Completo (do botão ao banco)

```
 FRONTEND                         BACKEND                              LLMs
 ────────                         ───────                              ────

 [▷ Traduzir                POST /admin/bible/
  Léxico     ──────────────→ ingestion/run/
  Hebraico]                  bible_translate_
                             hebrew_lexicon
                                    │
                                    ▼
                             BibleIngestionService
                             .translateHebrewLexicon()
                                    │
                                    ▼
                             getAllHebrewEntries()
                             ← 8.600 entries do DB
                                    │
                                    ▼
                             Para locale "pt":
                               filter untranslated
                               chunk em batches de 15
                                    │
                                    ▼
                             coroutineScope {
                               for batch in batches:
                                 launch {              ──────→  DeepSeek / OpenRouter
                                   semaphore.withPermit {        (round-robin)
                                     translateBatch()    ←────  SHORT: ... FULL: ...
                                     batchUpsert()  ──→ INSERT INTO
                                   }                     hebrew_lexicon_translations
                                 }
                             }
                                    │
                                    ▼
                             Repete para locale "es"
                                    │
                                    ▼
                             markSuccess()
                             ← fase completa
```

---

## 10. Glossário de Conceitos

### Semaphore (Semáforo)
Mecanismo de controle de concorrência que limita quantas operações podem rodar ao mesmo
tempo. Funciona como uma catraca: tem N permissões. Cada coroutine precisa adquirir uma
permissão antes de executar. Quando termina, libera a permissão para outra entrar.

**No projeto:** `Semaphore(16)` — máximo 16 batches traduzindo simultaneamente. Se 16 já
estão rodando, o 17º fica na fila esperando um terminar.

```
Semaphore(16):
  [B1][B2][B3]...[B16]  ← 16 executando
  [B17][B18][B19]...     ← aguardando na fila
  Quando B1 termina → B17 entra
```

### Batch (Lote)
Agrupar múltiplos itens em uma única operação. Em vez de fazer 1 chamada LLM por entry
do léxico, agrupa 15 entries num único prompt. O LLM traduz todas de uma vez e retorna
as 15 traduções juntas.

**Ganho:** Reduz overhead de rede, latência e número total de chamadas.
17.200 traduções ÷ 15 por batch = ~1.147 chamadas em vez de 17.200.

### Round-Robin
Estratégia de distribuição de carga que percorre uma lista circular de providers.
Um counter atômico (thread-safe) incrementa a cada chamada:
- Chamada #0 → provider[0], chamada #1 → provider[1], ...
- Quando chega no fim da lista, volta ao início.

**No projeto:** 26 slots (12 DeepSeek + 3 OpenAI + 11 OpenRouter). Cada batch vai
para o próximo slot na lista. Distribui a carga uniformemente entre todos os providers.

### Rate Limit
Limite imposto pelos providers de LLM em quantas requisições você pode fazer por
período de tempo. Exemplos:
- DeepSeek: sem limite agressivo
- OpenRouter free: 8 requisições por minuto por modelo
- OpenRouter pago: ~200 requisições por minuto

Quando você excede o limite, recebe HTTP 429 (Too Many Requests).

### Retry com Exponential Backoff
Quando uma chamada falha com 429 (rate limit), o sistema espera e tenta de novo.
O tempo de espera dobra a cada tentativa:

```
1ª tentativa falha → espera 5 segundos
2ª tentativa falha → espera 10 segundos
3ª tentativa falha → espera 20 segundos
...até o máximo de 60 segundos
```

**Jitter:** adiciona variação aleatória (1.0x a 1.5x) no tempo de espera para evitar
que múltiplas coroutines tentem ao mesmo tempo (thundering herd problem).

### Min Delay (Delay Mínimo)
Tempo mínimo obrigatório entre duas chamadas consecutivas ao mesmo provider.
Controlado por um Mutex (trava de exclusão mútua) que garante que apenas uma
coroutine por vez calcula o próximo horário permitido.

```
DeepSeek:    100ms entre chamadas → máx 10 req/s
OpenRouter:  200ms entre chamadas → máx 5 req/s
OpenAI:      500ms entre chamadas → máx 2 req/s
```

### Proactive Throttle (Throttle Proativo)
Em vez de esperar levar um 429, o sistema lê os headers de resposta do provider:
- `x-ratelimit-remaining-requests: 2` → "só restam 2 requisições"
- `x-ratelimit-reset-requests: 30s` → "reseta em 30 segundos"

Se restam ≤ 2 requisições, o sistema **pausa automaticamente** até o reset.
Previne o 429 antes dele acontecer.

### Coroutines (Kotlin)
Forma leve de executar código assíncrono em Kotlin. Diferente de threads do sistema
operacional, coroutines são gerenciadas pelo runtime do Kotlin e consomem muito menos
memória. Milhares de coroutines podem rodar em poucas threads.

**No projeto:**
- `coroutineScope { }` — cria um escopo que espera todas as coroutines filhas terminarem
- `launch { }` — lança uma nova coroutine (não bloqueia)
- `Semaphore.withPermit { }` — adquire permissão, executa, libera
- `suspend fun` — função que pode ser pausada e retomada sem bloquear a thread

```kotlin
coroutineScope {                    // Espera todas terminarem
    for (batch in batches) {
        launch {                    // Lança coroutine (não bloqueia)
            semaphore.withPermit {  // Espera permissão do semáforo
                translateBatch()    // Executa a tradução
            }                       // Libera permissão automaticamente
        }
    }
}
```

### Fallback (Contingência)
Mecanismo de recuperação quando algo falha. O sistema tem dois níveis:

1. **Fallback de provider:** Se DeepSeek falha, tenta OpenAI, depois OpenRouter
2. **Fallback de batch:** Se o batch de 15 falha, tenta traduzir 1 por 1

### Idempotência
Propriedade de poder executar a mesma operação múltiplas vezes sem efeito duplicado.
Antes de traduzir cada entry, verifica `hasHebrewTranslation(id, locale)`.
Se já existe, pula. Isso permite parar e retomar a fase sem retraduzir o que já foi feito.

