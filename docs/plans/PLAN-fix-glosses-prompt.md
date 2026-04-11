# Plano: Corrigir erros semanticos nos glosses PT (João 2)

## Contexto

A validacao de glosses PT em Joao 2 (v2) revelou 5 categorias de erro. O usuario pediu investigacao de causa raiz para os erros semanticos (casos 1, 2, 3, 4, 5) simulando internamente o que a aplicacao faz.

## Diagnostico da Causa Raiz

### O que o LLM recebe hoje

```
transliteration | morphology
```

Exemplo real: `egeusato | V-ADI-3S`

### O que o LLM NÃO recebe (mas existe no banco)

- `english_gloss` ("tasted") — **esta e a causa raiz principal**
- `lemma` ("γεύομαι")
- `strongs_number` ("G1089")
- `original_word` (grego original)
- Contexto do versiculo

### Analise por caso

| Caso | Input pro LLM | Output errado | Com english_gloss seria | Causa raiz |
|------|---------------|---------------|-------------------------|------------|
| 1: `ᾔδεισαν` → "temeram" | `ēdeisan \| V-2LAI-3P` | "temeram" | "knew" → "sabiam" trivial | **Prompt incompleto** — transliteracao de verbo irregular (οἶδα) e ambigua sem gloss EN |
| 2: `αὐτοῖς` → "nessas" | `autois \| P-DPM` | "nessas" | "to them" → "a eles/lhes" trivial | **Prompt incompleto** — pronome dativo sem referencia EN, modelo chutou preposicao |
| 3: `ἐγεύσατο` → "se perturbou" | `egeusato \| V-ADI-3S` | "se perturbou" | "tasted" → "provou" trivial | **Prompt incompleto** — transliteracao rara, modelo nao reconheceu γεύομαι |
| 4: `ἠντληκότες` → "encharcados" | `ēntlēkotes \| V-RAP-NPM` | "encharcados" | "having drawn" → "tendo tirado" | **Prompt incompleto** — modelo associou raiz a agua mas errou a acao |
| 5: `λύσατε` → "desatai" | `lusate \| V-AAM-2P` | "desatai" | "do destroy" → "destrui/derribai" | **Prompt incompleto** — λύω e polissemico (soltar/destruir), sem EN gloss modelo escolheu significado primario |

### Conclusao

**O problema NÃO e o modelo (gpt-4.1-mini).** O problema e o prompt que envia apenas `transliteration | morphology` quando o banco tem `english_gloss`, `lemma` e `strongs_number` disponiveis. Incluir o `english_gloss` no input resolveria os 5 casos sem trocar de modelo ou tier.

A transliteracao sozinha e insuficiente para verbos irregulares gregos (οἶδα, γεύομαι) e palavras polissemicas (λύω). O english_gloss ja e uma traducao humana validada que serve como ancora semantica.

---

## Plano de Correcao

### Alteracao unica: enriquecer o prompt com english_gloss e lemma

**Arquivo:** `backend/src/main/kotlin/com/ntcoverage/service/BibleIngestionService.kt`

#### 1. Alterar o formato do input (~linha 1093)

**De:**
```
transliteration | morphology
```

**Para:**
```
transliteration | morphology | english_gloss | lemma
```

#### 2. Atualizar o system prompt (~linhas 1080-1101)

Adicionar ao prompt:
- Explicacao do novo formato de 4 campos
- Instrucao para usar o english_gloss como referencia semantica
- Instrucao para adaptar gramaticalmente ao portugues (genero, numero, tempo) usando a morfologia

Prompt atualizado:
```
You are a biblical Greek-to-$language translator for interlinear Bible glosses.
Each line contains: transliteration | morphology | english_gloss | lemma

Translate each Greek word to a short $language gloss (1-3 words).
Use the english_gloss as semantic reference, then adapt to $language grammar using the morphology code.

Rules:
- Match grammatical number: 3P verbs → plural, 3S → singular
- Match grammatical gender for articles/pronouns: M→masculino, F→feminino, N→neutro
- For pronouns (P-DPM, P-DSF, etc), translate the grammatical function (dative=a/para, genitive=de, accusative=direct object)
- Prefer the contextual meaning of the english_gloss over the primary dictionary meaning of the lemma

[... morphology legend unchanged ...]

Return a JSON object mapping each transliteration to its $language translation.
```

#### 3. Atualizar o GlossTranslationEntry data class (~linha 292)

**De:**
```kotlin
data class GlossTranslationEntry(val transliteration: String, val morphology: String)
```

**Para:**
```kotlin
data class GlossTranslationEntry(val transliteration: String, val morphology: String, val englishGloss: String, val lemma: String)
```

#### 4. Atualizar a construcao dos entries (~linha 1030)

**De:**
```kotlin
val uniqueEntries = untranslated
    .map { GlossTranslationEntry(it.second.transliteration!!.trim(), it.second.morphology?.trim() ?: "") }
    .distinct()
```

**Para:**
```kotlin
val uniqueEntries = untranslated
    .map { GlossTranslationEntry(
        it.second.transliteration!!.trim(),
        it.second.morphology?.trim() ?: "",
        it.second.englishGloss?.trim() ?: "",
        it.second.lemma?.trim() ?: ""
    ) }
    .distinct()
```

#### 5. Atualizar o input do chunk (~linha 1094)

**De:**
```kotlin
val input = chunk.joinToString("\n") { "${it.transliteration} | ${it.morphology}" }
```

**Para:**
```kotlin
val input = chunk.joinToString("\n") { "${it.transliteration} | ${it.morphology} | ${it.englishGloss} | ${it.lemma}" }
```

#### 6. Atualizar o exemplo no prompt

Novo exemplo:
```
en | PREP | in | ἐν
archē | N-DSF | beginning | ἀρχή
ēn | V-IAI-3S | was | εἰμί
ho | T-NSM | the | ὁ
logos | N-NSM | Word | λόγος
```

---

## Verificacao

1. **Re-ingestao de Joao 2:** Limpar glosses PT de Joao 2 e re-executar `bible_translate_glosses` com filtro para Joao cap 2
2. **Verificar os 5 casos:**
   - `ᾔδεισαν` → deve ser "sabiam" (nao "temeram")
   - `αὐτοῖς` → deve ser "a eles" ou "lhes" (nao "nessas")
   - `ἐγεύσατο` → deve ser "provou" (nao "se perturbou")
   - `ἠντληκότες` → deve ser "tendo tirado" (nao "encharcados")
   - `λύσατε` → deve ser "destrui/derribai" (nao "desatai")
3. **Query de validacao:**
   ```sql
   SELECT iw.original_word, iw.portuguese_gloss, wa.aligned_text
   FROM interlinear_words iw
   JOIN bible_verses bv ON bv.id = iw.verse_id
   LEFT JOIN word_alignments wa ON wa.verse_id = iw.verse_id AND wa.word_position = iw.word_position AND wa.version_code = 'ARC69'
   WHERE bv.book_id = 67 AND bv.chapter = 2
     AND iw.word_position IN (18, 3, 20) AND bv.verse_number = 9;
   ```

## Arquivos a modificar

- `backend/src/main/kotlin/com/ntcoverage/service/BibleIngestionService.kt` — prompt, data class, input format
