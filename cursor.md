# NT Manuscript Coverage — Documentação Técnica e de Negócio

## Visão Geral

**NT Manuscript Coverage** é uma plataforma de **análise manuscritológica computacional do Novo Testamento**. O sistema responde à pergunta central da crítica textual: *quão bem o texto do Novo Testamento é atestado por evidências manuscritas dos primeiros séculos?*

A aplicação calcula, para cada livro, capítulo e século, a proporção de versículos que possuem ao menos um manuscrito como testemunho. O resultado é uma visão cumulativa e evolutiva da cobertura textual ao longo do tempo (séculos I a X).

---

## 1. Negócio e Domínio

### 1.1 Objetivo

Permitir que pesquisadores, estudantes e interessados em crítica textual do NT:

- **Visualizar** a cobertura textual por livro, capítulo e século
- **Identificar** versículos tardios ou ausentes em manuscritos antigos (ex.: Perícope da Adúltera em Jo 7:53–8:11)
- **Comparar** cobertura entre tipos de manuscrito (papiros vs. unciais)
- **Analisar** a evolução da atestação ao longo dos séculos (timeline evolutiva)

### 1.2 Regras de Negócio

| Regra | Descrição |
|-------|-----------|
| **Cobertura binária** | Um versículo é "coberto" se aparece em **ao menos um manuscrito** até o século considerado. Não há ponderação por número de testemunhos ou família textual. |
| **Cobertura cumulativa** | O século N inclui todos os manuscritos dos séculos 1..N. Ex.: século III = I + II + III. |
| **Datação conservadora** | Intervalos (ex.: II/III) usam o **século mais antigo** (`centuryMin`). |
| **Escopo temporal** | Manuscritos até o século X são considerados. Acima disso são ignorados. |
| **Deduplicação** | O mesmo versículo em vários manuscritos conta **uma vez**. |
| **Cânone** | 27 livros do NT, 7.957 versículos canônicos, estrutura tradicional. |

### 1.3 Casos de Uso Principais

- **Dashboard**: visão geral da cobertura por século com slider interativo
- **Mapa de versículos**: [✔][✔][✖][✔] por capítulo — quais versículos estão cobertos
- **Heatmap**: gradiente verde→amarelo→vermelho por capítulo
- **Timeline evolutiva**: gráfico de percentual acumulado ao longo dos séculos
- **Versículos faltantes**: lista exata de versículos não atestados até um século
- **Comparação**: Papiros vs. Unciais, Evangelhos vs. Epístolas, século II vs. IV

---

## 2. Arquitetura Técnica

### 2.1 Stack

| Camada | Tecnologia |
|--------|------------|
| **Backend** | Kotlin 2.1, Ktor 3.1, JVM 21 |
| **Banco** | PostgreSQL 16, Exposed ORM, Flyway |
| **Frontend** | Next.js 14 (App Router), TypeScript, React 18, TailwindCSS |
| **Visualização** | Recharts |
| **Data fetching** | TanStack Query (React Query) |
| **Infra** | Docker Compose (postgres, app, frontend) |

### 2.2 Componentes

```
┌─────────────────────────────────────────────────────────────────────────┐
│  FRONTEND (Next.js :3000)                                               │
│  /dashboard  /book/[name]  /timeline  /compare                           │
│  CenturySlider, VerseGrid, Heatmap, TimelineChart, ComparisonChart      │
│  TanStack Query → /api/* (rewrite para backend)                         │
├─────────────────────────────────────────────────────────────────────────┤
│  BACKEND (Ktor :8080)                                                   │
│  /coverage  /coverage/{book}/chapters/{century}  /coverage/gospels/{c}   │
│  /century/{n}  /timeline  /timeline/full  /missing/{book}/{century}       │
│  Filtros: ?type=papyrus,uncial  ?century=N                              │
├─────────────────────────────────────────────────────────────────────────┤
│  INGESTÃO                                                               │
│  NTVMR API (fonte primária) → TEI/XML → NtvmrVerseParser                │
│  manuscripts.json (fallback) → VerseExpander                            │
├─────────────────────────────────────────────────────────────────────────┤
│  DADOS (PostgreSQL)                                                     │
│  books | verses | manuscripts | manuscript_verses | coverage_by_century │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Estrutura de Diretórios

```
ia-manuscrito/
├── src/main/kotlin/com/ntcoverage/
│   ├── Application.kt
│   ├── config/          DatabaseConfig, FlywayConfig
│   ├── model/           Tables, DTOs
│   ├── repository/      CoverageRepository, ChapterCoverageRepository, VerseRepository, ManuscriptRepository
│   ├── service/         CoverageService, IngestionService, VerseExpander
│   ├── routes/          CoverageRoutes
│   ├── scraper/         NtvmrClient, NtvmrVerseParser, WikipediaScraper
│   └── seed/            CanonicalVerses, ManuscriptSeedData
├── src/main/resources/
│   ├── db/migration/    V1, V2 (índices)
│   ├── seed/            manuscripts.json
│   └── openapi/         documentation.yaml
├── frontend/
│   ├── app/             dashboard, book/[name], timeline, compare
│   ├── components/      CenturySlider, VerseGrid, Heatmap, TimelineChart, etc.
│   ├── hooks/           useCoverage, useTimeline, useMissing
│   ├── lib/             api.ts, utils.ts
│   └── types/           interfaces TypeScript
├── docker-compose.yml   postgres, app, frontend
└── cursor.md
```

---

## 3. Ingestão de Dados (NTVMR)

### 3.1 Fonte Primária: NTVMR

A API do **NTVMR** (New Testament Virtual Manuscript Room, INTF Münster) fornece transcrições TEI/XML com granularidade de versículo. É a fonte mais confiável para saber **exatamente** quais versículos cada manuscrito contém.

- **URL**: `http://ntvmr.uni-muenster.de/community/vmr/api/transcript/get/`
- **Parâmetros**: `docID` (5 dígitos), `indexContent` (livro, ex.: "John"), `format=teiraw`
- **Lógica**: `<ab n="B04K8V12">` com filhos = verso presente; `<ab/>` vazio = verso ausente

### 3.2 Fluxo de Ingestão

1. `USE_NTVMR=true` (padrão): para cada manuscrito e livro, busca TEI na API NTVMR
2. `NtvmrVerseParser` extrai versos presentes (elementos `<ab>` com conteúdo)
3. Fallback por livro: se NTVMR falhar para um livro, usa `VerseExpander` com ranges do seed
4. Fallback global: após 3 falhas consecutivas, troca para modo seed-only
5. `USE_NTVMR=false`: usa apenas `manuscripts.json` (comportamento legado)

### 3.3 Variáveis de Ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `USE_NTVMR` | `true` | Usar API NTVMR como fonte primária |
| `NTVMR_DELAY_MS` | `500` | Delay entre requisições (evitar rate limit) |

---

## 4. API REST

### 4.1 Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/` | Info do serviço e lista de endpoints |
| GET | `/coverage` | Cobertura completa (séculos I–X) ou `?century=N&type=papyrus` |
| GET | `/coverage/{book}` | Cobertura de um livro |
| GET | `/coverage/{book}/chapters/{century}` | Cobertura por capítulo (versos cobertos/faltantes) |
| GET | `/coverage/gospels/{century}` | Cobertura agregada dos 4 evangelhos |
| GET | `/century/{number}` | Cobertura cumulativa até o século N |
| GET | `/timeline` | Timeline evolutiva (`?book=John&type=papyrus`) |
| GET | `/timeline/full` | Timeline do NT completo |
| GET | `/missing/{book}/{century}` | Lista de versículos não cobertos |
| GET | `/swagger` | Documentação Swagger UI |

### 4.2 Filtros

- `?type=papyrus` ou `?type=uncial` ou `?type=papyrus,uncial`
- `?century=N` (1–10)

---

## 5. Modelo de Dados

- **books**: 27 livros canônicos
- **verses**: 7.957 versículos (book_id, chapter, verse)
- **manuscripts**: manuscritos com ga_id, century_min/max, manuscript_type
- **manuscript_verses**: N:N manuscrito ↔ versículo
- **coverage_by_century**: cache materializado (century, book_id, covered_verses, total_verses, coverage_percent)

Índices relevantes: `idx_manuscripts_type_century`, `idx_mv_verse_manuscript`, `idx_mv_manuscript`, `idx_mv_verse`.

---

## 6. Docker

```bash
./gradlew build
docker compose up -d
```

Serviços:

- **postgres** (5432): PostgreSQL 16
- **app** (8080): Backend Kotlin
- **frontend** (3000): Next.js (rewrite `/api/*` → backend)

O frontend usa `BACKEND_URL` para proxy das requisições em produção.

---

## 7. Decisões de Design

| Decisão | Razão |
|---------|-------|
| NTVMR como fonte primária | Precisão manuscritológica (ex.: Jo 7:53–8:11 ausente em P66/P75) |
| Cache materializado | Evitar recálculo a cada request |
| Ingestão no startup | Dados prontos antes de servir; NTVMR pode levar ~8 min |
| Fallback seed por livro | Manuscritos não disponíveis no NTVMR usam ranges curados |
| Next.js rewrites | Frontend e backend na mesma origem, sem CORS no browser |
| TanStack Query | Cache, refetch e estados de loading/erro no frontend |
