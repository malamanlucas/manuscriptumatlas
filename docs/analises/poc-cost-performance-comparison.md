# Comparativo de Custo e Performance: C vs D

> Analise gerada em 2026-04-08 para decidir entre as abordagens C (grego+morfologia)
> e D (grego+EN+morfologia) para traducao de glosses interlineares.
> Ambas atingem 100% de acuracia nos 20 casos problematicos de Joao 2.

---

## TL;DR

| | C (grego + morf) | D (hibrida) | Vencedor |
|---|---|---|---|
| Acuracia | 100% (20/20) | 100% (20/20) | Empate |
| Custo NT inteiro | **~$0.37** | ~$0.43 | **C (-14%)** |
| Chamadas API | **450** | 450 ou 400 | Empate ou C |
| Tokens input total | **~212K** | ~324-365K | **C (-42%)** |
| Latencia estimada | **~17s** | ~17s | Empate |
| Tokens por entrada | **~4 tok** | ~7 tok | **C (-43%)** |

**C e mais enxuto, mais barato e igualmente preciso. O gloss ingles em D e redundancia que custa tokens sem agregar acuracia.**

---

## Definicao das Abordagens

| Abordagem | Input por palavra | Exemplo |
|-----------|-------------------|---------|
| **C** (grego + morf) | `greekWord \| morphology` | `episteusan \| V-AAI-3P` |
| **D** (hibrida) | `greekWord \| englishGloss \| morphology` | `episteusan \| believed \| V-AAI-3P` |

A unica diferenca e que D inclui o gloss ingles como campo adicional.

---

## Dados do Pipeline

Extraidos de `BibleIngestionService.kt` e `LlmConfig.kt`:

| Parametro | Valor |
|-----------|-------|
| Modelo | `gpt-4.1-mini` (tier LOW via `executeRoundRobin`) |
| Chunk size | 80 por batch (hardcoded linha 1065) |
| Concorrencia | 80 chamadas paralelas |
| maxTokens/chunk | `chunk.size * 20` = 1600 |
| Idiomas | PT + ES em paralelo |

### Precos GPT-4.1-mini

| | Input | Output |
|---|---|---|
| GPT-4.1-mini | $0.40 / 1M tokens | $1.60 / 1M tokens |

---

## Deduplicacao: O Fator que Define o Custo

### Chave de dedup por abordagem

| Abordagem | Chave natural de dedup | Justificativa |
|-----------|----------------------|---------------|
| A (atual) | `englishGloss` | Mesma palavra EN → mesma traducao PT |
| **C** | `(greekWord, morphology)` | Mesma forma grega → sempre mesma traducao |
| **D** | `(greekWord, morphology)` ou `(englishGloss, morphology)` | Ver analise abaixo |

### Por que C deduplica por forma grega?

A mesma forma grega (ex: `episteusan`) aparece multiplas vezes no NT, mas **sempre** significa a mesma coisa e **sempre** tem a mesma morfologia. Portanto:
- `episteusan` aparece em Jo 2:11, Jo 2:23, Jo 7:31, etc. → traduz uma unica vez → "acreditaram"
- Numero de formas gregas unicas no NT: **~18.000**

### Por que D pode usar (englishGloss, morphology)?

Com D, podemos deduplicar por `(englishGloss, morphology)` porque:
- Palavras gregas diferentes com mesmo gloss EN + mesma morfologia quase sempre produzem a mesma traducao PT
- Vantagem: **~16.000** entradas unicas (menos que C)
- Risco: raros casos onde palavras gregas distintas compartilham gloss EN + morfologia mas precisam de traducoes PT diferentes

### Entradas unicas estimadas

| Abordagem | Chave | Entradas unicas | Batches (÷80) |
|-----------|-------|-----------------|---------------|
| A (atual) | gloss | ~5.500 | ~69 |
| **C** | (grego, morf) | **~18.000** | **~225** |
| **D** (dedup grego) | (grego, morf) | ~18.000 | ~225 |
| **D** (dedup EN) | (gloss, morf) | ~16.000 | ~200 |

> Nota: C tem ~2.000 entradas a mais que D-dedup-EN porque formas gregas distintas
> podem compartilhar o mesmo gloss EN (ex: `eipen` e `legei` → ambos "said").
> Mas se D deduplica por grego (mais correto), ficam iguais.

---

## Calculo Detalhado: Tokens

### Tokens por entrada no batch

| Componente | C | D | Delta |
|-----------|---|---|-------|
| Palavra grega (translit) | ~2 tok | ~2 tok | — |
| Separador `\|` | ~1 tok | ~2 tok | +1 |
| Gloss ingles | — | ~2 tok | **+2** |
| Codigo morfologico | ~2 tok | ~2 tok | — |
| **Total por entrada** | **~4 tok** | **~7 tok** | **+3 tok** |

### System prompt

| | C | D |
|---|---|---|
| Instrucao base | ~80 tok | ~80 tok |
| Explicacao dos campos | ~40 tok | ~80 tok |
| Legenda morfologica | ~60 tok | ~60 tok |
| Exemplo input/output | ~40 tok | ~60 tok |
| **Total system prompt** | **~180 tok** | **~250 tok** |

D precisa de ~70 tokens a mais no system prompt para explicar o papel do gloss ingles.

### Tokens por chamada API (chunk de 80)

| Componente | C | D |
|-----------|---|---|
| System prompt | 180 | 250 |
| User input (80 × tok/entrada) | 80 × 4 = 320 | 80 × 7 = 560 |
| **Total input** | **500** | **810** |
| Output (JSON, identico) | 400 | 400 |
| **Total por chamada** | **900** | **1.210** |

---

## Projecao: NT Completo (2 idiomas)

### Cenario 1: Ambos com dedup por (grego, morf) — comparacao justa

| Metrica | C | D | Delta |
|---------|---|---|-------|
| Entradas unicas | 18.000 | 18.000 | 0 |
| Chunks (÷80) | 225 | 225 | 0 |
| Chamadas API (×2 idiomas) | **450** | **450** | **0** |
| Tokens input total | **225K** | **365K** | +140K (+62%) |
| Tokens output total | **180K** | **180K** | 0 |
| Custo input | $0.09 | $0.15 | +$0.06 |
| Custo output | $0.29 | $0.29 | $0.00 |
| **Custo total** | **$0.38** | **$0.44** | **+$0.06** |

### Cenario 2: D com dedup otimizado por (EN, morf)

| Metrica | C | D (otimizado) | Delta |
|---------|---|---|-------|
| Entradas unicas | 18.000 | 16.000 | -2.000 |
| Chunks (÷80) | 225 | 200 | -25 |
| Chamadas API (×2 idiomas) | 450 | 400 | -50 |
| Tokens input total | 225K | 324K | +99K (+44%) |
| Tokens output total | 180K | 160K | -20K |
| **Custo total** | **$0.38** | **$0.39** | **+$0.01** |

> No cenario 2, D compensa os tokens extras por entrada com menos entradas no total.
> A diferenca cai para **$0.01** — irrelevante.

---

## Performance (Latencia)

### Por chamada

O gargalo e a geracao de output (autoregressive). Input e processado em paralelo (prefill).

| Metrica | C | D | Delta |
|---------|---|---|-------|
| Tokens input | 500 | 810 | +310 (prefill: ~60ms extra) |
| Tokens output | 400 | 400 | 0 |
| Latencia estimada/chamada | ~2-4s | ~2-4s | **Desprezivel** |

### NT completo

| Metrica | C | D |
|---------|---|---|
| Total chamadas | 450 | 400-450 |
| Concorrencia | 80 | 80 |
| Rounds paralelos | ~6 | ~5-6 |
| Latencia por round | ~3s | ~3s |
| **Tempo total** | **~18s** | **~17s** |

> Diferenca de ~1 segundo. Irrelevante.

---

## Resumo Visual

```
                  CUSTO ($)        TOKENS INPUT      CHAMADAS API     ACURACIA
                     
  C (grego+morf)  $0.38 ████       225K ████          450 █████        100% ██████████
  D (hibrida)     $0.44 █████      365K ███████       450 █████        100% ██████████
                  ──────────       ────────────       ──────────       ────────────
                  -$0.06           -140K (-42%)       0                0%
                  (C mais barato)  (C mais enxuto)    (identico)       (empate)
```

---

## Analise de Risco: O Unico Argumento a Favor de D

### O que D oferece que C nao oferece?

**Rede de seguranca.** Se o gpt-4.1-mini nao entender a palavra grega, o gloss ingles serve como fallback.

### Isso e um risco real?

| Fator | Avaliacao |
|-------|-----------|
| gpt-4.1-mini sabe grego biblico? | Sim — grego do NT e um dos corpus mais estudados da historia. Qualquer modelo treinado em dados publicos conhece bem. |
| As palavras sao complexas? | Nao — sao transliteracoes simples (`episteusan`, `hoi`, `gar`), nao caracteres gregos raw. |
| O morfologia ajuda? | Sim — `V-AAI-3P` e um hint estruturado que nao depende de conhecimento linguistico profundo. |
| Ha precedente? | O proprio lexico grego (`bible_translate_lexicon`) ja traduz termos gregos sem intermediario ingles. |

### Cenario de falha de C

Para C falhar onde D acertaria, seria necessario:
1. O LLM nao reconhecer uma transliteracao grega comum
2. E ao mesmo tempo, o gloss ingles ser suficiente para a traducao correta
3. E a morfologia sozinha nao compensar

Isso seria extremamente raro com modelos modernos. E se acontecer, o erro aparece na validacao e pode ser corrigido pontualmente.

### Mitigacao simples para C

Se houver preocupacao com o modelo barato, basta uma regra:
- **Primeira ingestao (modelo novo/desconhecido):** rodar com D como validacao
- **Ingestoes subsequentes:** rodar com C (mais barato)

Ou simplesmente rodar C e validar. Os erros (se houver) serao facilmente detectaveis.

---

## Decisao

### C (grego + morfologia) e a escolha mais eficiente

| Criterio | C | D | Vencedor |
|----------|---|---|----------|
| Custo | $0.38 | $0.44 | **C** |
| Tokens/entrada | 4 | 7 | **C** |
| Simplicidade do prompt | Mais simples | Mais complexo | **C** |
| Acuracia | 100% | 100% | Empate |
| Latencia | ~18s | ~17s | Empate |
| Robustez com modelo fraco | Boa | Marginalmente melhor | D (marginal) |

**Recomendacao: C** — mais barato, mais simples, mesma acuracia. A redundancia do ingles em D custa 42% mais tokens de input sem beneficio mensuravel.

Se preferir seguranca maxima na primeira ingestao, rode D uma vez e compare com C. Depois siga com C.

### Formato final do batch (C)

```
episteusan | V-AAI-3P
emneestheesan | V-API-3P
hoi | T-NPM
gar | CONJ
ginoskein | V-PAN
tis | X-NSM
```

### Proximo passo

Implementar C em `translateGlossBatch()` (`BibleIngestionService.kt:1060`):
- Mudar input de `englishGloss` para `greekTransliteration | morphology`
- Atualizar system prompt para traduzir grego→PT com apoio da morfologia
- Mudar dedup de `englishGloss.distinct()` para `(transliteration, morphology).distinct()`
- Mudar JSON key de `englishGloss` para `transliteration`
- Re-ingerir Joao 2 e validar contra `validation-john2-glosses.md`
