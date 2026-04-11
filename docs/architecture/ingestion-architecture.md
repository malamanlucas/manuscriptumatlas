# Arquitetura da Ingestão — Manuscriptum Atlas

Diagrama do fluxo de ingestão de manuscritos, dados patrísticos e materialização da cobertura.

---

## Visão geral (fluxo de alto nível)

```mermaid
flowchart TB
    subgraph Startup["Inicialização (Application)"]
        A[Application.module] --> B[DatabaseConfig.init]
        B --> C[FlywayConfig.migrate]
        C --> D[Repositórios + Services + Orchestrator]
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
        P --> Q{ENABLE_MANUSCRIPT_INGESTION?}
        Q -->|true| R[ingestManuscriptsAsync]
        R --> S[materializeCoverageAsync]
        Q -->|false| T{ENABLE_PATRISTIC_INGESTION?}
        S --> T
        T -->|true| U[PatristicIngestionService.ingestFromSeed]
        T -->|false| V[markSuccess / markFailed]
        U --> V
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

## Detalhe: ingestão patrística

```mermaid
flowchart TB
    subgraph SeedData["Dados de Seed (curadoria manual)"]
        Fathers["ChurchFathersSeedData\n35 pais da igreja"]
        FatherTrans["ChurchFatherTranslationsSeedData\nTraduções pt/es dos pais"]
        Statements["TextualStatementsSeedData\n36 declarações textuais"]
        StatementTrans["TextualStatementTranslationsSeedData\nTraduções pt/es das declarações"]
    end

    subgraph PatristicService["PatristicIngestionService.ingestFromSeed()"]
        IngestF[Inserir/atualizar pais] --> IngestFT[Inserir traduções dos pais]
        IngestFT --> IngestS[Inserir/atualizar declarações]
        IngestS --> IngestST[Inserir traduções das declarações]
    end

    subgraph Repos["Repositórios"]
        CFR[ChurchFatherRepository]
        FTSR[FatherTextualStatementRepository]
    end

    subgraph DB["PostgreSQL"]
        Tables[(church_fathers\nchurch_father_translations\nfather_textual_statements\nfather_statement_translations)]
    end

    Fathers --> IngestF
    FatherTrans --> IngestFT
    Statements --> IngestS
    StatementTrans --> IngestST

    PatristicService --> CFR
    PatristicService --> FTSR
    CFR --> DB
    FTSR --> DB
```

- **Idempotência**: `insertIfNotExists` com chave lógica (fatherId + sourceWork + sourceReference)
- **Traduções**: seed separado para pt e es, vinculado ao registro principal por fatherId/statementId

---

## Componentes e responsabilidades

```mermaid
flowchart TB
    subgraph Config["Configuração (env)"]
        IngestionConfig["IngestionConfig\nENABLE_INGESTION, SKIP_IF_POPULATED,\nENABLE_MANUSCRIPT_INGESTION,\nENABLE_PATRISTIC_INGESTION,\nINGESTION_TIMEOUT_MINUTES"]
        NTVMR["USE_NTVMR, NTVMR_DELAY_MS,\nLOAD_MANUSCRIPTS_FROM_NTVMR"]
    end

    subgraph Orchestrator["IngestionOrchestrator"]
        Retry["Retry: 3 tentativas\nBackoff 2s, 4s, 8s"]
        Timeout["withTimeout(INGESTION_TIMEOUT_MINUTES)"]
        Meta["IngestionMetadataRepository\nmarkRunning / markSuccess / markFailed"]
    end

    subgraph ManuscriptService["IngestionService"]
        SeedData["ManuscriptSeedData.load()"]
        Ingest["ingestManuscriptsAsync()"]
        Materialize["materializeCoverageAsync()"]
    end

    subgraph PatristicService["PatristicIngestionService"]
        PSeed["ChurchFathersSeedData\nTextualStatementsSeedData\n+ traduções pt/es"]
        PIngest["ingestFromSeed()"]
    end

    subgraph Scraper["Scrapers / API"]
        NtvmrClient["NtvmrClient\nrequestTimeout 60s\ngaId → docID, fetchBookTranscript"]
        Parser["NtvmrVerseParser\nTEI XML → versos presentes"]
        VerseExpander["VerseExpander\nranges texto → (chapter, verse)"]
    end

    subgraph Repos["Repositórios"]
        MR[ManuscriptRepository]
        VR[VerseRepository]
        CR[CoverageRepository]
        CFR[ChurchFatherRepository]
        FTSR[FatherTextualStatementRepository]
    end

    subgraph DB["PostgreSQL"]
        T[(manuscripts\nmanuscript_verses\ncoverage_by_century\ningestion_metadata\nchurch_fathers\nfather_textual_statements\n*_translations)]
    end

    IngestionConfig --> Orchestrator
    NTVMR --> ManuscriptService
    Orchestrator --> ManuscriptService
    Orchestrator --> PatristicService
    ManuscriptService --> SeedData
    ManuscriptService --> NtvmrClient
    ManuscriptService --> Parser
    ManuscriptService --> VerseExpander
    ManuscriptService --> MR
    ManuscriptService --> VR
    ManuscriptService --> CR
    PatristicService --> PSeed
    PatristicService --> CFR
    PatristicService --> FTSR
    MR --> DB
    VR --> DB
    CR --> DB
    CFR --> DB
    FTSR --> DB
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
| 7 | **IngestionService** | Se `ENABLE_MANUSCRIPT_INGESTION=true`: `ManuscriptSeedData.load()` → manuscritos do JSON |
| 8 | **IngestionService** | Se `USE_NTVMR=true`: para cada manuscrito (século ≤ X), chama NTVMR por livro; senão usa só seed |
| 9 | **NtvmrClient** | Converte GA-ID → docID, faz GET na API NTVMR (TEI), delay 500 ms entre chamadas |
| 10 | **NtvmrVerseParser** | Extrai versículos presentes do TEI; se falhar, usa `VerseExpander` + ranges do seed |
| 11 | **VerseRepository** | `insertManuscriptVerses` — liga manuscrito aos verse_id |
| 12 | **IngestionService** | `materializeCoverageAsync()` — para séculos 1..10, calcula e persiste cobertura |
| 13 | **PatristicIngestionService** | Se `ENABLE_PATRISTIC_INGESTION=true`: seed de 35 pais + 36 declarações + traduções (pt, es) |
| 14 | **IngestionMetadataRepository** | `markSuccess(duration, manuscripts, verses)` ou `markFailed()` |

---

## Pontos de decisão

- **Lista de manuscritos:** com `LOAD_MANUSCRIPTS_FROM_NTVMR=true`, a lista vem da API NTVMR `metadata/liste/search` (140+ papiros + unciais). Caso contrário, usa `manuscripts.json`.
- **Conteúdo (quais versículos):** NTVMR API quando `USE_NTVMR=true` e a API responder; senão, ranges do seed.
- **Manuscritos vs. Patrístico:** controlados independentemente por `ENABLE_MANUSCRIPT_INGESTION` e `ENABLE_PATRISTIC_INGESTION`.
- **Ingestão automática:** só roda se `ENABLE_INGESTION != false` e (se `INGESTION_SKIP_IF_POPULATED=true`) o banco estiver vazio.
- **Ingestão manual:** `POST /admin/ingestion/run` chama `orchestrator.triggerManual(scope)` (respeitando uma única execução por vez).
- **Ingestão patrística:** idempotente via chave lógica; inclui traduções (pt, es) para pais e declarações.

Para visualizar os diagramas Mermaid, use o GitHub, o VS Code (extensão Mermaid) ou [mermaid.live](https://mermaid.live).
