# Validacao de Glosses Portugueses — Joao 2 (ARC69) — v4

> Relatorio gerado em 2026-04-09. Prompt v4: english_gloss + lemma + imperativo classico + singular obrigatorio + artigos deterministicos (G3588).
> Modelo: gpt-4.1-mini (tier LOW, temperatura 0.0). Artigos mapeados por regra, sem LLM.

---

## Resumo Executivo

| Metrica | v1 | v2 | v3 | v4 (atual) |
|---------|----|----|----|----|
| Total de palavras | 437 | 437 | 437 | 437 |
| Rating OK | 5 (20%) | 14 (56%) | 23 (92%) | **24 (96%)** |
| Rating WARN | 18 (72%) | 10 (40%) | 2 (8%) | **1 (4%)** |
| Rating BAD | 2 (8%) | 1 (4%) | 0 (0%) | **0 (0%)** |
| Erros semanticos graves | 5 | 4 | 0 | **0** |
| Artigos errados | ~5 | ~5 | 0 | **0** (deterministico) |

### Veredito Geral

**96% dos versiculos estao OK. Zero erros semanticos. Zero artigos errados.** O unico WARN remanescente e `ζῆλος` → "zelos" (plural incorreto, deveria "zelo"). Alem disso, 3 pronomes indefinidos/interrogativos receberam "qualquer/qualquer coisa" — aceitavel para 2 deles, subotimo para 1.

---

## Status de Todos os Casos Rastreados

### 5 Erros Semanticos Originais (v1-v2)

| Caso | v2 (errado) | v4 | Status |
|------|-------------|-----|--------|
| `ᾔδεισαν` (9:18) — knew | temeram | **sabiam** | ✅ |
| `αὐτοῖς` (7:2, 8:3, 19:6, 22:13) — to them | nessas (4x) | **a eles** (4x) | ✅ |
| `ἐγεύσατο` (9:3) — tasted | se perturbou | **provou** | ✅ |
| `ἠντληκότες` (9:20) — having drawn | encharcados | **tendo tirado** | ✅ |
| `λύσατε` (19:7) — do destroy | desatai | **destruí** | ✅ |

### Imperativos 2P (v3 WARN)

| Grego | v3 | v4 | ARC69 | Status |
|-------|----|----|-------|--------|
| γεμίσατε (7:5) | encham | **enchei** | Enchei | ✅ |
| ἀντλήσατε (8:4) | retirem | **tirai** | Tirai | ✅ |
| φέρετε (8:7) | carreguem | **carregai** | levai | ✅ |
| ποιήσατε (5:12) | façam | **fazei** | Fazei | ✅ |
| ἄρατε (16:7) | levantai | **tomai** | Tirai | ✅ |
| ποιεῖτε (16:11) | fazeis | **fazei** | façais | ✅ |

### Artigos G3588 (78 ocorrencias)

**100% corretos** — mapeamento deterministico por morfologia, sem LLM:
- T-NSM/T-ASM → "o" | T-NSF/T-ASF → "a"
- T-NPM/T-APM → "os" | T-NPF/T-APF → "as"
- T-GSM/T-GSN → "do" | T-GSF → "da"
- T-DSM/T-DSN → "ao" | T-DSF → "à"
- T-GPM/T-GPN → "dos" | T-DPM/T-DPN → "aos"

---

## Problemas Remanescentes (menores)

### 1. `ζῆλος` → "zelos" (N-NSM = singular)

| Verso:Pos | Grego | Morph | EN | PT | ARC69 |
|-----------|-------|-------|----|----|-------|
| 17:10 | ζῆλος | N-NSM | zeal | zelos | zelo |

O LLM insiste no plural apesar da regra no prompt. Impacto minimo — 1 palavra.

### 2. Pronomes indefinidos/interrogativos → "qualquer"

| Verso:Pos | Grego | Morph | EN | PT | ARC69 | Avaliacao |
|-----------|-------|-------|----|----|-------|-----------|
| 4:6 | τί | I-NSN | What | qualquer coisa | que | ⚠️ Subotimo — "que" seria melhor |
| 5:7 | ὅ | R-ASN | Whatever | qualquer | quanto | Aceitavel |
| 5:8 | τι | X-ASN | anyhow | qualquer coisa | tudo | Aceitavel |

---

## Validacao Verso a Verso

| Verso | Rating | Observacoes |
|-------|--------|-------------|
| 1 | **OK** | Artigos corretos. `τρίτῃ` → "terceiro" (menor: poderia ser fem). |
| 2 | **OK** | `ὁ` → "o" ✅ (era "qualquer" na v4-anterior). |
| 3 | **OK** | Sem erros. |
| 4 | **OK** | `ὁ` → "o" ✅. `τί` → "qualquer coisa" — subotimo mas nao e artigo. |
| 5 | **OK** | `ποιήσατε` → "fazei" ✅. Pronomes indefinidos como "qualquer" — aceitavel. |
| 6 | **OK** | Sem erros. |
| 7 | **OK** | `γεμίσατε` → "enchei" ✅. `αὐτοῖς` → "a eles" ✅. |
| 8 | **OK** | `ἀντλήσατε` → "tirai" ✅. `φέρετε` → "carregai" ✅. |
| 9 | **OK** | `ἐγεύσατο` → "provou" ✅. `ᾔδεισαν` → "sabiam" ✅. `ἠντληκότες` → "tendo tirado" ✅. |
| 10 | **OK** | Sem erros. |
| 11 | **OK** | `ἐπίστευσαν` → "creram" ✅. |
| 12 | **OK** | `ἔμειναν` → "ficaram" ✅. |
| 13 | **OK** | `ἀνέβη` → "subiu" ✅. |
| 14 | **OK** | Sem erros. |
| 15 | **OK** | Sem erros. |
| 16 | **OK** | `ἄρατε` → "tomai" ✅. `ποιεῖτε` → "fazei" ✅. |
| 17 | **WARN** | `ζῆλος` → "zelos" (deveria "zelo", singular). |
| 18 | **OK** | `ἀπεκρίθησαν` → "responderam" ✅. `εἶπαν` → "disseram" ✅. |
| 19 | **OK** | `λύσατε` → "destruí" ✅. `αὐτοῖς` → "a eles" ✅. |
| 20 | **OK** | Sem erros. |
| 21 | **OK** | Sem problemas. |
| 22 | **OK** | `ἐμνήσθησαν` → "lembraram" ✅. `αὐτοῖς` → "a eles" ✅. |
| 23 | **OK** | `ἐπίστευσαν` → "creram" ✅. |
| 24 | **OK** | `γινώσκειν` → "conhecer" ✅. |
| 25 | **OK** | `τις` → "alguém" ✅. `γὰρ` → "pois" ✅. |

---

## Historico de Mudancas

| Versao | Mudanca | Impacto |
|--------|---------|---------|
| v1→v2 | Correcoes manuais no banco | 20%→56% OK |
| v2→v3 | Prompt: +english_gloss +lemma | 56%→92% OK, 0 erros semanticos |
| v3→v4 | Prompt: +imperativo classico +singular. Artigos G3588 deterministicos | 92%→96% OK, artigos 100% corretos |

### Resumo tecnico das alteracoes (v4)

**Arquivo:** `BibleIngestionService.kt`

1. **Prompt enriquecido** — 4 campos: `transliteration | morphology | english_gloss | lemma`
2. **Regras adicionais** — imperativo classico para 2P, singular obrigatorio para N-*S*
3. **`removeSurrounding("<", ">")`** — strip de colchetes angulares no english_gloss (`<the>` → `the`)
4. **`deterministicGloss()`** — funcao que mapeia artigos G3588 diretamente pela morfologia, sem LLM
5. **`GlossTranslationEntry`** — expandido com englishGloss e lemma para deduplicacao mais precisa
