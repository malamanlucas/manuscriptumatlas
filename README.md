# Manuscriptum Atlas

Plataforma de análise manuscritológica computacional do Novo Testamento grego. Calcula cobertura textual acumulativa por livro, capítulo e século, cataloga ~140 manuscritos Gregory-Aland, e inclui um domínio de **Pais da Igreja** com declarações patrísticas sobre transmissão textual.

## Stack

| Camada | Tecnologia |
|--------|------------|
| **Backend** | Kotlin 2.1, Ktor 3.1, JVM 21 |
| **Banco** | PostgreSQL 16, Exposed ORM, Flyway |
| **Frontend** | Next.js 16, React 19, TypeScript, Tailwind CSS 4 |
| **Visualização** | Recharts 3 |
| **Data fetching** | TanStack React Query 5 |
| **i18n** | next-intl (pt, en, es) |
| **Tema** | next-themes (light/dark/system) |
| **Infra** | Docker Compose (postgres, init, app, frontend) |

## Execução

### Docker (recomendado)

```bash
# Menu interativo (dev ou prod)
./up.sh

# Ou diretamente:
./up.dev.sh    # Desenvolvimento (PG exposto, cache habilitado)
./up.prod.sh   # Produção (sem cache, limites de memória)
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
./backup.sh              # Gera dump em backups/
./restore.sh [arquivo]   # Restaura dump específico
```

## Arquitetura

```
Routes → Service → Repository → Database (Exposed/PostgreSQL)
```

**Backend** (44 arquivos Kotlin):
- 4 configs, 12 tabelas, 37 DTOs, 9 repositórios, 10 serviços, 7 arquivos de rotas, 4 scrapers, 6 seeds

**Frontend** (Next.js):
- 20 páginas, 15 componentes, 9 hooks, 32 tipos, 3 locales

## API REST

25 endpoints organizados em 7 grupos:

| Grupo | Endpoints |
|-------|-----------|
| **Cobertura** | `/coverage`, `/coverage/{book}`, `/coverage/{book}/chapters/{century}`, `/coverage/gospels/{century}`, `/century/{number}`, `/timeline`, `/timeline/full`, `/missing/{book}/{century}` |
| **Estatísticas** | `/stats/overview`, `/stats/manuscripts-count` |
| **Manuscritos** | `/manuscripts`, `/manuscripts/{gaId}` |
| **Métricas** | `/metrics/nt`, `/metrics/{book}` |
| **Versículos** | `/books`, `/verses/manuscripts` |
| **Pais da Igreja** | `/fathers`, `/fathers/search`, `/fathers/{id}`, `/fathers/{id}/statements`, `/fathers/statements`, `/fathers/statements/search`, `/fathers/statements/topics/summary` |
| **Admin** | `/admin/ingestion/status`, `/admin/ingestion/run`, `/admin/ingestion/reset` |

Filtros: `?type=papyrus`, `?century=N`, `?page=1&limit=50`, `?topic=CANON`, `?tradition=greek`, `?q=keyword`, `?locale=pt`

## Modelo de Dados

12 tabelas PostgreSQL:

| Tabela | Descrição |
|--------|-----------|
| `books` | 27 livros canônicos do NT |
| `verses` | 7.956 versículos |
| `manuscripts` | Manuscritos com GA-ID, datação, tipo |
| `manuscript_verses` | N:N manuscrito ↔ versículo |
| `manuscript_sources` | Metadados acadêmicos 1:1 |
| `coverage_by_century` | Cache materializado de cobertura |
| `ingestion_metadata` | Status da ingestão |
| `book_translations` | Traduções i18n de livros |
| `church_fathers` | 35 pais da igreja |
| `father_textual_statements` | 36 declarações textuais curadas |
| `church_father_translations` | Traduções i18n dos pais (pt, es) |
| `father_statement_translations` | Traduções i18n das declarações (pt, es) |

## Ingestão

Orquestrada pelo `IngestionOrchestrator` no startup:

1. **Seed canônico**: 27 livros + 7.956 versículos (idempotente)
2. **Manuscritos** (`ENABLE_MANUSCRIPT_INGESTION`): NTVMR API → TEI/XML → manuscript_verses
3. **Patrístico** (`ENABLE_PATRISTIC_INGESTION`): 35 pais + 36 declarações + traduções (idempotente)

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `ENABLE_INGESTION` | `true` | Habilitar ingestão |
| `ENABLE_MANUSCRIPT_INGESTION` | `true` | Ingestão de manuscritos |
| `ENABLE_PATRISTIC_INGESTION` | `true` | Ingestão patrística |
| `INGESTION_SKIP_IF_POPULATED` | `false` | Pular se banco já populado |
| `USE_NTVMR` | `true` | Usar API NTVMR |
| `NTVMR_DELAY_MS` | `500` | Delay entre requisições |
| `LOAD_MANUSCRIPTS_FROM_NTVMR` | `false` | Carregar lista de manuscritos da API |
| `INGESTION_TIMEOUT_MINUTES` | `30` | Timeout global |

## Regras de Negócio

- Cobertura binária: versículo coberto se aparece em ao menos um manuscrito
- Cobertura cumulativa: século N inclui manuscritos dos séculos 1..N
- Datação conservadora: intervalos usam o século mais antigo
- Manuscritos até o século X são considerados
- Seed patrístico idempotente via chave lógica

## Documentação Adicional

- [`cursor.md`](cursor.md) — Documentação técnica completa
- [`docs/ingestion-architecture.md`](docs/ingestion-architecture.md) — Diagramas Mermaid do fluxo de ingestão
