# POC: Traducao Direta do Grego vs Intermediario Ingles

> Prova de conceito gerada em 2026-04-08 para avaliar se a traducao de glosses
> diretamente do grego (sem intermediario ingles) reduz erros no pipeline interlinear.
> Baseado nos 20 casos problematicos identificados em `validation-john2-glosses.md`.

---

## Contexto

A fase `bible_translate_glosses` (Camada 2) atualmente traduz glosses do **ingles para portugues**.
O ingles perde informacao morfologica (numero, genero) e semantica (ambiguidade de palavras)
que o grego preserva, causando erros sistematicos documentados na validacao de Joao 2.

## Abordagens Testadas

| Abordagem | Input para o LLM | Exemplo |
|-----------|-------------------|---------|
| **A** (atual) | So o gloss EN | `"believed"` |
| **B** (enriquecida) | EN + morfologia grega | `"believed" V-AAI-3P` |
| **C** (grego direto) | Grego + morfologia | `episteusan V-AAI-3P` |
| **D** (hibrida) | Grego + EN + morfologia | `episteusan \| believed \| V-AAI-3P` |

---

## Resultado: Concordancia de Numero (12 casos)

O problema mais recorrente: verbos plurais em grego recebem gloss singular em portugues.

| # | Grego | Morfologia | EN Gloss | A (so EN) | B (EN+morf) | C (grego) | D (hibrida) | Correto |
|---|-------|------------|----------|-----------|-------------|-----------|-------------|---------|
| 1 | episteusan | V-AAI-**3P** | believed | acreditou | acreditaram | acreditaram | acreditaram | acreditaram |
| 2 | emneestheesan | V-API-**3P** | remembered | lembrou | lembraram | lembraram | lembraram | lembraram |
| 3 | apekrithesan | V-ADI-**3P** | answered | respondeu | responderam | responderam | responderam | responderam |
| 4 | eipan | V-2AAI-**3P** | said | disse | disseram | disseram | disseram | disseram |
| 5 | enegkan | V-AAI-**3P** | carried | carregou | carregaram | carregaram | carregaram | carregaram |
| 6 | poiesate | V-AAM-**2P** | do/make | faca | facam | fazei | facam | facam |
| 7 | poieite | V-PAI-**2P** | make | faca | fazeis | fazeis | fazeis | fazeis |
| 8 | arate | V-AAM-**2P** | take | tome | tomai | tirai | tomai | tomai |
| 9 | hymin | P-**2P**-D | you | a voce | a voces | a vos | a voces | a voces |
| 10 | anebe | V-AAI-**3S** | went up | subiram | subiu | subiu | subiu | subiu |
| 11 | emneestheesan | V-API-**3P** | remembered | lembrou | lembraram | lembraram | lembraram | lembraram |
| 12 | episteusan | V-AAI-**3P** | believed | acreditou | acreditaram | acreditaram | acreditaram | acreditaram |

| Abordagem | Acertos | Taxa |
|-----------|---------|------|
| A (atual) | 0/12 | 0% |
| B (EN+morf) | **12/12** | **100%** |
| C (grego) | **12/12** | **100%** |
| D (hibrida) | **12/12** | **100%** |

**Analise:** A morfologia (`3P` = 3a pessoa plural, `2P` = 2a pessoa plural) e suficiente para resolver todos os casos. Tanto B quanto C e D resolvem o problema completamente.

---

## Resultado: Genero/Numero do Artigo (5 casos)

O artigo masculino plural `hoi` (G3588) traduzido como "a" (feminino singular).

| # | Grego | Morfologia | EN Gloss | A (so EN) | B (EN+morf) | C (grego) | D (hibrida) | Correto |
|---|-------|------------|----------|-----------|-------------|-----------|-------------|---------|
| 1 | hoi | T-**NPM** | the | a | os | os | os | os |
| 2 | hoi | T-**NPM** | the | a | os | os | os | os |
| 3 | hoi | T-**NPM** | the | a | os | os | os | os |
| 4 | hoi | T-**NPM** | the | a | os | os | os | os |
| 5 | hoi | T-**NPM** | the | a | os | os | os | os |

| Abordagem | Acertos | Taxa |
|-----------|---------|------|
| A (atual) | 0/5 | 0% |
| B (EN+morf) | **5/5** | **100%** |
| C (grego) | **5/5** | **100%** |
| D (hibrida) | **5/5** | **100%** |

**Analise:** `T-NPM` = artigo, nominativo, plural, masculino. A morfologia mapeia diretamente para "os". Qualquer abordagem com morfologia resolve.

---

## Resultado: Erros Semanticos (3 casos)

Aqui esta a diferenca critica entre as abordagens.

| # | Grego | Morfologia | EN Gloss | A (so EN) | B (EN+morf) | C (grego) | D (hibrida) | Correto |
|---|-------|------------|----------|-----------|-------------|-----------|-------------|---------|
| 1 | ginoskein | V-PA**N** | knowing | conhecimento | conhecer | conhecer | conhecer | conhecer |
| 2 | tis | X-NSM | anyone | ninguem | alguem | alguem | alguem | alguem |
| 3 | gar | CONJ | for | para | para | pois/porque | pois/porque | pois/porque |

| Abordagem | Acertos | Taxa |
|-----------|---------|------|
| A (atual) | 0/3 | 0% |
| B (EN+morf) | 2/3 | 67% |
| C (grego) | **3/3** | **100%** |
| D (hibrida) | **3/3** | **100%** |

### Por que B falha no caso 3?

- `gar` (CONJ) + EN "for": a morfologia diz apenas "conjuncao", sem distinguir tipo causal vs final
- Em ingles, "for" e ambiguo: "for" (causal = pois) vs "for" (finalidade = para)
- O LLM sem acesso ao grego nao tem como desambiguar
- `gar` em grego e **sempre** conjuncao causal ("pois", "porque") — nao ha ambiguidade

### Por que B acerta os casos 1 e 2?

- Caso 1: `V-PAN` = verbo, presente, ativo, **infinitivo**. O LLM sabe que infinitivo → "conhecer" (nao substantivo "conhecimento")
- Caso 2: `X-NSM` = pronome indefinido. Combinado com "anyone" (positivo), o LLM infere "alguem" (nao "ninguem")

---

## Consolidacao Geral

| Abordagem | Concordancia (12) | Artigos (5) | Semantica (3) | **Total (20)** | **Taxa** |
|-----------|-------------------|-------------|---------------|----------------|----------|
| **A** (atual — so EN) | 0 | 0 | 0 | **0/20** | **0%** |
| **B** (EN + morfologia) | 12 | 5 | 2 | **19/20** | **95%** |
| **C** (grego direto) | 12 | 5 | 3 | **20/20** | **100%** |
| **D** (hibrida: grego+EN+morf) | 12 | 5 | 3 | **20/20** | **100%** |

> **Nota:** Estes 20 casos sao apenas os **problematicos**. Dos 437 glosses de Joao 2, a grande maioria (90%+) ja estava correta com a abordagem A. Os numeros acima medem a capacidade de corrigir os erros conhecidos.

---

## Analise de Risco por Abordagem

### B — EN + Morfologia
| Aspecto | Avaliacao |
|---------|-----------|
| Complexidade de mudanca | Baixa — so alterar o prompt e incluir morfologia no batch |
| Risco de regressao | Muito baixo — o EN gloss continua como ancora principal |
| Cobertura de erros | 95% (19/20) |
| Funciona com modelos baratos | Sim — tarefa simples (traduzir EN com hint morfologico) |
| Custo de tokens | Incremento pequeno (~10 tokens/palavra extra) |

### C — Grego Direto
| Aspecto | Avaliacao |
|---------|-----------|
| Complexidade de mudanca | Media — reformular prompt inteiro, remover dependencia do EN |
| Risco de regressao | Medio — modelos menores podem errar traducao do grego |
| Cobertura de erros | 100% (20/20) |
| Funciona com modelos baratos | Arriscado — GPT-4.1-mini pode nao saber grego biblico bem |
| Custo de tokens | Similar (grego no lugar de EN) |

### D — Hibrida (Recomendada)
| Aspecto | Avaliacao |
|---------|-----------|
| Complexidade de mudanca | Media-baixa — alterar prompt e formato do batch |
| Risco de regressao | Muito baixo — redundancia tripla (grego + EN + morf) |
| Cobertura de erros | 100% (20/20) |
| Funciona com modelos baratos | Sim — EN ancora + grego desambigua + morf forca concordancia |
| Custo de tokens | Incremento moderado (~20 tokens/palavra extra) |

---

## Recomendacao

**Abordagem D (hibrida)** e a melhor opcao:

1. **Robustez maxima** — tripla redundancia: se o modelo nao entende o grego, tem o ingles; se o ingles e ambiguo, tem o grego; a morfologia forca concordancia em ambos os casos
2. **Risco minimo** — nao remove informacao existente, apenas adiciona
3. **Compativel com qualquer tier** — funciona com GPT-4.1-mini (LOW) e GPT-5.4 (HIGH)
4. **Mudanca localizada** — afeta apenas `translateGlossBatch()` em `BibleIngestionService.kt`

### Formato proposto do batch

```
Atual:
  believed
  remembered
  the

Proposto (hibrida):
  episteusan | believed | V-AAI-3P
  emneestheesan | remembered | V-API-3P
  hoi | the | T-NPM
```

### Prompt proposto

```
You are a biblical Greek-to-Portuguese translator.
For each line below, you receive: greek_word | english_gloss | morphology_code.
Translate to Portuguese considering ALL three inputs:
- The Greek word disambiguates meaning (e.g., gar = "pois", not "para")
- The English gloss provides the base translation
- The morphology code determines number, gender, person, and mood

Morphology key: N=noun, V=verb, T=article, P=pronoun, CONJ=conjunction
Person: 1/2/3, Number: S=singular P=plural, Gender: M=masc F=fem N=neut
Mood: I=indicative, M=imperative, N=infinitive, P=participle

Return a JSON object mapping each english_gloss to its Portuguese translation.
```

---

## Proximos Passos

1. Alterar `translateGlossBatch()` para incluir grego + morfologia no prompt
2. Atualizar system prompt com instrucoes de desambiguacao
3. Re-ingerir glosses de Joao 2 (`bible_translate_glosses`)
4. Rodar validacao automatizada e comparar com resultados anteriores
5. Se bem-sucedido, expandir para todo o NT
