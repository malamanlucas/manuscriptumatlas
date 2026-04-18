Você está trabalhando no backend Kotlin/Ktor/Exposed do Manuscriptum Atlas. Siga estas regras:

## Armadilhas
- Rotas literais ANTES de dinâmicas no Ktor (ex: `/fathers/statements` antes de `/fathers/{id}`)
- `transaction {}` no Repository, nunca no Service
- Serialização: sempre `@Serializable` (kotlinx), nunca Jackson
- Retry com backoff para chamadas externas (NTVMR, LLM)

## Pacotes (`backend/src/main/kotlin/com/ntcoverage/`)
- `config/` — DatabaseConfig, FlywayConfig, IngestionConfig, LocaleConfig, RateLimiter, SourceFileCache
- `llm/` — LlmConfig + providers (Anthropic/OpenAI/DeepSeek/OpenRouter). Orquestração síncrona em `LlmOrchestrator`. Assíncrono: `LlmQueueRepository` + Kafka `LlmResultsConsumer` (preferido para massa)
- `model/` — Tables.kt (tabelas Exposed), DTOs.kt (data classes @Serializable)
- `repository/` — um por domínio (ver diretório para lista atual)
- `service/` — lógica de negócio (ver diretório para lista atual)
- `routes/` — endpoints REST (ver diretório para lista atual)
- `scraper/` — extractors de fontes externas (Schaff, Hefele, CatholicEnc, Fordham, Wikipedia, Wikidata, NTVMR)
- `seed/` — dados iniciais e traduções (ver diretório para lista atual)
- `util/` — JwtUtil, NtvmrUrl

## LLM — 2 caminhos
- **Assíncrono (padrão massa):** enfileira em `llm_prompt_queue` via `LlmQueueRepository`. Drenado pelo Claude Code (`/drain-queue`, `/run-llm`). Resultados via Kafka (`LlmResultsConsumer`).
- **Síncrono (legado):** `LlmOrchestrator` com fallback encadeado Anthropic → OpenAI → DeepSeek → OpenRouter. Interface `LlmProvider.chatCompletion()`. Usado em endpoints request-scoped (ex: apologetics user-facing).

## Novo endpoint
1. Route em `routes/` — recebe Service via construtor
2. Service em `service/` — recebe Repository via construtor
3. Repository em `repository/` — usa `transaction {}` para queries
4. Registrar no `Application.kt`: instanciar, conectar rotas

## Novo domínio completo (10 passos)
1. Table em `Tables.kt` 2. DTOs em `DTOs.kt` 3. Repository 4. Service 5. Routes 6. Application.kt 7. FlywayConfig.kt (SchemaUtils) 8. Migration SQL 9. Seed 10. Frontend (i18n + responsividade)

Analise o pedido do usuário: $ARGUMENTS
