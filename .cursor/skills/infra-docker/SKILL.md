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
| prometheus | prom/prometheus:v3.2.1 | Coleta metricas (porta 9090) |
| loki | grafana/loki:3.4.2 | Agregacao de logs (porta 3100) |
| promtail | grafana/promtail:3.4.2 | Shipper de logs via Docker socket |
| grafana | grafana/grafana:11.5.2 | Dashboards e visualizacao (porta 3001) |

## Volumes

- `pgdata` — dados do PostgreSQL
- `source-cache` — cache de fontes externas (scrapers)
- `prometheus-data` — dados do Prometheus (metricas, retencao 15d)
- `loki-data` — dados do Loki (logs)
- `grafana-data` — dados do Grafana (dashboards customizados)

## Variáveis de ambiente (app)

DB: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`  
Ingestão: `ENABLE_INGESTION`, `ENABLE_MANUSCRIPT_INGESTION`, `ENABLE_PATRISTIC_INGESTION`, `ENABLE_COUNCIL_INGESTION`, `INGESTION_SKIP_IF_POPULATED`, `INGESTION_TIMEOUT_MINUTES`  
NTVMR: `USE_NTVMR`, `NTVMR_DELAY_MS`, `LOAD_MANUSCRIPTS_FROM_NTVMR`  
OpenAI: `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_TIMEOUT_MS`, `ENABLE_BIO_SUMMARIZATION`, `ENABLE_BIO_TRANSLATION`, `ENABLE_DATING_ENRICHMENT`  
Auth: `GOOGLE_CLIENT_ID`  
Outros: `SOURCE_CACHE_DIR`

## Build args (frontend)

`BACKEND_URL`, `NEXT_PUBLIC_GOOGLE_CLIENT_ID`

## Armadilha

O serviço `app` usa DNS customizado (`dns:`) para acessar APIs externas dentro do Docker. Se chamadas HTTP falharem, verificar conectividade de rede.
