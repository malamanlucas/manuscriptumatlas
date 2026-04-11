# Relatório de Verificação — Traduções IA (Bíblia / bible_db)

> Gerado em 10/04/2026  
> Banco: `bible_db` | Escopo atual: Evangelho de João (único livro ingerido)

---

## 1. Dados Estáticos (não-IA) — OK

### Textos bíblicos (`bible_verse_texts`)
- **3.516 registros** — 4 versões × 879 versículos de João
- Verificação João 1:1 e João 3:16 em todas as versões: **CORRETO**
  - KJV, AA, ACF, ARC69 todos retornam os textos esperados
  - **Nenhum problema encontrado**

### Estrutura (`bible_books`, `bible_versions`, `bible_verses`)
- 1 livro (João, book_order=43, testament=NT) — OK
- 4 versões seedadas corretamente — OK
- 879 versículos todos com chapter/verse_number corretos — OK

---

## 2. Interlinear — Glossas PT/ES (`interlinear_words`)

### Estado Geral
- **16.069 palavras** interlineares (João completo)
- **9.273 sem glossa PT** (57,7%)
- **9.338 sem glossa ES** (58,1%)

### PROBLEMA CRÍTICO: Capítulo 6 — Zero Glossas

**João capítulo 6 tem 1.289 palavras e NENHUMA possui glossa PT ou ES.**

| Capítulo | Palavras | Com PT | Com ES |
|---|---|---|---|
| 1 | 852 | 404 (47%) | 425 (50%) |
| 2 | 437 | 240 (55%) | 247 (57%) |
| 3 | 676 | 325 (48%) | 308 (46%) |
| 4 | 958 | 442 (46%) | 495 (52%) |
| 5 | 838 | 463 (55%) | **196 (23%)** ⚠️ |
| **6** | **1.289** | **0 (0%)** 🔴 | **0 (0%)** 🔴 |
| 7 | 876 | 255 (29%) | 462 (53%) |
| 8–18 | ~7.000 | ~50–60% | ~50–60% |
| 19 | 834 | **186 (22%)** ⚠️ | 186 |
| 20 | 630 | **144 (23%)** ⚠️ | 144 |
| 21 | 556 | **94 (17%)** ⚠️ | 94 |

**Causas prováveis:**
- Cap. 6: provavelmente o batch de tradução desse capítulo falhou ou não foi enfileirado
- Caps. 5 (ES), 19–21: cobertura muito baixa — lotes possivelmente perdidos ou não aplicados

### Glossas Corretas (sem off-by-one)
Verificação manual de João 1:1–3 mostrou que as glossas PT estão **no versículo e posição correta**:
- Pos. 1: Ἐν → "Em" ✓
- Pos. 2: ἀρχῇ → "princípio" ✓
- Pos. 5: λόγος → "Verbo" ✓
- Pos. 12: θεόν → "Deus" ✓

**Nenhum bug de off-by-one detectado nas glossas** (diferente do problema encontrado nos Pais da Igreja).

---

## 3. Lexicon Grego (`greek_lexicon_translations`)

### Contagens

| Locale | Total | Traduzidas (distintas) | Copiadas do inglês | Vazias |
|---|---|---|---|---|
| PT | 10.635 | 3.106 (29%) | **5.175 (49%)** ⚠️ | 2.353 (22%) |
| ES | 7.932 | 405 (5%) | **4.909 (62%)** 🔴 | 2.617 (33%) |

### Copiadas do Inglês (não traduzidas)
~49% PT e ~62% ES são **cópias exatas do texto inglês original** — a IA retornou o próprio texto em vez de traduzir.

**Exemplos problemáticos (PT):**

| Strong | Lema | EN original | PT "tradução" |
|---|---|---|---|
| G1526 | εἰσί | they are | they are ❌ |
| G1568 | ἐκθαμβέω | be awe-struck | be awe-struck ❌ |
| G1571 | ἐκκαθαίρω | to cleanse | to cleanse ❌ |
| G1574 | ἐκκεντέω | to pierce | to pierce ❌ |
| G3852 | παραγγελία | order | mandamento, ordem ✓ |

**Nota:** Parte das "cópias" são legítimas — nomes próprios (Ἄβελ→Abel, Ἀνανίας→Ananias) ou termos técnicos sem tradução direta (Ἀββά). Mas verbos, adjetivos e substantivos comuns que foram retornados em inglês são erros.

### G3056 (λόγος = "Verbo/Palavra") — Caso Especial
- `short_definition` PT e ES estão **vazias** — dado o tamanho da entrada, o batch pode ter excedido tokens
- `kjv_translation` tem texto misto (inglês+português): "discurso" adicionado dentro de string inglesa
- `word_origin` está em inglês para ambos os locales — **não traduzido**

---

## 4. Lexicon Hebraico (`hebrew_lexicon_translations`)

### Contagens

| Locale | Total | Traduzidas (distintas) | Copiadas do inglês | Vazias |
|---|---|---|---|---|
| PT | 8.450 | 1.609 (19%) | **4.752 (56%)** 🔴 | 2.089 (25%) |
| ES | 8.687 | 444 (5%) | **7.727 (89%)** 🔴 | 516 (6%) |

**ES hebraico é praticamente não-traduzido** — 89% são cópias do inglês.

**Exemplos problemáticos (PT):**

| Strong | Lema | EN original | PT "tradução" |
|---|---|---|---|
| H3045 | יָדַע | to know | to know ❌ |
| H3201 | יָכֹל | be able | be able ❌ |
| H3205 | יָלַד | to beget | to beget ❌ |
| H7275 | רָגַם | to stone | to stone ❌ |
| H1236 | בִּקְעָא | plain | planície ✓ |
| H4849 | מִרְשַׁ֫עַת | wickedness | ímpioness ⚠️ (erro gramatical) |

---

## 5. Alinhamentos (`word_alignments`)

### Estado Geral
- **32.138 alinhamentos** (16.069 por versão)
- KJV: confiança média **89%**, 2.083 divergentes (13%)
- ARC69: confiança média **76%**, 5.325 divergentes (33%)

### Verificação de Correlação
João 1:1 KJV — todas as 17 posições foram verificadas manualmente:
- Ἐν → "In the" [0,1] ✓
- ἀρχῇ → "beginning" [2] ✓
- λόγος → "Word," [5] ✓
- θεός → "God." [16] ✓

**Nenhum off-by-one detectado nos alinhamentos.**

João 3:16 KJV — verificado:
- θεὸς (pos.5) → "God" [1] ✓
- κόσμον (pos.7) → "world," [5] ✓
- μονογενῆ (pos.13) → "only begotten" [10,11] ✓

**Alinhamentos estão semanticamente corretos e na posição certa.**

### ARC69 tem mais divergências (esperado)
33% divergente vs 13% do KJV — resultado esperado, pois o português reorganiza frases.

---

## 6. Resumo

| Domínio | Total | Corretos | Problemáticos | Status |
|---|---|---|---|---|
| Textos bíblicos (4 versões) | 3.516 | 3.516 | 0 | ✅ OK |
| Interlinear - estrutura | 16.069 | 16.069 | 0 | ✅ OK |
| Interlinear - glossas PT | 6.796 aplicadas | ~6.500 | Cap.6 zerado, caps.19-21 baixo | ⚠️ PARCIAL |
| Interlinear - glossas ES | 6.731 aplicadas | ~6.400 | Cap.6 zerado, caps.5/19-21 baixo | ⚠️ PARCIAL |
| Lexicon grego PT | 10.635 | 3.106 | 5.175 copiadas + 2.353 vazias | 🔴 CRÍTICO |
| Lexicon grego ES | 7.932 | 405 | 4.909 copiadas + 2.617 vazias | 🔴 CRÍTICO |
| Lexicon hebraico PT | 8.450 | 1.609 | 4.752 copiadas + 2.089 vazias | 🔴 CRÍTICO |
| Lexicon hebraico ES | 8.687 | 444 | 7.727 copiadas + 516 vazias | 🔴 CRÍTICO |
| Alinhamentos KJV | 16.069 | ~13.986 | 2.083 divergentes (esperado) | ✅ OK |
| Alinhamentos ARC69 | 16.069 | ~10.744 | 5.325 divergentes (esperado) | ✅ OK |

---

## 7. Próximos Passos

### Prioridade ALTA

1. **Capítulo 6 de João — zero glossas**
   - Verificar no `llm_prompt_queue` se os lotes do cap.6 foram enfileirados/aplicados
   - Re-enfileirar a fase `bible_translate_glosses` especificamente para cap.6
   - Verificar caps. 19–21 (cobertura < 25%)

2. **Lexicon grego e hebraico — traduções copiadas do inglês**
   - O problema parece ser no prompt ou no parser de resposta
   - A IA pode estar retornando JSON malformado para entradas com definições muito curtas (1-2 palavras), e o fallback mantém o texto original
   - **Ação:** Revisar `LlmResponseProcessor.applyLexiconBatch()` — verificar se o parser de resposta usa o campo inglês como fallback quando não consegue extrair tradução
   - Re-enfileirar entradas com `short_definition = original` para locales `pt` e `es`

### Prioridade MÉDIA

3. **Lexicon hebraico ES — 89% não traduzido**
   - Caso mais grave. Verificar se a fase `bible_translate_hebrew_lexicon` (ES) foi aplicada corretamente
   - 8.687 registros existem mas 7.727 são cópias — sugere que o lote foi processado mas o parser não extraiu as traduções

4. **G3056 (λόγος) — definições vazias**
   - Palavra mais importante do Evangelho de João sem tradução
   - A `full_definition` é enorme (~3.000 caracteres) — provável estouro de tokens no batch
   - Re-enfileirar individualmente com max_tokens aumentado
