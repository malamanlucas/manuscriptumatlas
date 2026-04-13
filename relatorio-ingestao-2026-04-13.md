# Relatório de Verificação — Pipeline de Ingestão
**Data:** 2026-04-13 | **Camadas verificadas:** 1, 2, 3 | **Fila LLM:** ativa

---

## Resumo Executivo

As três primeiras camadas foram concluídas com sucesso estrutural. Todas as 45 fases registradas estão com status `success`. A fila LLM está **ativa e processando** (Camada 2, fase de glossas e enriquecimentos). A Camada 4 (Tokenização + Alinhamento) ainda não foi iniciada. Há um acúmulo de **80 itens `completed` não aplicados** na fila — detalhe abaixo.

---

## 1. Bible DB — Estado das Tabelas

### Estrutura (Layer 1) ✅
| Tabela | Registros |
|--------|-----------|
| `bible_versions` | 4 (KJV, AA, ACF, ARC69) |
| `bible_books` | 66 |
| `bible_chapters` | 1.189 |
| `bible_verses` | 31.088 |

**Versões carregadas:**
| Código | Nome | Idioma | Primary |
|--------|------|--------|---------|
| KJV | King James Version | en | ✅ |
| AA | Almeida Atualizada | pt | — |
| ACF | Almeida Corrigida Fiel | pt | — |
| ARC69 | Almeida Revista e Corrigida 1969 | pt | — |

---

### Grego & Léxico (Layer 2) ✅ com pendências LLM

#### Palavras Interlineares
| Métrica | Valor |
|---------|-------|
| Total palavras | **141.522** |
| Com gloss inglês | 141.522 (100%) |
| Com morfologia | 141.522 (100%) |
| Com Strong's | 141.522 (100%) |
| Com gloss português | **0** ⏳ pendente LLM |
| Com gloss espanhol | **0** ⏳ pendente LLM |
| Idioma | Somente grego (NT) |

> ⚠️ `bible_ingest_ot_interlinear` executou com 0 itens — o interlinear do AT (hebraico) **não foi ingerido**. O hebraico existe apenas no léxico separado.

#### Léxico Grego
| Métrica | Valor |
|---------|-------|
| Total entradas | **10.847** |
| Com definição curta | 10.846 (99,99%) |
| Com definição completa | 10.847 (100%) |
| Com tradução KJV | 5.522 (51%) |
| `usage_count` preenchido | **0** — coluna vazia em todas as entradas |
| Traduções PT/ES (`greek_lexicon_translations`) | **0** ⏳ pendente LLM |

#### Léxico Hebraico
| Métrica | Valor |
|---------|-------|
| Total entradas | **8.696** |
| Com definição curta | 8.181 (94%) |
| Com definição completa | 8.181 (94%) |
| Traduções PT/ES (`hebrew_lexicon_translations`) | **0** ⏳ pendente LLM |

---

### Textos Bíblicos (Layer 3) ✅
| Métrica | Valor |
|---------|-------|
| Total textos de versículos | **124.342** |
| Cobertura esperada (4 versões × 31.088) | 124.352 |
| Diferença | −10 versículos (< 0,01%) — normal |

---

### Tokenização & Alinhamento (Layer 4) ❌ Não iniciada
| Tabela | Registros |
|--------|-----------|
| `bible_verse_tokens` | **0** |
| `word_alignments` | **0** |

> Dependências da Layer 4: Layers 1+2+3 (OK/KJV) + Layer 2 (ARC69). Pode ser iniciada.

---

## 2. Atlas DB (nt_coverage) — Estado das Tabelas

| Domínio | Tabela | Registros |
|---------|--------|-----------|
| Manuscritos | `manuscripts` | **473** (332 unciais + 141 papiros) |
| Manuscritos | `manuscript_verses` | **44.627** vínculos |
| Manuscritos | `verses` | **7.957** versículos do NT |
| Bíblia (NT) | `books` | **27** livros |
| Patrística | `church_fathers` | **35** Pais da Igreja |
| Patrística | `father_textual_statements` | **35** testemunhos |
| Patrística | `church_father_translations` | **70** (PT + ES por Pai) |
| Concílios | `councils` | **144** concílios |
| Concílios | `council_canons` | **77** cânones |
| Heresies | `heresies` | **27** heresias |
| Fontes | `sources` | **9** fontes externas |

> **Manuscritos:** Todos 473 têm tipo e datação preenchidos. A fase `manuscript_ingest` processou 57 de 473 via NTVMR (os restantes não têm imagens/dados digitalizados disponíveis no NTVMR).

---

## 3. Fases de Ingestão — Status Completo (45 fases)

Todas as 45 fases estão com status **`success`**.

### Fases Bíblicas Relevantes
| Fase | Items Processados | Duração |
|------|-------------------|---------|
| `bible_seed_versions` | 4 | 0s |
| `bible_seed_books` | 32.343 / 32.357 | 38s |
| `bible_ingest_nt_interlinear` | 2 (arquivos) | 120s |
| `bible_ingest_ot_interlinear` | **0** ⚠️ | 0s |
| `bible_ingest_greek_lexicon` | 11.035 | 8s |
| `bible_ingest_hebrew_lexicon` | 10.258 | 6s |
| `bible_enrich_greek_lexicon` | 5.624 | 41s |
| `bible_enrich_hebrew_lexicon` | 8.674 | 62s |
| `bible_fill_missing_hebrew` | 542 | 4s |
| `bible_ingest_text_kjv` | 66 (livros) | 57s |
| `bible_ingest_text_arc69` | 66 (livros) | 25s |
| `bible_ingest_text_aa` | 66 (livros) | 28s |
| `bible_ingest_text_acf` | 66 (livros) | 25s |
| `bible_translate_lexicon` | 21.694 (enfileirado) | 4s |
| `bible_translate_hebrew_lexicon` | 16.362 (enfileirado) | 3s |
| `bible_translate_glosses` | 189.302 (enfileirado) | 3s |
| `bible_translate_enrichment_greek` | 11.046 (enfileirado) | 22s |
| `bible_translate_enrichment_hebrew` | 17.294 (enfileirado) | 31s |

> As fases `bible_translate_*` marcam `success` após **enfileirar** os prompts na `llm_prompt_queue`. O processamento LLM ocorre de forma assíncrona.

### Outras Fases — Resumo
| Domínio | Fases | Status |
|---------|-------|--------|
| Patrística (seed, translate, dating) | 6 fases | ✅ success |
| Concílios (seed, extract, consensus, translate) | 15 fases | ✅ success |
| Manuscritos (seed, ingest, coverage, dating) | 4 fases | ✅ success |
| Heresias (translate) | 1 fase | ✅ success |

---

## 4. Fila LLM — Estado Atual

### Visão Geral
| Status | Itens | Fases |
|--------|-------|-------|
| `applied` | **405** | 7 fases (bio, council, dating, heresy, apologetics) |
| `completed` | **80** | bible_translate_glosses ⚠️ |
| `processing` | **280** | bible_translate_glosses (ativo agora) |
| `pending` | **31.092** | 5 fases |
| **Total** | **31.857** | |

### Detalhamento dos Pendentes
| Fase | Pendentes | Descrição |
|------|-----------|-----------|
| `bible_translate_enrichment_greek` | 11.046 | Traduzir campos do léxico grego (KJV, origin, exhaustive) |
| `bible_translate_enrichment_hebrew` | 17.294 | Traduzir campos do léxico hebraico |
| `bible_translate_glosses` | 2.274 | Glossas PT/ES das palavras interlineares |
| `bible_translate_lexicon` | 272 | Traduções curta+longa do léxico grego |
| `bible_translate_hebrew_lexicon` | 206 | Traduções curta+longa do léxico hebraico |

### Itens `applied` já concluídos
| Fase | Itens | Período |
|------|-------|---------|
| `bio_translate_prepare` | 70 | 10/04 19h–21h |
| `council_translate_prepare` | 194 | 10/04 21h–21h48 |
| `dating_manuscripts_prepare` | 86 | 10/04 18h51–19h13 |
| `dating_fathers_prepare` | 35 | 10/04 19h21–19h40 |
| `heresy_translate_prepare` | 18 | 10/04 19h23–20h46 |
| `apologetics_generate_topic` | 1 | 10/04 04h43 |
| `apologetics_complement_response` | 1 | 10/04 04h45 |

### Modelo em uso atual
- **`claude-haiku-4-5`** para `bible_translate_glosses` (PT/ES)
- Padrão de lote: POST `/admin/llm/queue/{id}/complete` a cada ~5 itens

---

## 5. Achados e Pontos de Atenção

### ⚠️ ATENÇÃO 1 — 80 itens `completed` aguardando `apply`
**Fase:** `bible_translate_glosses`  
**Conteúdo:** JSON com traduções PT e ES de glossas das palavras interlineares  
**Mais recente processado:** 2026-04-13 04:54:33 UTC  
**Causa:** O `LlmResponseProcessor.processCompleted()` precisa ser acionado para mover de `completed → applied` e gravar as glossas na tabela `interlinear_words`.  
**Ação:** Chamar o endpoint de apply ou aguardar o mecanismo automático (Kafka/scheduler) disparar.

---

### ⚠️ ATENÇÃO 2 — `greek_lexicon_translations` e `hebrew_lexicon_translations` vazias
Ambas as tabelas de traduções do léxico (PT/ES) estão com **0 registros**. Aguardam as fases:
- `bible_translate_lexicon` (272 pending)
- `bible_translate_hebrew_lexicon` (206 pending)
- `bible_translate_enrichment_greek` (11.046 pending)
- `bible_translate_enrichment_hebrew` (17.294 pending)

---

### ⚠️ ATENÇÃO 3 — `bible_ingest_ot_interlinear` com 0 itens
O interlinear hebraico (AT) executou sem processar nada. A tabela `interlinear_words` contém **apenas palavras gregas** (NT). O hebraico existe somente no léxico (`hebrew_lexicon`). Verificar se o arquivo fonte do interlinear AT está disponível.

---

### ⚠️ ATENÇÃO 4 — `usage_count` do léxico grego zerado
Coluna `greek_lexicon.usage_count` está `0` em todas as 10.847 entradas. Se a intenção é contar quantas vezes cada palavra Strong's aparece no NT interlinear, esse cálculo não foi executado.

---

### ✅ OK — Dados consistentes
- Layer 3 com cobertura ~99,99% (déficit de 10 textos em 124.352 esperados)
- 44.627 vínculos manuscrito-versículo íntegros
- Todos os 473 manuscritos com tipo e datação
- Todos os 35 Pais da Igreja com traduções PT e ES
- 144 concílios com consenso aplicado
- 27 heresias traduzidas

---

## 6. Histórico de Uso LLM (llm_usage_logs)

| Modelo | Chamadas | Tokens Entrada | Tokens Saída |
|--------|----------|----------------|--------------|
| openclaw:sonnet-ingest | 131.404 | — | — |
| openclaw | 95.138 | — | — |
| openclaw:opus-ingest | 65.380 | — | — |
| openai/gpt-4o-mini | 28.763 | 4.549.501 | 4.795.462 |
| gpt-4o-mini | 21.590 | 6.117.407 | 3.989.904 |
| deepseek-chat | 8.735 | 1.530.297 | 799.251 |
| gpt-5.4 | 5.870 | **10.993.916** | 5.089.243 |
| claude-haiku-4-5 | ativo agora | — | — |

> Os tokens do gpt-5.4 são os mais altos por chamada (~1.873 tokens/entrada). Os modelos `openclaw` são registros do sistema legado (tokens zerados = não rastreados).

---

## 7. Próximos Passos Recomendados

| Prioridade | Ação |
|-----------|------|
| 🔴 Alta | Verificar por que 80 itens `completed` não foram aplicados — acionar `processCompleted("bible_translate_glosses")` |
| 🟡 Média | Aguardar/monitorar conclusão das 5 fases pending (31.092 itens) |
| 🟡 Média | Após glossas aplicadas, verificar `interlinear_words` com `portuguese_gloss` e `spanish_gloss` preenchidos |
| 🟢 Baixa | Após Layer 2 completa, iniciar **Layer 4** (Tokenização ARC69+KJV → `bible_tokenize_arc69`, `bible_tokenize_kjv`) |
| 🟢 Baixa | Investigar `bible_ingest_ot_interlinear` (arquivo AT hebraico disponível?) |
| 🟢 Baixa | Calcular `usage_count` do léxico grego baseado na frequência no interlinear |

---

*Gerado automaticamente via queries diretas no PostgreSQL — deploy-postgres-1*
