# Validacao de Glosses Portugueses — Joao 2 (ARC69)

> Relatorio gerado em 2026-04-09 apos re-ingestao dos glosses portugueses corrigidos.
> Validacao automatizada verso a verso de 437 palavras gregas contra seus glosses PT.

---

## Resumo Executivo

| Metrica | Valor |
|---------|-------|
| Total de palavras | 437 |
| Versiculos | 25 |
| Rating OK | 5 (20%) |
| Rating WARN | 18 (72%) |
| Rating BAD | 2 (8%) |
| Glosses corrompidos (JSON) | 0 (corrigidos) |

### Veredito Geral

Os glosses portugueses estao **semanticamente corretos na maioria** — as traducoes capturam o significado do grego. Os problemas encontrados sao majoritariamente de **concordancia gramatical** (numero singular/plural, genero do artigo), nao erros semanticos graves. Apenas 2 versiculos (24 e 25) possuem erros semanticos claros que alteram o significado.

---

## Problemas Sistematicos

### 1. Concordancia de Numero (Singular/Plural) — 12 ocorrencias

O problema mais recorrente: verbos plurais em grego recebem gloss singular em portugues (e vice-versa).

| Verso:Pos | Grego | Gloss PT (errado) | Correto | Tipo |
|-----------|-------|--------------------|---------|------|
| 5:11 | hymin (2pl dat) | "a voce" | "a voces/vos" | pronome sg→pl |
| 5:12 | poiesate (2pl imp) | "faca" | "facam/fazei" | verbo sg→pl |
| 8:12 | enegkan (3pl aor) | "carregou [isso]" | "carregaram" | verbo sg→pl |
| 11:19 | episteusan (3pl aor) | "acreditou" | "acreditaram" | verbo sg→pl |
| 13:9 | anebe (3sg aor) | "subiram" | "subiu" | verbo pl→sg |
| 16:7 | arate (2pl imp) | "tome" | "tomai/tirai" | verbo sg→pl |
| 16:11 | poieite (2pl pres) | "faca" | "facais/fazei" | verbo sg→pl |
| 17:1 | emneestheesan (3pl aor) | "Lembrou" | "Lembraram" | verbo sg→pl |
| 18:1 | apekrithesan (3pl aor) | "Respondeu" | "Responderam" | verbo sg→pl |
| 18:6 | eipan (3pl aor) | "disse" | "disseram" | verbo sg→pl |
| 22:6 | emneestheesan (3pl aor) | "lembrou" | "lembraram" | verbo sg→pl |
| 23:14 | episteusan (3pl aor) | "acreditou" | "acreditaram" | verbo sg→pl |

**Causa provavel:** O LLM traduz o gloss ingles (que nao marca numero: "believed", "remembered") sem consultar a morfologia grega.

### 2. Genero/Numero do Artigo — 5 ocorrencias

O artigo masculino plural `hoi` (G3588) e consistentemente traduzido como "a" (feminino singular) ao inves de "os".

| Verso:Pos | Grego | Gloss PT | Correto |
|-----------|-------|----------|---------|
| 2:7 | hoi | "a" | "os" |
| 9:15 | hoi | "a" | "os" |
| 18:3 | hoi | "a" | "os" |
| 20:3 | hoi | "a" | "os" |
| 22:7 | hoi | "a" | "os" |

**Causa provavel:** O ingles "the" nao carrega genero/numero. O LLM padroniza para "a" (feminino singular).

### 3. Erros Semanticos Graves — 2 ocorrencias

| Verso:Pos | Grego | Strong | EN Gloss | PT Gloss (errado) | Correto | Impacto |
|-----------|-------|--------|----------|--------------------|---------|----|
| 24:12 | ginoskein | G1097 | knowing | "conhecimento" (subst.) | "conhecer" (verbo) | Troca classe gramatical |
| 25:7 | tis | G5100 | anyone | "ninguem" | "alguem" | Inverte o significado |

- **24:12**: `ginoskein` e infinitivo ("conhecer"), mas o gloss usa o substantivo "conhecimento". A negacao vem de outro elemento.
- **25:7**: `tis` e pronome indefinido ("alguem/qualquer um"), mas o gloss diz "ninguem" — significado oposto.

Tambem em 25:13, `gar` (conjuncao causal = "pois/porque") foi traduzido como "para" (preposicao de finalidade).

---

## Validacao Verso a Verso

### Versiculos 1-9

| Verso | Rating | Observacoes |
|-------|--------|-------------|
| 1 | **OK** | Todos os glosses corretos. "la" vs "ali" e variante estilistica valida. |
| 2 | **WARN** | `hoi` → "a" (deveria "os"). Artigo masc. plural com gloss fem. singular. |
| 3 | **OK** | Sem problemas. |
| 4 | **WARN** | `mou` (genitivo possessivo) → "para mim" (deveria "minha"). |
| 5 | **WARN** | `hymin` singular; `poiesate` singular; `an` como "pode ser"; `ti` como "de qualquer forma". |
| 6 | **WARN** | `keimenai` → "em pe" (deveria "postas/colocadas"). `de` → "agora" (melhor "e/ora"). |
| 7 | **WARN** | `ano` → "[a] borda" (melhor "em cima/o alto"). Menor. |
| 8 | **WARN** | `enegkan` singular ("carregou") deveria ser plural ("carregaram"). `antlesate` → "puxem" (melhor "tirai"). |
| 9 | **WARN** | `hoi` → "a" (deveria "os"). `gegenemenon` como "tornou-se" (melhor passivo "feita/transformada"). |

### Versiculos 10-17

| Verso | Rating | Observacoes |
|-------|--------|-------------|
| 10 | **OK** | Sem erros significativos. Diferencas estilísticas menores. |
| 11 | **WARN** | `episteusan` → "acreditou" (deveria "acreditaram", plural). |
| 12 | **OK** | Sem problemas. |
| 13 | **WARN** | `anebe` → "subiram" (deveria "subiu", singular — sujeito e Jesus). |
| 14 | **WARN** | `peristeras` → "pombas" vs ARC69 "pombos". Convencao de genero menor. |
| 15 | **OK** | Diferencas sao moderno vs arcaico (chicote/azorrague, cordas/cordeis). |
| 16 | **WARN** | `arate` → "tome" e `poieite` → "faca" — ambos deveriam ser plural. |
| 17 | **WARN** | `emneestheesan` → "Lembrou" (singular, deveria "Lembraram"). `zelos` pode confundir com "ciumes" — melhor "zelo". `Teus` deveria ser "Tua" (concorda com "casa"). |

### Versiculos 18-25

| Verso | Rating | Observacoes |
|-------|--------|-------------|
| 18 | **WARN** | 3 erros de numero: "Respondeu"→"Responderam", "disse"→"disseram", "a"→"os". |
| 19 | **WARN** | `lusate` → "destroi" (singular, deveria plural "destrui"). Demonstrativos menores. |
| 20 | **WARN** | `hoi` → "a" (deveria "os"). `dos` deveria ser "do" (singular). |
| 21 | **WARN** | `tou` (genitivo singular) → "dos" (plural, deveria "do"). |
| 22 | **WARN** | `emneestheesan` → "lembrou" (singular→plural). `hoi` → "a" (genero errado). |
| 23 | **WARN** | `episteusan` → "acreditou" (singular→plural). |
| 24 | **BAD** | `ginoskein` (verbo) → "conhecimento" (substantivo). Classe gramatical errada. |
| 25 | **BAD** | `tis` → "ninguem" (inverte significado, deveria "alguem"). `gar` → "para" (deveria "pois/porque"). |

---

## Qualidade do Alinhamento (ARC69)

O `alignedText` (texto alinhado com ARC69) esta **consistentemente correto** e geralmente **superior** ao `portugueseGloss` em qualidade. Em quase todos os casos WARN/BAD, o alignedText mostra a traducao correta enquanto o gloss tem o erro.

| Aspecto | Avaliacao |
|---------|-----------|
| Indices de alinhamento (kjvIndices) | Corretos em todos os versiculos verificados |
| alignedText | Corresponde ao texto ARC69 em 95%+ dos casos |
| Confianca media | Alta (90+) para a maioria das palavras |
| isDivergent | Corretamente marcado para particulas/artigos sem correspondencia |

---

## Recomendacoes

1. **Incluir morfologia no prompt de traducao** — Enviar o codigo morfologico (ex: `V-AAI-3P` = verbo, aoristo, indicativo, 3a pessoa plural) junto com o gloss ingles para que o LLM conjugue corretamente em portugues.

2. **Pos-processamento de artigos** — Artigos gregos (G3588) podem ser mapeados diretamente pela morfologia: `T-NPM` → "os", `T-NSF` → "a", `T-APM` → "os", etc., sem depender do LLM.

3. **Corrigir erros semanticos graves** — Os 3 erros de v24-25 (`conhecimento`, `ninguem`, `para`) devem ser corrigidos manualmente ou com re-ingestao direcionada.

4. **Validacao automatica pos-ingestao** — Implementar check que compare o numero gramatical do gloss PT com a morfologia grega para detectar discordancias sistematicas.
