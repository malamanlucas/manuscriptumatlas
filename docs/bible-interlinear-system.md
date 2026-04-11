# Sistema Interlinear Bblico — Guia Completo

> Documento tcnico que explica como funciona o sistema interlinear grego-portugus
> do Manuscriptum Atlas, desde a ingestao dos dados at a renderizacao na tela.

---

## Indice

1. [Visao Geral](#1-visao-geral)
2. [Arquitetura em Camadas](#2-arquitetura-em-camadas)
3. [Camada 1 — Dados Fonte (TAGNT)](#3-camada-1--dados-fonte-tagnt)
4. [Camada 2 — Traducao de Glosses via LLM](#4-camada-2--traducao-de-glosses-via-llm)
5. [Camada 3 — Textos Biblicos por Versao](#5-camada-3--textos-biblicos-por-versao)
6. [Camada 4 — Alinhamento de Palavras (LLM)](#6-camada-4--alinhamento-de-palavras-llm)
7. [Esquema do Banco de Dados](#7-esquema-do-banco-de-dados)
8. [API Backend — Servindo os Dados](#8-api-backend--servindo-os-dados)
9. [Frontend — Renderizacao e Interacao](#9-frontend--renderizacao-e-interacao)
10. [Fluxo Completo: Do TAGNT a Tela](#10-fluxo-completo-do-tagnt-a-tela)
11. [Bug dos Glosses Desalinhados (Fix 2026-03)](#11-bug-dos-glosses-desalinhados-fix-2026-03)
12. [Re-ingestao via UI](#12-re-ingestao-via-ui)

---

## 1. Visao Geral

O sistema interlinear permite que o usuario veja, para cada versiculo do Novo Testamento:

- **Texto grego** original (palavra por palavra)
- **Transliteracao** (caracteres latinos)
- **Gloss** (traducao palavra-a-palavra em EN/PT/ES)
- **Numero Strong** (referencia ao lexico grego)
- **Alinhamento** com traducoes (KJV, ARC69)
- **Highlighting bidirecional** — clicar na palavra grega destaca a traducao, e vice-versa

```
 Versculo ARC69:  "No princpio, era o Verbo, e o Verbo estava com Deus..."
                      |                 |                      |
                      v                 v                      v
 Grego:           [ archē ]        [ logos ]              [ theon ]
 Gloss PT:         princpio         Palavra                 Deus
 Strong:            G0746            G3056                  G2316
```

---

## 2. Arquitetura em Camadas

O sistema  construido em 4 camadas de ingestao, cada uma dependendo da anterior:

```
┌─────────────────────────────────────────────────────────────────┐
│                    CAMADA 1 — ESTRUTURA                         │
│  Seeds: versoes (KJV, ARC69), livros, capitulos, versiculos    │
│  Tabelas: bible_versions, bible_books, bible_verses             │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────v──────────────────────────────────────┐
│                CAMADA 2 — GREGO & LXICO                        │
│  TAGNT → interlinear_words (grego + gloss EN)                  │
│  Lxico grego/hebraico (Strong's)                               │
│  Traducao de glosses EN → PT/ES via LLM                        │
│  Enriquecimento via BibleHub                                    │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────v──────────────────────────────────────┐
│              CAMADA 3 — TEXTOS BBLICOS                         │
│  Texto dos versiculos por versao (KJV, AA, ACF, ARC69)         │
│  Tabela: bible_verse_texts                                      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────v──────────────────────────────────────┐
│             CAMADA 4 — ALINHAMENTO (LLM)                       │
│  Alinhamento grego → KJV (ingles)                              │
│  Alinhamento grego → ARC69 (portugues)                         │
│  Tabela: word_alignments                                        │
└─────────────────────────────────────────────────────────────────┘
```

### Fases de Ingestao


| Camada | Fase                          | Descricao                               |
| ------ | ----------------------------- | --------------------------------------- |
| 1      | `bible_seed_versions`         | Cria KJV, AA, ACF, ARC69                |
| 1      | `bible_seed_books`            | Cria 66 livros + capitulos + versiculos |
| 1      | `bible_seed_abbreviations`    | Abreviacoes multi-locale                |
| 2      | `bible_ingest_nt_interlinear` | Importa TAGNT (grego + gloss EN)        |
| 2      | `bible_ingest_greek_lexicon`  | Lexico grego (Strong's)                 |
| 2      | `bible_ingest_hebrew_lexicon` | Lexico hebraico                         |
| 2      | `bible_translate_lexicon`     | Traduz lexico grego PT/ES (LLM)         |
| 2      | `bible_translate_glosses`     | **Traduz glosses EN → PT/ES (LLM)**     |
| 2      | `bible_enrich_greek_lexicon`  | Enriquece via BibleHub                  |
| 3      | `bible_ingest_text_kjv`       | Texto KJV                               |
| 3      | `bible_ingest_text_arc69`     | Texto ARC69 (scraper)                   |
| 4      | `bible_align_kjv`             | **Alinha grego → KJV (LLM)**            |
| 4      | `bible_align_arc69`           | **Alinha grego → ARC69 (LLM)**          |


---

## 3. Camada 1 — Dados Fonte (TAGNT)

### O que e TAGNT?

**Translators Amalgamated Greek NT** — dataset open-source (CC BY 4.0) do STEPBible
que contm cada palavra grega do NT com transliteracao, gloss em ingles,
numero Strong e codigo morfologico.

### Formato TSV do TAGNT

Cada linha  uma palavra grega:

```
Mat.1.1#01=NKO	Βίβλος (Biblos)	[The] book	G0976=N-NSF	βίβλος=book
```


| Coluna | Conteudo                           | Exemplo           |
| ------ | ---------------------------------- | ----------------- |
| 0      | Referencia `Livro.Cap.Vers#Pos=MS` | `Mat.1.1#01=NKO`  |
| 1      | Grego `(Transliteracao)`           | `Βίβλος (Biblos)` |
| 2      | Gloss ingles                       | `[The] book`      |
| 3      | Strong=Morfologia                  | `G0976=N-NSF`     |
| 4      | Lemma=Definicao                    | `βίβλος=book`     |


### Parsing

```
Referencia: Mat.1.1#01 → livro=Matthew, cap=1, vers=1, pos=1
Grego:      Βίβλος (Biblos) → original="Βίβλος", translit="Biblos"
Gloss:      [The] book → englishGloss="[The] book"
Strong:     G0976=N-NSF → strongsNumber="G0976", morphology="N-NSF"
Lemma:      βίβλος=book → lemma="βίβλος"
```

### Convencoes do Gloss

- `[The]` — colchetes = palavra implicita no grego (artigo/pronome)
- `<the>` — angulares = palavra opcional na traducao
- `Word,` — pontuacao preservada do contexto

### Codigo Morfologico

```
N-NSF = Substantivo, Nominativo, Singular, Feminino
V-PAI-3S = Verbo, Presente, Ativo, Indicativo, 3 Pessoa, Singular
T-NSM = Artigo, Nominativo, Singular, Masculino
```

**Arquivo:** `backend/.../service/BibleIngestionService.kt` — funcao `parseTAGNTAndInsert()`

---

## 4. Camada 2 — Traducao de Glosses via LLM

### Problema

O TAGNT s contm glosses em **ingles**. Para o interlinear em portugues,
precisamos traduzir cada gloss.

### Fluxo

```
interlinear_words.english_gloss ("In [the]", "beginning", "was", ...)
         │
         ▼
   Coleta glosses nicos por captulo
         │
         ▼
   Envia ao LLM em batches de 100 (1 gloss por linha)
         │
         ▼
   LLM retorna tradues (1 por linha)
         │
         ▼
   Mapeia: english_gloss → portuguese_gloss
         │
         ▼
interlinear_words.portuguese_gloss ("Em", "princpio", "era", ...)
```

### System Prompt do LLM

```
You are a biblical Greek-to-Portuguese translator.
Translate each English gloss (one per line) to Portuguese.
Return EXACTLY the same number of lines, one translation per line.
Keep brackets [], angle brackets <>, and punctuation style.
For articles (the, a), use the Portuguese equivalent.
Short, concise translations — single words or very short phrases.
Do NOT add explanations. Just the translated gloss, one per line.
```

### Exemplo de Entrada/Saida

```
Entrada (EN):        Saida (PT):
In [the]         →   Em
beginning        →   princpio
was              →   era
the              →   o
Word             →   Palavra
and              →   e
with             →   com
God              →   Deus
```

### Parsing Two-Pass (apos fix)

```
Resposta do LLM (pode ter preambulo):
"Aqui esta a traducao:"     ← FILTRADO (Pass 1: meta-texto)
"Em"                         ← Mapeado para chunk[0]
"princpio"                  ← Mapeado para chunk[1]
"era"                        ← Mapeado para chunk[2]
...
```

**Pass 1:** Remove linhas de meta-texto (padres: "traduo", "here is", "certainly", etc.)
**Pass 1b:** Se ainda sobrou mais linhas que o esperado, remove excesso do inicio
**Pass 2:** Mapeia posicionalmente linhas limpas → glosses de entrada

**Arquivo:** `backend/.../service/BibleIngestionService.kt` — funcoes `translateGlosses()` e `processGlossResponse()`

---

## 5. Camada 3 — Textos Biblicos por Versao

Armazena o texto de cada versiculo em cada versao:


| Versao | Idioma    | Fonte                       |
| ------ | --------- | --------------------------- |
| KJV    | Ingles    | GitHub (dominio publico)    |
| AA     | Portugues | GitHub                      |
| ACF    | Portugues | GitHub                      |
| ARC69  | Portugues | Scraper bibliaonline.com.br |


Cada versiculo e armazenado em `bible_verse_texts`:

```
verse_id + version_id → texto do versiculo
```

---

## 6. Camada 4 — Alinhamento de Palavras (LLM)

### O que e?

O alinhamento mapeia cada palavra grega para as palavras correspondentes
na traducao. Por exemplo:

```
 Grego:   ν     ρχ     ν      ὁ     λόγος   κα
 KJV:     In    the   beginning was   the    Word    and
 Align:   [0]   [1,2]          [3]          [4,5]   [6]

 ARC69:   No    princpio,  era   o    Verbo,  e
 Align:   [0]   [1]        [2]  [3]   [4]    [5]
```

### Fluxo para cada versiculo

```
1. Carregar palavras gregas do versiculo (InterlinearWords)
2. Carregar texto da versao alvo (ex: ARC69)
3. Tokenizar texto alvo em palavras
4. Detectar expressoes multi-palavras (pre-alinhamento)
5. Construir prompt com:
   - Palavras gregas + gloss EN + gloss PT (localGloss)
   - Morfologia (artigos, verbos, etc.)
   - Candidatos com score de similaridade
   - Posicao relativa no versiculo
6. LLM retorna JSON de alinhamentos
7. Pos-processar: resolver conflitos, reatribuir orfaos
8. Salvar em word_alignments
```

### Prompt do LLM (Portugues)

```json
Greek: [
  {"pos":1,"word":"Ἐν","gloss":"In [the]","localGloss":"Em","morph":"P","relPos":0.00,
   "candidates":[{"idx":0,"word":"No","sim":75,"dist":0.00}]},
  {"pos":2,"word":"ἀρχῇ","gloss":"beginning","localGloss":"princpio","morph":"N-DSF","relPos":0.06,
   "candidates":[{"idx":1,"word":"princpio,","sim":100,"dist":0.07}]},
  ...
]
Translation words: ["0:No","1:princpio,","2:era","3:o","4:Verbo,","5:e",...]

Return: {"a":[{"g":<greekPos>,"k":[<translationIdx>,...],"t":"<translationText>","c":<confidence>},...]}
```

### Regras especificas para Portugues

- **Contracoes:** "no"="em+o", "do"="de+o", "ao"="a+o", "na"="em+a"
  - Uma contracao pode corresponder a 2 palavras gregas (preposicao + artigo)
- **Artigos obliquos** (acusativo T-A*, genitivo T-G*, dativo T-D*):
  - Frequentemente absorvidos pela traducao (ex: grego "τὸν θεόν" → PT "Deus", nao "o Deus")
  - Quando nao tem correspondencia, `k: null` com confianca 60-70
- **Artigos nominativos** (T-N*): geralmente aparecem como "o"/"a" na traducao

### Confianca do Alinhamento


| Score  | Significado                                |
| ------ | ------------------------------------------ |
| 95-100 | Match exato ou quase exato                 |
| 80-94  | Match semantico claro / sinonimo           |
| 60-79  | Match razoavel com interpretacao           |
| < 60   | Fraco/incerto (marcado como `isDivergent`) |


**Arquivo:** `backend/.../service/WordAlignmentService.kt`

---

## 7. Esquema do Banco de Dados

### Diagrama ER

```
bible_versions          bible_books           bible_verses
┌──────────────┐      ┌──────────────┐      ┌──────────────────┐
│ id           │      │ id           │      │ id               │
│ code (KJV)   │      │ name         │◄─────│ book_id          │
│ name         │      │ testament    │      │ chapter          │
│ language     │      │ total_chaps  │      │ verse_number     │
│ description  │      └──────────────┘      └────────┬─────────┘
└──────┬───────┘                                     │
       │                                             │
       │         bible_verse_texts                   │
       │        ┌──────────────────┐                 │
       └───────►│ verse_id ────────│─────────────────┘
                │ version_id       │
                │ text             │
                └──────────────────┘

                    interlinear_words
                   ┌─────────────────────────────┐
                   │ id                           │
              ┌────│ verse_id                     │
              │    │ word_position (1-based)       │
              │    │ original_word (Βίβλος)        │
              │    │ transliteration (Biblos)      │
              │    │ lemma (βίβλος)                │
              │    │ morphology (N-NSF)            │
              │    │ strongs_number (G0976)        │
              │    │ english_gloss (book)          │
              │    │ portuguese_gloss (livro)      │
              │    │ spanish_gloss (libro)         │
              │    │ language (greek/hebrew)       │
              │    └─────────────────────────────┘
              │
              │         word_alignments
              │        ┌─────────────────────────────┐
              └───────►│ verse_id                     │
                       │ word_position                │
                       │ version_code (KJV/ARC69)     │
                       │ kjv_indices ([0,1,2] JSON)   │
                       │ aligned_text ("the book")    │
                       │ is_divergent (bool)          │
                       │ confidence (0-100)           │
                       └─────────────────────────────┘

       greek_lexicon                greek_lexicon_translations
      ┌───────────────────┐       ┌─────────────────────────┐
      │ id                │◄──────│ lexicon_id              │
      │ strongs_number    │       │ locale (pt/es)          │
      │ lemma             │       │ short_definition        │
      │ transliteration   │       │ full_definition         │
      │ short_definition  │       │ kjv_translation         │
      │ full_definition   │       │ word_origin             │
      │ part_of_speech    │       └─────────────────────────┘
      │ usage_count       │
      │ (enrichment...)   │
      └───────────────────┘
```

### Chaves Unicas


| Tabela                       | Unique Index                              |
| ---------------------------- | ----------------------------------------- |
| `interlinear_words`          | `(verse_id, word_position)`               |
| `word_alignments`            | `(verse_id, word_position, version_code)` |
| `greek_lexicon`              | `(strongs_number)`                        |
| `greek_lexicon_translations` | `(lexicon_id, locale)`                    |


---

## 8. API Backend — Servindo os Dados

### Endpoints Interlineares

#### `GET /bible/interlinear/{book}/{chapter}?alignVersion=ARC69`

Retorna o capitulo inteiro com dados interlineares + alinhamento:

```json
{
  "book": "John",
  "chapter": 1,
  "verses": [
    {
      "verseNumber": 1,
      "words": [
        {
          "wordPosition": 1,
          "originalWord": "Ἐν",
          "transliteration": "En",
          "lemma": "ἐν",
          "morphology": "P",
          "strongsNumber": "G1722",
          "englishGloss": "In [the]",
          "portugueseGloss": "Em",
          "spanishGloss": "En",
          "language": "greek",
          "kjvAlignment": {
            "wordPosition": 1,
            "kjvIndices": [0],
            "alignedText": "No",
            "isDivergent": false,
            "confidence": 93
          }
        }
      ],
      "kjvText": "No princpio, era o Verbo, e o Verbo estava com Deus, e o Verbo era Deus."
    }
  ]
}
```

### Fluxo no BibleService

```
getInterlinearChapter(bookName, chapter, alignVersion)
    │
    ├─► interlinearRepository.getWordsForChapter(bookId, chapter)
    │     → Map<verseNum, List<InterlinearWordDTO>>
    │
    ├─► interlinearRepository.getAlignmentsForChapter(bookId, chapter, alignVersion)
    │     → Map<Pair(verseNum, wordPos), WordAlignmentDTO>
    │
    ├─► verseRepository.getChapterTexts(versionId, bookId, chapter)
    │     → Map<verseNum, text>
    │
    └─► Enriquecer cada word com seu alignment (se existir)
          word.copy(kjvAlignment = alignmentMap[Pair(verseNum, wordPos)])
```

### Outros Endpoints


| Endpoint                                                 | Descricao                                         |
| -------------------------------------------------------- | ------------------------------------------------- |
| `GET /bible/strongs/{number}`                            | Concordancia: todos os versiculos com esse Strong |
| `GET /bible/lexicon/{number}?locale=pt`                  | Entrada do lexico com traducao                    |
| `GET /bible/search?q=amor&locale=pt`                     | Busca full-text                                   |
| `GET /bible/compare/{book}/{chapter}?versions=KJV,ARC69` | Comparacao paralela                               |


---

## 9. Frontend — Renderizacao e Interacao

### Hierarquia de Componentes

```
BiblePage (page.tsx)
    │
    ├── StudySidebar
    │   ├── Toggle "Modo Interlinear" (Ctrl+I)
    │   └── Seletor de versao de alinhamento (KJV / ARC69)
    │
    └── LinkedVerseReader
        │
        ├── Texto do versiculo (versao primaria)
        │   ├── KjvTextWithHighlight()  ← se tem alinhamento
        │   └── LinkedText()            ← fallback sem alinhamento
        │
        └── Cards interlineares (se modo interlinear ativo)
            └── Para cada palavra grega:
                ┌──────────────┐
                │  Ἐν          │  ← original_word (amarelo)
                │  en          │  ← transliteration (cinza)
                │  Em          │  ← gloss PT (azul)
                │  G1722       │  ← strongs_number (link)
                │  No          │  ← aligned_text (se divergente)
                └──────────────┘
```

### Selecao do Gloss por Idioma

O gloss exibido depende da **versao de alinhamento**, nao do locale da UI:

```typescript
// page.tsx — Logica de selecao de gloss
const alignLang = allVersions.find(v => v.code === alignVersion)?.language ?? "en";

// Se alinhamento = ARC69 (language: "pt"):
//   portugueseGloss → substitui englishGloss para exibicao
// Se alinhamento = KJV (language: "en"):
//   englishGloss usado diretamente

const words = alignLang === "en" ? v.words : v.words.map(w => {
  const localGloss = alignLang === "pt" ? w.portugueseGloss
    : alignLang === "es" ? w.spanishGloss : null;
  return localGloss ? { ...w, englishGloss: localGloss } : w;
});
```

### Highlighting Bidirecional

Quando o usuario interage com uma palavra:

```
Hover no card grego (Ἐν, pos=1)
    │
    ├─► setHoveredGreekPos(1)
    │
    ├─► No texto ARC69: destaca todas as palavras em kjvIndices do word pos=1
    │   Ex: kjvIndices=[0] → destaca "No" (indice 0 do texto ARC69)
    │
    └─► Borda azul no card grego

Hover na palavra do texto ARC69 ("No", idx=0)
    │
    ├─► setHoveredKjvIdx(0)
    │
    ├─► Busca reversa: quais words gregas tem idx 0 nos kjvIndices?
    │   → Encontra word pos=1 (Ἐν)
    │
    ├─► Destaca TODOS os indices desses words gregos (grupo de expressao)
    │
    └─► Destaca o card grego correspondente
```

### Cores de Confianca


| Confianca | Cor da borda          | Background        |
| --------- | --------------------- | ----------------- |
| >= 80     | `border-blue-500/30`  | `bg-blue-500/5`   |
| 60-79     | `border-blue-400/20`  | `bg-blue-400/5`   |
| < 60      | `border-amber-500/40` | `bg-amber-500/10` |
| Hover     | `border-primary`      | `bg-primary/15`   |


---

## 10. Fluxo Completo: Do TAGNT a Tela

```
                         INGESTAO (Backend)
                         ═══════════════════

  ┌──────────┐     ┌──────────────────┐     ┌───────────────────┐
  │  TAGNT   │────►│  parseTAGNT      │────►│ interlinear_words │
  │  (TSV)   │     │  AndInsert()     │     │ (english_gloss)   │
  └──────────┘     └──────────────────┘     └────────┬──────────┘
                                                      │
                                            ┌─────────▼──────────┐
                                            │  translateGlosses() │
                                            │  via LLM batch     │
                                            └─────────┬──────────┘
                                                      │
                                            ┌─────────▼──────────┐
                                            │ interlinear_words   │
                                            │ (portuguese_gloss)  │
                                            │ (spanish_gloss)     │
                                            └─────────┬──────────┘
                                                      │
  ┌──────────┐     ┌──────────────────┐               │
  │ ARC69    │────►│ bible_verse_texts │               │
  │ (texto)  │     └───────┬──────────┘               │
  └──────────┘             │                           │
                           │     ┌─────────────────────┘
                           │     │
                    ┌──────▼─────▼──────┐
                    │  alignVerse()     │
                    │  via LLM         │
                    │  (grego → ARC69) │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │ word_alignments   │
                    │ (kjv_indices,     │
                    │  aligned_text,    │
                    │  confidence)      │
                    └────────┬─────────┘
                             │
                         SERVINDO (API)
                         ══════════════

                    ┌────────▼─────────┐
                    │  BibleService    │
                    │  .getInterlinear │
                    │   Chapter()      │
                    └────────┬─────────┘
                             │
                    ┌────────▼──────────────────┐
                    │  InterlinearChapterDTO     │
                    │  ├─ verses[]               │
                    │  │  ├─ words[]             │
                    │  │  │  ├─ originalWord     │
                    │  │  │  ├─ englishGloss     │
                    │  │  │  ├─ portugueseGloss  │
                    │  │  │  ├─ strongsNumber    │
                    │  │  │  └─ kjvAlignment     │
                    │  │  │     ├─ kjvIndices    │
                    │  │  │     ├─ alignedText   │
                    │  │  │     └─ confidence    │
                    │  │  └─ kjvText             │
                    └────────┬──────────────────┘
                             │
                        RENDERIZANDO (Frontend)
                        ═══════════════════════

                    ┌────────▼─────────┐
                    │  page.tsx         │
                    │  Seleciona gloss  │
                    │  por idioma       │
                    └────────┬─────────┘
                             │
                    ┌────────▼──────────────────┐
                    │  LinkedVerseReader         │
                    │  ├─ Texto com highlighting │
                    │  └─ Cards interlineares    │
                    │     ├─ Palavra grega       │
                    │     ├─ Transliteracao      │
                    │     ├─ Gloss (PT/EN)       │
                    │     ├─ Strong's (link)     │
                    │     └─ Confianca (cor)     │
                    └───────────────────────────┘
```

---

## 11. Historico de Bugs de Glosses

### 11.1 Bug de Preambulo Deslocado (Fix 2026-03-inicial)

**Sintoma:** Glosses PT deslocados em 1 posicao quando LLM adicionava preambulo.

**Causa:** Mapeamento posicional nao compensava a remocao do preambulo.

**Correcao:** Two-pass filtering — Pass 1 filtra meta-texto, Pass 2 mapeia posicionalmente
sobre as linhas limpas.

### 11.2 Bug de Gap-Filling com JSON (Fix 2026-04)

**Sintoma:** Glosses PT continham fragmentos JSON literais como `"[thing]": "[coisa]",`
ao inves de traducoes limpas. Afetou Joao 1 (50% corrompido), Joao 2 (23%), Joao 3 (<1%).
Glosses em espanhol e ingles estavam corretos.

| Grego | Strong | Gloss exibido (corrompido)       | Gloss correto |
| ----- | ------ | -------------------------------- | ------------- |
| Ἐν    | G1722  | `"[thing]": "[coisa]",`          | Em            |
| ἀρχῇ  | G0746  | `"that": "que",`                 | principio     |
| ἦν    | G1510  | `"has": "tenha",`                | era           |
| ὁ     | G3588  | `"come": "vindo",`               | o             |
| λόγος | G3056  | `"into": "a",`                   | Palavra       |

**Causa Raiz:** Na funcao `processGlossResponse()`, quando `tryParseJsonGlosses()` fazia
match parcial (<80%) das chaves JSON, o fallback chamava `processGlossResponseLineBased(rawContent, missingGlosses)`.
O problema: o fallback recebia o `rawContent` COMPLETO (JSON inteiro) mas apenas os glosses
faltantes como chunk. O mapeamento posicional atribuia valores de TODAS as linhas JSON aos
glosses errados. Se a regex KV nao batia, a linha JSON inteira era armazenada como gloss.

```
Antes (bugado):
  tryParseJsonGlosses() → match parcial (ex: 30%)
  processGlossResponseLineBased(rawContent, missingGlosses)
    → extrai valores de TODAS as linhas JSON
    → mapeia posicionalmente para missingGlosses
    → RESULTADO: glosses errados ou fragmentos JSON armazenados

Depois (corrigido):
  tryParseJsonGlosses() → match parcial → retorna APENAS os matched
  Se conteudo parece JSON (contem { e }) → retorna mapa vazio
  Line-by-line fallback → SOMENTE para respostas plain-text
  Glosses faltantes ficam NULL → reprocessados no proximo run
```

**Arquivos modificados:**
- `backend/.../service/BibleIngestionService.kt` — `processGlossResponse()` (removido gap-filling)
- `backend/.../repository/InterlinearRepository.kt` — `clearCorruptedPortugueseGlosses()`
- `backend/.../routes/AdminRoutes.kt` — `POST /admin/bible/glosses/fix-corrupted`

**Testes adicionados:** 4 novos em `BibleIngestionServiceGlossTest.kt`:
- JSON com chaves incompativeis → mapa vazio
- Match parcial → retorna apenas matched, sem gap-filling
- Conteudo JSON-like com parse failure → nao cai em line-by-line
- Regressao: plain-text continua funcionando

### 11.3 Limitacoes Conhecidas dos Glosses PT

Validacao de Joao 2 (437 palavras) revelou problemas sistematicos de concordancia,
nao erros semanticos (ver `docs/validation-john2-glosses.md`):

- **Numero singular/plural** (12 ocorrencias): LLM traduz "believed" → "acreditou" (sg)
  quando grego e plural "acreditaram". Causa: ingles nao marca numero nos verbos.
- **Genero do artigo** (5 ocorrencias): `hoi` (masc. plural) → "a" (fem. singular)
  ao inves de "os". Causa: "the" em ingles nao carrega genero.
- **2 erros semanticos** em v24-25: `ginoskein` (verbo) → "conhecimento" (subst.);
  `tis` ("alguem") → "ninguem" (oposto).

**Melhoria futura:** Incluir morfologia grega no prompt de traducao de glosses.

---

## 12. Re-ingestao e Limpeza

### Limpeza direcionada (somente PT corrompidos)

```bash
POST /admin/bible/glosses/fix-corrupted
# Limpa apenas portugueseGloss que contem fragmentos JSON (padrao ": ")
# Nao afeta spanishGloss nem word_alignments
# Glosses limpos ficam NULL → reprocessados pela fase bible_translate_glosses
```

### Limpeza completa (PT + ES + alinhamentos)

Via UI: `/ingestion-status` → aba Biblia → Camada 2 → "Limpar Glosses e Alinhamentos"

```
POST /admin/bible/glosses/clear
└─ Limpa: portuguese_gloss, spanish_gloss = NULL
└─ Limpa: TODOS os word_alignments
└─ Reseta fases de traducao e alinhamento
```

### Re-ingestao

```
1. Executar limpeza (direcionada ou completa)

2. Camada 2 → Clicar ▶ em "Traduzir Glosses (IA)"
   └─ Re-traduz glosses EN → PT/ES com codigo corrigido

3. Camada 4 → Clicar ▶ em "Alinhar Grego → KJV (IA)"
   └─ Re-alinha grego com KJV (ingles)

4. Camada 4 → Clicar ▶ em "Alinhar Grego → ARC69 (IA)"
   └─ Re-alinha grego com ARC69 (portugues)
```

### Verificacao

Apos a re-ingestao, abrir `/pt/bible` → Joao 1 → Modo Interlinear → ARC69:

- Cada gloss PT deve corresponder semanticamente a palavra grega
- Nenhum gloss deve conter `": "` (fragmentos JSON)
- Clicar em λόγος deve destacar "Verbo" no texto
- Clicar em θεόν deve destacar "Deus" no texto

---

## Arquivos de Referencia


| Arquivo                                           | Responsabilidade                             |
| ------------------------------------------------- | -------------------------------------------- |
| `backend/.../service/BibleIngestionService.kt`    | Ingestao TAGNT, traducao de glosses          |
| `backend/.../service/WordAlignmentService.kt`     | Alinhamento grego → traducao (LLM)           |
| `backend/.../service/BibleService.kt`             | Servico API: merge interlinear + alinhamento |
| `backend/.../repository/InterlinearRepository.kt` | CRUD interlinear_words + word_alignments     |
| `backend/.../model/BibleTables.kt`                | Schema do banco (Exposed)                    |
| `backend/.../model/BibleDTOs.kt`                  | DTOs da API                                  |
| `frontend/.../bible/page.tsx`                     | Pagina principal: fetch + selecao de gloss   |
| `frontend/.../bible/LinkedVerseReader.tsx`        | Renderizacao interlinear + highlighting      |
| `frontend/.../bible/StudySidebar.tsx`             | Seletor de versao de alinhamento             |
| `frontend/hooks/useBible.ts`                      | React Query hooks para API                   |
| `frontend/types/bible.ts`                         | Tipos TypeScript (espelho dos DTOs)          |


