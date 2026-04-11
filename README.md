# Manuscriptum Atlas

Plataforma acadêmica de cobertura textual do Novo Testamento grego — manuscritos, testemunhos patrísticos, concílios, heresias, e estudo bíblico interlinear. Cataloga ~140 manuscritos Gregory-Aland, oferece cobertura textual acumulativa por livro/capítulo/século, leitor bíblico multi-tradução com interlinear grego/hebraico, e motor de consenso multi-fonte para dados históricos.

## Stack

| Camada | Tecnologia |
|--------|------------|
| **Backend** | Kotlin 2.1, Ktor 3.1, JVM 21 |
| **Banco** | PostgreSQL 16, Exposed ORM, Flyway (21 migrations) |
| **Frontend** | Next.js 16, React 19, TypeScript, Tailwind CSS 4 |
| **LLM** | OpenAI GPT-5.4 / GPT-4.1-mini (3-tier) + LLM Queue (Claude Code) |
| **Messaging** | Apache Kafka (KRaft mode) |
| **Visualização** | Recharts 3, Leaflet (mapas) |
| **Data fetching** | TanStack React Query 5 |
| **i18n** | next-intl (pt, en, es) |
| **Tema** | next-themes (light/dark/system) |
| **Observability** | Prometheus + Grafana + Loki + Micrometer |
| **Infra** | Docker Compose (postgres, kafka, init, app, frontend, prometheus, grafana, loki, promtail) |

## Execução

### Docker (recomendado)

```bash
# Menu interativo (dev ou prod)
./scripts/up.sh

# Ou diretamente:
./scripts/up.dev.sh    # Desenvolvimento (PG exposto, cache habilitado)
./scripts/up.prod.sh   # Produção (sem cache, limites de memória)
```

**Serviços:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- PostgreSQL: localhost:5432 (somente dev)

### Manual

```bash
# Pré-requisitos: Java 21+, PostgreSQL 16+, Node.js 22+
./gradlew clean jar
docker compose up --build -d
```

### Backup e Restore

```bash
./scripts/backup.sh              # Gera dump em backups/
./scripts/restore.sh [arquivo]   # Restaura dump específico
```

## Arquitetura

```
Routes → Service → Repository → Database (Exposed/PostgreSQL)
```

**Backend** (100+ arquivos Kotlin):
- 9 configs, 45 tabelas (33 main + 12 bible), 126 DTOs, 27 repositórios, 30 serviços, 12 arquivos de rotas, 15 scrapers, 17 seeds, 6 componentes LLM

**Frontend** (Next.js):
- 24 páginas, 47+ componentes (11 diretórios), 18 hooks, 14 arquivos de tipos, 16 módulos API, 44 namespaces i18n, 3 locales

## API REST

60+ endpoints organizados em 12 grupos:

| Grupo | Exemplos |
|-------|----------|
| **Cobertura** | `/coverage`, `/coverage/{book}`, `/timeline`, `/missing/{book}/{century}` |
| **Estatísticas** | `/stats/overview`, `/stats/manuscripts-count` |
| **Manuscritos** | `/manuscripts`, `/manuscripts/{gaId}`, `/verses/manuscripts` |
| **Métricas** | `/metrics/nt`, `/metrics/{book}` |
| **Pais da Igreja** | `/fathers`, `/fathers/{id}`, `/fathers/statements`, `/fathers/search` |
| **Concílios** | `/councils`, `/councils/{slug}`, `/councils/{slug}/canons` |
| **Heresias** | `/heresies`, `/heresies/{slug}` |
| **Bíblia** | `/bible/versions`, `/bible/chapter`, `/bible/interlinear`, `/bible/search` |
| **Apologética** | `/apologetics/topics`, `/apologetics/topics/{id}/responses` |
| **Auth** | `/auth/google`, `/auth/me`, `/auth/dev-login` |
| **Admin/Ingestão** | `/admin/ingestion/*`, `/admin/reset/*`, `/admin/{domain}/ingestion/*` |
| **Admin/LLM Queue** | `/admin/llm/queue/stats`, `/admin/llm/queue/pending`, `/admin/llm/queue/apply/*` |

Filtros: `?type=`, `?century=`, `?page=&limit=`, `?topic=`, `?tradition=`, `?q=`, `?locale=`

Documentação completa: [`docs/api-reference.md`](docs/api-reference.md)

## Modelo de Dados

45 tabelas PostgreSQL em 2 databases:

**nt_coverage** (33 tabelas): manuscritos, patrístico, concílios, heresias, apologética, observatório, auth, LLM queue
**bible_db** (12 tabelas): versões, livros, versículos, interlinear, léxico grego/hebraico

Domínios principais:

| Domínio | Tabelas-chave |
|---------|---------------|
| Manuscritos | `manuscripts`, `manuscript_verses`, `coverage_by_century` |
| Patrístico | `church_fathers`, `father_textual_statements` + traduções |
| Concílios | `councils`, `sources`, `council_source_claims`, `council_canons` |
| Heresias | `heresies`, `council_heresies`, `council_heretic_participants` |
| Bíblia | `bible_versions`, `bible_verses`, `interlinear_words`, `greek_lexicon` |
| Apologética | `apologetic_topics`, `apologetic_responses` + traduções |
| Observatório | `visitor_sessions` (particionada), `page_views` (particionada) |
| LLM | `llm_usage_logs`, `llm_prompt_queue` |

Documentação completa: [`docs/database.md`](docs/database.md)

## Ingestão

4 pipelines independentes com 49 fases no total, orquestrados pelo `IngestionOrchestrator`:

| Pipeline | Fases | Descrição |
|----------|-------|-----------|
| **Manuscritos** | 4 | NTVMR API → TEI/XML → cobertura materializada |
| **Patrístico** | 6 | Seed + traduções + biografias LLM + dating |
| **Concílios** | 14 | 6 fontes acadêmicas → consenso ponderado → tradução |
| **Bíblia** | 25 | 4 versões + interlinear grego/hebraico + léxico + alinhamento |

Fases LLM usam sistema de queue assíncrono: `_prepare` → `/run-llm` (Claude Code) → `_apply`.

Documentação completa: [`docs/ingestion-pipelines.md`](docs/ingestion-pipelines.md)

## Regras de Negócio

- Cobertura binária: versículo coberto se aparece em ao menos um manuscrito
- Cobertura cumulativa: século N inclui manuscritos dos séculos 1..N
- Datação conservadora: intervalos usam o século mais antigo
- Manuscritos até o século X são considerados
- Seed patrístico idempotente via chave lógica

## Documentação

- [`docs/architecture.md`](docs/architecture.md) — Arquitetura do sistema, stack, fluxos de dados
- [`docs/backend.md`](docs/backend.md) — Backend Kotlin: packages, serviços, repositórios, LLM
- [`docs/frontend.md`](docs/frontend.md) — Frontend Next.js: páginas, componentes, hooks, i18n
- [`docs/database.md`](docs/database.md) — Esquema PostgreSQL: tabelas, migrations, índices
- [`docs/api-reference.md`](docs/api-reference.md) — Referência completa da API REST (60+ endpoints)
- [`docs/infrastructure.md`](docs/infrastructure.md) — Docker, scripts, variáveis de ambiente
- [`docs/ingestion-pipelines.md`](docs/ingestion-pipelines.md) — Pipelines de ingestão (4 domínios, 49 fases)
- [`docs/llm-concepts.md`](docs/llm-concepts.md) — Arquitetura LLM: tiers, queue, Kafka
- [`docs/bible-interlinear-system.md`](docs/bible-interlinear-system.md) — Sistema interlinear grego/hebraico
- [`docs/architecture/ingestion-architecture.md`](docs/architecture/ingestion-architecture.md) — Diagramas Mermaid do fluxo de ingestão
