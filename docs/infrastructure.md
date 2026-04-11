# Infrastructure — Docker / Scripts / Environment

## Docker Compose

Location: `deploy/docker-compose.yml` (dev), `deploy/docker-compose.prod.yml` (prod override)

### Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| postgres | postgres:16-alpine | 5432 (dev) / 35857 (prod) | PostgreSQL 16 with healthcheck |
| init | postgres:16-alpine | — | Restore backup + create bible_db |
| kafka | confluentinc/cp-kafka:7.7.0 | 9092 | Kafka broker (KRaft mode, no Zookeeper) |
| app | Custom (Kotlin/Ktor) | 8080 (dev) / 35856 (prod) | Backend API server |
| frontend | Custom (Next.js) | 3000 (dev) / 35855 (prod) | Frontend application |
| prometheus | prom/prometheus:v3.2.1 | 9091 | Metrics collection (scrapes backend /prometheus) |
| loki | grafana/loki:3.4.2 | 3100 | Log aggregation |
| promtail | grafana/promtail:3.4.2 | — | Ships container logs to Loki via Docker socket |
| grafana | grafana/grafana:11.5.2 | 3001 | Dashboards, alerting, log exploration |

### Volumes
- **pgdata** — PostgreSQL data persistence
- **source-cache** — Scraped source documents (Schaff, Hefele, etc.)
- **bible-data** — Bible database files
- **kafka-data** — Kafka broker data (KRaft mode)
- **prometheus-data** — Prometheus TSDB (15d retention)
- **loki-data** — Loki log storage
- **grafana-data** — Grafana state (custom dashboards, preferences)

### Startup Order
```
postgres (healthcheck) → init (restore + bible_db) ─┐
kafka (healthcheck) ─────────────────────────────────┤→ app (backend) → frontend
```

### Dev vs Prod Differences

| Setting | Dev | Prod |
|---------|-----|------|
| ENABLE_INGESTION | false | true |
| NTVMR_DELAY_MS | 50 | 100 |
| OPENAI_MODEL | gpt-4o-mini | gpt-5.4 |
| JVM | default | -Xms128m -Xmx512m |
| Memory limits | none | app: 768M, frontend: 256M |
| DB password | postgres | strong password |
| API keys | hardcoded | env vars |
| Ports | standard | non-standard (358xx) |

## Environment Variables

### Database
```bash
DB_HOST=postgres        DB_PORT=5432        DB_NAME=nt_coverage
DB_USER=postgres        DB_PASSWORD=postgres
DATABASE_URL=jdbc:postgresql://postgres:5432/nt_coverage

BIBLE_DB_HOST=postgres  BIBLE_DB_PORT=5432  BIBLE_DB_NAME=bible_db
BIBLE_DATABASE_URL=jdbc:postgresql://postgres:5432/bible_db
```

### LLM
```bash
ANTHROPIC_API_KEY=sk-ant-...
ANTHROPIC_TIMEOUT_MS=30000
ANTHROPIC_CALL_DELAY_MS=500
ANTHROPIC_MAX_RETRIES=5
ANTHROPIC_BACKOFF_MS=5000

OPENAI_API_KEY=sk-proj-...
OPENAI_MODEL=gpt-5.4          # or gpt-4o-mini (dev)
OPENAI_TIMEOUT_MS=30000
```

### Kafka
```bash
KAFKA_BOOTSTRAP_SERVERS=kafka:9092   # Kafka broker address (default: localhost:9092)
```

### Ingestion
```bash
ENABLE_INGESTION=true|false
ENABLE_MANUSCRIPT_INGESTION=true|false
ENABLE_PATRISTIC_INGESTION=true|false
ENABLE_COUNCIL_INGESTION=true|false
INGESTION_SKIP_IF_POPULATED=true|false
INGESTION_TIMEOUT_MINUTES=15|30
ENABLE_DATING_ENRICHMENT=true|false

USE_NTVMR=true|false
NTVMR_DELAY_MS=50|100
LOAD_MANUSCRIPTS_FROM_NTVMR=true|false
SOURCE_CACHE_DIR=/data/source-cache
```

### Authentication
```bash
GOOGLE_CLIENT_ID=789788292255-...
JWT_SECRET=manuscriptum-dev-secret-...
```

### Frontend Build Args
```bash
BACKEND_URL=http://app:8080
NEXT_PUBLIC_GOOGLE_CLIENT_ID=789788292255-...
```

### Monitoring (Prometheus + Grafana + Loki)

- **Prometheus**: http://localhost:9091 — scrapes `/prometheus` from backend every 15s
- **Grafana**: http://localhost:3001 — login `admin/admin` (dev), `admin/$GRAFANA_ADMIN_PASSWORD` (prod)
- **Loki**: http://localhost:3100 — log aggregation backend
- **Promtail**: collects logs from all containers via Docker socket

Pre-provisioned Grafana dashboards (folder "Manuscriptum Atlas"):
- Application Overview — request rate, error rate, latency percentiles
- JVM Metrics — heap, GC, threads, CPU
- Logs Explorer — log volume, backend/frontend logs, error filter

Config files: `deploy/monitoring/`

## Scripts

Location: `scripts/`

| Script | Purpose |
|--------|---------|
| `up.sh` | Interactive menu: dev or prod startup |
| `up.dev.sh` | Dev startup with cache, exposed PG, full re-ingestion |
| `up.prod.sh` | Prod startup with clean build, JVM limits, skip-if-populated |
| `up.db.prod.sh` | Production database only |
| `backup.sh` | pg_dump backup via Docker (saves to `backups/`) |
| `restore.sh` | Restore from backup dump |

## Common Dev Commands

```bash
# Start everything
cd deploy && docker compose up -d --build

# Rebuild only frontend (faster)
cd deploy && docker compose up -d --build frontend

# Restart backend (for stuck ingestion)
docker restart deploy-app-1

# Dev login (admin JWT without Google SSO)
curl -s -X POST http://localhost:8080/auth/dev-login

# Backend logs
docker logs deploy-app-1 --tail 50

# Frontend logs
docker logs deploy-frontend-1 --tail 50

# Build backend locally
cd backend && ./gradlew build -x test

# Frontend dev (outside Docker)
cd frontend && npm run dev

# Health check
curl http://localhost:8080/health

# LLM Queue — check pending items
curl http://localhost:8080/admin/llm/queue/stats

# LLM Queue — run prepared items with Claude Code
# (use /run-llm skill in Claude Code)

# LLM Queue — manually apply completed items
curl -X POST http://localhost:8080/admin/llm/queue/apply/council_consensus

# Prometheus metrics
curl http://localhost:8080/prometheus

# Grafana
open http://localhost:3001

# Query logs via Loki
curl -s 'http://localhost:3100/loki/api/v1/query_range?query={container=~".*app.*"}&limit=10'
```

## Build

### Backend
- **Build tool:** Gradle with Kotlin DSL
- **JVM:** 21
- **Output:** Fat JAR with all dependencies
- **Entry point:** `com.ntcoverage.ApplicationKt`

### Frontend
- **Build tool:** Next.js built-in
- **Output:** Standalone (no node_modules needed in production)
- **API Proxy:** `/api/*` → `BACKEND_URL/*` (transparent rewrite in next.config.ts)

## Project Structure

```
manuscriptumatlas/
├── backend/
│   ├── src/main/kotlin/com/ntcoverage/   — Kotlin source
│   ├── src/main/resources/db/migration/  — Flyway SQL files (V1-V19)
│   └── build.gradle.kts                  — Gradle build config
├── frontend/
│   ├── app/                              — Next.js pages
│   ├── components/                       — React components
│   ├── hooks/                            — TanStack Query hooks
│   ├── lib/api/                          — API functions
│   ├── types/                            — TypeScript interfaces
│   ├── messages/                         — i18n (en, pt, es)
│   ├── i18n/                             — Routing config
│   └── package.json                      — Dependencies
├── deploy/
│   ├── docker-compose.yml                — Dev compose
│   └── docker-compose.prod.yml           — Prod override
├── scripts/                              — Shell scripts (up, backup, restore)
├── backups/                              — Database dumps
├── docs/                                 — This documentation
└── CLAUDE.md                             — AI assistant instructions
```
