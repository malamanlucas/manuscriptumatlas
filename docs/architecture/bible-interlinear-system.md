# Sistema Interlinear Bíblico — Plano de Implementação

> **Status:** Aguardando aprovação | **Versão:** 2026-04-12

---

## Contexto — O Problema

O projeto já possui um pipeline de ingestão bíblica em 4 camadas e rotas de sidebar apontando para `/bible`, `/bible/interlinear`, `/bible/compare`, `/bible/search`. Contudo a sincronização entre as palavras portuguesas da ARC69 e os `gloss_pt` do léxico grego está semanticamente quebrada:

- `gloss_pt` é um **significado lexical** (o que a palavra grega quer dizer em abstrato)
- ARC69 é uma **tradução contextual dinâmica**
- O sistema atual trata os dois como se fossem a mesma coisa

O resultado: o interlinear não sabe quando a tradução da ARC69 "diverge" do gloss, nem exibe as duas camadas separadamente.

**Causa raiz complementar:** ARC69 está armazenado apenas em nível de versículo (`verse_texts`), impedindo alinhamento preciso no nível do token (palavra). Sem tokenização do português, qualquer alinhamento Grego→ARC69 é posicional/string e não pode ser indexado com confiança.

---

## O Que Muda — Diagrama Geral

```
ANTES (problema)
────────────────────────────────────────────────────
greek_word.gloss_pt ──── confundido com ──── ARC69 token
         ↑ do léxico Strong's (sentido lexical)    ↑ tradução dinâmica

DEPOIS (corrigido)
────────────────────────────────────────────────────
                         ┌─── gloss_pt  (léxico)
greek_word ──────────────┤
                         └─── aligned_arc69_text (alinhamento)
                                    ↑
                         verse_tokens.token (ARC69 tokenizado)

Frontend mostra as duas camadas + sinaliza divergência
```

---

## Modelo de Dados Revisado

### Tabela Nova: `bible_verse_tokens`

```sql
bible_verse_tokens
  id            BIGSERIAL PK
  verse_text_id BIGINT FK → verse_texts.id
  version_id    BIGINT FK → bible_versions.id
  position      INT            -- ordem da palavra no versículo
  token         TEXT           -- palavra limpa (sem pontuação)
  token_raw     TEXT           -- forma original com pontuação
  UNIQUE (verse_text_id, position)
```

### Tabela Alterada: `word_alignments`

```sql
word_alignments
  id                 BIGSERIAL PK
  greek_word_id      BIGINT FK → greek_words.id
  version_id         BIGINT FK → bible_versions.id
  token_positions    INT[]          -- posições em bible_verse_tokens
  aligned_text       TEXT           -- texto resultante (desnormalizado p/ leitura rápida)
  confidence         FLOAT          -- 0.0–1.0
  method             TEXT           -- 'exact', 'stemmed', 'ai', 'manual'
  gloss_match        BOOLEAN        -- gloss_pt ≈ aligned_text?
  UNIQUE (greek_word_id, version_id)
```

> **Regra fundamental:**
> - `greek_lexicon.gloss_pt` — significado lexical (permanece como está)
> - `word_alignments.aligned_text` — token real da ARC69 (coisa distinta)

---

## Pipeline de Ingestão Revisado (5 Camadas)

### Camada 1 — Estrutura (idempotente, rodar 1×)
```
bible_seed_versions        → bible_versions
bible_seed_books           → bible_books
bible_seed_abbreviations   → bible_abbreviations
```

### Camada 2 — Interlinear Grego + Léxico
```
bible_ingest_nt_interlinear      → greek_words (NT)
bible_ingest_ot_interlinear      → hebrew_words (AT)
bible_ingest_greek_lexicon       → greek_lexicon (gloss_en, def_en)
bible_ingest_hebrew_lexicon      → hebrew_lexicon
bible_translate_lexicon          → greek_lexicon.gloss_pt, gloss_es  [LLM]
bible_translate_glosses          → greek_words.gloss_pt              [LLM]
bible_enrich_greek_lexicon       → greek_lexicon campos extras (BibleHub)
bible_translate_enrichment_greek → tradução do enrichment            [LLM]
```

### Camada 3 — Textos Bíblicos
```
bible_ingest_text_kjv   → verse_texts (KJV)
bible_ingest_text_aa    → verse_texts (AA)
bible_ingest_text_acf   → verse_texts (ACF)
bible_ingest_text_arc69 → verse_texts (ARC69) [Scraper]
```

### Camada 4 — Tokenização ⭐ NOVA
```
bible_tokenize_arc69  → bible_verse_tokens (ARC69 word-level)
bible_tokenize_kjv    → bible_verse_tokens (KJV word-level)
```
- Tokenizador: split por espaço + normalização (strip pontuação)
- Idempotente via `(verse_text_id, position) UNIQUE`

### Camada 5 — Alinhamento
```
bible_align_arc69  → word_alignments (Grego → ARC69)
bible_align_kjv    → word_alignments (Grego → KJV)
```

**Algoritmo em 3 níveis (por ordem de confiança):**
1. **Exact match:** `gloss_pt == token ARC69` (ignora case/acento) → `method='exact'`, `confidence=1.0`
2. **Stem match:** radical de `gloss_pt` ≈ radical do token ARC69 → `method='stemmed'`, `confidence=0.85`
3. **AI alignment:** LLM alinha posicionalmente → `method='ai'`, `confidence=score_llm`

```
gloss_match = (Levenshtein(gloss_pt, aligned_text) / max_len) < 0.3
```

### Dependências entre Camadas
```
Camada 1
    │
    ├── Camada 2 (grego/léxico independente)
    │
    └── Camada 3 (textos bíblicos)
              │
              └── Camada 4 (tokenização — depende de Camada 3)
                        │
                        └── Camada 5 (alinhamento — depende de Camadas 2+4)
```

---

## AULA: O que é Tokenização e Por que Ela Existe

### Analogia do Dicionário

Imagine que você tem um dicionário de grego→português lado a lado com a Bíblia em português. Para cada palavra grega (por exemplo, γάμος), o dicionário diz que ela significa "casamento". Mas quando você abre a Bíblia ARC69 em João 2:1, ela usa a palavra "bodas" — não "casamento". Sem um sistema inteligente, o computador não consegue:

1. Saber que "bodas" na ARC69 corresponde a γάμος no grego
2. Saber **onde está** a palavra "bodas" dentro da frase para destacá-la na tela
3. Perceber que existe uma **divergência** entre o que o dicionário diz ("casamento") e o que a Bíblia usou ("bodas")

### O Problema do Texto em Bloco

Sem tokenização, o ARC69 de João 2:1 é uma string monolítica:

```
"E ao terceiro dia fizeram-se bodas em Caná da Galileia, e estava ali a mãe de Jesus."
```

O computador não sabe que essa string tem 17 palavras, que "bodas" é a 6ª palavra, ou que "mãe" é a 15ª.

### A Solução: Tokenizar = Numerar Cada Palavra

```
Texto original:
"E ao terceiro dia fizeram-se bodas em Caná da Galileia, e estava ali a mãe de Jesus."

Após tokenização:
 [0]=E  [1]=ao  [2]=terceiro  [3]=dia  [4]=fizeram-se  [5]=bodas
 [6]=em  [7]=Caná  [8]=da  [9]=Galileia  [10]=e  [11]=estava
 [12]=ali  [13]=a  [14]=mãe  [15]=de  [16]=Jesus
```

Agora quando o sistema faz a ligação γάμος → posição [5], o frontend sabe exatamente qual palavra destacar ("bodas") e onde ela está.

### Dois Glossários Distintos que NUNCA devem ser confundidos

| | `gloss_pt` (léxico) | `aligned_text` (alinhamento) |
|---|---|---|
| **O que é** | Significado do dicionário grego | Palavra real usada na ARC69 |
| **Exemplo** | "casamento" (G1062) | "bodas" |
| **Muda entre versículos?** | Não — é fixo no léxico | Sim — depende do contexto |
| **Fonte** | `greek_lexicon.gloss_pt` | `word_alignments.aligned_text` |
| **Cor no frontend** | Verde se coincide com ARC69 | Laranja se diverge do gloss |

### O que é Levenshtein Distance

`gloss_match = levenshteinDistance(gloss_pt, aligned_text) / max(len) < 0.3`

É o número mínimo de operações (inserir, deletar, substituir uma letra) para transformar uma string em outra.

```
"casamento" → "bodas"
  distância = 14, max(9,5) = 9
  ratio = 14/9 = 1.55 → > 0.3 → gloss_match = false ✗

"dia" → "dia"
  distância = 0
  ratio = 0/3 = 0.0 → < 0.3 → gloss_match = true ✓

"disse" → "disse-lhe"
  distância = 4 (inserir "-lhe")
  ratio = 4/9 = 0.44 → > 0.3 → gloss_match = false ✗
```

---

## Por que a Camada 4 é Necessária — Exemplo João 2:1

### Texto Grego (18 palavras posicionadas)

```
pos=0  Καὶ       G2532  "e/também"
pos=1  τῇ        G3588  "o/a" (artigo)
pos=2  ἡμέρᾳ     G2250  "dia"
pos=3  τῇ        G3588  "o/a" (artigo)
pos=4  τρίτῃ     G5154  "terceiro"
pos=5  γάμος     G1062  "casamento"
pos=6  ἐγένετο   G1096  "aconteceu/foi"
pos=7  ἐν        G1722  "em"
pos=8  Κανὰ      G2580  "Caná"
pos=9  τῆς       G3588  "o/a" (artigo gen.)
pos=10 Γαλιλαίας G1056  "Galileia"
pos=11 καὶ       G2532  "e"
pos=12 ἦν        G1510  "era/estava"
pos=13 ἡ         G3588  "a" (artigo)
pos=14 μήτηρ     G3384  "mãe"
pos=15 τοῦ       G3588  "o/a" (artigo gen.)
pos=16 Ἰησοῦ    G2424  "Jesus"
pos=17 ἐκεῖ      G1563  "ali/lá"
```

### Tokens ARC69 (Camada 4 produz)

```
pos | token        | token_raw
----+--------------+-----------
 0  | E            | E
 1  | ao           | ao
 2  | terceiro     | terceiro
 3  | dia          | dia
 4  | fizeram-se   | fizeram-se
 5  | bodas        | bodas
 6  | em           | em
 7  | Caná         | Caná
 8  | da           | da
 9  | Galileia     | Galileia,   ← token_raw preserva vírgula
10  | e            | e
11  | estava       | estava
12  | ali          | ali
13  | a            | a
14  | mãe          | mãe
15  | de           | de
16  | Jesus        | Jesus.      ← token_raw preserva ponto
```

### Alinhamentos (Camada 5 produz)

```
greek_word (γάμος, pos=5)  →  token_positions=[5]   aligned_text="bodas"
greek_word (ἐγένετο,pos=6) →  token_positions=[4]   aligned_text="fizeram-se"
greek_word (τῇ, pos=1)     →  token_positions=[1]   aligned_text="ao"
greek_word (τῇ, pos=3)     →  token_positions=[]    aligned_text=null  ← artigo absorvido
greek_word (τοῦ, pos=15)   →  token_positions=[15]  aligned_text="de"
```

---

## Mapeamento Completo: João 2:1–5

> Legenda: ✓=glossMatch (verde), ✗=divergente (laranja), —=absorvido (sem destaque)

### João 2:1
ARC69: *"E ao terceiro dia fizeram-se bodas em Caná da Galileia, e estava ali a mãe de Jesus."*

| gr_pos | Grego | Strong's | gloss_pt | token_positions | Texto ARC69 | aligned_text | Match | Método |
|--------|-------|----------|----------|-----------------|-------------|--------------|-------|--------|
| 0 | Καὶ | G2532 | e | [0] | E | E | ✓ | exact |
| 1 | τῇ | G3588 | o/a | [1] | ao | ao | ✗ | ai (artigo+prep fundidos) |
| 2 | ἡμέρᾳ | G2250 | dia | [3] | dia | dia | ✓ | exact |
| 3 | τῇ | G3588 | o/a | [] | — | — | — | absorvido |
| 4 | τρίτῃ | G5154 | terceiro | [2] | terceiro | terceiro | ✓ | exact |
| 5 | γάμος | G1062 | casamento | [5] | bodas | bodas | ✗ | ai |
| 6 | ἐγένετο | G1096 | aconteceu | [4] | fizeram-se | fizeram-se | ✗ | ai |
| 7 | ἐν | G1722 | em | [6] | em | em | ✓ | exact |
| 8 | Κανὰ | G2580 | Caná | [7] | Caná | Caná | ✓ | exact |
| 9 | τῆς | G3588 | o/a | [8] | da | da | ✗ | ai (artigo→contração) |
| 10 | Γαλιλαίας | G1056 | Galileia | [9] | Galileia, | Galileia | ✓ | exact |
| 11 | καὶ | G2532 | e | [10] | e | e | ✓ | exact |
| 12 | ἦν | G1510 | era/estava | [11] | estava | estava | ✗ | stem (era≈estava) |
| 13 | ἡ | G3588 | a | [13] | a | a | ✓ | exact |
| 14 | μήτηρ | G3384 | mãe | [14] | mãe | mãe | ✓ | exact |
| 15 | τοῦ | G3588 | o/a | [15] | de | de | ✗ | ai (artigo→prep) |
| 16 | Ἰησοῦ | G2424 | Jesus | [16] | Jesus. | Jesus | ✓ | exact |
| 17 | ἐκεῖ | G1563 | ali/lá | [12] | ali | ali | ✓ | stem |

> **Nota:** O artigo grego τῇ (pos=1) foi fundido com "ao" (contração de "a"+"o" em PT). Caso N:1 grego→português.

### João 2:2
ARC69: *"E Jesus também foi convidado com os seus discípulos para as bodas."*

| gr_pos | Grego | Strong's | gloss_pt | token_positions | aligned_text | Match | Método |
|--------|-------|----------|----------|-----------------|--------------|-------|--------|
| 0 | ἐκλήθη | G2564 | chamado | [3,4] | foi convidado | ✗ | ai (multi-token) |
| 1 | δὲ | G1161 | mas/e | [0] | E | ✗ | ai |
| 2 | καὶ | G2532 | e/também | [2] | também | ✗ | ai (partícula enfática) |
| 3 | ὁ | G3588 | o | [] | — | — | absorvido |
| 4 | Ἰησοῦς | G2424 | Jesus | [1] | Jesus | ✓ | exact |
| 5 | καὶ | G2532 | e | [5] | com | ✗ | ai (καὶ→"com" contextual) |
| 6 | οἱ | G3588 | os | [6] | os | ✓ | exact |
| 7 | μαθηταὶ | G3101 | discípulos | [8] | discípulos | ✓ | exact |
| 8 | αὐτοῦ | G846 | dele/seu | [7] | seus | ✗ | stem |
| 9 | εἰς | G1519 | para/em | [9] | para | ✓ | stem |
| 10 | τὸν | G3588 | o | [10] | as | ✗ | ai (gênero diferente) |
| 11 | γάμον | G1062 | casamento | [11] | bodas | ✗ | ai |

> **Nota:** ἐκλήθη (foi chamado) gerou dois tokens [3,4] = "foi convidado" — 1:N grego→português.

### João 2:3
ARC69: *"E, faltando o vinho, a mãe de Jesus disse-lhe: Eles não têm vinho."*

> ⚠️ Texto parcialmente verificado (confirmado: "faltando").

| gr_pos | Grego | Strong's | gloss_pt | token_positions | aligned_text | Match | Método |
|--------|-------|----------|----------|-----------------|--------------|-------|--------|
| 0 | καὶ | G2532 | e | [0] | E | ✓ | exact |
| 1 | ὑστερήσαντος | G5302 | faltou | [1] | faltando | ✗ | stem |
| 2 | οἴνου | G3631 | vinho | [3] | vinho | ✓ | exact |
| 3 | λέγει | G3004 | diz/disse | [8] | disse-lhe | ✗ | stem ("disse"⊂"disse-lhe") |
| 4 | ἡ | G3588 | a | [4] | a | ✓ | exact |
| 5 | μήτηρ | G3384 | mãe | [5] | mãe | ✓ | exact |
| 6 | τοῦ | G3588 | o/a | [6] | de | ✗ | ai |
| 7 | Ἰησοῦ | G2424 | Jesus | [7] | Jesus | ✓ | exact |
| 8 | πρὸς | G4314 | para/a | [] | — | — | absorvido (fundido em "disse-lhe") |
| 9 | αὐτόν | G846 | ele/lhe | [] | — | — | absorvido (fundido em "disse-lhe") |
| 10 | Οἶνον | G3631 | vinho | [12] | vinho | ✓ | exact |
| 11 | οὐκ | G3756 | não | [10] | não | ✓ | exact |
| 12 | ἔχουσιν | G2192 | têm | [9,11] | Eles têm | ✗ | ai (sujeito explicitado em PT) |

> **Nota:** πρὸς αὐτόν ("a ele") foi absorvido pelo clítico "-lhe" em "disse-lhe".

### João 2:4
ARC69: *"Jesus disse-lhe: Mulher, que tenho eu contigo? ainda não é chegada a minha hora."*

| gr_pos | Grego | Strong's | gloss_pt | token_positions | aligned_text | Match | Método |
|--------|-------|----------|----------|-----------------|--------------|-------|--------|
| 0 | λέγει | G3004 | diz/disse | [1] | disse-lhe | ✗ | stem |
| 1 | αὐτῇ | G846 | ela/lhe | [] | — | — | absorvido em "disse-lhe" |
| 2 | ὁ | G3588 | o | [] | — | — | absorvido |
| 3 | Ἰησοῦς | G2424 | Jesus | [0] | Jesus | ✓ | exact |
| 4 | Τί | G5101 | que/o quê | [3] | que | ✓ | exact |
| 5 | ἐμοὶ | G1473 | eu/mim | [4,5] | tenho eu | ✗ | ai (idioma: Τί ἐμοὶ καὶ σοί) |
| 6 | καὶ | G2532 | e | [] | — | — | absorvido (parte do idioma) |
| 7 | σοί | G4771 | você/te | [6] | contigo | ✗ | ai |
| 8 | γύναι | G1135 | mulher | [2] | Mulher | ✓ | exact |
| 9 | οὔπω | G3768 | ainda não | [7,8] | ainda não | ✓ | stem |
| 10 | ἥκει | G2240 | chegou | [9,10] | é chegada | ✗ | ai (perífrase) |
| 11 | ἡ | G3588 | a | [11] | a | ✓ | exact |
| 12 | ὥρα | G5610 | hora | [13] | hora | ✓ | exact |
| 13 | μου | G1473 | meu/minha | [12] | minha | ✓ | stem |

> **Nota:** Τί ἐμοὶ καὶ σοί é um idioma semítico. Apenas AI alignment consegue resolver.

### João 2:5
ARC69: *"Disse a mãe aos servos: Fazei tudo o que ele vos disser."*

| gr_pos | Grego | Strong's | gloss_pt | token_positions | aligned_text | Match | Método |
|--------|-------|----------|----------|-----------------|--------------|-------|--------|
| 0 | λέγει | G3004 | diz/disse | [0] | Disse | ✗ | stem |
| 1 | ἡ | G3588 | a | [1] | a | ✓ | exact |
| 2 | μήτηρ | G3384 | mãe | [2] | mãe | ✓ | exact |
| 3 | αὐτοῦ | G846 | dele/seu | [] | — | — | absorvido (poss. omitido em PT) |
| 4 | τοῖς | G3588 | os/aos | [3] | aos | ✗ | ai (artigo→contração) |
| 5 | διακόνοις | G1249 | servos | [4] | servos | ✓ | stem |
| 6 | Ὅ | G3739 | o que | [7,8] | o que | ✓ | exact |
| 7 | τι | G5100 | algum | [] | — | — | absorvido (Ὅ τι = pronome composto) |
| 8 | ἂν | G302 | se/que | [] | — | — | absorvido (partícula modal) |
| 9 | λέγῃ | G3004 | disser | [11] | disser | ✓ | stem |
| 10 | ὑμῖν | G4771 | a vós/vos | [10] | vos | ✓ | stem |
| 11 | ποιήσατε | G4160 | fazei | [5] | Fazei | ✓ | exact |

---

## Exemplos João 3:16–21 — Os Versículos Mais Complexos

### João 3:16
ARC69 (ARC1969): *"Porque Deus amou o mundo de tal maneira que deu o seu Filho unigênito, para que todo aquele que nele crê não pereça, mas tenha a vida eterna."*

```
Tokens ARC69 (aprox.):
[0]=Porque  [1]=Deus  [2]=amou  [3]=o  [4]=mundo  [5]=de
[6]=tal  [7]=maneira  [8]=que  [9]=deu  [10]=o  [11]=seu
[12]=Filho  [13]=unigênito  [14]=para  [15]=que  [16]=todo
[17]=aquele  [18]=que  [19]=nele  [20]=crê  [21]=não
[22]=pereça  [23]=mas  [24]=tenha  [25]=a  [26]=vida  [27]=eterna
```

**Palavras gregas-chave e seus alinhamentos:**

| Grego | Strong's | gloss_pt | aligned_text | glossMatch | Por quê |
|-------|----------|----------|--------------|------------|---------|
| Οὕτως | G3779 | assim/de tal modo | de tal maneira | ✗ | "assim" ≠ "de tal maneira" — AI alignment |
| ἠγάπησεν | G25 | amou | amou | ✓ | exact match |
| ὁ θεός | G2316 | Deus | Deus | ✓ | exact match |
| τὸν κόσμον | G2889 | mundo | mundo | ✓ | exact match |
| ὥστε | G5620 | de modo que | que | ✗ | partícula absorvida na construção PT |
| τὸν υἱόν | G5207 | filho | Filho | ✓ | exact (case-insensitive) |
| τὸν μονογενῆ | G3439 | unigênito | unigênito | ✓ | exact match |
| ἔδωκεν | G1325 | deu | deu | ✓ | exact match |
| πιστεύων | G4100 | crendo/crê | crê | ✗ | stem match (crer→crê) |
| εἰς αὐτόν | G1519+G846 | em ele | nele | ✗ | AI: "em ele" fundido em "nele" |
| ἀπόληται | G622 | pereça | pereça | ✓ | exact match |
| ζωὴν αἰώνιον | G2222+G166 | vida eterna | vida eterna | ✓ | exact match (2 tokens) |

**Caso especial — ἠγάπησεν (G25):**

Este é o versículo de controle do sistema. "amou" é exact match com gloss_pt "amou" — aparece **verde** no frontend.

```
┌─────────────────┐
│  ἠγάπησεν       │
│  ēgapēsen       │
│  G25            │
│─── gloss ───────│
│  amou           │  ← gloss_pt do léxico
│─── ARC69 ───────│
│ 🟢 amou         │  ← aligned_text
└─────────────────┘
  glossMatch=true
```

**Caso especial — Οὕτως (G3779):**

"assim" (gloss) vs "de tal maneira" (ARC69). Levenshtein alto → AI alignment.

```
┌─────────────────┐
│  Οὕτως          │
│  houtōs         │
│  G3779          │
│─── gloss ───────│
│  assim          │  ← gloss_pt
│─── ARC69 ───────│
│ 🟠 de tal       │  ← aligned_text (multi-token)
│    maneira      │
└─────────────────┘
  glossMatch=false
  tokenPositions=[5,6,7]
```

### João 3:17
ARC69: *"Porque Deus enviou o seu Filho ao mundo, não para que condenasse o mundo, mas para que o mundo fosse salvo por ele."*

**Palavras-chave:**

| Grego | Strong's | gloss_pt | aligned_text | Match | Método |
|-------|----------|----------|--------------|-------|--------|
| ἀπέστειλεν | G649 | enviou | enviou | ✓ | exact |
| κρίνῃ | G2919 | julgue/condene | condenasse | ✗ | AI (krinō = julgar, ARC usa "condenar") |
| σωθῇ | G4982 | seja salvo | fosse salvo | ✗ | stem + AI (passiva perifrástica) |
| δι᾽ αὐτοῦ | G1223+G846 | por ele | por ele | ✓ | exact (2 tokens) |

> **Divergência teológica detectada:** κρίνῃ tem gloss_pt "julgue" mas ARC69 usa "condenasse". O sistema mostra isso como laranja — permite ao estudante ver a escolha tradutória.

### João 3:18
ARC69: *"Quem crê nele não é condenado; mas quem não crê já está condenado, porquanto não crê no nome do Filho unigênito de Deus."*

**Caso especial — κέκριται (perfeito passivo):**

```
κέκριται = G2919 (krinō, perfeito passivo, 3ª sg.)
gloss_pt = "foi julgado/condenado"
ARC69 token = "condenado"
method = 'stemmed'  (condena- ≈ condena-)
glossMatch = false  ("foi julgado/condenado" vs "condenado")
```

O português precisou de só um token onde o grego usa o perfeito para enfatizar estado resultante.

### João 3:19
ARC69: *"E é este o julgamento: que a luz veio ao mundo, e os homens amaram mais as trevas do que a luz, porque as suas obras eram más."*

**Caso especial — κρίσις (G2920):**

```
κρίσις = G2920
gloss_pt = "julgamento"
ARC69 token = "julgamento"  ← exact match ✓
glossMatch = true
```

**Caso especial — ἠγάπησαν (G25) — mesma raiz de Jo 3:16!**

```
ἠγάπησαν = G25 (amar, aoristo 3ª pl.)
gloss_pt = "amaram"
ARC69 token = "amaram"  ← exact match ✓
glossMatch = true
```

O Strong's G25 aparece em Jo 3:16 como "amou" (sg.) e aqui como "amaram" (pl.) — mas é o mesmo Strong's G25 no léxico. O sistema mostra as duas ocorrências no painel lateral.

### João 3:20
ARC69: *"Porque todo aquele que pratica o mal odeia a luz, e não vem para a luz, para que as suas obras não sejam reprovadas."*

**Caso especial — φαῦλα (G5337) — "coisas vis/más":**

```
φαῦλα = G5337
gloss_pt = "coisas vis/más"
ARC69 token = "mal"
method = 'ai'  (sem match por texto ou radical)
confidence = 0.88
glossMatch = false
```

**Caso especial — ἐλεγχθῇ (G1651) — passiva + subjuntivo:**

```
ἐλεγχθῇ = G1651 (repreender/reprovar, passiva subjuntivo)
gloss_pt = "seja repreendido"
ARC69: "sejam reprovadas" [posições 14,15]
method = 'ai'
glossMatch = false
```

Aqui vemos: mesmo radical "reprova-/repreende-" mas formas diferentes + plural.

### João 3:21
ARC69: *"Mas quem pratica a verdade vem para a luz, para que as suas obras sejam manifestas, porque são feitas em Deus."*

**Caso especial — ποιῶν τὴν ἀλήθειαν:**

```
ποιῶν = G4160 (fazer, particípio)
gloss_pt = "fazendo/pratica"
ARC69 token = "pratica"
method = 'stemmed'  (prática- ≈ prática-)
glossMatch = false

τὴν ἀλήθειαν = G225 (verdade, acusativo)
gloss_pt = "verdade"
ARC69 token = "verdade"
method = 'exact'
glossMatch = true ✓
```

**Caso especial — φανερωθῇ (G5319) — "seja manifestada":**

```
φανερωθῇ = G5319
gloss_pt = "seja manifestada"
ARC69 token = "manifestas"
method = 'stemmed'  (manifest- ≈ manifest-)
confidence = 0.85
glossMatch = false
```

---

## Visualização do Frontend

### TokenCard (por palavra grega)

```
┌───────────────┐  ┌───────────────┐
│   γάμος       │  │  ἐγένετο      │
│   gamos       │  │  egeneto      │
│   G1062       │  │  G1096        │
│─── gloss ─────│  │─── gloss ─────│
│ casamento     │  │  aconteceu    │  ← gloss_pt do léxico
│─── ARC69 ─────│  │─── ARC69 ─────│
│ 🟠 bodas      │  │ 🟠 fizeram-se │  ← aligned_text
└───────────────┘  └───────────────┘
  glossMatch=false   glossMatch=false
```

```
┌───────────────┐  ┌───────────────┐
│   ἡμέρᾳ       │  │   μήτηρ       │
│   hemera      │  │   meter       │
│   G2250       │  │   G3384       │
│─── gloss ─────│  │─── gloss ─────│
│  dia          │  │   mãe         │  ← gloss_pt
│─── ARC69 ─────│  │─── ARC69 ─────│
│ 🟢 dia        │  │ 🟢 mãe        │  ← aligned_text
└───────────────┘  └───────────────┘
  glossMatch=true    glossMatch=true
```

### Hover: Destaque na Frase ARC69

```
João 2:1
─────────────────────────────────────────────────────────────────────
 E ao terceiro dia fizeram-se ╔══════╗ em Caná da Galileia, e estava
                               ║ bodas ║         ← destaque amarelo
                               ╚══════╝
 ali a mãe de Jesus.
─────────────────────────────────────────────────────────────────────
```

### Tooltip Flutuante

```
╭─────────────────────────────────────────╮
│  γάμος  (gamos)                         │
│  ─────────────────────────────────────  │
│  Strong's:  G1062                        │
│  Morph:     N-NSM (substantivo nom. sg.) │
│  ─────────────────────────────────────  │
│  Significado lexical:  casamento         │
│                                          │
│  Na ARC69 João 2:1:    bodas             │
│  ⚠ Tradução diverge do gloss             │
│                                          │
│  Confiança: 92%  │  Método: ai          │
│  ─────────────────────────────────────  │
│  [ Ver Strong's G1062 completo → ]       │
╰─────────────────────────────────────────╯
```

### Painel Lateral Strong's (ao clicar no número)

```
╔═══════════════════════════════════════════════╗
║  Strong's G1062  ×                            ║
╠═══════════════════════════════════════════════╣
║  γάμος  (gamos)                               ║
║  Substantivo masculino                        ║
║                                               ║
║  SIGNIFICADO                                  ║
║  Casamento, festa de casamento, bodas         ║
║                                               ║
║  DEFINIÇÃO COMPLETA                           ║
║  Originalmente a cerimônia de casamento       ║
║  e depois a festa que a acompanhava.          ║
║  No NT refere-se ao banquete nupcial.         ║
║                                               ║
║  OCORRÊNCIAS NO NT  (16×)                     ║
║  Mt 22:2 · Mt 22:3 · Mt 22:4 ...             ║
║  Jo 2:1 ← você está aqui                     ║
║  Jo 2:2 · Ap 19:7 ...                        ║
║                                               ║
║  NA ARC69 aparece como:                       ║
║  "bodas" (12×)  "casamento" (4×)              ║
╚═══════════════════════════════════════════════╝
```

### Lógica de Cores no TokenCard

```typescript
// glossMatch já vem do backend — frontend só aplica classe
const color = token.glossMatch
  ? "text-emerald-600"        // matchingGloss (verde)
  : token.confidence > 0.7
    ? "text-amber-500"        // divergentGloss com confiança razoável (laranja)
    : "text-muted-foreground"; // sem alinhamento / baixa confiança
```

---

## API Endpoints

### Interlinear por Capítulo (principal)

```
GET /bible/interlinear/{book}/{chapter}?version=ARC69
```

**Response (por versículo):**
```json
{
  "verse": 1,
  "greekText": "...",
  "arc69Text": "...",
  "tokens": [
    {
      "position": 0,
      "greekWord": "Ἐν",
      "lemma": "ἐν",
      "strongs": "G1722",
      "morphCode": "PREP",
      "transliteration": "En",
      "glossPt": "em",
      "alignedText": "No",
      "tokenPositions": [0, 1],
      "confidence": 0.9,
      "method": "stemmed",
      "glossMatch": false
    }
  ]
}
```

### Strong's Detail

```
GET /bible/strongs/{number}?lang=pt
→ greek_lexicon completo com gloss_pt, definition_pt, occurrences
```

### Admin (Tokenização/Alinhamento)

```
POST /admin/bible/tokenize         -- dispara bible_tokenize_arc69/kjv
GET  /admin/bible/alignment/stats  -- cobertura do alinhamento por livro
POST /admin/bible/align/reset      -- limpa word_alignments para re-rodar
```

---

## Componentes Frontend

### Hierarquia de Componentes

```
frontend/app/[locale]/(atlas)/bible/interlinear/page.tsx
  └── InterlinearViewer
        ├── VerseSelector (livro / capítulo / versão)
        ├── para cada versículo:
        │     ├── ARC69VerseText  -- frase completa PT em cima
        │     ├── TokenRow        -- scroll horizontal de tokens gregos
        │     │     └── TokenCard (por palavra grega)
        │     │           ├── greekWord  (fonte grande)
        │     │           ├── transliteration
        │     │           ├── strongs number (link)
        │     │           ├── glossPt   (verde=match / laranja=diverge)
        │     │           └── alignedText (palavra ARC69 correspondente)
        │     └── LegendBar  (só na primeira vez)
        └── StrongsSidePanel (abre ao clicar no número Strong's)
```

---

## Arquivos a Criar / Modificar

| Arquivo | Ação |
|---------|------|
| `docs/bible-interlinear-system.md` | Este documento |
| `backend/src/main/resources/db/migration/V_NNN__bible_verse_tokens.sql` | Nova tabela |
| Migration para `word_alignments` | Adicionar `token_positions[]`, `method`, `gloss_match` |
| `BibleTokenizationService.kt` | Fase `bible_tokenize_arc69` |
| `WordAlignmentService.kt` | Hierarquia exact→stem→AI + `gloss_match` |
| `InterlinearRoute.kt` | Endpoint `/bible/interlinear/{book}/{chapter}` |
| `frontend/app/[locale]/(atlas)/bible/interlinear/page.tsx` | Criar página |
| `frontend/components/bible/TokenCard.tsx` | Componente palavra |
| `frontend/components/bible/StrongsSidePanel.tsx` | Painel lateral Strong's |
| `frontend/hooks/useBibleInterlinear.ts` | Hook TanStack Query |
| `frontend/messages/pt.json` | Chaves `glossMatch`, `divergentGloss`, `alignedWord` |

---

## Verificação End-to-End

1. **Ingestão:** Rodar Camada 4 (`bible_tokenize_arc69`) → confirmar `bible_verse_tokens` populada para João 3 (~150 tokens)
2. **Alinhamento:** Rodar `bible_align_arc69` → checar distribuição `exact`/`stemmed`/`ai` (espera-se ~60% exact+stemmed para NT)
3. **API:** `GET /api/bible/interlinear/John/3?version=ARC69` → cada token deve ter `glossPt`, `alignedText`, `glossMatch`
4. **Frontend:** Abrir `/bible/interlinear` → João 3:16 → verificar que `ἠγάπησεν` mostra `glossPt: "amou"` e `alignedText: "amou"` com cor **verde** (exact match)
5. **Strong's panel:** Clicar G25 → painel lateral mostra `definition_pt` completa e lista ocorrências

---

## Notas de Implementação

- **Tokenizador PT:** strip de pontuação `[.,;:!?()\[\]«»""]`, lowercase para comparação, manter `token_raw` com pontuação original para display
- **Levenshtein threshold:** 30% da string mais longa (configurável via env `GLOSS_MATCH_THRESHOLD=0.3`)
- **ARC69 fonte:** confirmar qualidade do scraper antes de tokenizar
- **Re-run seguro:** `bible_tokenize_arc69` é idempotente via `UNIQUE`; `bible_align_arc69` deve ter opção de rodar só para livros não alinhados
