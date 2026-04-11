# Guia de Arquitetura — Modulo Bible do Manuscriptum Atlas

## 1. Visao Geral: Dois Mundos, Um Sistema

```
┌─────────────────────────────────────────────────┐
│                    Ktor App (JVM unico)           │
│                                                   │
│  ┌──────────────┐      ┌──────────────────────┐  │
│  │  Atlas        │      │  Bible                │  │
│  │  (manuscritos,│      │  (texto, interlinear, │  │
│  │   patristica, │      │   lexico, Strong's)   │  │
│  │   concilios)  │      │                       │  │
│  │       ↕       │      │          ↕            │  │
│  │   atlas_db    │      │      bible_db         │  │
│  └──────────────┘      └──────────────────────┘  │
│                                                   │
│  ┌─────────────────────────────────────────────┐  │
│  │  Infraestrutura compartilhada:               │  │
│  │  - IngestionPhaseTracker (em atlas_db)       │  │
│  │  - LlmOrchestrator (OpenAI)                  │  │
│  │  - SourceFileCache (filesystem)              │  │
│  └─────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

**Decisao chave:** Dois databases no mesmo PostgreSQL, um JVM. O Bible nao depende do Atlas e vice-versa, mas compartilham infraestrutura (phase tracking, LLM, cache).

**Armadilha resolvida:** O Exposed ORM tenta usar o ultimo `Database.connect()` como default. Solucao: salvar e restaurar o `TransactionManager.defaultDatabase` apos conectar `bible_db`, e usar `transaction(bibleDb) {}` explicito nos repositories Bible.

---

## 2. Camadas e Fluxo de Dados

```
HTTP Request
    ↓
  Routes (recebe request, valida params, responde)
    ↓
  Service (logica de negocio, orquestra repositories)
    ↓
  Repository (queries SQL via Exposed, transaction{})
    ↓
  Database (PostgreSQL)
```

**Regra de ouro:** `transaction {}` NUNCA no Service, SEMPRE no Repository. O Service coordena mas nao abre transacoes.

**Injecao de dependencia:** Por construtor, sem framework. Tudo instanciado no `Application.kt` na ordem correta.

---

## 3. Pipeline de Ingestao — O Coracao do Sistema

### 3.1 Conceito de Fases

Cada dominio tem um pipeline de fases sequenciais. As fases sao:
- **Idempotentes** — rodar 2x nao duplica dados (upsert)
- **Rastreadas** — status persistido no banco (idle → running → success/failed)
- **Recuperaveis** — fases stuck sao marcadas como failed no restart

```
┌─────────────────────────────────────────┐
│  Phase Lifecycle                         │
│                                          │
│  idle ──→ running ──→ success            │
│              │                           │
│              └──→ failed                 │
│                                          │
│  No restart: running → failed (recovery) │
└─────────────────────────────────────────┘
```

### 3.2 Padrao `runPhaseTracked`

Toda fase usa este wrapper:

```
runPhaseTracked("nome_da_fase") {
    // 1. markRunning(phase, total)
    // 2. ... trabalho real ...
    //    markProgress(phase, processed) a cada N items
    // 3. markSuccess(phase, total) — automatico no wrapper
    //    ou markFailed(phase, error) — automatico no catch
}
```

**Resiliencia:** Se o servidor cair durante uma fase `running`, no proximo startup o `recoverStuckPhases()` marca como `failed` para que possa ser re-executada.

### 3.3 Concorrencia

```
POST /admin/bible/ingestion/run/{phase}
    ↓
  AdminRoutes verifica: phaseTracker.isAnyRunningByPrefix("bible_")
    ↓
  Se running → 409 Conflict (rejeita)
  Se idle → ingestionScope.launch { service.runPhase(phase) }
    ↓
  Responde 202 Accepted (assincrono)
```

**Mecanismo:** Nao usa locks de banco. Usa verificacao de status (`isAnyRunning`) + `AtomicBoolean` no orchestrator. Funciona porque e single-server.

### 3.4 Fluxo Completo do Bible Pipeline

```
 1. bible_seed_versions         → Insere KJV, AA, ACF, ARC69 em bible_versions
 2. bible_seed_books            → Insere 66 livros + capitulos + versiculos
 3. bible_seed_abbreviations    → Insere siglas por locale (Mt, Jo, Gn...)
 4. bible_ingest_text_kjv       → Download JSON GitHub → parse → insert bible_verse_texts
 5. bible_ingest_text_aa        → Idem para Almeida Atualizada
 6. bible_ingest_text_acf       → Idem para Almeida Corrigida Fiel
 7. bible_ingest_text_arc69     → Scraper: bible-api.com → parse JSON → insert
 8. bible_ingest_nt_interlinear → Download TAGNT (2 arquivos ~30MB) → parse TSV → insert interlinear_words
 9. bible_ingest_greek_lexicon  → Download TBESG → parse TSV → insert greek_lexicon
10. bible_translate_lexicon     → LLM: traduz definicoes EN→PT/ES
11. bible_translate_glosses     → LLM: traduz glosses EN→PT/ES por capitulo
```

---

## 4. Sistema de Filtros

```
┌──────────────────────────────────┐
│  bible-ingestion-filter.txt      │
│                                  │
│  John:1-3                        │  ← so Joao capitulos 1 a 3
│  Genesis:1                       │  ← so Genesis 1
│  Matthew                         │  ← todo Mateus
│  # ALL                           │  ← descomentado = tudo
└──────────────────────────────────┘
         ↓
  loadBookFilter() → List<BookFilter>?
         ↓
  shouldProcessBook(name, filters) → bool
  shouldProcessChapter(name, ch, filters) → bool
         ↓
  Usado por: seedBooks, ingestText*, ingestTAGNT, translateGlosses
  NAO usado por: seedVersions, lexicon (sao globais)
```

**Empacotado no JAR** — precisa rebuild apos edicao. Tradeoff: simples mas requer deploy.

---

## 5. Fontes de Dados e Estrategias de Download

### 5.1 Texto Biblico (KJV, AA, ACF)

```
GitHub (thiagobodruk/bible)
    ↓ HTTP GET (JSON ~4MB)
    ↓ retry 3x com backoff (2s, 4s, 6s)
    ↓
  Parse JSON: [{abbrev, chapters: [[v1,v2,...],...]}, ...]
    ↓
  Para cada livro x capitulo x versiculo:
    getVerseId(bookId, chapter, verse) → verseId
    upsertVerseText(versionId, verseId, text)
```

### 5.2 Texto ARC69 (Scraper)

```
bible-api.com/joao+1?translation=almeida
    ↓ HTTP GET (JSON com versos estruturados)
    ↓ Cache em filesystem (SourceFileCache)
    ↓ delay 200ms entre requests
    ↓
  Parse JSON: {verses: [{verse: 1, text: "..."}, ...]}
    ↓
  upsertVerseText(versionId, verseId, text)
```

**Resiliencia do cache:** Se o arquivo ja existe em `/data/source-cache/`, nao baixa de novo. Permite re-executar sem re-download.

### 5.3 Interlinear NT (TAGNT)

```
GitHub (STEPBible-Data)
    ↓ 2 arquivos TSV (~15MB cada)
    ↓
  Parse cada linha:
    Mat.1.1#01=NKO  Biblos (Biblos)  [The] book  G0976=N-NSF  biblos=book
    ↓
  Extrai: bookName, chapter, verse, wordPosition,
          originalWord, transliteration, englishGloss,
          strongsNumber, morphology, lemma
    ↓
  upsertWord(verseId, position, word, gloss, strongs, ...)
```

**Antes de inserir:** Deleta todos os interlinear words existentes (full refresh).

### 5.4 Lexico Grego (TBESG)

```
GitHub (STEPBible-Data/Lexicons)
    ↓ 1 arquivo TSV (~5MB, ~11.000 entradas)
    ↓
  Parse: G3056 → lemma=logos, transliteration=logos,
         shortDef=word, fullDef=[definicao academica longa]
    ↓
  upsertGreek(strongsNumber, lemma, shortDef, fullDef, ...)
```

---

## 6. LLM Orchestrator — Traducao via IA

```
BibleIngestionService
    ↓ chamada de traducao
LlmOrchestrator
    ↓ tenta provider 1 (OpenAI gpt-4o-mini)
    ↓ se falhar → fallback provider 2 (se houver)
    ↓
LlmRateLimiter
    ↓ controla tokens/min e requests/min por provider
    ↓
OpenAiProvider
    ↓ HTTP POST para api.openai.com
    ↓
  Resposta → parse → retorna conteudo
```

### 6.1 Traducao do Lexico (batch individual)

```
Para cada entrada do greek_lexicon:
  Para cada locale (pt, es):
    Se ja traduzida → skip
    Senao → LLM request:
      System: "Translate this Greek lexicon entry to Portuguese..."
      User: "SHORT: word\nFULL: [definicao longa]"
      ↓
    Parse resposta: "SHORT: palavra\nFULL: [definicao traduzida]"
      ↓
    upsertGreekTranslation(lexiconId, locale, shortDef, fullDef)
```

**Rate limiting:** 200ms delay entre requests.
**Skip inteligente:** `hasTranslation(id, locale)` evita re-traduzir.

### 6.2 Traducao de Glosses (batch por capitulo)

```
Para cada livro/capitulo (do filtro):
  Coleta glosses unicos do capitulo (ex: 38 palavras unicas)
    ↓
  Chunking: divide em lotes de 100 glosses
    ↓
  LLM request (1 request por chunk):
    System: "Translate each English gloss to Portuguese, one per line"
    User: "In [the]\nbeginning\nwas\nthe\nWord\n..."
    ↓
  Parse: resposta linha-por-linha → Map<englishGloss, translatedGloss>
    ↓
  Aplica a todas as palavras do capitulo:
    updateGlosses(wordId, portugueseGloss, spanishGloss)
```

**Eficiencia:** Traduz glosses UNICOS por capitulo, nao por palavra. Se "the" aparece 50x, traduz 1x e aplica a 50 rows.

---

## 7. Frontend — Dados do Interlinear no Leitor

```
Pagina /bible (LinkedVerseReader)
    ↓
  useBibleChapter(version, book, chapter)    → texto dos versiculos
  useBibleInterlinearChapter(book, chapter)  → palavras gregas com glosses
    ↓
  Para cada versiculo:
    1. Texto principal (ex: KJV) com palavras clicaveis
       LinkedText: mapeia gloss→Strong's, renderiza links
    2. Versoes comparativas (AA, ACF) inline
    3. Botao "Mostrar interlinear grego" → expande cards de palavras
```

### 7.1 LinkedText — Como as palavras viram links

```
Input: "In the beginning was the Word"
Interlinear: [{gloss:"In [the]", strongs:"G1722"}, {gloss:"beginning", strongs:"G0746"}, ...]
    ↓
  Monta mapa: {"in" → G1722, "beginning" → G0746, "was" → G1510, ...}
    ↓
  Split texto em palavras
    ↓
  Para cada palavra: lookup no mapa
    Match → renderiza como <Link href="/bible/strongs/G1722">In</Link>
    Sem match → renderiza como <span>the</span>
```

---

## 8. Concordancia Strong's

```
GET /bible/strongs/G3056
    ↓
BibleService.getStrongsConcordance("G3056")
    ↓
  1. lexiconRepository.findByStrongsNumberWithTranslation("G3056", locale)
     → Busca definicao (com traducao se disponivel)
  2. interlinearRepository.getStrongsConcordance("G3056", page, limit)
     → SELECT ... FROM interlinear_words WHERE strongs_number = 'G3056'
        JOIN bible_verses JOIN bible_books
        ORDER BY book_order, chapter, verse
     → Lista de todos os versiculos onde logos aparece
    ↓
  StrongsConcordanceDTO {
    lexiconEntry: { lemma, definition, ... }
    totalOccurrences: 332
    occurrences: [{ book, chapter, verse, originalWord, morphology }, ...]
  }
```

---

## 9. Busca Biblica

```
GET /bible/search?q=amor
    ↓
BibleService.searchText("amor")
    ↓
  1. referenceParser.parse("amor") → null (nao e referencia)
  2. verseRepository.searchText("amor", ...)
     → SELECT ... WHERE LOWER(text) LIKE '%amor%'
        JOIN bible_verses, bible_books, bible_versions
     → Resultados com snippet highlight (+-40 chars em volta do match)
    ↓
  BibleSearchResponse { results: [...], totalResults: 150 }

GET /bible/search?q=Jo 3.16
    ↓
  1. referenceParser.parse("Jo 3.16") → ResolvedReferenceDTO
     → Regex: ^(\d?\s?[A-Za-z]+)\s+(\d+)[.:](\d+)$
     → Lookup "Jo" em bible_book_abbreviations → "John"
  2. isReference = true → retorna referencia sem buscar texto
```

---

## 10. Reset e Recuperacao

### Reset (botao lixeira)

```
POST /admin/reset/bible
    ↓
IngestionOrchestrator.resetBible()
    ↓
  1. transaction(bible_db) {
       TRUNCATE: translations → interlinear → lexicon →
                 verse_texts → abbreviations → chapters →
                 verses → books → versions
     }
  2. phaseTracker.deleteByPrefix("bible_")  ← roda em atlas_db (fora da transaction)
```

**Armadilha resolvida:** O phaseTracker usa `atlas_db`, mas o reset roda dentro de `transaction(bible_db)`. Solucao: separar a limpeza de fases para FORA da transaction do bible_db.

### Recovery no startup

```
Application.start()
    ↓
  phaseTracker.recoverStuckPhases()
    ↓
  SELECT * FROM council_ingestion_phases WHERE status = 'running'
    ↓
  Para cada: markFailed(phase, "Interrupted by server restart")
```

---

## 11. Diagrama de Dependencias

```
Application.kt (wiring)
    │
    ├── BibleDatabaseConfig (HikariCP pool → bible_db)
    ├── BibleFlywayConfig (SchemaUtils → cria tabelas)
    │
    ├── Repositories (todos usam transaction(bibleDb))
    │   ├── BibleVersionRepository
    │   ├── BibleBookRepository
    │   ├── BibleVerseRepository (+ search, compare)
    │   ├── InterlinearRepository (+ concordancia Strong's)
    │   └── LexiconRepository (+ traducoes)
    │
    ├── Services
    │   ├── BibleService (leitura: chapter, search, compare, interlinear, lexicon)
    │   ├── BibleIngestionService (13 fases + filtro + LLM)
    │   └── BibleReferenceParser (regex + lookup abbreviations)
    │
    ├── Scrapers
    │   └── BibleOnlineScraper (bible-api.com + SourceFileCache)
    │
    └── Routes
        └── BibleRoutes (route("/bible") { ... })
            + AdminRoutes (bible ingestion endpoints)
```

---

## 12. Tabelas do bible_db

| Tabela | Rows (estimado) | Proposito |
|--------|-----------------|-----------|
| bible_versions | 4 | KJV, AA, ACF, ARC69 |
| bible_books | 66 | 39 AT + 27 NT |
| bible_book_abbreviations | ~400 | Siglas por locale (Mt, Jo, Gn) |
| bible_chapters | 1.189 | Metadata de capitulos |
| bible_verses | 31.102 | Um row por versiculo |
| bible_verse_texts | ~124k | Texto por versao x versiculo |
| interlinear_words | ~141k | Palavra-por-palavra grego NT |
| greek_lexicon | ~11k | Dicionario Strong's grego |
| greek_lexicon_translations | ~22k | Traducoes PT/ES do lexico |
| hebrew_lexicon | 0 | (futuro) Dicionario hebraico |

## 13. Tabela de Fases (em atlas_db)

| Fase | Tipo | Filtro | Fonte |
|------|------|--------|-------|
| bible_seed_versions | seed | nao | codigo |
| bible_seed_books | seed | livro | codigo |
| bible_seed_abbreviations | seed | livro | codigo |
| bible_ingest_text_kjv | download | livro+cap | GitHub JSON |
| bible_ingest_text_aa | download | livro+cap | GitHub JSON |
| bible_ingest_text_acf | download | livro+cap | GitHub JSON |
| bible_ingest_text_arc69 | scraper | livro+cap | bible-api.com |
| bible_ingest_nt_interlinear | download | livro+cap | TAGNT TSV |
| bible_ingest_ot_interlinear | placeholder | - | (futuro) OSHB |
| bible_ingest_greek_lexicon | download | nao | TBESG TSV |
| bible_ingest_hebrew_lexicon | placeholder | - | (futuro) |
| bible_translate_lexicon | LLM | nao | OpenAI gpt-4o-mini |
| bible_translate_glosses | LLM | livro+cap | OpenAI gpt-4o-mini |
