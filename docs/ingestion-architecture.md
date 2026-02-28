# Arquitetura da Ingestão — NT Manuscript Coverage

Diagrama do fluxo de ingestão de manuscritos e materialização da cobertura.

---

## Visão geral (fluxo de alto nível)

```mermaid
flowchart TB
    subgraph Startup["Inicialização (Application)"]
        A[Application.module] --> B[DatabaseConfig.init]
        B --> C[FlywayConfig.migrate]
        C --> D[Repositórios + IngestionService + Orchestrator]
        D --> E[seedBooksAndVerses]
        E --> F[API pronta]
        F --> G[ApplicationStarted]
    end

    G --> H[ingestionScope.launch]
    H --> I[IngestionOrchestrator.launchIfEnabled]

    subgraph Orchestrator["IngestionOrchestrator"]
        I --> J{ENABLE_INGESTION?}
        J -->|false| K[INGESTION_SKIPPED]
        J -->|true| L{SKIP_IF_POPULATED?}
        L -->|true + DB já tem dados| K
        L -->|false ou DB vazio| M[executeIngestion]
        M --> N[executeWithRetry max 3]
        N --> O[executeIngestionInner]
        O --> P[markRunning]
        P --> Q[ingestManuscriptsAsync]
        Q --> R[materializeCoverageAsync]
        R --> S[markSuccess / markFailed]
    end
```

---

## Detalhe: ingestão de manuscritos

```mermaid
flowchart LR
    subgraph Fonte["Fonte de dados"]
        JSON["resources/seed\nmanuscripts.json"]
        API["NTVMR API\nntvmr.uni-muenster.de"]
    end

    subgraph IngestionService["IngestionService.ingestManuscriptsAsync()"]
        L[ManuscriptSeedData.load] --> List["Lista de 57 ManuscriptSeed"]
        List --> Branch{USE_NTVMR?}
        Branch -->|true| NTVMR[ingestWithNtvmr]
        Branch -->|false| SeedOnly[ingestFromSeedOnly]
    end

    JSON --> L

    subgraph ingestWithNtvmr["ingestWithNtvmr (por manuscrito)"]
        NTVMR --> Filtro{centuryMin ≤ 10?}
        Filtro -->|não| Skip[skip]
        Filtro -->|sim| Ins[ManuscriptRepository.insertIfNotExists]
        Ins --> Try[tryNtvmrIngestion]
        Try --> Ok{NTVMR retornou versos?}
        Ok -->|sim| LinkAPI[linkVersesByChapterVerse]
        Ok -->|não| Fallback[linkFromSeed]
        LinkAPI --> Delay[delay NTVMR_DELAY_MS]
        Fallback --> Delay
        Delay --> Next[próximo manuscrito]
    end

    subgraph tryNtvmrIngestion["tryNtvmrIngestion (por livro do manuscrito)"]
        NtvmrClient[NtvmrClient.fetchBookTranscript]
        NtvmrClient --> TEI[TEI/XML]
        TEI --> Parser[NtvmrVerseParser.extractPresentVerses]
        Parser --> Versos{versos extraídos?}
        Versos -->|sim| linkVerses[linkVersesByChapterVerse]
        Versos -->|não| linkBookFromSeed[linkBookFromSeed + VerseExpander]
    end

    API --> NtvmrClient
    JSON --> Fallback
    JSON --> linkBookFromSeed
```

---

## Componentes e responsabilidades

```mermaid
flowchart TB
    subgraph Config["Configuração (env)"]
        IngestionConfig["IngestionConfig\nENABLE_INGESTION, SKIP_IF_POPULATED,\nINGESTION_TIMEOUT_MINUTES"]
        NTVMR["USE_NTVMR, NTVMR_DELAY_MS"]
    end

    subgraph Orchestrator["IngestionOrchestrator"]
        Retry["Retry: 3 tentativas\nBackoff 2s, 4s, 8s"]
        Timeout["withTimeout(INGESTION_TIMEOUT_MINUTES)"]
        Meta["IngestionMetadataRepository\nmarkRunning / markSuccess / markFailed"]
    end

    subgraph Service["IngestionService"]
        SeedData["ManuscriptSeedData.load()"]
        Ingest["ingestManuscriptsAsync()"]
        Materialize["materializeCoverageAsync()"]
    end

    subgraph Scraper["Scrapers / API"]
        NtvmrClient["NtvmrClient\nrequestTimeout 60s\n gaId → docID, fetchBookTranscript"]
        Parser["NtvmrVerseParser\nTEI XML → versos presentes"]
        VerseExpander["VerseExpander\nranges texto → (chapter, verse)"]
    end

    subgraph Repos["Repositórios"]
        MR[ManuscriptRepository]
        VR[VerseRepository]
        CR[CoverageRepository]
        SR[StatsRepository]
    end

    subgraph DB["PostgreSQL"]
        T[(manuscripts\nmanuscript_verses\ncoverage_by_century\ningestion_metadata)]
    end

    IngestionConfig --> Orchestrator
    NTVMR --> Service
    Orchestrator --> Service
    Service --> SeedData
    Service --> NtvmrClient
    Service --> Parser
    Service --> VerseExpander
    Service --> MR
    Service --> VR
    Service --> CR
    MR --> DB
    VR --> DB
    CR --> DB
```

---

## Fluxo sequencial resumido

| Etapa | Componente | Ação |
|-------|------------|------|
| 1 | **Application** | `seedBooksAndVerses()` — insere 27 livros e 7.956 versículos canônicos |
| 2 | **Application** | No evento `ApplicationStarted`, dispara `orchestrator.launchIfEnabled()` em coroutine |
| 3 | **IngestionOrchestrator** | Verifica `ENABLE_INGESTION` e `INGESTION_SKIP_IF_POPULATED` (e se DB já tem manuscritos) |
| 4 | **IngestionOrchestrator** | `executeWithRetry(3)` → em falha, espera 2s / 4s / 8s e tenta de novo |
| 5 | **IngestionOrchestrator** | `executeIngestionInner()` com timeout global (ex.: 30 min) |
| 6 | **IngestionMetadataRepository** | `markRunning()` |
| 7 | **IngestionService** | `ManuscriptSeedData.load()` → 57 manuscritos do JSON |
| 8 | **IngestionService** | Se `USE_NTVMR=true`: para cada manuscrito (século ≤ X), chama NTVMR por livro; senão usa só seed |
| 9 | **NtvmrClient** | Converte GA-ID → docID, faz GET na API NTVMR (TEI), delay 500 ms entre chamadas |
| 10 | **NtvmrVerseParser** | Extrai versículos presentes do TEI; se falhar, usa `VerseExpander` + ranges do seed |
| 11 | **VerseRepository** | `insertManuscriptVerses` — liga manuscrito aos verse_id |
| 12 | **IngestionService** | `materializeCoverageAsync()` — para séculos 1..10, calcula e persiste cobertura |
| 13 | **IngestionMetadataRepository** | `markSuccess(duration, manuscripts, verses)` ou `markFailed()` |

---

## Pontos de decisão

- **Lista de manuscritos:** com `LOAD_MANUSCRIPTS_FROM_NTVMR=true` (DEV), a lista vem da API NTVMR `metadata/liste/search` (140+ papiros + unciais). Caso contrário, usa `manuscripts.json` (57 itens).
- **Conteúdo (quais versículos):** NTVMR API quando `USE_NTVMR=true` e a API responder; senão, ranges do seed.
- **Ingestão automática:** só roda se `ENABLE_INGESTION != false` e (se `INGESTION_SKIP_IF_POPULATED=true`) o banco estiver vazio.
- **Ingestão manual:** `POST /admin/ingestion/run` chama `orchestrator.triggerManual(scope)` (respeitando uma única execução por vez).

Para visualizar os diagramas Mermaid, use o GitHub, o VS Code (extensão Mermaid) ou [mermaid.live](https://mermaid.live).
