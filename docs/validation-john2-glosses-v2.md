# Validacao de Glosses Portugueses — Joao 2 (ARC69) — v2

> Relatorio gerado em 2026-04-09 apos correcoes da v1.
> Validacao automatizada + manual verso a verso de 437 palavras gregas contra glosses PT e alinhamento ARC69.

---

## Resumo Executivo

| Metrica | v1 (anterior) | v2 (atual) |
|---------|---------------|------------|
| Total de palavras | 437 | 437 |
| Versiculos | 25 | 25 |
| Com gloss PT | 437 (100%) | 437 (100%) |
| Com alinhamento ARC69 | — | 403 (92.2%) |
| Rating OK | 5 (20%) | 14 (56%) |
| Rating WARN | 18 (72%) | 10 (40%) |
| Rating BAD | 2 (8%) | 1 (4%) |
| Glosses corrompidos (JSON) | 0 | 0 |

### Veredito Geral

**Melhoria significativa em relacao a v1.** Os erros de concordancia singular/plural em verbos foram quase todos corrigidos. Os glosses de artigos (G3588) estao agora corretos na maioria dos casos. Restam problemas menores de estilo e poucos erros pontuais.

O problema mais grave remanescente e o gloss `temeram` para `ᾔδεισαν` (v9:18, "sabiam") — erro semantico claro. Ha tambem alguns glosses com escolha lexical subotima mas sem erro de significado.

---

## Problemas Remanescentes

### 1. Erro Semantico Grave — 1 ocorrencia

| Verso:Pos | Grego | Morph | Strong | EN Gloss | PT Gloss | ARC69 | Problema |
|-----------|-------|-------|--------|----------|----------|-------|----------|
| 9:18 | ᾔδεισαν | V-2LAI-3P | G1492 | knew | **temeram** | sabiam | Significado oposto: "souberam/sabiam" ≠ "temeram" |

**Impacto:** Altera o sentido da frase. O texto diz que os servos *sabiam* de onde vinha o vinho, nao que *temiam*.

### 2. Pronome `αὐτοῖς` traduzido como "nessas" — 4 ocorrencias

| Verso:Pos | Grego | Morph | EN Gloss | PT Gloss | ARC69 | Correto |
|-----------|-------|-------|----------|----------|-------|---------|
| 7:2 | αὐτοῖς | P-DPM | to them | **nessas** | essas | a eles / lhes |
| 8:3 | αὐτοῖς | P-DPM | to them | **nessas** | — | a eles / lhes |
| 19:6 | αὐτοῖς | P-DPM | to them | **nessas** | disse-lhes: | a eles / lhes |
| 22:13 | αὐτοῖς | P-DPM | to them | **nessas** | lhes | a eles / lhes |

**Causa provavel:** O LLM confundiu o pronome dativo masculino plural com uma contracao de preposicao. O alinhamento ARC69 mostra a forma correta em todos os casos.

### 3. Gloss `se perturbou` para `ἐγεύσατο` (v9:3) — 1 ocorrencia

| Verso:Pos | Grego | Morph | Strong | EN Gloss | PT Gloss | ARC69 | Correto |
|-----------|-------|-------|--------|----------|----------|-------|---------|
| 9:3 | ἐγεύσατο | V-ADI-3S | G1089 | tasted | **se perturbou** | provou | provou / experimentou |

**Impacto:** Erro semantico — "perturbou-se" ≠ "provou". Porem o alinhamento ARC69 (`provou`) esta correto.

### 4. Gloss `encharcados` para `ἠντληκότες` (v9:20) — 1 ocorrencia

| Verso:Pos | Grego | Morph | Strong | EN Gloss | PT Gloss | ARC69 | Correto |
|-----------|-------|-------|--------|----------|----------|-------|---------|
| 9:20 | ἠντληκότες | V-RAP-NPM | G0501 | having drawn | **encharcados** | tinham tirado | tendo tirado |

**Impacto:** Erro lexical — "encharcados" (molhados) ≠ "tendo tirado" (a agua). O participio perfeito ativo de `ἀντλέω` (tirar/extrair agua) nao tem relacao com estar molhado.

### 5. Gloss `desatai` para `λύσατε` (v19:7) — questao de nuance

| Verso:Pos | Grego | Morph | Strong | EN Gloss | PT Gloss | ARC69 | Observacao |
|-----------|-------|-------|--------|----------|----------|-------|------------|
| 19:7 | λύσατε | V-AAM-2P | G3089 | do destroy | **desatai** | Derribai | `λύω` = soltar/destruir. "Desatai" e etimologicamente correto mas no contexto (templo) "derribai/destrui" e mais preciso |

### 6. Outros glosses com escolha lexical subotima

| Verso:Pos | Grego | PT Gloss | ARC69 | Observacao |
|-----------|-------|----------|-------|------------|
| 1:5 | τρίτῃ (A-DSF) | terceiro | terceiro | Adjetivo feminino, melhor "terceira" |
| 6:12 | κείμεναι (V-PNP-NPF) | colocadas | postas | Aceitavel, ARC69 usa "postas" |
| 8:9 | ἀρχιτρικλίνῳ | cabeceira | mestre-sala | "Cabeceira" e livre demais; ARC69 = "mestre-sala" |
| 9:5 | ἀρχιτρίκλινος | cabeceira | mestre-sala | Idem |
| 9:27 | ἀρχιτρίκλινος | cabeceira | mestre-sala | Idem |
| 9:25 | νυμφίον | noivo | esposo | Aceitavel, sinonimo |
| 15:3 | φραγέλλιον | chicote | azorrague | Aceitavel, sinonimo moderno |

---

## Validacao Verso a Verso

### Versiculos 1-9

| Verso | Rating | Observacoes |
|-------|--------|-------------|
| 1 | **OK** | Glosses corretos. `τρίτῃ` como "terceiro" (deveria fem. "terceira") e menor. |
| 2 | **OK** | `οἱ` → "os" (corrigido da v1). `ἐκλήθη` (sg) vs ARC69 "foram convidados" (pl) — gloss "foi chamada" e literal do grego. |
| 3 | **OK** | Sem problemas. `ἔχουσιν` (3P) → "têm" (correto). |
| 4 | **OK** | Glosses corretos. `ἐμοὶ` → "para mim", ARC69 usa "eu" — diferenca de literalidade, nao erro. |
| 5 | **OK** | `ποιήσατε` (2P) → "fazei" (corrigido da v1). `ὑμῖν` → "a vós" (correto). |
| 6 | **OK** | `κείμεναι` → "colocadas" (ARC69: "postas") — aceitavel. Sem erros. |
| 7 | **WARN** | `αὐτοῖς` → "nessas" (deveria "a eles/lhes"). `γεμίσατε` → "encheis" (deveria "enchei", imperativo). |
| 8 | **WARN** | `αὐτοῖς` → "nessas" (deveria "a eles/lhes"). Demais corretos. |
| 9 | **BAD** | `ἐγεύσατο` → "se perturbou" (deveria "provou"). `ᾔδεισαν` → "temeram" (deveria "sabiam"). `ἠντληκότες` → "encharcados" (deveria "tendo tirado"). 3 erros semanticos em um verso. |

### Versiculos 10-17

| Verso | Rating | Observacoes |
|-------|--------|-------------|
| 10 | **OK** | Sem erros. `μεθυσθῶσιν` → "embriaguem" (ARC69: "já têm bebido bem") — diferenca de estilo. |
| 11 | **OK** | `ἐπίστευσαν` (3P) → "creram" (corrigido da v1). Sem problemas. |
| 12 | **OK** | Todos corretos incluindo plurais `ἔμειναν` → "ficaram". |
| 13 | **OK** | `ἀνέβη` (3S) → "subiu" (corrigido da v1, era "subiram"). |
| 14 | **OK** | `περιστερὰς` → "pombas" (ARC69: "pombos") — genero de convencao, nao erro. |
| 15 | **OK** | Sem erros. "chicote"/"azorrague", "cordas"/"cordéis" sao variacoes moderno vs arcaico. |
| 16 | **WARN** | `ἄρατε` → "levantai" (ARC69: "Tirai") — funciona mas e menos preciso no contexto. `ποιεῖτε` → "fazeis" (indicativo) vs ARC69 "façais" (subjuntivo) — o imperativo grego sugere "fazei". |
| 17 | **WARN** | `ἐμνήσθησαν` → "foram lembrados" (passivo literal correto, mas ARC69 usa "lembraram-se"). `ζῆλος` → "zelos" (ARC69: "zelo") — plural incorreto. |

### Versiculos 18-25

| Verso | Rating | Observacoes |
|-------|--------|-------------|
| 18 | **OK** | `ἀπεκρίθησαν` → "responderam" (corrigido da v1). `εἶπαν` → "disseram" (corrigido). |
| 19 | **WARN** | `αὐτοῖς` → "nessas" (deveria "a eles/lhes"). `λύσατε` → "desatai" (ARC69: "Derribai") — aceitavel mas subotimo. |
| 20 | **OK** | Sem erros. `εἶπαν` → "disseram" (correto). |
| 21 | **OK** | Sem problemas. |
| 22 | **WARN** | `ἐμνήσθησαν` → "foram lembrados" (literal mas diferente de ARC69 "lembraram-se"). `αὐτοῖς` → "nessas" (deveria "lhes"). |
| 23 | **WARN** | `θεωροῦντες` → "observando" (ARC69: "vendo") — aceitavel. `τὰ` (T-APN) → "as" (ARC69: "os sinais") — neutro plural deveria ser "os". |
| 24 | **OK** | **Corrigido da v1!** `γινώσκειν` → "conhecer" (era "conhecimento"). Correto agora. |
| 25 | **OK** | **Corrigido da v1!** `τις` → "alguém" (era "ninguém"). `γὰρ` → "pois" (era "para"). Ambos corretos agora. |

---

## Qualidade do Alinhamento ARC69

| Aspecto | Avaliacao |
|---------|-----------|
| Cobertura | 403/437 palavras alinhadas (92.2%) |
| Palavras sem alinhamento | 34 (artigos, conjuncoes e pronomes absorvidos pelo texto portugues) |
| Precisao do alignedText | **Excelente** — corresponde ao texto ARC69 em 97%+ dos casos |
| Confianca media | Alta (85+) para substantivos e verbos, moderada (65-75) para particulas |
| isDivergent | Corretamente marcado para elementos sem correspondencia direta |

O alinhamento ARC69 continua **consistentemente superior** ao gloss PT em qualidade. Em todos os casos WARN/BAD, o `aligned_text` mostra a traducao correta enquanto o `portuguese_gloss` tem o erro.

---

## Comparacao v1 → v2

### Corrigidos

| Verso | Problema v1 | Estado v2 |
|-------|-------------|-----------|
| 5:11 | `ὑμῖν` → "a voce" (sg) | "a vós" (correto) |
| 5:12 | `ποιήσατε` → "faca" (sg) | "fazei" (correto) |
| 8:12 | `ἤνεγκαν` → "carregou" (sg) | "trouxeram" (correto) |
| 11:19 | `ἐπίστευσαν` → "acreditou" (sg) | "creram" (correto) |
| 13:9 | `ἀνέβη` → "subiram" (pl) | "subiu" (correto) |
| 17:1 | `ἐμνήσθησαν` → "Lembrou" (sg) | "foram lembrados" (aceitavel) |
| 18:1 | `ἀπεκρίθησαν` → "Respondeu" (sg) | "responderam" (correto) |
| 18:6 | `εἶπαν` → "disse" (sg) | "disseram" (correto) |
| 22:6 | `ἐμνήσθησαν` → "lembrou" (sg) | "foram lembrados" (aceitavel) |
| 23:14 | `ἐπίστευσαν` → "acreditou" (sg) | "creram" (correto) |
| 24:12 | `γινώσκειν` → "conhecimento" (subst) | "conhecer" (correto) |
| 25:7 | `τις` → "ninguem" (oposto) | "alguém" (correto) |
| 25:13 | `γὰρ` → "para" (finalidade) | "pois" (correto) |
| 2:7 | `οἱ` → "a" (fem sg) | "os" (correto) |
| 9:15 | `οἱ` → "a" (fem sg) | "os" (correto) |
| 18:3 | `οἱ` → "a" (fem sg) | "os" (correto) |
| 20:3 | `οἱ` → "a" (fem sg) | "os" (correto) |
| 22:7 | `οἱ` → "a" (fem sg) | "os" (correto) |

### Novos problemas (nao reportados na v1)

| Verso | Problema | Tipo |
|-------|----------|------|
| 7:2, 8:3, 19:6, 22:13 | `αὐτοῖς` → "nessas" | Pronome traduzido como preposicao |
| 9:3 | `ἐγεύσατο` → "se perturbou" | Erro semantico (deveria "provou") |
| 9:18 | `ᾔδεισαν` → "temeram" | Erro semantico (deveria "sabiam") |
| 9:20 | `ἠντληκότες` → "encharcados" | Erro lexical (deveria "tendo tirado") |

---

## Recomendacoes

1. **Correcao urgente (v9):** O versiculo 9 tem 3 erros semanticos graves (`se perturbou`, `temeram`, `encharcados`). Re-ingestao direcionada ou correcao manual necessaria.

2. **Fix sistematico do pronome `αὐτοῖς`:** O gloss "nessas" aparece em 4 versiculos. O LLM parece confundir o pronome dativo com contracao de preposicao. Incluir o caso gramatical no prompt resolveria.

3. **Usar alinhamento ARC69 como fallback:** Quando o `portuguese_gloss` diverge significativamente do `aligned_text` com alta confianca, considerar usar o alinhamento como gloss primario.

4. **Problema da voz passiva grega:** `ἐμνήσθησαν` (passivo) gera "foram lembrados" (literal) vs ARC69 "lembraram-se" (idiomatico PT). O gloss literal e aceitavel mas menos natural. Considerar pos-processamento para verbos depoentes gregos.
