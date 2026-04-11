# Conceitos de Arquitetura LLM — Manuscriptum Atlas

Guia dos padrões e conceitos usados na camada de integração com LLMs do projeto.

---

## 0. Sistema de Tiers (TaskComplexity)

O projeto usa um sistema de 3 tiers para rotear invocações LLM ao modelo adequado. Definido em `LlmProvider.kt`.

### Configuração dos tiers

| Tier | OpenAI (direto) | Claude (/run-llm) | Effort | Concurrency | Batch Size | Timeout | Uso |
|------|----------------|-------------------|--------|-------------|------------|---------|-----|
| **LOW** | `gpt-4.1-mini` | `claude-haiku-4-5` | Minimo | 80 | 150 | 30s | Glosses, traduções simples 1:1 |
| **MEDIUM** | `gpt-4.1` | `claude-haiku-4-5` (enrichment) / `claude-sonnet-4-6` (lexicon, bios, councils) | Minimo/Medio | 40 | 80 | 60s | Batches de léxico, enrichment, biografias, traduções, extração, conflitos |
| **HIGH** | `gpt-5.4` | `claude-opus-4-6` | Alto | 15 | 20 | 120s | Dating acadêmico, alignment interlinear, apologética |

Configurável via env vars: `OPENAI_LOW_MODEL`, `OPENAI_MEDIUM_MODEL`, `OPENAI_HIGH_MODEL`, `LLM_{TIER}_CONCURRENCY`, `LLM_{TIER}_BATCH_SIZE`, `LLM_{TIER}_TIMEOUT_MS`.

### Fallback entre tiers

Cadeia de fallback automática — se o tier configurado falhar, sobe:
- LOW → MEDIUM → HIGH
- MEDIUM → HIGH
- HIGH → (sem fallback, erro)

### Mapa de invocações

| Label | Serviço | Tier | Descrição |
|-------|---------|------|-----------|
| `GLOSS_TRANSLATE_{lang}_chunk{idx}` | `BibleIngestionService` | LOW | Tradução de glosses interlineares |
| `LEXICON_TRANSLATE_{strongs}` | `BibleIngestionService` | LOW | Tradução individual de léxico (fallback) |
| `LEXICON_BATCH_{type}_{size}` | `BibleIngestionService` | MEDIUM | Tradução em batch de léxico grego/hebraico |
| `ENRICHMENT_TRANSLATE_{strongs}_{locale}` | `BibleIngestionService` | MEDIUM | Enriquecimento de definições de léxico |
| `WORD_ALIGN_{version}_{book}_{ch}v{verse}` | `WordAlignmentService` | HIGH | Alinhamento interlinear palavra-a-palavra |
| `DatingEnrichment:{label}` | `DatingEnrichmentService` | HIGH | Enriquecimento de datação de manuscritos/pais |
| `APOLOGETICS_*` | `ApologeticsService` | HIGH | Geração e complementação de respostas apologéticas |
| `ConflictResolution` | `ConflictResolutionService` | MEDIUM | Resolução de conflitos entre fontes |
| `AcademicTextExtractor` | `AcademicTextExtractor` | MEDIUM | Extração estruturada de textos acadêmicos |
| `BIO_SUMMARIZATION` | `BiographySummarizationService` | MEDIUM | Resumo de biografias de pais da igreja |
| `BIO_TRANSLATION` | `BiographySummarizationService` | MEDIUM | Tradução de biografias |
| `COUNCIL_TRANSLATION` | `BiographySummarizationService` | MEDIUM | Tradução de campos de concílios |
| `COUNCIL_OVERVIEW_AI` | `BiographySummarizationService` | MEDIUM | Geração de overview de concílios |
| `HERESY_TRANSLATION` | `BiographySummarizationService` | MEDIUM | Tradução de campos de heresias |

### Regra para novas invocações

Ao adicionar nova chamada LLM:
1. Escolher o tier pela complexidade da tarefa (ver tabela acima)
2. Definir um label descritivo e único
3. Usar `llmOrchestrator.execute(request, TaskComplexity.X)`
4. LOW para tarefas simples/curtas, MEDIUM para batches/multi-campo, HIGH para tarefas que exigem raciocínio

### Providers

- **Direto (ApologeticsService):** OpenAI (`LlmProviderType.OPENAI`). Round-robin entre instâncias do mesmo tier, semáforos por tier para controlar concorrência, rastreamento via `LlmUsageService`.
- **Queue (/run-llm):** Claude Code (Anthropic). Modelo selecionado por tier: Haiku (LOW + enrichment), Sonnet (MEDIUM lexicon/bios/councils), Opus (HIGH). Custo zero (assinatura fixa).

---

## 0b. LLM Prompt Queue (modo assíncrono)

Sistema para eliminar custos de API OpenAI nas fases de ingestão, delegando o processamento LLM para o Claude Code (assinatura fixa).

### Arquitetura

```
ANTES:  Backend → LlmOrchestrator → OpenAI API ($$$) → parse → save
DEPOIS: Backend (prepare) → llm_prompt_queue → Claude Code (/run-llm) → apply → save
```

### Componentes

| Componente | Arquivo | Responsabilidade |
|-----------|---------|------------------|
| `LlmPromptQueue` | `Tables.kt` | Tabela Exposed ORM (Flyway V21) |
| `LlmQueueRepository` | `LlmQueueRepository.kt` | CRUD da fila: enqueue, getPending, markCompleted, markApplied, stats |
| `LlmResponseProcessor` | `LlmResponseProcessor.kt` | Aplica respostas completadas ao banco original |
| `KafkaProducerService` | `KafkaProducerService.kt` | Publica no tópico `llm-results-ready` |
| `LlmResultsConsumer` | `LlmResultsConsumer.kt` | Consome do Kafka e auto-aplica via LlmResponseProcessor |
| `/run-llm` skill | `.claude/skills/run-llm/SKILL.md` | Claude Code processa prompts pendentes |

### Fluxo de status

```
pending → processing → completed → applied
                    ↘ failed
```

### Fases prepare/apply

Cada serviço LLM se divide em duas fases de ingestão:

| Fase | Ação |
|------|------|
| `{service}_prepare` | Constrói prompts e enqueue na fila |
| `/run-llm` | Claude Code lê pending, processa, salva respostas |
| `{service}_apply` | LlmResponseProcessor aplica ao banco original |

### API Endpoints da fila

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/admin/llm/queue/stats` | Contadores por fase e status |
| GET | `/admin/llm/queue/pending?limit=&tier=&phase=` | Items pendentes |
| GET | `/admin/llm/queue/completed/{phase}` | Items completados por fase |
| POST | `/admin/llm/queue/{id}/processing` | Marcar como em processamento |
| POST | `/admin/llm/queue/{id}/complete` | Salvar resposta LLM |
| POST | `/admin/llm/queue/{id}/fail` | Marcar como falho |
| POST | `/admin/llm/queue/batch-complete` | Completar vários de uma vez |
| POST | `/admin/llm/queue/apply/{phase}` | Aplicar respostas ao banco (direto) |
| POST | `/admin/llm/queue/notify-ready/{phase}` | Notificar via Kafka |
| DELETE | `/admin/llm/queue/{phase}` | Limpar fila de uma fase |

### Migração por serviço

| Serviço | Status | Fases |
|---------|--------|-------|
| ConflictResolutionService | ✅ Migrado | `council_consensus_prepare` / `council_consensus_apply` |
| BibleIngestionService (translate) | ✅ Migrado | `bible_translate_lexicon`, `bible_translate_hebrew_lexicon`, `bible_translate_glosses` |
| BibleIngestionService (enrichment) | ✅ Migrado | `bible_translate_enrichment_greek`, `bible_translate_enrichment_hebrew` |
| WordAlignmentService | ✅ Migrado | `bible_align_kjv`, `bible_align_arc69`, `bible_align_hebrew_kjv`, `bible_align_hebrew_arc69` |
| DatingEnrichmentService | ✅ Migrado | `DatingEnrichment:manuscript`, `DatingEnrichment:father` |
| BiographySummarizationService | ✅ Migrado | `BIO_SUMMARIZE_*`, `BIO_TRANSLATE_*`, `COUNCIL_TRANSLATE_*`, `HERESY_TRANSLATE_*`, `COUNCIL_OVERVIEW_*` |
| AcademicTextExtractor | Pendente | — |
| ApologeticsService | **Exceção** | Mantém OpenAI direto (real-time, baixo custo) |

### Kafka

- **Broker:** Confluent Kafka 7.7.0 (KRaft mode, sem Zookeeper)
- **Tópico:** `llm-results-ready` (mensagem = nome da fase)
- **Consumer group:** `manuscriptum-llm-processor`
- **Env var:** `KAFKA_BOOTSTRAP_SERVERS` (default: `localhost:9092`)

---

## 1. Coroutines

Coroutines são a forma nativa do Kotlin de lidar com operações assíncronas — como chamadas HTTP para APIs de LLM — sem bloquear threads.

Uma thread bloqueada fica parada esperando resposta. Uma coroutine **suspende** (libera a thread para outro trabalho) e **retoma** quando a resposta chega.

```
Thread tradicional:           Coroutine:
[chamada HTTP]                [chamada HTTP]
[esperando... bloqueada]      [suspende → thread livre]
[esperando... bloqueada]      [outra coroutine usa a thread]
[resposta chegou]             [retoma quando resposta chega]
```

No projeto, toda função que faz chamada LLM é `suspend fun` — isso permite que dezenas de traduções rodem "ao mesmo tempo" usando poucas threads reais.

**Palavras-chave no código:**
- `suspend fun` — função que pode suspender
- `launch { }` — inicia uma coroutine (fire-and-forget)
- `async { }` / `await()` — inicia coroutine e espera o resultado
- `coroutineScope { }` — cria um escopo que só termina quando todas as coroutines filhas terminam
- `withContext(Dispatchers.IO)` — executa em thread pool de I/O

---

## 2. Semaphore (Semáforo)

Um semáforo controla **quantas coroutines podem executar simultaneamente**. É como um estacionamento com vagas limitadas.

```
Semaphore(8) = 8 vagas

Coroutine 1 → [entra, vaga 1]  → chamando DeepSeek...
Coroutine 2 → [entra, vaga 2]  → chamando OpenRouter...
Coroutine 3 → [entra, vaga 3]  → chamando DeepSeek...
...
Coroutine 8 → [entra, vaga 8]  → chamando OpenRouter...
Coroutine 9 → [ESPERA]         → todas as vagas ocupadas
             → Coroutine 1 termina, libera vaga
Coroutine 9 → [entra, vaga 1]  → chamando DeepSeek...
```

**Por que não usar 100 simultâneas?** Porque os providers têm rate limits. Com 8 vagas, mantemos pressão suficiente para throughput alto sem estourar os limites.

**No código:**
```kotlin
val semaphore = Semaphore(8)

launch {
    semaphore.withPermit {  // espera uma vaga, executa, libera ao sair
        val result = llmOrchestrator.executeRoundRobin(request)
    }
}
```

---

## 3. Round-Robin

Distribuição circular de chamadas entre múltiplos providers LLM. Cada chamada vai para o próximo provider da lista.

```
Providers: [DeepSeek, DeepSeek, DeepSeek, Llama, Gemma]
                                          (pesos: DeepSeek 3x)

Call  1 → DeepSeek     (índice 0)
Call  2 → DeepSeek     (índice 1)
Call  3 → DeepSeek     (índice 2)
Call  4 → Llama        (índice 3)
Call  5 → Gemma        (índice 4)
Call  6 → DeepSeek     (índice 0, volta ao início)
Call  7 → DeepSeek     (índice 1)
...
```

**Por que não usar só o DeepSeek?** Porque cada provider tem seu próprio rate limit. Usando vários em paralelo, a capacidade total é a soma dos limites individuais.

**Por que o DeepSeek aparece 3 vezes?** Porque ele tem rate limits mais generosos que os providers free do OpenRouter. Dando mais "peso" ao DeepSeek, ele absorve ~60% da carga enquanto os free contribuem com os outros ~40%.

**No código:**
```kotlin
val counter = AtomicInteger(0)  // contador global thread-safe

fun selectProvider(): LlmProvider {
    val index = counter.getAndIncrement() % providers.size
    return providers[index]
}
```

**Se o provider selecionado falhar**, o round-robin tenta o próximo da lista (fallback automático).

---

## 4. Rate Limit

Rate limit é o controle de quantas requisições um provider permite por unidade de tempo. Existem dois tipos:

- **RPM** (Requests Per Minute) — quantas chamadas por minuto (ex: OpenRouter free = ~20 RPM)
- **TPM** (Tokens Per Minute) — quantos tokens por minuto (ex: OpenAI tier 1 = 30.000 TPM)

Quando você ultrapassa o limite, o provider retorna **HTTP 429 (Too Many Requests)**.

```
Seu app:     |--req--|--req--|--req--|--req--|--req--|
Provider:    [OK]    [OK]    [OK]    [429!]  ← limite atingido
             "espere 30 segundos antes de tentar de novo"
```

**Headers que o provider envia:**
```
x-ratelimit-remaining-requests: 2    ← só restam 2 chamadas
x-ratelimit-remaining-tokens: 1500   ← só restam 1500 tokens
x-ratelimit-reset-requests: 30s      ← limites resetam em 30s
Retry-After: 30                       ← espere 30s antes de tentar
```

---

## 5. Retry com Backoff Exponencial

Quando uma chamada falha (429, 500, timeout), o sistema **tenta novamente** com esperas progressivamente maiores.

```
Tentativa 1: falhou (429)  → espera  5 segundos
Tentativa 2: falhou (429)  → espera 10 segundos  (5s × 2)
Tentativa 3: falhou (429)  → espera 20 segundos  (10s × 2)
Tentativa 4: falhou (500)  → espera 40 segundos  (20s × 2)
Tentativa 5: falhou        → DESISTE, passa para o próximo provider
```

**Por que exponencial?** Se o provider está sobrecarregado, esperar mais a cada tentativa dá tempo para ele se recuperar. Esperar sempre o mesmo tempo pode manter a pressão.

**Jitter (variação aleatória):** Cada espera tem um fator aleatório de 1.0x a 1.5x para evitar que múltiplas coroutines tentem todas ao mesmo tempo (efeito "manada").

```
Backoff base: 10s
Com jitter:   10s × random(1.0, 1.5) = entre 10s e 15s
```

**Cap (limite máximo):** O backoff nunca passa de 60 segundos, mesmo após muitas tentativas.

**No código:**
```kotlin
maxRetries = 2          // máximo de tentativas
initialBackoffMs = 5000 // primeira espera: 5s
// backoff = min(initialBackoff × 2^attempt, 60000) × jitter
```

---

## 6. Min Delay (Delay Mínimo entre Chamadas)

Um intervalo mínimo forçado entre cada chamada ao mesmo provider. Previne burst (rajada de requisições).

```
DEEPSEEK_CALL_DELAY_MS=100

Chamada 1: t=0ms      → envia
Chamada 2: t=50ms     → espera 50ms (precisa completar 100ms desde a anterior)
Chamada 2: t=100ms    → envia
Chamada 3: t=150ms    → espera 0ms (Chamada 2 não terminou, mas o delay mínimo já passou)
```

**Valores no projeto:**
- OpenAI: configurável por tier via `LlmConfig` (default 500ms)

**Implementação:** Um `Mutex` (trava) protege o cálculo do delay, mas libera antes da chamada HTTP — permitindo que múltiplas chamadas estejam "em voo" ao mesmo tempo.

---

## 7. Proactive Throttle (Throttling Preventivo)

Em vez de esperar levar um 429, o sistema **antecipa** que o limite está próximo e desacelera voluntariamente.

```
Header recebido: x-ratelimit-remaining-requests: 2
                 x-ratelimit-reset-requests: 45s

Sistema pensa: "só restam 2 chamadas e o reset é em 45s"
Ação: pausa TODAS as chamadas para este provider por 45 segundos
```

Isso é melhor que levar 429 porque:
- Evita erros desnecessários
- Não gasta tentativas de retry
- O provider não registra "abuso" na sua conta

**No código:**
```kotlin
if (remaining != null && remaining <= 2) {
    val resetDuration = parseResetDuration(response.headers[resetHeader])
    if (resetDuration > 0) {
        log.info("PROACTIVE_THROTTLE: remaining=$remaining, waiting ${resetDuration}ms")
        delay(resetDuration)
    }
}
```

---

## 8. Batch (Agrupamento)

Em vez de fazer 1 chamada LLM por item, agrupar vários itens em 1 chamada só. Reduz drasticamente o número de requisições.

```
Sem batch (1 chamada por gloss):
  "the"  → 1 chamada → "o"
  "and"  → 1 chamada → "e"
  "love" → 1 chamada → "amor"
  = 3 chamadas

Com batch (100 glosses por chamada):
  "the\nand\nlove\n..." → 1 chamada → "o\ne\namor\n..."
  = 1 chamada para 100 itens
```

**Trade-offs:**
- Batch maior = menos chamadas = mais rápido
- Batch grande demais = pode estourar o limite de tokens do modelo
- Se 1 item do batch falha, pode comprometer os outros

**No projeto:**
- `translateGlossBatch()` — batches de 100 glosses por chamada
- `translateLexiconBatch()` — batches de 15 entries de léxico por chamada (mais texto por entry)
- `translateLexiconEntry()` — 1 entry por chamada (fallback quando batch falha)

---

## 9. Fallback Sequencial vs Round-Robin

O projeto usa **dois padrões diferentes** dependendo do tipo de tarefa:

### Tier-based execution (`execute(request, complexity)`) — API primária
```
execute(request, TaskComplexity.HIGH) → usa providers do tier HIGH
  Se falhar → erro (sem fallback para tier inferior)

execute(request, TaskComplexity.LOW) → usa providers do tier LOW
  Se falhar → tenta MEDIUM → se falhar → tenta HIGH

Usado para: todas as invocações. Tier escolhido pela complexidade da tarefa.
```

### Legacy wrappers (backward-compat)
```
execute(request)           → mapeia para TaskComplexity.HIGH
executeRoundRobin(request) → mapeia para TaskComplexity.LOW
executeOrNull(request)     → HIGH com retorno null em vez de exceção
```

---

## 10. Diagrama Completo do Fluxo

```
translateLexicon() inicia
        │
        ▼
  ┌─────────────┐
  │ Semaphore(8) │ ← controla concorrência máxima
  └──────┬──────┘
         │
    ┌────┼────┬────┬────┬────┬────┬────┐
    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼   (8 coroutines simultâneas)
    │    │    │    │    │    │    │    │
    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼
  Round-Robin seleciona provider para cada chamada
    │    │    │    │    │    │    │    │
    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼
   DS   DS   DS  Llama DS  Gemma DS   DS
    │    │    │    │    │    │    │    │
    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼
  LlmRateLimiter (por provider):
    ├─ minDelay entre chamadas
    ├─ proactive throttle se remaining ≤ 2
    └─ retry com backoff exponencial se 429/500
    │    │    │    │    │    │    │    │
    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼
  HTTP POST → API do provider
    │    │    │    │    │    │    │    │
    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼
  Resposta → parse → salva no banco
    │    │    │    │    │    │    │    │
    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼
  Libera vaga do Semaphore → próxima coroutine entra
```

---

## Glossário Rápido

| Termo | Significado |
|-------|-------------|
| **Coroutine** | Unidade de execução assíncrona do Kotlin (leve, não bloqueia thread) |
| **Semaphore** | Controle de concorrência — limita quantas coroutines rodam ao mesmo tempo |
| **Round-Robin** | Distribuição circular de chamadas entre múltiplos providers |
| **Rate Limit** | Limite de requisições/tokens por minuto imposto pelo provider |
| **RPM / TPM** | Requests Per Minute / Tokens Per Minute |
| **429** | HTTP status "Too Many Requests" — rate limit atingido |
| **Retry** | Tentar novamente após falha |
| **Backoff Exponencial** | Espera que dobra a cada tentativa (5s → 10s → 20s → ...) |
| **Jitter** | Variação aleatória no backoff para evitar efeito manada |
| **Min Delay** | Intervalo mínimo forçado entre chamadas ao mesmo provider |
| **Proactive Throttle** | Desacelerar antes de atingir o limite (baseado nos headers) |
| **Batch** | Agrupar múltiplos itens em uma única chamada LLM |
| **Fallback** | Se provider A falha, tenta provider B automaticamente |
| **Mutex** | Trava que garante acesso exclusivo a um recurso (1 coroutine por vez) |
| **AtomicInteger** | Contador thread-safe para uso em coroutines paralelas |
