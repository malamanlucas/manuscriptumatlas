# NT Manuscriptum Atlas — Documentação Técnica e de Negócio

## Visão Geral

**NT Manuscriptum Atlas** é uma plataforma de **análise manuscritológica computacional do Novo Testamento**. O sistema responde à pergunta central da crítica textual: *quão bem o texto do Novo Testamento é atestado por evidências manuscritas dos primeiros séculos?*

A aplicação calcula, para cada livro, capítulo e século, a proporção de versículos que possuem ao menos um manuscrito como testemunho. Inclui estatísticas globais, explorer de papiros (~140 Gregory-Aland), indicadores acadêmicos avançados e seção educacional.

---

## 1. Negócio e Domínio

### 1.1 Objetivo

Permitir que pesquisadores, estudantes e interessados em crítica textual do NT:

- **Visualizar** a cobertura textual por livro, capítulo e século
- **Identificar** versículos tardios ou ausentes em manuscritos antigos
- **Comparar** cobertura entre tipos de manuscrito (papiros vs. unciais)
- **Analisar** a evolução da atestação ao longo dos séculos (timeline evolutiva)
- **Explorar** manuscritos individuais (papiros, unciais) com detalhes e link NTVMR
- **Consultar** métricas acadêmicas (growth rate, estabilização, fragmentação, densidade)
- **Estudar** contexto histórico (origem dos papiros, códices, expansão textual)

### 1.2 Regras de Negócio

| Regra | Descrição |
|-------|-----------|
| **Cobertura binária** | Um versículo é "coberto" se aparece em **ao menos um manuscrito** até o século considerado. |
| **Cobertura cumulativa** | O século N inclui todos os manuscritos dos séculos 1..N. |
| **Datação conservadora** | Intervalos (ex.: II/III) usam o **século mais antigo** (`centuryMin`). |
| **Escopo temporal** | Manuscritos até o século X são considerados. |
| **Deduplicação** | O mesmo versículo em vários manuscritos conta **uma vez**. |
| **Cânone** | 27 livros do NT, 7.956 versículos canônicos, estrutura tradicional. |

### 1.3 Casos de Uso Principais

- **Dashboard**: visão geral com estatísticas globais e cobertura por século
- **Explorer**: lista de manuscritos (papiros/unciais) com filtros e detalhe individual
- **Mapa de versículos**: [✔][✔][✖][✔] por capítulo
- **Heatmap**: gradiente verde→amarelo→vermelho por capítulo
- **Timeline evolutiva**: gráfico de percentual acumulado
- **Versículos faltantes**: lista exata de versículos não cobertos
- **Métricas acadêmicas**: Century Growth Rate, Stabilization Century, Fragmentation Index, Coverage Density, Manuscript Concentration Score
- **História**: conteúdo educacional sobre transmissão textual

---

## 2. Arquitetura Técnica

### 2.1 Stack

| Camada | Tecnologia |
|--------|------------|
| **Backend** | Kotlin 2.1, Ktor 3.1, JVM 21 |
| **Banco** | PostgreSQL 16, Exposed ORM, Flyway |
| **Frontend** | Next.js 16, React 19, TypeScript, Tailwind CSS 4 |
| **Visualização** | Recharts 3 |
| **Data fetching** | TanStack React Query 5 |
| **Infra** | Docker Compose (postgres, app, frontend) |

### 2.2 Componentes

```
┌─────────────────────────────────────────────────────────────────────────┐
│  FRONTEND (Next.js :3000)                                               │
│  /dashboard  /manuscripts  /manuscripts/[gaId]  /timeline  /compare      │
│  /metrics  /metrics/[book]  /history                                    │
│  StatsOverview, ManuscriptExplorer, MetricsPanel, HistorySection          │
│  TanStack Query → /api/* (rewrite para backend)                         │
├─────────────────────────────────────────────────────────────────────────┤
│  BACKEND (Ktor :8080)                                                   │
│  /coverage  /century/{n}  /timeline  /missing/{book}/{century}           │
│  /stats/overview  /manuscripts  /manuscripts/{gaId}                     │
│  /metrics/nt  /metrics/{book}                                            │
│  Filtros: ?type=papyrus,uncial  ?century=N  ?page=1&limit=50            │
├─────────────────────────────────────────────────────────────────────────┤
│  INGESTÃO                                                               │
│  NTVMR API (fonte primária) → TEI/XML → NtvmrVerseParser                │
│  manuscripts.json (fallback) → VerseExpander                            │
├─────────────────────────────────────────────────────────────────────────┤
│  DADOS (PostgreSQL)                                                     │
│  books | verses | manuscripts | manuscript_verses | manuscript_sources  │
│  coverage_by_century                                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Estrutura de Diretórios

```
manuscriptumatlas/
├── src/main/kotlin/com/ntcoverage/
│   ├── Application.kt
│   ├── config/          DatabaseConfig, FlywayConfig
│   ├── model/           Tables, DTOs
│   ├── repository/      CoverageRepository, ChapterCoverageRepository,
│   │                    VerseRepository, ManuscriptRepository,
│   │                    StatsRepository, MetricsRepository
│   ├── service/         CoverageService, IngestionService, VerseExpander,
│   │                    StatsService, ManuscriptService, MetricsService,
│   │                    VerseRangeCompressor
│   ├── routes/          CoverageRoutes, StatsRoutes, ManuscriptRoutes, MetricsRoutes
│   ├── scraper/         NtvmrClient, NtvmrVerseParser
│   ├── util/             NtvmrUrl
│   └── seed/            CanonicalVerses, ManuscriptSeedData
├── src/main/resources/
│   ├── db/migration/    V1, V2, V3 (manuscript_sources + colunas extras)
│   ├── seed/            manuscripts.json
│   └── openapi/         documentation.yaml
├── frontend/
│   ├── app/             dashboard, manuscripts, manuscripts/[gaId],
│   │                    timeline, compare, metrics, metrics/[book], history
│   ├── components/      CenturySlider, VerseGrid, Heatmap, StatsOverview,
│   │                    ManuscriptExplorer, etc.
│   ├── hooks/           useCoverage, useTimeline, useMissing, useStats,
│   │                    useManuscripts, useMetrics
│   ├── lib/             api.ts, utils.ts
│   └── types/           interfaces TypeScript
├── docker-compose.yml   postgres, app, frontend
├── up.sh                Script para subir com build forçado (sem cache)
└── cursor.md
```

---

## 3. API REST

### 3.1 Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/` | Info do serviço e lista de endpoints |
| GET | `/coverage` | Cobertura completa ou `?century=N&type=papyrus` |
| GET | `/coverage/{book}` | Cobertura de um livro |
| GET | `/coverage/{book}/chapters/{century}` | Cobertura por capítulo |
| GET | `/coverage/gospels/{century}` | Cobertura agregada dos 4 evangelhos |
| GET | `/century/{number}` | Cobertura cumulativa até o século N |
| GET | `/timeline` | Timeline evolutiva (`?book=John&type=papyrus`) |
| GET | `/timeline/full` | Timeline do NT completo |
| GET | `/missing/{book}/{century}` | Lista de versículos não cobertos |
| GET | `/stats/overview` | Estatísticas globais (totais, por tipo, por século, por livro) |
| GET | `/manuscripts` | Lista manuscritos (`?type=papyrus&century=3&page=1&limit=50`) |
| GET | `/manuscripts/{gaId}` | Detalhe de manuscrito (livros, intervalos, link NTVMR) |
| GET | `/metrics/nt` | Métricas acadêmicas do NT inteiro |
| GET | `/metrics/{book}` | Métricas acadêmicas de um livro |
| GET | `/swagger` | Documentação Swagger UI |

### 3.2 Filtros

- `?type=papyrus` ou `?type=uncial`
- `?century=N` (1–10)
- `?page=1&limit=50` (manuscripts)

---

## 4. Modelo de Dados

### 4.1 Tabelas

- **books**: 27 livros canônicos
- **verses**: 7.956 versículos (book_id, chapter, verse)
- **manuscripts**: ga_id, century_min/max, manuscript_type, historical_notes, geographic_origin, discovery_location, ntvmr_url
- **manuscript_verses**: N:N manuscrito ↔ versículo
- **manuscript_sources**: metadados acadêmicos (source_name, ntvmr_url, historical_notes, geographic_origin, discovery_location) — 1:1 com manuscripts
- **coverage_by_century**: cache materializado (century, book_id, covered_verses, total_verses, coverage_percent)

### 4.2 Migrations

- **V1**: tabelas base (books, verses, manuscripts, manuscript_verses, coverage_by_century)
- **V2**: índices (idx_manuscripts_type_century, idx_mv_verse_manuscript)
- **V3**: manuscript_sources + colunas opcionais em manuscripts

**Importante**: Em banco novo via Docker, a migration V3 deve rodar automaticamente. Se o Flyway não aplicar V3 (ex.: baseline pré-existente), a tabela `manuscript_sources` pode não existir e o endpoint `/manuscripts/{gaId}` falhará. Solução: executar manualmente o SQL de V3 ou garantir que o volume do postgres seja limpo (`docker compose down -v` antes de `up`).

---

## 5. Ingestão de Dados (NTVMR)

### 5.1 Fonte Primária: NTVMR

- **URL**: `http://ntvmr.uni-muenster.de/community/vmr/api/transcript/get/`
- **Parâmetros**: `docID` (5 dígitos), `indexContent` (livro), `format=teiraw`
- **Lógica**: `<ab n="B04K8V12">` com filhos = verso presente

### 5.2 Variáveis de Ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `USE_NTVMR` | `true` | Usar API NTVMR como fonte primária |
| `NTVMR_DELAY_MS` | `500` | Delay entre requisições (evitar rate limit) |

---

## 6. Métricas Acadêmicas

| Métrica | Fórmula |
|---------|---------|
| **Century Growth Rate** | `(coverage_N - coverage_N-1) / coverage_N-1 * 100` |
| **Stabilization Century** | Primeiro século onde coverage >= 90% |
| **Fragmentation Index** | `1 - (1 / avg_manuscripts_per_verse)` |
| **Coverage Density** | `covered_verses / manuscript_count` |
| **Manuscript Concentration Score** | `manuscripts_covering_book / total_manuscripts` |

---

## 7. Docker e Scripts

### 7.1 Subir com build forçado (sem cache)

```bash
./up.sh
```

O script `up.sh`:
1. Compila o backend (`./gradlew build -x test`)
2. Faz build Docker sem cache (`docker compose build --no-cache`)
3. Sobe os containers (`docker compose up -d`)

### 7.2 Subir manualmente

```bash
./gradlew build -x test
docker compose up -d
```

### 7.3 Parar

```bash
docker compose down
```

### 7.4 Serviços

- **postgres** (5432): PostgreSQL 16
- **app** (8080): Backend Kotlin
- **frontend** (3000): Next.js (rewrite `/api/*` → backend)

O frontend usa `BACKEND_URL` para proxy das requisições em produção.

---

## 8. Decisões de Design

| Decisão | Razão |
|---------|-------|
| NTVMR como fonte primária | Precisão manuscritológica |
| Cache materializado | Evitar recálculo a cada request |
| Ingestão no startup | Dados prontos antes de servir |
| Fallback seed por livro | Manuscritos não disponíveis no NTVMR |
| Next.js rewrites | Frontend e backend na mesma origem |
| TanStack Query | Cache e estados de loading/erro |
| manuscript_sources separada | Metadados acadêmicos extensos, opcionais |
| getHistoricalNotesFromSource | Consulta opcional; falha se tabela não existir |
