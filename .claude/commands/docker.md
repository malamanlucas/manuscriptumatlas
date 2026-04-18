Você está trabalhando na infra Docker do Manuscriptum Atlas. Siga estas regras:

## Serviços
| Serviço | Porta | Função |
|---------|-------|--------|
| postgres | 5432 | PostgreSQL 16 (bancos `nt_coverage` + `bible_db`) |
| kafka | 9092 | KRaft mode — resultados LLM processados |
| init | — | Restore opcional via RESTORE_FILE |
| app | 8080 | Backend Ktor |
| frontend | 3000 | Next.js |
| prometheus | 9091 | Métricas (profile `monitoring`) |
| grafana | 3001 | Dashboards (profile `monitoring`) |
| loki | 3100 | Agregação de logs |
| promtail | — | Log shipper |

## Volumes
- `pgdata` — dados PostgreSQL
- `source-cache` — cache de fontes externas (scrapers)
- `bible-data` — stateful do banco `bible_db`
- `kafka-data`, `prometheus-data`, `grafana-data`, `loki-data` — stateful da stack de observability/messaging

## Variáveis de ambiente (app)
- **DB:** `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- **Ingestão:** `ENABLE_INGESTION`, `ENABLE_MANUSCRIPT_INGESTION`, `ENABLE_PATRISTIC_INGESTION`, `ENABLE_COUNCIL_INGESTION`, `INGESTION_SKIP_IF_POPULATED`, `INGESTION_TIMEOUT_MINUTES`
- **NTVMR:** `USE_NTVMR`, `NTVMR_DELAY_MS`, `LOAD_MANUSCRIPTS_FROM_NTVMR`
- **LLM (síncrono):** `ANTHROPIC_API_KEY`, `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_TIMEOUT_MS`, `ENABLE_BIO_SUMMARIZATION`, `ENABLE_BIO_TRANSLATION`, `ENABLE_DATING_ENRICHMENT`
- **LLM Queue/Kafka:** `KAFKA_BOOTSTRAP_SERVERS`, `LLM_QUEUE_ENABLED`, `SERVICE_CLIENT_ID`, `SERVICE_CLIENT_SECRET`
- **LLM Tiers (concorrência):** `LLM_TIER_LOW_CONCURRENCY` (80), `LLM_TIER_MEDIUM_CONCURRENCY` (40), `LLM_TIER_HIGH_CONCURRENCY` (15)
- **Auth:** `GOOGLE_CLIENT_ID`, `JWT_SECRET`
- **Outros:** `SOURCE_CACHE_DIR`, `NEW_RELIC_ENVIRONMENT`

## Build args (frontend)
`BACKEND_URL` (default `http://app:8080`), `NEXT_PUBLIC_GOOGLE_CLIENT_ID`

## API Rewrite (Next.js)
`/api/*` → `${BACKEND_URL}/*` (proxy transparente)

## Armadilha
O serviço `app` usa DNS customizado (`dns:`) para acessar APIs externas. Se chamadas HTTP falharem, verificar conectividade de rede.

## Comandos dev frequentes

```bash
# Subir tudo
docker compose -f deploy/docker-compose.yml up -d --build

# Rebuild só frontend (mais rápido)
docker compose -f deploy/docker-compose.yml up -d --build frontend

# Restart backend (para ingestion stuck)
docker restart deploy-app-1

# Dev login (JWT admin sem Google SSO)
curl -s -X POST http://localhost:8080/auth/dev-login

# Logs backend (últimas 50 linhas)
docker logs deploy-app-1 --tail 50

# Logs frontend
docker logs deploy-frontend-1 --tail 50

# Build backend JAR
cd backend && ./gradlew build -x test
```

Analise o pedido do usuário: $ARGUMENTS
