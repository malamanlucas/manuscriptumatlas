# Relatório de Auditoria — LLM Queue Pipeline

**Data:** 2026-04-16  
**Escopo:** Verificar integridade, qualidade e consistência dos dados gerados pelo LLM Queue

---

## 1. Visão Geral da Fila

### 1.1 Comparação Screenshot vs Banco

| Métrica       | Screenshot | Banco (real) | Diferença |
|---------------|-----------|-------------|-----------|
| Pendentes     | 25.143    | 25.093      | -50 (timing) |
| Processando   | 450       | 489         | +39 (timing) |
| Concluídos    | 0         | 11          | **11 items completed mas não exibidos** |
| Aplicados     | 6.254     | 6.254       | 0 (match exato) |
| Falharam      | 0         | 0           | 0 (match exato) |
| **Total**     | **31.847**| **31.847**  | **match exato** |

**Diagnóstico:** As pequenas diferenças em pendentes/processando são naturais (items transitaram entre o snapshot e a consulta). Os **11 items "completed" não exibidos como concluídos** no dashboard merecem atenção — provavelmente o frontend agrega "completed" e "applied" ou não exibe completed quando é zero visual.

### 1.2 Detalhamento por Fase

| Fase | Pendentes | Processando | Concluídos | Aplicados | DB Match? |
|------|-----------|-------------|------------|-----------|-----------|
| apologetics_complement_response | 0 | 0 | 0 | 1 | OK |
| apologetics_generate_topic | 0 | 0 | 0 | 1 | OK |
| bible_translate_enrichment_greek | 9.593 | 150 | 10 | 1.303 | OK |
| bible_translate_enrichment_hebrew | 13.549 | 141 | 47 | 3.595 | OK |
| bible_translate_glosses | 1.834 | 98 | 5 | 690 | OK |
| bible_translate_hebrew_lexicon | 55 | 50 | 0 | 101 | OK |
| bible_translate_lexicon | 62 | 50 | 0 | 160 | OK |
| bio_translate_prepare | 0 | 0 | 0 | 70 | OK |
| council_translate_prepare | 0 | 0 | 0 | 194 | OK |
| dating_fathers_prepare | 0 | 0 | 0 | 35 | OK |
| dating_manuscripts_prepare | 0 | 0 | 0 | 86 | OK |
| heresy_translate_prepare | 0 | 0 | 0 | 18 | OK |

**62 items completed aguardando apply** (10 greek enrichment + 47 hebrew enrichment + 5 glosses). Precisam de trigger de apply.

---

## 2. Arquitetura de Banco

O sistema usa **dois bancos PostgreSQL separados**:

| Banco | Propósito | Tabelas |
|-------|-----------|---------|
| `nt_coverage` | Manuscritos, patrística, concílios, heresias, apologética, fila LLM | 42 tabelas |
| `bible_db` | Versões bíblicas, versículos, interlinear, léxico grego/hebraico, alinhamentos | 13 tabelas |

Flyway **não está rastreando migrações** (flyway_schema_history vazio). Todas as tabelas são criadas via Exposed `SchemaUtils.createMissingTablesAndColumns()`.

---

## 3. Verificação de IDs e Integridade Referencial

### 3.1 Léxico (bible_db)

| Verificação | Resultado |
|-------------|-----------|
| Orphan greek_lexicon_translations (lexicon_id sem entry) | **0** — OK |
| Orphan hebrew_lexicon_translations (lexicon_id sem entry) | **0** — OK |

### 3.2 Concílios (nt_coverage)

| Verificação | Resultado |
|-------------|-----------|
| Queue callback_context.councilId → councils.id | **Todos válidos** — OK |
| council_translations.council_id → councils.id | **Todos válidos** — OK |

**Amostra de cruzamento:**

| Queue ID | council_id | Nome Original | Nome Traduzido (es) |
|----------|-----------|---------------|---------------------|
| 62573 | 270 | Council of Benevento | Concilio de Benevento |
| 62571 | 202 | Seventeenth Council of Toledo | Decimoséptimo Concilio de Toledo |
| 62569 | 201 | Sixteenth Council of Toledo | Decimosexto Concilio de Toledo |

### 3.3 Datação (nt_coverage)

| Verificação | Resultado |
|-------------|-----------|
| Queue callback_context.fatherId → church_fathers.id | **Todos válidos** — OK |
| Queue callback_context.gaId → manuscripts.ga_id | **Todos válidos** — OK |

---

## 4. Qualidade dos Dados Aplicados

### 4.1 Léxico Grego — Traduções (bible_db)

**Total:** 10.847 entradas no léxico | 10.082 traduções criadas (93% cobertura)

| Locale | Total | Com short_def | Com full_def | Com KJV | Com word_origin | Com exhaustive |
|--------|-------|---------------|-------------|---------|-----------------|----------------|
| pt | 8.046 | 7.418 (92%) | 7.418 (92%) | 636 (8%) | 633 (8%) | 632 (8%) |
| es | 2.036 | 1.428 (70%) | 1.428 (70%) | 645 (32%) | 643 (32%) | 642 (32%) |

**Problema identificado:** 628 registros pt e 608 registros es têm short_definition e full_definition **VAZIOS**, mas possuem kjv_translation e word_origin preenchidos. Isso ocorre porque esses registros foram criados pela fase `bible_translate_enrichment_greek` (que só preenche campos de enriquecimento) antes da fase `bible_translate_lexicon` (que preenche short/full definition) processar a mesma entrada.

**Amostra de qualidade (bom):**

| Strongs | Locale | Short Definition | KJV Translation |
|---------|--------|-----------------|-----------------|
| G0089 | pt | incessantemente | sem cessar |
| G0163 | pt | levar cativo | levar cativo, trazer para o cativeiro |
| G0165 | pt | era; eternidade | idade, curso, eterno, (para) sempre |

### 4.2 Léxico Hebraico — Traduções (bible_db)

**Total:** 8.696 entradas | 8.059 traduções (93% cobertura)

| Locale | Total | Com short_def | Com full_def | Com KJV | Com word_origin | Com exhaustive |
|--------|-------|---------------|-------------|---------|-----------------|----------------|
| pt | 6.287 | 4.832 (77%) | 4.832 (77%) | 1.461 (23%) | 1.475 (23%) | 1.393 (22%) |
| es | 1.772 | 279 (16%) | 279 (16%) | 1.477 (83%) | 1.491 (84%) | 1.412 (80%) |

**Problema severo:** Locale `es` tem apenas **16% de short_definition preenchido** mas 83% de KJV. A fase de enriquecimento está à frente da fase de tradução base para o espanhol. 1.493 registros es e 1.455 registros pt estão com definições vazias.

### 4.3 Glosses Interlineares (bible_db)

| Métrica | Valor |
|---------|-------|
| Total de palavras | 141.522 |
| Com gloss inglês | 141.522 (100%) |
| Com gloss português | 20.012 (14%) |
| Com gloss espanhol | 19.221 (14%) |

**Amostra (Mateus 1:1):**

| Grego | Transliteração | Inglês | Português | Espanhol |
|-------|---------------|--------|-----------|----------|
| Βίβλος | Biblos | [The] book | livro | el libro |
| γενέσεως | geneseōs | of [the] genealogy | da genealogia | de la genealogía |
| Ἰησοῦ | Iēsou | of Jesus | de Jesus | Jesús |
| Χριστοῦ | Christou | Christ | Cristo | Cristo |

**Qualidade:** Excelente. Traduções contextualizadas e corretas.

### 4.4 Concílios — Traduções (nt_coverage)

**Total:** 103 concílios traduzidos × 2 locales = 206 registros

| Locale | Total | Com display_name | Com short_description | Com summary |
|--------|-------|------------------|-----------------------|-------------|
| pt | 103 | 103 (100%) | 13 (13%) | 0 (0%) |
| es | 103 | 103 (100%) | 13 (13%) | 0 (0%) |

**Problema:** O LLM response para `council_translate_prepare` retorna apenas `displayName` e `location`. Os campos `shortDescription`, `mainTopics` e `summary` **não estão sendo gerados pelo LLM**, resultando em preenchimento nulo. Isso é um problema no **prompt de enqueue**, não no apply.

**Amostra de response do LLM:**
```json
{"displayName":"Concílio de Benevento","location":"benevento"}
{"displayName":"Décimo Sétimo Concílio de Toledo","location":"toledo"}
```
O prompt precisa ser atualizado para pedir todos os campos.

### 4.5 Heresias — Traduções (nt_coverage)

**Total:** 18 items aplicados (9 heresias × 2 locales)

| Heresia Original | PT | ES |
|-----------------|----|----|
| Arianism | Arianismo | Arrianismo |
| Nestorianism | Nestorianismo | Nestorianismo |
| Monophysitism | Monofisismo | Monofisismo |
| Pelagianism | Pelagianismo | Pelagianismo |

**Qualidade:** Excelente. Nomes corretos e descrições teologicamente precisas.

### 4.6 Biografias — Traduções (nt_coverage)

**Total:** 70 registros (35 padres × 2 locales)

| Campo | Preenchido |
|-------|-----------|
| display_name | **0 (0%)** |
| short_description | **0 (0%)** |
| primary_location | **0 (0%)** |
| biography_original | **70 (100%)** |
| biography_summary | **0 (0%)** |

**Problema crítico:** O `applyBioTranslation()` recebe `ctx.displayName` do `BioTranslateContext`, mas esse campo está defaultando para `""` na serialização. O LLM traduz apenas o `biography_original`. Os metadados (nome, descrição, localização) deveriam ser passados no contexto mas estão vazios.

### 4.7 Datação — Manuscritos e Padres (nt_coverage)

**Manuscritos datados pelo LLM:** 43 de 430 com data NTVMR

| Métrica | Valor |
|---------|-------|
| Com year_min/year_max | 43 (100% dos processados) |
| Com year_best | **0 (0%)** |
| Dating confidence | LOW (todos) |

**Padres datados pelo LLM:** 35

| Métrica | Valor |
|---------|-------|
| Com year_min/year_max | 35 (100%) |
| Com year_best | **2 (6%)** — apenas Clement of Rome e Irenaeus of Lyon |
| Dating confidence | LOW (todos) |

**Amostra:**

| Padre | year_min | year_max | year_best | Referência |
|-------|---------|---------|-----------|------------|
| Clement of Rome | 35 | 99 | 96 | Lightfoot, J.B. (1890). The Apostolic Fathers |
| Irenaeus of Lyon | 130 | 202 | 180 | Grant, R.M. (1997). Irenaeus of Lyons |
| Ignatius of Antioch | 35 | 108 | null | Schoedel, W.R. (1985). Ignatius of Antioch |
| Origen | 185 | 254 | null | Crouzel, H. (1989). Origen |

**Qualidade das referências:** Boa. Fontes acadêmicas legítimas e relevantes.

**Problema:** `year_best` quase sempre null. O campo é opcional na `DatingResponse` e o LLM raramente o preenche.

### 4.8 Apologética (nt_coverage)

| Tópico | Status |
|--------|--------|
| A contradição dos dois pais de José: Jacó ou Heli? | DRAFT |
| A alegação de erro profético em Mateus 24:34 | DRAFT |
| O argumento de que a Bíblia endossa a escravidão | DRAFT |

**Qualidade:** Boa. Textos substanciais e bem argumentados.

---

## 5. Problemas Encontrados

### Severidade Alta

| # | Problema | Impacto | Fase Afetada |
|---|---------|---------|-------------|
| 1 | **church_father_translations: display_name, short_description e biography_summary VAZIOS** em todos os 70 registros | Padres da Igreja sem nome traduzido e sem resumo biográfico | bio_translate_prepare |
| 2 | **hebrew_lexicon_translations (es): apenas 16% com short_definition** | Léxico hebraico praticamente inutilizável em espanhol | bible_translate_hebrew_lexicon |

### Severidade Média

| # | Problema | Impacto | Fase Afetada |
|---|---------|---------|-------------|
| 3 | **council_translations: 0% com summary, 87% sem short_description** | Concílios sem descrição nem resumo traduzidos | council_translate_prepare |
| 4 | **year_best null em 98% das datações** (manuscritos e padres) | Sem data estimada precisa, apenas ranges | dating_*_prepare |
| 5 | **628 registros greek e 1.455 hebrew com short/full_definition vazios** | Entradas de léxico incompletas | bible_translate_lexicon |
| 6 | **62 items "completed" não aplicados** | Dados processados mas não persistidos nas tabelas destino | enrichment + glosses |

### Severidade Baixa

| # | Problema | Impacto |
|---|---------|---------|
| 7 | Flyway schema_history vazio — schema gerido apenas por Exposed | Sem controle de versão de migrações |
| 8 | word_alignments e bible_verse_tokens com 0 registros | Nenhuma fase de alinhamento foi enfileirada ainda |
| 9 | Glosses interlineares com 14% de cobertura (pt/es) | Em progresso — 1.834 items pendentes |

---

## 6. Causas Raiz

### Problema 1 — Bio translations sem metadados
**Causa:** `BioTranslateContext` define `displayName: String = ""` como default. O código de enqueue não popula esses campos ao criar o item da fila. O `applyBioTranslation()` usa `ctx.displayName` (vazio) para salvar na tabela destino.

### Problema 2/5 — Léxico com definições vazias
**Causa:** A fase `bible_translate_enrichment_*` cria registros na tabela de translations via `upsertGreek/HebrewEnrichmentTranslation()` preenchendo apenas os campos de enriquecimento (kjv, origin, exhaustive). Esses registros ficam com short/full_definition vazios. A fase `bible_translate_lexicon` deveria preencher esses campos depois, mas como faz upsert, precisa que o registro já exista ou cria um novo — e nem todos foram processados ainda.

### Problema 3 — Council translations incompletas
**Causa:** O prompt do LLM para tradução de concílios produz JSON com apenas `displayName` e `location`. Os campos `shortDescription`, `mainTopics` e `summary` não estão na resposta, provavelmente porque o prompt de enqueue não fornece esses dados originais ao LLM ou não os solicita na resposta.

### Problema 4 — year_best null
**Causa:** O campo `yearBest` é `Int? = null` no `DatingResponse`. O LLM raramente consegue determinar um "melhor ano estimado" — tende a dar apenas ranges.

---

## 7. Recomendações

1. ~~**Corrigir upserts de léxico**~~ — **CORRIGIDO** em 2026-04-16. Branch `else` com UPDATE adicionado em `upsertGreekTranslation`, `upsertHebrewTranslation` e `batchUpsertHebrewTranslations`. Repair executado: 1.611 registros corrigidos sem custo LLM.
2. **Corrigir enqueue de bio_translate_prepare** — popular `displayName`, `shortDescription` e `primaryLocation` no `BioTranslateContext` a partir dos dados do `church_fathers`
3. **Corrigir prompt de council_translate_prepare** — incluir `shortDescription`, `mainTopics` e `summary` originais no user_content e solicitar tradução no response
4. **Triggerar apply dos 62 items completed** — chamar `POST /admin/llm/queue/apply/{phase}` para as 3 fases pendentes
5. **Re-enfileirar léxico com definições vazias** — ~2.718 registros criados por enrichment nunca foram enfileirados no léxico (filtro `hasTranslation` retorna true). Ajustar filtro no `translateLexiconPrepare` para verificar se `short_definition` está preenchido.
6. **Considerar tornar year_best obrigatório** no prompt de datação — instruir o LLM a sempre fornecer sua melhor estimativa
