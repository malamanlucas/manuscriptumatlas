---
name: infra-docker
description: Docker Compose structure and environment variables. Use when working on Docker, docker-compose, infra, deploy, ambiente, variável, or build.
---

# Infra Docker — Estrutura

## Serviços

| Serviço | Imagem/Build | Função |
|---------|--------------|--------|
| postgres | postgres:16-alpine | Banco de dados (porta 5432) |
| init | postgres:16-alpine | Restore opcional via RESTORE_FILE (backups/) |
| app | build: . | Backend Ktor (porta 8080) |
| frontend | build: ./frontend | Next.js (porta 3000) |

## Volumes

- `pgdata` — dados do PostgreSQL
- `source-cache` — cache de fontes externas (scrapers)

## Variáveis de ambiente (app)

DB: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`  
Ingestão: `ENABLE_INGESTION`, `ENABLE_MANUSCRIPT_INGESTION`, `ENABLE_PATRISTIC_INGESTION`, `ENABLE_COUNCIL_INGESTION`, `INGESTION_SKIP_IF_POPULATED`, `INGESTION_TIMEOUT_MINUTES`  
NTVMR: `USE_NTVMR`, `NTVMR_DELAY_MS`, `LOAD_MANUSCRIPTS_FROM_NTVMR`  
OpenAI: `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_TIMEOUT_MS`, `ENABLE_BIO_SUMMARIZATION`, `ENABLE_BIO_TRANSLATION`, `ENABLE_DATING_ENRICHMENT`  
Auth: `GOOGLE_CLIENT_ID`  
Outros: `SOURCE_CACHE_DIR`, `NEW_RELIC_ENVIRONMENT`

## Build args (frontend)

`BACKEND_URL`, `NEXT_PUBLIC_GOOGLE_CLIENT_ID`

## Armadilha

O serviço `app` usa DNS customizado (`dns:`) para acessar APIs externas dentro do Docker. Se chamadas HTTP falharem, verificar conectividade de rede.
