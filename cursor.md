# NT Manuscriptum Atlas — Documentação Técnica e de Negócio

## Visão Geral

**NT Manuscriptum Atlas** é uma plataforma de **análise manuscritológica computacional do Novo Testamento**. O sistema responde à pergunta central da crítica textual: *quão bem o texto do Novo Testamento é atestado por evidências manuscritas dos primeiros séculos?*

A aplicação calcula, para cada livro, capítulo e século, a proporção de versículos que possuem ao menos um manuscrito como testemunho. Inclui estatísticas globais, explorer de manuscritos (~140 Gregory-Aland), indicadores acadêmicos avançados, seção educacional, e um domínio de **Pais da Igreja** com declarações patrísticas sobre transmissão textual.

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
- **Pesquisar** declarações patrísticas sobre manuscritos, cânone e variantes textuais

### 1.2 Regras de Negócio

| Regra | Descrição |
|-------|-----------|
| **Cobertura binária** | Um versículo é "coberto" se aparece em **ao menos um manuscrito** até o século considerado. |
| **Cobertura cumulativa** | O século N inclui todos os manuscritos dos séculos 1..N. |
| **Datação conservadora** | Intervalos (ex.: II/III) usam o **século mais antigo** (`centuryMin`). |
| **Escopo temporal** | Manuscritos até o século X são considerados. |
| **Deduplicação** | O mesmo versículo em vários manuscritos conta **uma vez**. |
| **Cânone** | 27 livros do NT, 7.956 versículos canônicos, estrutura tradicional. |
| **Seed idempotente** | Ingestão de pais/declarações usa chave lógica para evitar duplicação em re-execuções. |

### 1.3 Casos de Uso Principais

- **Dashboard**: visão geral com estatísticas globais e cobertura por século
- **Explorer**: lista de manuscritos (papiros/unciais) com filtros e detalhe individual
- **Mapa de versículos**: [✔][✔][✖][✔] por capítulo
- **Heatmap**: gradiente verde→amarelo→vermelho por capítulo
- **Timeline evolutiva**: gráfico de percentual acumulado
- **Versículos faltantes**: lista exata de versículos não cobertos
- **Busca de versículos**: lookup de manuscritos que atestam um versículo específico
- **Métricas acadêmicas**: Century Growth Rate, Stabilization Century, Fragmentation Index, Coverage Density, Manuscript Concentration Score
- **Pais da Igreja**: catálogo de 35 pais com timeline, busca e filtros por tradição/século
- **Declarações textuais**: 32+ citações patrísticas sobre transmissão textual, filtráveis por tópico, século e tradição
- **História**: conteúdo educacional sobre transmissão textual
- **FAQ**: perguntas frequentes sobre o projeto

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
| **i18n** | next-intl (pt, en, es) |
| **Tema** | next-themes (light/dark/system) |
| **Ícones** | Lucide React |
| **Infra** | Docker Compose (postgres, app, frontend, init) |

### 2.2 Componentes

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  FRONTEND (Next.js :3000)                                                    │
│  /dashboard  /manuscripts  /manuscripts/[gaId]  /timeline  /compare          │
│  /metrics  /metrics/[book]  /book/[name]  /verse-lookup  /manuscript-count   │
│  /fathers  /fathers/[id]  /fathers/testimony                                 │
│  /history  /sources  /ingestion-status  /faq                                 │
│  TanStack Query → /api/* (rewrite para backend)                              │
├──────────────────────────────────────────────────────────────────────────────┤
│  BACKEND (Ktor :8080)                                                        │
│  /coverage  /century/{n}  /timeline  /missing/{book}/{century}               │
│  /stats/overview  /manuscripts  /manuscripts/{gaId}                          │
│  /metrics/nt  /metrics/{book}  /books  /verses/manuscripts                   │
│  /fathers  /fathers/{id}  /fathers/statements  /fathers/statements/search     │
│  /admin/ingestion/*                                                          │
│  Filtros: ?type=papyrus,uncial  ?century=N  ?page=1&limit=50                │
│           ?topic=CANON  ?tradition=greek  ?q=keyword                         │
├──────────────────────────────────────────────────────────────────────────────┤
│  INGESTÃO                                                                    │
│  NTVMR API (fonte primária) → TEI/XML → NtvmrVerseParser                    │
│  manuscripts.json (fallback) → VerseExpander                                 │
│  ChurchFathersSeedData + TextualStatementsSeedData (curadoria manual)         │
├──────────────────────────────────────────────────────────────────────────────┤
│  DADOS (PostgreSQL)                                                          │
│  books | verses | manuscripts | manuscript_verses | manuscript_sources        │
│  coverage_by_century | ingestion_metadata | book_translations                │
│  church_fathers | father_textual_statements                                  │
│  church_father_translations | father_statement_translations                  │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Estrutura de Diretórios

```
manuscriptumatlas/
├── src/main/kotlin/com/ntcoverage/
│   ├── Application.kt              Ponto de entrada, configuração Ktor, registro de rotas
│   ├── config/
│   │   ├── DatabaseConfig.kt       Configuração HikariCP/PostgreSQL
│   │   ├── FlywayConfig.kt         Migrações Flyway + fallback SchemaUtils + índices extras
│   │   ├── IngestionConfig.kt      Variáveis de ambiente para ingestão
│   │   └── LocaleConfig.kt         Locales permitidos (en, pt, es)
│   ├── model/
│   │   ├── Tables.kt               12 tabelas Exposed (Books→FatherStatementTranslations)
│   │   └── DTOs.kt                 37 DTOs @Serializable, enums (TextualTopic)
│   ├── repository/
│   │   ├── CoverageRepository.kt
│   │   ├── ChapterCoverageRepository.kt
│   │   ├── VerseRepository.kt
│   │   ├── ManuscriptRepository.kt
│   │   ├── StatsRepository.kt
│   │   ├── MetricsRepository.kt
│   │   ├── IngestionMetadataRepository.kt
│   │   ├── ChurchFatherRepository.kt
│   │   └── FatherTextualStatementRepository.kt
│   ├── service/
│   │   ├── CoverageService.kt
│   │   ├── IngestionService.kt       Ingestão de manuscritos NTVMR
│   │   ├── PatristicIngestionService.kt  Seed de pais + declarações
│   │   ├── IngestionOrchestrator.kt   Orquestração com retry
│   │   ├── StatsService.kt
│   │   ├── ManuscriptService.kt
│   │   ├── MetricsService.kt
│   │   ├── ChurchFatherService.kt
│   │   ├── VerseExpander.kt
│   │   └── VerseRangeCompressor.kt
│   ├── routes/
│   │   ├── CoverageRoutes.kt
│   │   ├── StatsRoutes.kt
│   │   ├── ManuscriptRoutes.kt
│   │   ├── MetricsRoutes.kt
│   │   ├── VerseRoutes.kt
│   │   ├── ChurchFatherRoutes.kt
│   │   └── AdminRoutes.kt
│   ├── scraper/
│   │   ├── NtvmrClient.kt          Cliente HTTP para transcrições NTVMR
│   │   ├── NtvmrListClient.kt      Listagem de manuscritos NTVMR
│   │   ├── NtvmrVerseParser.kt     Parser TEI/XML
│   │   └── WikipediaScraper.kt
│   ├── seed/
│   │   ├── CanonicalVerses.kt      27 livros, 7.956 versículos canônicos
│   │   ├── ManuscriptSeedData.kt   Manuscritos pré-definidos
│   │   ├── ChurchFathersSeedData.kt   35 pais da igreja
│   │   ├── ChurchFatherTranslationsSeedData.kt  Traduções (pt, es) dos pais
│   │   ├── TextualStatementsSeedData.kt  36 declarações textuais curadas
│   │   └── TextualStatementTranslationsSeedData.kt  Traduções (pt, es) das declarações
│   └── util/
│       └── NtvmrUrl.kt
├── src/main/resources/
│   ├── db/migration/               V1–V8 (Flyway SQL)
│   ├── seed/                       manuscripts.json
│   └── openapi/                    documentation.yaml
├── frontend/
│   ├── app/[locale]/               20 páginas (dashboard, fathers, metrics, etc.)
│   ├── components/
│   │   ├── charts/                 TimelineChart, Heatmap, ComparisonChart, FathersTimelineChart
│   │   ├── coverage/               BookCard, CenturySlider, GospelPanel, VerseGrid
│   │   ├── layout/                 Header, Sidebar, SidebarContext, LanguageSelector, ThemeToggle
│   │   ├── stats/                  StatsOverview
│   │   └── providers.tsx           React Query + Theme providers
│   ├── hooks/                      9 hooks (useCoverage, useManuscripts, useChurchFathers, etc.)
│   ├── lib/                        api.ts, utils.ts
│   ├── types/                      index.ts (32 interfaces/tipos)
│   └── messages/                   en.json, pt.json, es.json
├── docker-compose.yml              Dev: postgres, app, frontend, init
├── docker-compose.prod.yml         Prod: restart policies, limites de memória
├── backup.sh                       pg_dump via Docker
├── restore.sh                      pg_restore via Docker
├── up.sh                           Menu dev/prod
├── up.dev.sh                       Build dev com cache
├── up.prod.sh                      Build prod sem cache
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
| GET | `/stats/overview` | Estatísticas globais |
| GET | `/stats/manuscripts-count` | Contagem por tipo de manuscrito |
| GET | `/manuscripts` | Lista manuscritos (`?type=papyrus&century=3&page=1&limit=50`) |
| GET | `/manuscripts/{gaId}` | Detalhe de manuscrito |
| GET | `/books` | Lista de livros (`?locale=pt`) |
| GET | `/verses/manuscripts` | Manuscritos para um versículo (`?book=John&chapter=1&verse=1`) |
| GET | `/metrics/nt` | Métricas acadêmicas do NT inteiro |
| GET | `/metrics/{book}` | Métricas acadêmicas de um livro |
| GET | `/fathers` | Lista pais da igreja (`?century=3&tradition=greek&locale=pt&page=1&limit=50`) |
| GET | `/fathers/search` | Busca de pais (`?q=Augustine&limit=20&locale=pt`) |
| GET | `/fathers/{id}` | Detalhe de um pai da igreja (`?locale=pt`) |
| GET | `/fathers/{id}/statements` | Declarações textuais de um pai (`?locale=pt`) |
| GET | `/fathers/statements` | Lista declarações (`?topic=CANON&century=4&tradition=latin&locale=pt&page=1&limit=20`) |
| GET | `/fathers/statements/search` | Busca fuzzy em declarações (`?q=manuscripts&limit=20&locale=pt`) |
| GET | `/fathers/statements/topics/summary` | Contagem de declarações por tópico |
| GET | `/admin/ingestion/status` | Status da ingestão |
| POST | `/admin/ingestion/run` | Disparar ingestão manual |
| POST | `/admin/ingestion/reset` | Reset e re-ingestão |
| GET | `/swagger` | Swagger UI |

### 3.2 Filtros

- `?type=papyrus` ou `?type=uncial`
- `?century=N` (1–10)
- `?page=1&limit=50` (paginação)
- `?topic=MANUSCRIPTS` (tópicos: MANUSCRIPTS, AUTOGRAPHS, APOCRYPHA, CANON, TEXTUAL_VARIANTS, TRANSLATION, CORRUPTION, SCRIPTURE_AUTHORITY)
- `?tradition=greek` (tradições: greek, latin, syriac, etc.)
- `?q=keyword` (busca fuzzy com trigram similarity)
- `?locale=pt` (i18n: pt, en, es)

---

## 4. Modelo de Dados

### 4.1 Tabelas

- **books**: 27 livros canônicos (name, abbreviation, totalChapters, totalVerses, bookOrder)
- **verses**: 7.956 versículos (bookId, chapter, verse)
- **manuscripts**: ga_id, centuryMin/Max, effectiveCentury, manuscriptType, historicalNotes, geographicOrigin, discoveryLocation, ntvmrUrl
- **manuscript_verses**: N:N manuscrito ↔ versículo
- **manuscript_sources**: metadados acadêmicos (sourceName, ntvmrUrl, historicalNotes) — 1:1 com manuscripts
- **coverage_by_century**: cache materializado (century, bookId, coveredVerses, totalVerses, coveragePercent)
- **ingestion_metadata**: status da ingestão (status, startedAt, finishedAt, durationMs, contadores, errorMessage)
- **book_translations**: nomes traduzidos dos livros (locale, name, abbreviation)
- **church_fathers**: 35 pais da igreja (displayName, normalizedName, centuryMin/Max, shortDescription, primaryLocation, tradition, dataSource)
- **father_textual_statements**: declarações textuais (fatherId, topic, statementText, originalLanguage, originalText, sourceWork, sourceReference, approximateYear)
- **church_father_translations**: traduções i18n dos pais (fatherId, locale, displayName, shortDescription, primaryLocation)
- **father_statement_translations**: traduções i18n das declarações (statementId, locale, statementText)

### 4.2 Migrations

- **V1**: tabelas base (books, verses, manuscripts, manuscript_verses, coverage_by_century)
- **V2**: índices (idx_manuscripts_type_century, idx_mv_verse_manuscript)
- **V3**: manuscript_sources + colunas opcionais em manuscripts
- **V4**: ingestion_metadata (status, timestamps, contadores)
- **V5**: book_translations + dados iniciais (pt, es)
- **V6**: church_fathers + extensão pg_trgm + índices GIN
- **V7**: father_textual_statements + CHECK constraints (topic, approximate_year) + índices (father_id, topic, year, topic+year composto, GIN trigram em statement_text)
- **V8**: church_father_translations + father_statement_translations + índices de lookup por locale + GIN trigram em statement_text traduzido

### 4.3 Nota sobre Flyway

O Flyway atualmente não aplica as migrações SQL por incompatibilidade de naming convention. O fallback é `Exposed SchemaUtils.createMissingTablesAndColumns()` que cria todas as tabelas registradas. Índices extras (GIN trigram, compostos) e a extensão `pg_trgm` são aplicados via SQL direto no método `applyExtraIndexesAndConstraints()` do `FlywayConfig`.

---

## 5. Ingestão de Dados

### 5.1 Orquestração

O `IngestionOrchestrator` executa no startup:
1. **Seed canônico**: 27 livros + 7.956 versículos (sempre, idempotente)
2. **Ingestão de manuscritos** (se `ENABLE_MANUSCRIPT_INGESTION=true`): NTVMR API → TEI/XML → manuscript_verses
3. **Ingestão patrística** (se `ENABLE_PATRISTIC_INGESTION=true`): seed de 35 pais + 36 declarações textuais (idempotente via chave lógica)

### 5.2 Fonte Primária: NTVMR

- **URL**: `http://ntvmr.uni-muenster.de/community/vmr/api/transcript/get/`
- **Parâmetros**: `docID` (5 dígitos), `indexContent` (livro), `format=teiraw`
- **Lógica**: `<ab n="B04K8V12">` com filhos = verso presente

### 5.3 Seed Patrístico

- **ChurchFathersSeedData**: 35 pais com nome, período, localização, tradição
- **ChurchFatherTranslationsSeedData**: traduções (pt, es) dos 35 pais
- **TextualStatementsSeedData**: 36 declarações curadas de Irenaeus, Origen, Eusebius, Jerome, Augustine, Athanasius, Tertullian e outros
- **TextualStatementTranslationsSeedData**: traduções (pt, es) das 36 declarações
- **Idempotência**: `insertIfNotExists` com chave lógica (fatherId, sourceWork, sourceReference)

### 5.4 Variáveis de Ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `USE_NTVMR` | `true` | Usar API NTVMR como fonte primária |
| `NTVMR_DELAY_MS` | `500` | Delay entre requisições |
| `ENABLE_MANUSCRIPT_INGESTION` | `true` | Habilitar ingestão de manuscritos |
| `ENABLE_PATRISTIC_INGESTION` | `true` | Habilitar ingestão patrística |

---

## 6. Domínio: Pais da Igreja

### 6.1 Tópicos Textuais (TextualTopic)

| Tópico | Descrição |
|--------|-----------|
| `MANUSCRIPTS` | Referências a manuscritos e cópias |
| `AUTOGRAPHS` | Sobre os originais apostólicos |
| `APOCRYPHA` | Textos apócrifos e pseudepígrafos |
| `CANON` | Formação e limites do cânone |
| `TEXTUAL_VARIANTS` | Variantes textuais e leituras |
| `TRANSLATION` | Tradução das Escrituras |
| `CORRUPTION` | Corrupção e adulteração textual |
| `SCRIPTURE_AUTHORITY` | Autoridade e inspiração das Escrituras |

### 6.2 Busca

- **Pais**: busca por nome via ILIKE
- **Declarações**: busca fuzzy via `pg_trgm` com `similarity()` e ranking por relevância
- **Índices**: B-tree em father_id, topic, year; composto em (topic, year); GIN trigram em statement_text

### 6.3 Filtragem por Século

O filtro por século usa `century_min`/`century_max` da tabela `church_fathers` via JOIN, não o `approximate_year` da declaração, garantindo estabilidade.

---

## 7. Métricas Acadêmicas

| Métrica | Fórmula |
|---------|---------|
| **Century Growth Rate** | `(coverage_N - coverage_N-1) / coverage_N-1 * 100` |
| **Stabilization Century** | Primeiro século onde coverage >= 90% |
| **Fragmentation Index** | `1 - (1 / avg_manuscripts_per_verse)` |
| **Coverage Density** | `covered_verses / manuscript_count` |
| **Manuscript Concentration Score** | `manuscripts_covering_book / total_manuscripts` |

---

## 8. Docker e Scripts

### 8.1 Scripts

| Script | Função |
|--------|--------|
| `up.sh` | Menu interativo para escolher dev ou prod |
| `up.dev.sh` | Build dev com cache, PG exposto, re-ingestão completa |
| `up.prod.sh` | Build prod sem cache, limites de memória, skip-if-populated |
| `backup.sh` | Backup PostgreSQL via `pg_dump -Fc` (Docker) |
| `restore.sh` | Restore PostgreSQL via `pg_restore` (Docker) |

### 8.2 Docker Compose

**Serviços (docker-compose.yml)**:
- **postgres** (5432): PostgreSQL 16 com volume persistente
- **init**: restore de backup se disponível (executa e sai)
- **app** (8080): Backend Kotlin/Ktor
- **frontend** (3000): Next.js com rewrite `/api/*` → backend

**Produção (docker-compose.prod.yml)**:
- Restart policies
- Limites de memória
- Variáveis de ambiente de produção

### 8.3 Subir manualmente

```bash
./gradlew clean jar
docker compose up --build -d
```

---

## 9. Frontend — Páginas

| Rota | Descrição |
|------|-----------|
| `/` | Home / redirect |
| `/dashboard` | Dashboard com estatísticas globais |
| `/manuscripts` | Explorer de manuscritos |
| `/manuscripts/[gaId]` | Detalhe de manuscrito |
| `/book/[name]` | Detalhe de livro |
| `/timeline` | Timeline evolutiva |
| `/compare` | Comparação de cobertura |
| `/metrics` | Métricas acadêmicas gerais |
| `/metrics/[book]` | Métricas de um livro |
| `/verse-lookup` | Busca de versículos |
| `/manuscript-count` | Contagem de manuscritos por tipo |
| `/fathers` | Catálogo de pais da igreja |
| `/fathers/[id]` | Detalhe de pai (abas: Informações, Declarações Textuais) |
| `/fathers/testimony` | Testemunhos patrísticos com filtros globais |
| `/history` | Conteúdo educacional |
| `/sources` | Fontes e referências |
| `/ingestion-status` | Status da ingestão de dados |
| `/faq` | Perguntas frequentes |

---

## 10. Decisões de Design

| Decisão | Razão |
|---------|-------|
| NTVMR como fonte primária | Precisão manuscritológica |
| Cache materializado | Evitar recálculo a cada request |
| Ingestão no startup com orchestrator | Dados prontos antes de servir, com retry |
| Fallback seed por livro | Manuscritos não disponíveis no NTVMR |
| SchemaUtils como fallback de Flyway | Flyway não aplica migrations (naming issue), SchemaUtils garante tabelas |
| Índices GIN trigram | Busca fuzzy performática em declarações textuais |
| CHECK constraints no banco | Integridade de topic e approximate_year no nível do banco |
| Seed idempotente com chave lógica | Reexecução segura sem duplicação |
| Next.js rewrites | Frontend e backend na mesma origem |
| TanStack Query | Cache e estados de loading/erro |
| next-intl | Internacionalização (pt, en, es) |
| Rotas literais antes de dinâmicas no Ktor | Evitar conflito de roteamento |
| ORDER BY determinístico | `approximate_year ASC NULLS LAST, id ASC` para paginação estável |
