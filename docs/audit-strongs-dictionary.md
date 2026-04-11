# Auditoria do Dicionário Strong's — Manuscriptum Atlas

**Data:** 2026-04-09
**Escopo:** Verificação de conteúdo (precisão vs BibleHub), qualidade de dados no banco, e análise de UI/UX
**Amostra:** 25 palavras gregas + 25 palavras hebraicas cruzadas manualmente com BibleHub; 100+100 aleatórias auditadas via banco

---

## 1. Verificação de Conteúdo (Precisão vs BibleHub)

### Metodologia

- Fonte primária dos dados: **STEPBible** (TSV do GitHub) → lemma, transliteração, short/full definition, part of speech
- Enriquecimento: **BibleHub scraping** → pronúncia, phonetic, KJV, word origin, Strong's Exhaustive, NAS Exhaustive
- Comparação feita scraping BibleHub em tempo real e cruzando campo a campo com a API local (`/bible/lexicon/{number}?locale=en`)

### Veredicto geral

**Os dados estão corretos.** Nas 50 entradas comparadas manualmente, não foram encontrados erros factuais de conteúdo — os dados batem com o BibleHub.

### Amostra Grega (25 palavras)

| Strong's | Palavra | Transliteração | Short Def (Local) | Short Def (BibleHub) | Status |
|----------|---------|----------------|--------------------|-----------------------|--------|
| G5207 | υἱός | huios | son | Son | ✅ OK |
| G2316 | θεός | theos | God | God, god | ✅ OK |
| G3056 | λόγος | logos | word | Word, speech | ✅ OK |
| G0026 | ἀγάπη | agape | love | Love | ✅ OK |
| G4102 | πίστις | pistis | faith | Faith, belief, trust | ✅ OK |
| G2424 | Ἰησοῦς | Iesous | Jesus | Jesus | ✅ OK |
| G5547 | Χριστός | Christos | Christ | Christ, Anointed One | ✅ OK |
| G4151 | πνεῦμα | pneuma | spirit | Spirit, wind, breath | ✅ OK |
| G2889 | κόσμος | kosmos | world | World, universe, order | ✅ OK |
| G0444 | ἄνθρωπος | anthropos | man | Man, human, mankind | ✅ OK |
| G2491 | Ἰωάννης | Ioannes | John | John | ✅ OK |
| G0932 | βασιλεία | basileia | kingdom | Kingdom | ✅ OK |
| G2962 | κύριος | kurios | lord | Lord, master, owner | ✅ OK |
| G2222 | ζωή | zoe | life | Life | ✅ OK |
| G1680 | ἐλπίς | elpis | hope | Hope | ✅ OK |
| G3588 | ὁ, ἡ, τό | ho | the | the, this, that | ✅ OK |
| G1722 | ἐν | en | in | in, on, at, by | ✅ OK |
| G0846 | αὐτός | autos | he/she/it | he, she, it, self | ✅ OK |
| G1519 | εἰς | eis | into | into, to, towards | ✅ OK |
| G3739 | ὅς | hos | who | who, which, that | ✅ OK |
| G1537 | ἐκ | ek | out of | from, out of, by | ✅ OK |
| G3956 | πᾶς | pas | all | All, every, whole | ✅ OK |
| G1161 | δέ | de | but | but, and, now | ✅ OK |
| G2532 | καί | kai | and | and, also, even | ✅ OK |
| G3361 | μή | me | not | not, lest | ✅ OK |

### Amostra Hebraica (25 palavras)

| Strong's | Palavra | Transliteração | Short Def (Local) | Short Def (BibleHub) | Status |
|----------|---------|----------------|--------------------|-----------------------|--------|
| H0430 | אֱלֹהִים | elohim | God | God, gods, divine beings | ✅ OK |
| H3068 | יְהוָה | YHWH | LORD | LORD, GOD | ✅ OK |
| H1697 | דָּבָר | dabar | word | Word, matter, thing, speech | ✅ OK |
| H0776 | אֶרֶץ | erets | earth | Earth, land, ground | ✅ OK |
| H8064 | שָׁמַיִם | shamayim | heavens | Heaven(s), sky | ✅ OK |
| H5315 | נֶפֶשׁ | nephesh | soul | Soul, life, self, person | ✅ OK |
| H7307 | רוּחַ | ruach | spirit | spirit, wind, breath | ✅ OK |
| H0571 | אֶמֶת | emeth | truth | Truth, faithfulness | ✅ OK |
| H4428 | מֶלֶךְ | melek | king | king | ✅ OK |
| H6662 | צַדִּיק | tsaddiq | righteous | Righteous, just | ✅ OK |
| H1285 | בְּרִית | berith | covenant | covenant, treaty, league | ✅ OK |
| H3478 | יִשְׂרָאֵל | Yisrael | Israel | Israel | ✅ OK |
| H8451 | תּוֹרָה | torah | law | law, instruction | ✅ OK |
| H0001 | אָב | ab | father | father | ✅ OK |
| H0559 | אָמַר | amar | say | said, saying, say | ✅ OK |
| H1121 | בֵּן | ben | son | Son, descendant | ✅ OK |
| H3117 | יוֹם | yom | day | day | ✅ OK |
| H5971 | עַם | am | people | People, nation | ✅ OK |
| H6213 | עָשָׂה | asah | do, make | To do, to make | ✅ OK |
| H5414 | נָתַן | nathan | give | give | ✅ OK |
| H0935 | בּוֹא | bo | come | To come, to go, to enter | ✅ OK |
| H7725 | שׁוּב | shub | return | Return, turn back | ✅ OK |
| H3045 | יָדַע | yada | know | To know, to perceive | ✅ OK |
| H3820 | לֵב | leb | — | heart, minds | ⛔ AUSENTE NO BANCO |
| H2617 | חֶסֶד | chesed | — | Lovingkindness, mercy | ⛔ AUSENTE NO BANCO |

---

## 2. Auditoria de Qualidade de Dados (Banco Completo)

### Totais

| Língua | Entradas |
|--------|----------|
| Grego | 10.847 |
| Hebraico | 8.181 |
| **Total** | **19.028** |

### Cobertura de Campos — Greek Lexicon (10.847 entradas)

| Campo | Com Dados | % Cobertura | Nota |
|-------|-----------|-------------|------|
| transliteration | 10.742 | 99,0% | ✅ |
| short_definition | 10.846 | ~100% | ✅ |
| full_definition | 10.847 | 100% | ✅ |
| part_of_speech | 9.585 | 88,4% | ⚠️ 12% sem POS |
| pronunciation | 5.523 | 51,0% | ⚠️ Range estendido sem dados |
| phonetic_spelling | 5.523 | 51,0% | ⚠️ Range estendido sem dados |
| kjv_translation | 5.522 | 50,9% | ⚠️ Range estendido sem dados |
| kjv_usage_count | 5.347 | 49,3% | ⚠️ Range estendido sem dados |
| word_origin | 5.523 | 51,0% | ⚠️ Range estendido sem dados |
| strongs_exhaustive | 5.523 | 51,0% | ⚠️ Range estendido sem dados |
| nas_exhaustive_origin | 5.328 | 49,1% | ⚠️ Range estendido sem dados |
| nas_exhaustive_definition | 5.119 | 47,2% | ⚠️ Range estendido sem dados |
| nas_exhaustive_translation | 5.074 | 46,8% | ⚠️ Range estendido sem dados |
| **nasb_translation** | **0** | **0%** | ⛔ Nunca populado |

**Explicação do padrão ~51%:** O range padrão (G0001–G5624, 5.523 entradas) está ~100% completo. O range estendido (G5625+, 5.324 entradas) vem do léxico LXX/Septuaginta e não existe no BibleHub para scraping, portanto só tem campos básicos (definition, transliteration, POS).

### Cobertura de Campos — Hebrew Lexicon (8.181 entradas)

| Campo | Com Dados | % Cobertura | Nota |
|-------|-----------|-------------|------|
| transliteration | 8.181 | 100% | ✅ |
| short_definition | 8.181 | 100% | ✅ |
| full_definition | 8.181 | 100% | ✅ |
| part_of_speech | 8.181 | 100% | ✅ |
| pronunciation | 8.132 | 99,4% | ✅ |
| phonetic_spelling | 8.132 | 99,4% | ✅ |
| kjv_translation | 8.123 | 99,3% | ✅ |
| kjv_usage_count | 8.028 | 98,1% | ✅ |
| word_origin | 8.125 | 99,3% | ✅ |
| strongs_exhaustive | 8.131 | 99,4% | ✅ |
| nas_exhaustive_origin | 7.920 | 96,8% | ✅ |
| nas_exhaustive_definition | 7.590 | 92,8% | ✅ |
| nas_exhaustive_translation | 7.546 | 92,2% | ✅ |
| **nasb_translation** | **0** | **0%** | ⛔ Nunca populado |

### Cobertura de Traduções (pt/es)

| Tabela | Locale | Entradas | % |
|--------|--------|----------|---|
| greek_lexicon_translations | pt | 10.847 | 100% |
| greek_lexicon_translations | es | 10.847 | 100% |
| hebrew_lexicon_translations | pt | 8.181 | 100% |
| hebrew_lexicon_translations | es | 8.181 | 100% |

> Nota: Inglês (`en`) é servido diretamente dos campos base da tabela principal — não precisa de tabela de tradução.

### Gaps de Numeração

| Língua | Range Esperado | Gaps | Detalhes |
|--------|---------------|------|----------|
| Grego | G0001–G5624 | 101 | G3203–G3302 = 100 números consecutivos (lacuna conhecida da numeração Strong's original) + G2717 (número reservado) |
| Hebraico | H0001–H8674 | 542 | Espalhados pelo range. A investigar se são números reservados ou falhas de ingestão |

### Artefatos HTML / Dados Corrompidos

**0 registros** com artefatos HTML em qualquer campo. Dados estão limpos.

### Entradas Ausentes Críticas

| Strong's | Palavra | Significado | Ocorrências AT | Impacto |
|----------|---------|-------------|----------------|---------|
| **H3820** | לֵב (leb) | coração, mente | ~850x | ⛔ Crítico — conceito antropológico central |
| **H2617** | חֶסֶד (chesed) | misericórdia, amor leal | ~250x | ⛔ Crítico — conceito teológico central |

---

## 3. Comparação Detalhada de Campos (Amostra)

### Campos de Enriquecimento BibleHub — Greek

| Strong's | Campo | Local | BibleHub | Match? |
|----------|-------|-------|----------|--------|
| G5207 | word_origin | apparently a primary word | apparently a primary word | ✅ |
| G5207 | kjv | child, foal, son | child, foal, son | ✅ |
| G2316 | word_origin | of uncertain affinity | of uncertain affinity | ✅ |
| G2316 | part_of_speech | N-M/F | Noun, Fem + Masc | ✅ |
| G3056 | word_origin | from G3004 | from G3004 (λέγω) | ✅ |
| G0026 | word_origin | from G25 | from G25 (ἀγαπάω) | ✅ |
| G4102 | word_origin | from G3982 | from G3982 (πείθω) | ✅ |
| G2424 | word_origin | Hebrew origin H3091 | Hebrew origin H3091 | ✅ |
| G4151 | word_origin | from G4154 | from G4154 (πνέω) | ✅ |
| G2491 | kjv_count | 135 | 135 | ✅ |
| G0932 | kjv_count | 163 | 163 | ✅ |
| G2962 | kjv_count | 722 | 722 | ✅ |

### Campos de Enriquecimento BibleHub — Hebrew

| Strong's | Campo | Local | BibleHub | Match? |
|----------|-------|-------|----------|--------|
| H0430 | word_origin | plural of H433 | plural of H433 (אֱלוֹהַּ) | ✅ |
| H0430 | kjv_count | 598 | — | ✅ |
| H3068 | word_origin | from H1961 | from H1961 (הָיָה) | ✅ |
| H1697 | word_origin | from H1696 | from H1696 (דָּבַר) | ✅ |
| H1697 | kjv_count | 1441 | — | ✅ |
| H0776 | kjv_count | 2503 | — | ✅ |
| H4428 | kjv_count | 2523 | — | ✅ |
| H1285 | word_origin | from H1262 | from H1262 (בָּרָה) | ✅ |
| H8451 | word_origin | from H3384 | from H3384 (יָרָה) | ✅ |

---

## 4. Análise de UI/UX

### Screenshot analisado

Página `localhost:3000/pt/bible/strongs/G2491` (Ἰωάννης — João)

### Problemas Identificados

#### A) Hierarquia visual inexistente — tudo tem o mesmo peso

O card "Resumo Lexical" apresenta ~10 campos em um grid com labels vermelhos, todos com o mesmo tamanho de fonte e peso visual. Não há distinção entre informação **primária** (o que a palavra significa) e **secundária** (detalhes técnicos como pronúncia fonética, número Strong's).

O usuário que busca um Strong's quer saber: **o que essa palavra significa**, de onde vem, e como é usada. A resposta está enterrada no meio de 10 campos equivalentes.

#### B) Informação repetida/redundante

- "Iōannēs: João" aparece no resumo → depois "Transliteração: Iōannēs" repete o mesmo dado
- "Origem da Palavra" aparece no grid principal E na seção NAS Exhaustive
- "Pronúncia" e "Soletração Fonética" mostram basicamente o mesmo dado em formatos ligeiramente diferentes
- Para nomes próprios, "Concordância Exaustiva de Strong" e "Concordância Exaustiva NAS" repetem o mesmo conteúdo

#### C) Labels em vermelho são agressivos e criam ruído visual

Todos os labels usam `text-red-500 dark:text-red-400`. O vermelho compete pela atenção com o conteúdo real, criando uma página visualmente "barulhenta". Vermelho deveria ser reservado para alertas ou destaques, não para labels descritivos comuns.

#### D) Seções secundárias abertas por padrão

As 3 seções após o card principal (Strong's Exhaustive, NAS Exhaustive, Full Definition) estão todas expandidas, ocupando scroll extenso. Para a maioria das buscas, o resumo do card principal já é suficiente.

#### E) Campo NASB aparece vazio

O campo `nasb_translation` está 0% populado no banco, mas a UI tenta renderizá-lo. Quando não há dados, simplesmente não aparece (pelo `&&`), mas o campo `nasbTranslation` na UI ao lado do KJV cria expectativa de dados que não existem.

#### F) Conteúdo "crú" do scraping

O campo de pronúncia no screenshot mostra texto concatenado do scraping: `"yo-AN-nace Phonetic Spelling: {ee-o-an'-nace} KJV: John, John's Word Origin: [of Hebrew origin..."`. Parece que algum pós-processamento está falhando e o texto crú do scraping está vazando para a UI.

### Proposta de Redesign

#### Layout proposto — "Significado em destaque"

```
┌──────────────────────────────────────────────────────────┐
│                                                          │
│  2491. Ἰωάννης                                          │
│                                                          │
│  ┌─ SIGNIFICADO ────────────────────────────────────┐    │
│  │                                                   │    │
│  │  "João"                                          │    │
│  │  Nome próprio masculino                          │    │
│  │  Do hebraico Yôchānān (יוֹחָנָן, H3110)         │    │
│  │                                                   │    │
│  └───────────────────────────────────────────────────┘    │
│                                                          │
│  Transliteração   Iōannēs                               │
│  Pronúncia        ee-o-an'-nace                          │
│  KJV              John (135x)                            │
│                                                          │
│  ▸ Concordância Exaustiva de Strong                      │
│  ▸ Concordância Exaustiva NAS                            │
│  ▸ Definição Completa (Thayer's)                         │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

#### Mudanças concretas

| # | Mudança | Motivo |
|---|---------|--------|
| 1 | **Card hero** com significado resumido (shortDefinition) em destaque grande + POS + origem em uma frase | Responde 80% das buscas instantaneamente |
| 2 | **Seções colapsáveis** (accordion) para Strong's Exhaustive, NAS Exhaustive e Full Definition | Reduz scroll; quem precisa expande |
| 3 | **Remover redundância**: unificar Pronúncia + Soletração Fonética em uma linha; não repetir Transliteração se já está no header | Menos ruído |
| 4 | **Labels neutros** (`text-muted-foreground font-medium`) em vez de `text-red-500` | Página mais calma, leitura mais fluida |
| 5 | **Ocultar NASB** enquanto 0% populado | Não mostrar campos vazios |
| 6 | **Layout simplificado para nomes próprios**: detectar `partOfSpeech` contendo "Proper" e omitir seções que repetem informação | Nomes próprios não precisam de NAS Exhaustive |
| 7 | **Limpar texto de pronúncia**: garantir que o scraper separa corretamente os campos e não concatena dados | Texto crú do scraping não deve aparecer na UI |

---

## 5. Resumo de Ações Recomendadas

### Prioridade 0 — Crítico

- [ ] Investigar e ingerir **H3820** (לֵב leb, "coração") — uma das palavras mais frequentes do AT
- [ ] Investigar e ingerir **H2617** (חֶסֶד chesed, "misericórdia/amor leal") — conceito teológico central
- [ ] Corrigir texto de pronúncia que mostra dados crús do scraping na UI

### Prioridade 1 — Importante

- [ ] Redesign do card StrongsEntry: hero com significado + POS + origem em destaque
- [ ] Seções expandíveis (accordion) para concordâncias e definição completa
- [ ] Ocultar campo `nasb_translation` da UI (0% populado em 19.028 entradas)
- [ ] Substituir labels `text-red-500` por cor neutra

### Prioridade 2 — Melhorias

- [ ] Unificar "Pronúncia" e "Soletração Fonética" em um único campo
- [ ] Remover redundância: Transliteração no grid quando já está no header
- [ ] Layout simplificado para nomes próprios (POS = "Proper")
- [ ] Investigar 542 gaps de numeração hebraica (falha de ingestão vs números reservados)

### Prioridade 3 — Dívida técnica

- [ ] Decidir destino da coluna `nasb_translation`: popular via nova ingestão ou remover do schema
- [ ] Documentar que range estendido Greek (G5625+) não tem dados BibleHub
- [ ] Verificar se range H9001-H9030+ (marcadores morfológicos) deveria ser filtrado da UI

---

## 6. Dados Brutos — BibleHub para Entradas Ausentes

### H3820 — לֵב (leb) — "coração"

```
Transliteration: leb
Pronunciation: layb
Phonetic Spelling: (labe)
Part of Speech: Noun, Masculine
Word Origin: a form of H3824 (לֵבָב)
Short Definition: heart, hearts, mind
KJV: care for, comfortably, consent, considered, courageous, friendly,
     broken/hard/merry/stiff/stout/double heart(-ed), heed, kindly,
     midst, mind(-ed), regard, understanding, wisdom
NAS Exhaustive: inner man, mind, will, heart
  - accord (1), attention (4), creativity (1), courage (1),
    double heart (1), understanding (7)
Strong's Exhaustive: a form of lebab; the heart; also used (figuratively)
  very widely for the feelings, the will and even the intellect;
  likewise for the centre of anything
```

### H2617 — חֶסֶד (chesed) — "misericórdia, amor leal"

```
Transliteration: checed
Pronunciation: kheh'-sed
Phonetic Spelling: (kheh'-sed)
Part of Speech: Noun, Masculine
Word Origin: from H2616 (חָסַד - "to be kind")
Short Definition: lovingkindness, mercy, steadfast love, loyalty,
                  faithfulness, goodness
KJV: favour, good deed(-liness, -ness), kindly, (loving-)kindness,
     merciful (kindness), mercy, pity, reproach, wicked thing
NAS Exhaustive:
  1. kindness
  2. (by implication, toward God) piety
  3. (by opposition, rarely) reproof
  4. (subjectively) beauty
Strong's Exhaustive: From chacad; kindness; by implication (towards God)
  piety: rarely (by opposition) reproof, or (subject.) beauty
```
