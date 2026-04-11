# Arquitetura de Tradução com IA — Manuscriptum Atlas

Documentacao completa de como o sistema usa Inteligencia Artificial (LLMs) para traduzir
conteudo biblico, patristico e conciliar do ingles para portugues e espanhol.

---

## 1. Visao Geral

O Manuscriptum Atlas traduz **4 dominios** de conteudo usando LLMs:

| Dominio | O que traduz | Volume |
|---------|-------------|--------|
| **Lexico Biblico** | Definicoes de palavras gregas e hebraicas (Strong's) | ~21.694 grego + ~16.362 hebraico |
| **Glosses Interlineares** | Glosses palavra-a-palavra do NT/AT | Milhares de versiculos |
| **Enriquecimento Lexico** | Campos avancados (KJV, etymologia, NAS) | ~5.624 grego + ~8.674 hebraico |
| **Patristica** | Biografias dos Pais da Igreja | ~50 biografias |
| **Concilios** | Nomes, descricoes, resumos de concilios | ~40 concilios |
| **Heresias** | Nomes e descricoes de heresias | ~30 heresias |

**Idioma fonte:** Ingles
**Idiomas alvo:** Portugues (pt) e Espanhol (es)

---

## 2. Provider de IA: OpenAI API (3-Tier)

O sistema usa um unico provider ativo: o **OpenAI API**, que roteia chamadas
para a API da OpenAI com 3 tiers de modelos (LOW/MEDIUM/HIGH).

```
┌─────────────────────────────────────────────────────────────┐
│                    OpenAI API                          │
│            (rodando localmente na porta 18789)               │
│                                                              │
│  Endpoint: POST /v1/chat/completions                        │
│  Protocolo: OpenAI-compatible                               │
│  Auth: Bearer token (OPENAI_API_KEY)                │
│  Custo: por token (varia por modelo)                        │
│                                                              │
│  Agentes disponiveis:                                       │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │  Default Agent   │  │  Opus Agent      │                 │
│  │  (Sonnet/padrao) │  │  (opus-ingest)   │                 │
│  │  Tarefas simples │  │  Raciocinio      │                 │
│  │  de traducao     │  │  complexo        │                 │
│  └──────────────────┘  └──────────────────┘                 │
│                                                              │
│  Header de roteamento: model (per-tier)                  │
│  Sem ID → agente padrao | "opus-ingest" → agente Opus       │
└─────────────────────────────────────────────────────────────┘
```

### Providers legado (desativados, mantidos como referencia)

| Provider | Modelo | Status | Custo (USD/1M tokens) |
|----------|--------|--------|-----------------------|
| OpenAI HIGH | gpt-5.4 | Ativo | $5 input / $15 output |
| OpenAI | gpt-5.4 | Desativado | $5 input / $15 output |
| DeepSeek | deepseek-chat | Desativado | $0.28 input / $0.42 output |
| OpenRouter | Multiplos modelos | Desativado | Variavel |

---

## 3. Arquitetura de Componentes

```
┌──────────────────────────────────────────────────────────────────┐
│                         SERVICES                                  │
│                                                                    │
│  BibleIngestionService        PatristicIngestionService           │
│  ├─ translateLexicon()        ├─ runTranslateBiographies()       │
│  ├─ translateHebrewLexicon()  ├─ runTranslateFathers() (seed)    │
│  ├─ translateGlosses()        └─ runTranslateStatements() (seed) │
│  ├─ translateEnrichmentGreek()                                    │
│  └─ translateEnrichmentHebrew()                                   │
│                                                                    │
│  CouncilIngestionService      BiographySummarizationService      │
│  ├─ phase7TranslateAll()      ├─ translateBiography()            │
│  ├─ phase8TranslateHeresies() ├─ translateCouncilFields()        │
│  └─ phase9OverviewEnrichment()├─ translateHeresyFields()         │
│                                └─ generateCouncilOverview()       │
└───────────────────────────────────┬──────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────┐
│                       LlmOrchestrator                             │
│                                                                    │
│  execute()          → tenta providers em ordem de prioridade      │
│  executeRoundRobin()→ distribui carga com counter atomico         │
│  executeOrNull()    → retorna null se todos falharem              │
│                                                                    │
│  Cada chamada:                                                     │
│  1. Seleciona provider disponivel                                 │
│  2. Envia LlmRequest (systemPrompt + userContent)                │
│  3. Recebe LlmResponse (content + tokens + provider info)        │
│  4. Registra metricas no LlmUsageService                         │
│  5. Se falhar → tenta proximo provider                            │
└───────────────────────────────────┬──────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────┐
│                       OpenAiProvider                            │
│                                                                    │
│  POST /v1/chat/completions                                        │
│  Headers: Authorization: Bearer {token}                           │
│           model (per-tier): {agentId} (opcional)               │
│                                                                    │
│  Body: { model, messages: [{role, content}], temperature, max }   │
│  Response: { choices: [{message}], usage: {tokens} }              │
└───────────────────────────────────┬──────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────┐
│                       LlmUsageService                             │
│                                                                    │
│  Registra cada chamada na tabela llm_usage_logs:                  │
│  provider, model, label, success, tokens, custo, latencia, erro   │
│                                                                    │
│  Dashboard em /admin/llm/usage com metricas por periodo           │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. Tipos de Dados e Interfaces

```kotlin
// Requisicao para o LLM
data class LlmRequest(
    val systemPrompt: String,   // instrucao de sistema (papel do tradutor)
    val userContent: String,    // conteudo a traduzir
    val temperature: Double,    // 0.3 (baixa criatividade, alta fidelidade)
    val maxTokens: Int,         // limite de tokens na resposta
    val label: String           // identificador para logs (ex: "LEXICON_TRANSLATE_G1234")
)

// Resposta do LLM
data class LlmResponse(
    val content: String,        // texto traduzido
    val provider: LlmProviderType,  // OPENAI, ANTHROPIC, etc.
    val model: String,          // "gpt-5.4", "gpt-5.4", etc.
    val inputTokens: Int,       // tokens consumidos no prompt
    val outputTokens: Int,      // tokens gerados na resposta
    val totalTokens: Int        // input + output
)

// Providers disponiveis
enum class LlmProviderType { OPENAI }
```

---

## 5. Pipelines de Traducao

### 5.1 Lexico Biblico (Grego e Hebraico)

**Fases:** `bible_translate_lexicon` (grego), `bible_translate_hebrew_lexicon` (hebraico)

```
┌─────────────────────────────────────────────────────────────┐
│  translateLexicon("greek")                                   │
│                                                              │
│  1. Busca todas as entries do lexico (ex: 10.847 gregas)    │
│  2. Filtra as que ja tem traducao no locale                 │
│  3. Agrupa em batches (LLM_MEDIUM_BATCH_SIZE, padrao 20)      │
│  4. Processa em paralelo (Semaphore, padrao 15 slots)       │
│                                                              │
│  Para cada batch:                                            │
│  ┌────────────────────────────────────────────────────┐     │
│  │  LLM recebe:                                       │     │
│  │  "Translate these Greek lexicon entries to          │     │
│  │   Portuguese/Spanish..."                            │     │
│  │                                                     │     │
│  │  [G3056]                                            │     │
│  │  SHORT: word, speech, reason                        │     │
│  │  FULL: a word, speech, divine utterance...          │     │
│  │  [G0026]                                            │     │
│  │  SHORT: love, benevolence                           │     │
│  │  FULL: love, affection, goodwill...                 │     │
│  ├────────────────────────────────────────────────────┤     │
│  │  LLM retorna:                                      │     │
│  │                                                     │     │
│  │  [G3056]                                            │     │
│  │  SHORT: palavra, discurso, razao                    │     │
│  │  FULL: uma palavra, discurso, pronunciamento...     │     │
│  │  [G0026]                                            │     │
│  │  SHORT: amor, benevolencia                          │     │
│  │  FULL: amor, afeicao, boa vontade...                │     │
│  └────────────────────────────────────────────────────┘     │
│                                                              │
│  5. Parse da resposta via regex (SHORT/FULL por Strong's #) │
│  6. Upsert em greek_lexicon_translations (ou hebrew_)       │
└─────────────────────────────────────────────────────────────┘
```

**Prompt do sistema (lexico):**
```
Translate these Greek lexicon entries to {Language}.
Return ONLY translations in this exact format, one block per entry:

[STRONG_NUMBER]
SHORT: translated short definition
FULL: translated full definition

Rules:
- Translate ALL English text to {Language}
- Keep Greek/Hebrew words and Bible references unchanged
- Keep Strong's numbers (G3056, H7225) unchanged
- Use academic biblical terminology
- Preserve numbered definitions (1., 2., etc.)
```

**Fallback:** Se o batch falhar, tenta traduzir entry por entry individualmente.

**Tabelas destino:** `greek_lexicon_translations`, `hebrew_lexicon_translations`

---

### 5.2 Glosses Interlineares

**Fase:** `bible_translate_glosses`

Traduz glosses palavra-a-palavra do texto interlinear (grego/hebraico → portugues/espanhol).

```
┌─────────────────────────────────────────────────────────────┐
│  translateGlosses()                                          │
│                                                              │
│  1. Percorre capitulo por capitulo de cada livro             │
│  2. Busca palavras interlineares sem gloss traduzido         │
│  3. Agrupa em chunks de 100 glosses                          │
│  4. Traduz PT e ES em paralelo (async)                       │
│                                                              │
│  Prompt:                                                     │
│  "Translate each English gloss (one per line) to {Language}" │
│                                                              │
│  Entrada:         Saida:                                     │
│  the         →    o                                          │
│  word        →    palavra                                    │
│  [was]       →    [era]                                      │
│  in          →    em                                          │
│  <the>       →    <o>                                        │
│  beginning   →    principio                                  │
│                                                              │
│  5. Atualiza campos portugueseGloss / spanishGloss           │
│     na tabela interlinear_words                              │
└─────────────────────────────────────────────────────────────┘
```

**Regras do prompt:**
- Manter colchetes `[]`, angulares `<>` e pontuacao
- Traducoes curtas (palavra ou frase muito breve)
- Sem explicacoes — apenas o gloss traduzido, um por linha

---

### 5.3 Enriquecimento de Lexico

**Fases:** `bible_translate_enrichment_greek`, `bible_translate_enrichment_hebrew`

Traduz campos avancados que foram obtidos via scraping do BibleHub (etymologia, uso exaustivo, etc).

```
┌─────────────────────────────────────────────────────────────┐
│  translateEnrichmentLexicon("greek")                         │
│                                                              │
│  Campos traduzidos:                                          │
│  ┌──────────────────────┬─────────────────────────────┐     │
│  │ KJV_TRANSLATION      │ Traducao usada na King James│     │
│  │ WORD_ORIGIN           │ Etymologia da palavra       │     │
│  │ STRONGS_EXHAUSTIVE    │ Concordancia exaustiva      │     │
│  │ NAS_ORIGIN            │ Origem no NAS               │     │
│  │ NAS_DEFINITION        │ Definicao no NAS            │     │
│  │ NAS_TRANSLATION       │ Traducao no NAS             │     │
│  └──────────────────────┴─────────────────────────────┘     │
│                                                              │
│  Concorrencia: 15 coroutines (Semaphore)                    │
│  Cada entry e traduzida individualmente (nao em batch)       │
│  Progresso atualizado a cada 50 itens (Mutex protege DB)    │
│  Retry com backoff exponencial para erros de serializacao    │
└─────────────────────────────────────────────────────────────┘
```

**Prompt do sistema:**
```
Translate these lexicon fields to {Language}.
Keep Greek/Hebrew words, Strong's numbers (G1234, H5678),
and Bible references unchanged.
Use academic biblical terminology.
```

---

### 5.4 Biografias Patristicas

**Fase:** `patristic_translate_biographies`

Traduz biografias completas dos Pais da Igreja.

```
┌─────────────────────────────────────────────────────────────┐
│  runTranslateBiographies()                                   │
│                                                              │
│  Para cada Pai da Igreja (ex: Clemente de Roma):             │
│  1. Busca biografia original em ingles                       │
│  2. Chama BiographySummarizationService.translateBiography() │
│  3. Salva em church_father_translations                      │
│     com translation_source = "machine"                       │
│                                                              │
│  Campos traduzidos:                                          │
│  - displayName (nome de exibicao)                            │
│  - shortDescription (descricao curta)                        │
│  - primaryLocation (local principal)                         │
│  - mannerOfDeath (forma de morte)                            │
│  - biographyOriginal (biografia completa)                    │
│  - biographySummary (resumo da biografia)                    │
└─────────────────────────────────────────────────────────────┘
```

**Prompt do sistema:**
```
Translate this Church Father biography completely to {Language}.
Translate every sentence — leave nothing in English.
Preserve proper names, dates, and references.
Use academic ecclesiastical terminology. Neutral tone.
```

---

### 5.5 Concilios Eclesiasticos

**Fase:** `council_translate_all`

```
┌─────────────────────────────────────────────────────────────┐
│  phase7TranslateAll()                                        │
│                                                              │
│  Para cada concilio (ex: Concilio de Niceia):                │
│  1. Monta JSON com campos: displayName, shortDescription,   │
│     location, mainTopics, summary                            │
│  2. LLM retorna JSON traduzido                              │
│  3. Parse e upsert em council_translations                   │
│                                                              │
│  Exemplo de entrada (JSON):                                  │
│  {                                                           │
│    "displayName": "Council of Nicaea",                       │
│    "shortDescription": "First ecumenical council...",        │
│    "location": "Nicaea, Bithynia",                           │
│    "mainTopics": "Arianism, Easter date, Creed"              │
│  }                                                           │
│                                                              │
│  Exemplo de saida (JSON):                                    │
│  {                                                           │
│    "displayName": "Concilio de Niceia",                      │
│    "shortDescription": "Primeiro concilio ecumenico...",     │
│    "location": "Niceia, Bitinia",                            │
│    "mainTopics": "Arianismo, data da Pascoa, Credo"          │
│  }                                                           │
└─────────────────────────────────────────────────────────────┘
```

**Prompt do sistema:**
```
Translate these council data fields to {Language}. Return ONLY valid JSON with same keys.
Use correct ecclesiastical terms: 'Council' → 'Concilio'/'Concilio',
'Synod' → 'Sinodo', 'Ecumenical' → 'Ecumenico'/'Ecumenico'.
Use academic names for locations. Preserve dates. JSON only, no extra text.
```

---

### 5.6 Heresias

**Fase:** `heresy_translate_all`

Mesmo padrao dos concilios, traduz `name` e `description` de cada heresia.

**Prompt do sistema:**
```
Translate these heresy data fields to {Language}. Return ONLY valid JSON with same keys.
Use correct ecclesiastical terminology. Preserve proper nouns.
Academic tone. JSON only, no extra text.
```

---

### 5.7 Resumos de Concilios (Geracao + Traducao)

**Fase:** `council_overview_enrichment`

Diferente das outras fases, esta **gera** conteudo novo com IA antes de traduzir.

```
┌─────────────────────────────────────────────────────────────┐
│  phase9OverviewEnrichment()                                  │
│                                                              │
│  1. GERAR: LLM recebe metadados do concilio e gera          │
│     um resumo de 3-5 frases em ingles                        │
│     → generateCouncilOverviewFromMetadata()                  │
│                                                              │
│  2. TRADUZIR: Resumo gerado e traduzido para pt/es           │
│     → translateAndSaveCouncilSummary()                       │
│                                                              │
│  Prompt de geracao:                                          │
│  "Write a brief 3-5 sentence overview of this               │
│   ecclesiastical council based on the metadata provided.     │
│   Neutral academic tone. Do not invent facts."               │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. Modelo de Concorrencia

```
┌──────────────────────────────────────────────────────────────┐
│                   Controle de Concorrencia                    │
│                                                               │
│  LLM_CONCURRENCY = 15 (env var, padrao)                 │
│                                                               │
│  ┌────────────────────────────────────────┐                  │
│  │          Semaphore(15)                 │                  │
│  │                                        │                  │
│  │  15 coroutines executam em paralelo    │                  │
│  │  Demais aguardam na fila               │                  │
│  │                                        │                  │
│  │  [C1][C2][C3]...[C15]  ← executando   │                  │
│  │  [C16][C17][C18]...    ← aguardando   │                  │
│  │                                        │                  │
│  │  Quando C1 termina → C16 entra        │                  │
│  └────────────────────────────────────────┘                  │
│                                                               │
│  Cada coroutine:                                              │
│  1. Adquire permissao do Semaphore                           │
│  2. Chama LLM (pode levar 1-30 segundos)                    │
│  3. Faz upsert no banco de dados                             │
│  4. Libera permissao do Semaphore                            │
│                                                               │
│  Phase Tracking (protegido por Mutex):                       │
│  - markProgress() atualiza a cada 50 itens                   │
│  - Mutex impede updates concorrentes na mesma row            │
│  - Evita PSQLException de serializacao                       │
└──────────────────────────────────────────────────────────────┘
```

---

## 7. Estrategias do Orchestrator

### 7.1 Execute (prioridade sequencial)

Usado para tarefas unicas (biografias, concilios, heresias).

```
execute(request):
  providers = [OPENAI]  (ordenados por prioridade)

  para cada provider:
    try:
      resposta = provider.chatCompletion(request)
      registra sucesso no LlmUsageService
      return resposta
    catch NonRetryable:
      registra erro, pula para proximo
    catch Exception:
      registra erro, pula para proximo

  throw AllProvidersFailedException
```

### 7.2 ExecuteRoundRobin (distribuicao de carga)

Usado para traducoes em massa (lexico, enrichment).

```
executeRoundRobin(request):
  counter atomico incrementa a cada chamada
  startIdx = counter % totalProviders

  Chamada #0  → provider[0]
  Chamada #1  → provider[1]
  Chamada #2  → provider[0]  (volta ao inicio com 2 providers)
  ...

  Se o provider selecionado falhar → tenta o proximo na lista
```

### 7.3 ExecuteOrNull (tolerante a falhas)

Usado quando a traducao e opcional (ex: overview enrichment).

```
executeOrNull(request):
  try:
    return execute(request)
  catch AllProvidersFailedException:
    log.error("todos falharam")
    return null  ← continua sem traduzir
```

---

## 8. Resiliencia e Tratamento de Erros

### 8.1 Rate Limiter

```
┌─────────────────────────────────────────────────────────────┐
│                     LlmRateLimiter                           │
│                                                              │
│  (1) Min Delay (Mutex)                                       │
│      Intervalo minimo entre chamadas ao mesmo provider       │
│      Default: 500ms                                          │
│                                                              │
│  (2) Proactive Throttle                                      │
│      Le header x-ratelimit-remaining-requests                │
│      Se <= 2 restantes → pausa ate reset time                │
│                                                              │
│  (3) Retry com Exponential Backoff + Jitter                  │
│      HTTP 429 → espera com backoff crescente:                │
│      5s → 10s → 20s → 40s → 60s (cap)                       │
│      + fator aleatorio (1.0x a 1.5x)                         │
│      Max retries: 5                                          │
│                                                              │
│  (4) Non-Retryable Detection                                 │
│      HTTP 401/403 → erro de auth, falha imediata             │
│      Nao desperdiça retries em erros permanentes             │
│                                                              │
│  (5) Server Error Retry                                      │
│      HTTP 500+ → retry com backoff                           │
│      Max retries: 5                                          │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 Retry de Serializacao (banco de dados)

Quando multiplas coroutines tentam atualizar a mesma row no PostgreSQL
(sob isolation level REPEATABLE_READ), o banco lanca:

```
PSQLException: could not serialize access due to concurrent update
```

Solucao implementada:

```kotlin
retryOnSerializationError(maxRetries = 3) {
    // operacao de banco que pode falhar
}

// Backoff: 100ms → 200ms → 400ms
// So retenta erros de serializacao
// Outros erros sao propagados normalmente
```

### 8.3 Idempotencia

Toda fase de traducao verifica se o item ja foi traduzido antes de chamar o LLM:

```
hasGreekTranslation(lexiconId, locale)     → pula se ja existe
hasGreekEnrichmentTranslation(id, locale)  → pula se ja existe
hasTranslation(fatherId, locale)           → pula se ja existe
```

Beneficio: pode parar e retomar qualquer fase sem retraduzir itens ja concluidos.

### 8.4 Fallback Batch → Individual

Na traducao de lexico, se o batch de 20 entries falhar:

```
translateLexiconBatch(batch_de_20)
  │
  ├── SUCESSO → batchUpsert(20 traducoes de uma vez)
  │
  └── FALHA → for entry in batch:
                translateLexiconEntry(entry)  // 1 por 1
                upsert(entry)
```

### 8.5 Hierarquia de Exceptions

```
LlmNonRetryableException
  → Erro de auth (401/403) ou billing
  → Nao retenta, pula para proximo provider

LlmAllProvidersFailedException
  → Todos os providers falharam
  → Contém mapa de erros por provider
  → Tratado no service (log + skip ou throw)
```

---

## 9. Monitoramento (LLM Usage Dashboard)

Toda chamada ao LLM e registrada na tabela `llm_usage_logs`:

| Campo | Descricao |
|-------|-----------|
| `provider` | OPENAI, ANTHROPIC, etc. |
| `model` | gpt-5.4, gpt-4.1-mini, etc. |
| `label` | Identificador da tarefa (ex: `LEXICON_TRANSLATE_G3056_pt`) |
| `success` | true/false |
| `input_tokens` | Tokens consumidos no prompt |
| `output_tokens` | Tokens gerados na resposta |
| `estimated_cost_usd` | Custo estimado (por modelo) |
| `latency_ms` | Tempo de resposta em milissegundos |
| `error_message` | Mensagem de erro (se falhou) |
| `created_at` | Timestamp da chamada |

### Endpoints de monitoramento

```
GET /admin/llm/usage?period={periodo}
    Periodos: 5m, 15m, 30m, 1h, 3h, 6h, 12h, today, 7d, 30d, all
    Retorna: dashboard com sumario por provider, modelo, erros

GET /admin/llm/usage/errors?type={tipo}&limit=20&period=7d
    Tipos: TIMEOUT, RATE_LIMIT, BAD_REQUEST, AUTH_ERROR, SERVER_ERROR, OTHER

GET /admin/llm/usage/logs?limit=50&provider={provider}
    Retorna: logs recentes com filtro opcional por provider

GET /admin/llm/rate-limits
    Retorna: status dos rate limiters (remaining requests/tokens)
```

### Estimativa de custo por provider

| Provider | Input (USD/1M tokens) | Output (USD/1M tokens) |
|----------|----------------------|------------------------|
| gpt-4.1-mini (LOW) | $0.40 | $1.60 |
| gpt-5.4 (MEDIUM/HIGH) | $5.00 | $15.00 |
| DeepSeek | $0.28 | $0.42 |
| OpenRouter | $0.00 | $0.00 (modelos free) |

---

## 10. Schema do Banco (Tabelas de Traducao)

### Traducoes do Lexico Biblico

```sql
-- Traducoes do lexico grego (definicoes + enriquecimento)
greek_lexicon_translations (
    id              SERIAL PRIMARY KEY,
    lexicon_id      INT REFERENCES greek_lexicon(id),
    locale          VARCHAR(5),           -- "pt" ou "es"
    short_definition TEXT,                -- definicao curta traduzida
    full_definition  TEXT,                -- definicao completa traduzida
    kjv_translation  TEXT,                -- traducao KJV (enriquecimento)
    word_origin      TEXT,                -- etymologia (enriquecimento)
    strongs_exhaustive TEXT,              -- concordancia (enriquecimento)
    nas_exhaustive_origin TEXT,           -- origem NAS (enriquecimento)
    nas_exhaustive_definition TEXT,       -- definicao NAS (enriquecimento)
    nas_exhaustive_translation TEXT,      -- traducao NAS (enriquecimento)
    UNIQUE(lexicon_id, locale)
)

-- Mesma estrutura para hebraico
hebrew_lexicon_translations ( ... )
```

### Glosses Interlineares

```sql
-- Campos de gloss traduzidos diretamente na tabela de palavras
interlinear_words (
    ...
    english_gloss    TEXT,    -- gloss original em ingles
    portuguese_gloss TEXT,    -- gloss traduzido para PT
    spanish_gloss    TEXT,    -- gloss traduzido para ES
    ...
)
```

### Traducoes Patristicas

```sql
church_father_translations (
    id                 SERIAL PRIMARY KEY,
    father_id          INT REFERENCES church_fathers(id),
    locale             VARCHAR(5),
    display_name       TEXT,
    short_description  TEXT,
    primary_location   TEXT,
    manner_of_death    TEXT,
    biography_original TEXT,
    biography_summary  TEXT,
    translation_source VARCHAR(20),  -- "seed", "machine", "reviewed"
    UNIQUE(father_id, locale)
)

father_statement_translations (
    id              SERIAL PRIMARY KEY,
    statement_id    INT REFERENCES father_textual_statements(id),
    locale          VARCHAR(5),
    statement_text  TEXT,
    UNIQUE(statement_id, locale)
)
```

### Traducoes de Concilios e Heresias

```sql
council_translations (
    id                SERIAL PRIMARY KEY,
    council_id        INT REFERENCES councils(id),
    locale            VARCHAR(5),
    display_name      TEXT,
    short_description TEXT,
    location          TEXT,
    main_topics       TEXT,
    summary           TEXT,
    translation_source VARCHAR(20),
    UNIQUE(council_id, locale)
)

heresy_translations (
    id                SERIAL PRIMARY KEY,
    heresy_id         INT REFERENCES heresies(id),
    locale            VARCHAR(5),
    name              TEXT,
    description       TEXT,
    translation_source VARCHAR(20),
    UNIQUE(heresy_id, locale)
)
```

---

## 11. Variaveis de Ambiente

| Variavel | Default | Descricao |
|----------|---------|-----------|
| `OPENAI_API_KEY` | *(obrigatorio)* | Chave da API OpenAI |
| `OPENAI_API_KEY` | *(obrigatorio)* | Token de autenticacao |
| `OPENAI_MEDIUM_MODEL` | `gpt-5.4` | Modelo a usar no gateway |
| `LLM_CONCURRENCY` | `15` | Max coroutines paralelas para traducao |
| `LLM_MEDIUM_BATCH_SIZE` | `20` | Entries por batch na traducao de lexico |
| `LLM_ALIGNMENT_DELAY_MS` | `50` | Delay entre operacoes de alinhamento |
| `LLM_MEDIUM_TIMEOUT_MS` | `60000` | Timeout por chamada LLM (ms) |
| `OPENAI_HIGH_MODEL` | *(opcional)* | ID do agente Opus para round-robin |
| `ENABLE_BIO_SUMMARIZATION` | `true` | Habilitar geracao de resumos |
| `ENABLE_BIO_TRANSLATION` | `true` | Habilitar traducao de biografias |
| `ENABLE_DATING_ENRICHMENT` | `false` | Habilitar enriquecimento de datas via LLM |

---

## 12. Fases de Ingestao (Ordem Completa)

### Pipeline Biblico

| # | Fase | Usa LLM? | Descricao |
|---|------|----------|-----------|
| 1 | `bible_seed_versions` | Nao | Seed das versoes biblicas |
| 2 | `bible_seed_books` | Nao | Seed dos 66 livros |
| 3 | `bible_seed_abbreviations` | Nao | Seed das abreviacoes |
| 4-7 | `bible_ingest_text_*` | Nao | Ingere textos (KJV, AA, ACF, ARC69) |
| 8-9 | `bible_ingest_*_interlinear` | Nao | Ingere dados interlineares |
| 10-11 | `bible_ingest_*_lexicon` | Nao | Ingere lexico grego/hebraico |
| **12** | **`bible_translate_lexicon`** | **Sim** | **Traduz lexico grego (PT/ES)** |
| **13** | **`bible_translate_hebrew_lexicon`** | **Sim** | **Traduz lexico hebraico (PT/ES)** |
| **14** | **`bible_translate_glosses`** | **Sim** | **Traduz glosses interlineares** |
| 15-16 | `bible_enrich_*_lexicon` | Nao | Enriquece via BibleHub scraping |
| **17** | **`bible_translate_enrichment_greek`** | **Sim** | **Traduz enriquecimento grego** |
| **18** | **`bible_translate_enrichment_hebrew`** | **Sim** | **Traduz enriquecimento hebraico** |

### Pipeline Patristico

| # | Fase | Usa LLM? | Descricao |
|---|------|----------|-----------|
| 1 | `patristic_seed_fathers` | Nao | Seed dos Pais da Igreja |
| 2 | `patristic_seed_statements` | Nao | Seed dos testemunhos |
| 3 | `patristic_translate_fathers` | Nao | Traducoes seed (pre-traduzidas) |
| 4 | `patristic_translate_statements` | Nao | Traducoes seed (pre-traduzidas) |
| **5** | **`patristic_translate_biographies`** | **Sim** | **Traduz biografias via LLM** |

### Pipeline de Concilios

| # | Fase | Usa LLM? | Descricao |
|---|------|----------|-----------|
| 1 | `council_seed` | Nao | Seed dos concilios |
| 2-5 | `council_extract_*` | Nao | Extracao de fontes academicas |
| 6 | `council_consensus` | Nao | Consolidacao de dados |
| **7** | **`council_translate_all`** | **Sim** | **Traduz concilios (PT/ES)** |
| **8** | **`heresy_translate_all`** | **Sim** | **Traduz heresias (PT/ES)** |
| **9** | **`council_overview_enrichment`** | **Sim** | **Gera + traduz resumos** |

---

## 13. Fluxo Completo (Botao → Banco)

```
 FRONTEND                      BACKEND                           OPENAI API
 ────────                      ───────                           ────────────────

 [▷ Executar]     POST /admin/bible/
                  ingestion/run/
                  bible_translate_lexicon
                        │
                        ▼
                  BibleIngestionService
                  .translateLexicon("greek")
                        │
                        ▼
                  getAllGreekEntries()
                  ← 10.847 entries do banco
                        │
                        ▼
                  Para locale "pt":
                    filtra nao-traduzidas
                    agrupa em batches de 20
                        │
                        ▼
                  coroutineScope {
                    for batch in batches:
                      launch {
                        semaphore.withPermit {
                          executeRoundRobin()  ──────────→  POST /v1/chat/completions
                                                            { model: "gpt-5.4",
                                                              messages: [...],
                                               ←──────────   temperature: 0.3 }
                          parse resposta
                          batchUpsert()  ──→ INSERT INTO greek_lexicon_translations
                        }
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

## 14. Glossario

| Termo | Descricao |
|-------|-----------|
| **LLM** | Large Language Model — modelo de linguagem grande (Claude, GPT, etc.) |
| **OpenAI** | API direta da OpenAI com 3 tiers de modelos (gpt-4.1-mini / gpt-5.4) |
| **Strong's Number** | Identificador unico para cada palavra grega/hebraica (G3056, H7225) |
| **Gloss** | Traducao curta de uma palavra no texto interlinear |
| **Batch** | Grupo de itens processados numa unica chamada LLM |
| **Semaphore** | Controle de concorrencia: limita quantas operacoes rodam em paralelo |
| **Round-Robin** | Distribuicao circular de carga entre providers |
| **Rate Limiter** | Controle de frequencia de chamadas para evitar HTTP 429 |
| **Backoff Exponencial** | Tempo de espera que dobra a cada retry (5s → 10s → 20s → ...) |
| **Jitter** | Variacao aleatoria no backoff para evitar thundering herd |
| **Idempotencia** | Poder executar a mesma operacao varias vezes sem efeito duplicado |
| **Upsert** | INSERT se nao existe, UPDATE se ja existe |
| **Coroutine** | Execucao assincrona leve do Kotlin (mais eficiente que threads) |
| **Mutex** | Trava de exclusao mutua — garante acesso sequencial a um recurso |
| **REPEATABLE_READ** | Nivel de isolamento do PostgreSQL que detecta conflitos de escrita |

---

*Gerado em 2026-03-26 | Manuscriptum Atlas v2.0*
