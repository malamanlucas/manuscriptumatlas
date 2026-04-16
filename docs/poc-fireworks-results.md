# POC Fireworks KIMI k2p5 — Resultados

**Data:** 2026-04-16 01:29  
**Modelo testado:** `accounts/fireworks/models/kimi-k2p5`  
**Total amostras:** 50 (20 LOW glosses · 15 MEDIUM lexicon · 15 MEDIUM enrichment)

---

## Resumo por fase

| Fase | Tier | N | JSON válido | Campos OK | Erros | Lat p50 | Lat p95 |
|------|------|---|-------------|-----------|-------|---------|---------|
| bible_translate_glosses | LOW | 20 | N/A | N/A | 0 | 9005ms | 23822ms |
| bible_translate_lexicon | MEDIUM | 15 | 0% | N/A | 0 | 8852ms | 9969ms |
| bible_translate_enrichment_greek | MEDIUM | 15 | 0% | N/A | 0 | 9015ms | 37100ms |

---

## Glosses — LOW

| ID | Label | Claude model | Linhas OK | KIMI (80ch) | Claude (80ch) |
|----|-------|-------------|-----------|-------------|---------------|
| 63862 | GLOSS_TRANSLATE_Spanish_chunk2_Luke_10 | claude-haiku-4-5 | ❌ | The user wants me to translate a list of biblical Greek words into Spanish gloss… | {"ginōskete": "sabéis", "hoti": "que", "ēngiken": "se ha acercado", "eph᾽": "sob… |
| 63861 | GLOSS_TRANSLATE_Spanish_chunk1_Luke_10 | claude-haiku-4-5 | ❌ | The user wants me to translate a series of biblical Greek words into Spanish glo… | {"tō": "al", "oikō": "hogar", "toutō": "este", "kai": "y", "ean": "si", "ekei": … |
| 63860 | GLOSS_TRANSLATE_Spanish_chunk0_Luke_10 | claude-haiku-4-5 | ✅ | The user wants me to translate a series of biblical Greek words into Spanish glo… | {"Meta": "Después de", "de": "y", "tauta": "estas cosas", "anedeixen": "designó"… |
| 63859 | GLOSS_TRANSLATE_Portuguese_chunk6_Luke_1… | claude-haiku-4-5 | ✅ | The user wants me to translate a list of biblical Greek words into Portuguese gl… | {"poiei": "fazes", "homoiōs": "igualmente", "Egeneto": "aconteceu", "poreuesthai… |
| 63858 | GLOSS_TRANSLATE_Portuguese_chunk5_Luke_1… | claude-haiku-4-5 | ❌ | The user wants me to translate a list of biblical Greek words into Portuguese gl… | {"epithentes": "infligindo", "apēlthon": "partiram", "aphentes": "deixando", "hē… |
| 63857 | GLOSS_TRANSLATE_Portuguese_chunk4_Luke_1… | claude-haiku-4-5 | ✅ | The user wants me to translate a list of biblical Greek words into Portuguese gl… | {"ha": "que", "blepete": "vedes", "polloi": "muitos", "prophētai": "profetas", "… |
| 63856 | GLOSS_TRANSLATE_Portuguese_chunk3_Luke_1… | claude-haiku-4-5 | ✅ | The user wants me to translate a list of biblical Greek words into Portuguese gl… | {"exousian": "autoridade", "tou": "o", "patein": "pisar", "epanō": "sobre", "oph… |
| 63855 | GLOSS_TRANSLATE_Portuguese_chunk2_Luke_1… | claude-haiku-4-5 | ✅ | The user wants me to translate a list of biblical Greek words into Portuguese gl… | {"ginōskete": "sabeis", "hoti": "que", "ēngiken": "se aproximou", "eph᾽": "sobre… |
| 63854 | GLOSS_TRANSLATE_Portuguese_chunk1_Luke_1… | claude-haiku-4-5 | ✅ | The user wants me to translate a list of biblical Greek words into Portuguese fo… | {"tō": "ao", "oikō": "casa", "toutō": "este", "kai": "e", "ean": "se", "ekei": "… |
| 63853 | GLOSS_TRANSLATE_Portuguese_chunk0_Luke_1… | claude-haiku-4-5 | ✅ | The user wants me to translate a series of biblical Greek words into Portuguese … | {"Meta": "Depois de", "de": "pois", "tauta": "estas coisas", "anedeixen": "desig… |
| 63802 | GLOSS_TRANSLATE_Portuguese_chunk3_Luke_7 | claude-haiku-4-5 | ✅ | The user wants me to translate a list of biblical Greek words into Portuguese gl… | {"Iōannē": "a João", "autou": "dele", "pantōn": "todos", "toutōn": "estas coisas… |
| 63801 | GLOSS_TRANSLATE_Portuguese_chunk2_Luke_7 | claude-haiku-4-5 | ✅ | The user wants me to translate biblical Greek words into Portuguese glosses for … | {"eporeuthē": "foi", "polin": "cidade", "kaloumenēn": "chamada", "Nain": "Naim",… |
| 63800 | GLOSS_TRANSLATE_Portuguese_chunk1_Luke_7 | claude-haiku-4-5 | ✅ | The user wants me to translate a list of biblical Greek words into Portuguese gl… | {"epempsen": "enviou", "auton": "o", "philous": "amigos", "ho": "o", "hekatontar… |
| 63799 | GLOSS_TRANSLATE_Portuguese_chunk0_Luke_7 | claude-haiku-4-5 | ✅ | The user wants me to translate a set of biblical Greek words into Portuguese glo… | {"Epeidē": "Visto que", "eplērōsen": "completou", "panta": "todas", "ta": "as", … |
| 63798 | GLOSS_TRANSLATE_Spanish_chunk7_Luke_6 | claude-haiku-4-5 | ✅ | The user wants me to translate a list of biblical Greek words into Spanish gloss… | {"homoios": "semejante", "anthrōpō": "a un hombre", "oikodomounti": "que constru… |
| 63797 | GLOSS_TRANSLATE_Spanish_chunk6_Luke_6 | claude-haiku-4-5 | ✅ | The user wants me to translate a list of biblical Greek words into Spanish gloss… | {"ekbalō": "expulso", "autos": "tú", "sou": "tu", "dokon": "viga", "blepōn": "vi… |
| 63796 | GLOSS_TRANSLATE_Spanish_chunk5_Luke_6 | claude-haiku-4-5 | ✅ | The user wants me to translate Greek words to Spanish glosses based on the provi… | {"polus": "grande", "esesthe": "seréis", "huioi": "hijos", "hupsistou": "del Alt… |
| 63795 | GLOSS_TRANSLATE_Spanish_chunk4_Luke_6 | claude-haiku-4-5 | ✅ | The user wants me to translate a list of biblical Greek words into Spanish gloss… | {"kalōs": "bien", "eipōsin": "hablen", "pantes": "todos", "anthrōpoi": "hombres"… |
| 63794 | GLOSS_TRANSLATE_Spanish_chunk3_Luke_6 | claude-haiku-4-5 | ❌ | The user wants me to translate Greek words into Spanish glosses for an interline… | {"apo": "de", "pneumatōn": "espíritus", "akathartōn": "inmundos", "etherapeuonto… |
| 63793 | GLOSS_TRANSLATE_Spanish_chunk2_Luke_6 | claude-haiku-4-5 | ✅ | The user wants me to translate a list of biblical Greek words into Spanish gloss… | {"eplēsthēsan": "fueron llenos", "anoias": "de locura", "dielaloun": "discutían"… |

---

## Léxico Grego — MEDIUM

| ID | Label | Claude model | JSON OK | pt OK | es OK | KIMI campos | Claude campos | KIMI (80ch) | Claude (80ch) |
|----|-------|-------------|---------|-------|-------|------------|--------------|-------------|---------------|
| 62864 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries from English to Spanish. I … | [G2574] SHORT: camello FULL: κάμηλος, -ου, ὁ, ἡ [en LXX para גָּמַל ;] camello: … |
| 62863 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries from English to Spanish. I … | [G2500] SHORT: José FULL: Ἰωσῆς , -ῆ (Rec. -ή Luk.3:29; AV, Jose; véase: Ἰησοῦς,… |
| 62862 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries to Spanish. I need to follo… | [G2421] SHORT: Jesé FULL: Ἰεσσαί (FlJ, -σσαῖος), ὁ (Heb. יִשַׁי Rut.4:17 , al.) … |
| 62861 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries from English to Spanish. I … | [G2337] SHORT: amamantar FULL: θηλάζω (θηλή, un pecho), [en LXX principalmente p… |
| 62860 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries to Spanish. I need to follo… | [G2261] SHORT: gentil FULL: ἤπιος , -α, -ον suave, gentil : 1Th.2:7 (WH, R, mg.,… |
| 62802 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries to Portuguese. I need to fo… | [G8996] SHORT: "vermelho-ardente" FULL: "vermelho-ardente; avermelhado"  [G8997]… |
| 62801 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries from English to Portuguese.… | [G8913] SHORT: unir-se a FULL: unir-se a  [G8914] SHORT: aceitável FULL: aceitáv… |
| 62800 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries to Portuguese. I need to fo… | [G8828] SHORT: erva FULL: 1. grama, erva, (Homer), etc.; ποία Μηδική, Lat. herba… |
| 62799 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries from English to Portuguese.… | [G8747] SHORT: uma aldeia FULL: um posto para περίπολοι, uma casa de guarda, (Th… |
| 62798 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries from English to Portuguese.… | [G8667] SHORT: "pisado" FULL: "pisado"  [G8668] SHORT: "um tio" FULL: "um tio"  … |
| 62854 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries to Spanish. The format shou… | [G1801] SHORT: escuchar, dar oído a FULL: ἐνωτίζομαι (οὖς), depon. mid., [en LXX… |
| 62853 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries from English to Spanish. I … | [G1720] SHORT: "soplar en/sobre" FULL: ἐμ-φυσάω , -ῶ (φυσάω, soplar), [en LXX pa… |
| 62852 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries from English to Spanish. I … | [G1639] SHORT: elamita FULL: Ἐλαμείτης (Rec. -αμίτης), -ου, ὁ (Heb. אֵילָם) [en … |
| 62851 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries from English to Spanish. I … | [G1563] SHORT: allí FULL: ἐκεῖ adv. , [en LXX principalmente para שָׁם ;] __1. p… |
| 62850 | LEXICON_BATCH_Greek_80 | claude-sonnet-4-6 | ❌ | ❌ | ❌ | — | — | The user wants me to translate Greek lexicon entries from English to Spanish. I … | [G1486] SHORT: tener por costumbre FULL: ἔθω, pf. con sentido presente εἴωθα, [e… |

---

## Enrichment Grego — MEDIUM

| ID | Label | Claude model | JSON OK | Campos OK | KIMI campos | Claude campos | KIMI (80ch) | Claude (80ch) |
|----|-------|-------------|---------|-----------|------------|--------------|-------------|---------------|
| 67049 | ENRICHMENT_TRANSLATE_G1628_pt | claude-haiku-4-5-20251001 | ❌ | — | — | — | The user wants me to translate a biblical lexicon entry from English to Portugue… | KJV_TRANSLATION: escapar, fugir WORD_ORIGIN: [de G1537 (ἐκ - entre) e G5343 (φεύ… |
| 67048 | ENRICHMENT_TRANSLATE_G1627_es | claude-haiku-4-5-20251001 | ❌ | — | — | — | The user wants me to translate a biblical lexicon entry from English to Spanish.… | KJV_TRANSLATION: llevar, traer, cargar (sacar) WORD_ORIGIN: [de G1537 (ἐκ - entr… |
| 67047 | ENRICHMENT_TRANSLATE_G1627_pt | claude-haiku-4-5-20251001 | ❌ | — | — | — | The user wants me to translate specific lexicon fields from English to Portugues… | KJV_TRANSLATION: carregar, produzir, levar para fora WORD_ORIGIN: [de G1537 (ἐκ … |
| 67046 | ENRICHMENT_TRANSLATE_G0089_es | claude-haiku-4-5-20251001 | ❌ | — | — | — | The user wants me to translate specific lexicon fields from English to Spanish w… | KJV_TRANSLATION: sin cesar WORD_ORIGIN: [adverbio de G88 (ἀδιάλειπτος - constant… |
| 67045 | ENRICHMENT_TRANSLATE_G0089_pt | claude-haiku-4-5-20251001 | ❌ | — | — | — | The user wants me to translate specific lexicon fields from English to Portugues… | KJV_TRANSLATION: sem cessar WORD_ORIGIN: [advérbio de G88 (ἀδιάλειπτος - constan… |
| 67044 | ENRICHMENT_TRANSLATE_G4175_es | claude-haiku-4-5-20251001 | ❌ | — | — | — | The user wants me to translate a biblical lexicon entry from English to Spanish.… | KJV_TRANSLATION: conversación WORD_ORIGIN: [from G4176 (πολιτεύομαι - conduct) ]… |
| 67043 | ENRICHMENT_TRANSLATE_G4175_pt | claude-haiku-4-5-20251001 | ❌ | — | — | — | The user wants me to translate a biblical lexicon entry from English to Portugue… | KJV_TRANSLATION: conversa WORD_ORIGIN: [from G4176 (πολιτεύομαι - conduta) ] 1. … |
| 67042 | ENRICHMENT_TRANSLATE_G1620_es | claude-haiku-4-5-20251001 | ❌ | — | — | — | The user wants me to translate specific lexicon fields from English to Spanish w… | KJV_TRANSLATION: expulsar, exponer WORD_ORIGIN: [from G1537 (ἐκ - entre) and G50… |
| 67041 | ENRICHMENT_TRANSLATE_G1620_pt | claude-haiku-4-5-20251001 | ❌ | — | — | — | The user wants me to translate specific lexicon fields from English to Portugues… | KJV_TRANSLATION: expor, expor WORD_ORIGIN: [from G1537 (ἐκ - entre) and G5087 (τ… |
| 67040 | ENRICHMENT_TRANSLATE_G1615_es | claude-haiku-4-5-20251001 | ❌ | — | — | — | The user wants me to translate a biblical lexicon entry from English to Spanish.… | KJV_TRANSLATION: terminar WORD_ORIGIN: [from G1537 (ἐκ - entre) and G5055 (τελέω… |
| 67098 | ENRICHMENT_TRANSLATE_G1557_es | claude-haiku-4-5 | ❌ | — | — | — | The user wants me to translate a biblical lexicon entry from English to Spanish.… | KJV_TRANSLATION: (a-, re-)venganza(-nza), castigo WORD_ORIGIN: [from G1556 (ἐκδι… |
| 67097 | ENRICHMENT_TRANSLATE_G1557_pt | claude-haiku-4-5 | ❌ | — | — | 6 | The user wants me to translate a biblical lexicon entry from English to Portugue… | {   "KJV_TRANSLATION": "(a-, re-)vingança(-ança), castigo",   "WORD_ORIGIN": "[d… |
| 67096 | ENRICHMENT_TRANSLATE_G0169_es | claude-haiku-4-5 | ❌ | — | — | 6 | The user wants me to translate a biblical lexicon entry from English to Spanish.… | {   "KJV_TRANSLATION": "sucio, inmundo",   "WORD_ORIGIN": "[de G1 (α - Alfa) (co… |
| 67095 | ENRICHMENT_TRANSLATE_G0169_pt | claude-haiku-4-5 | ❌ | — | — | 6 | The user wants me to translate a biblical lexicon entry from English to Portugue… | {"KJV_TRANSLATION": "imundo, impuro", "WORD_ORIGIN": "[de G1 (α - Alpha) (como p… |
| 67099 | ENRICHMENT_TRANSLATE_G1692_pt | claude-haiku-4-5 | ❌ | — | — | 6 | The user wants me to translate a biblical lexicon entry from English to Portugue… | {   "KJV_TRANSLATION": "(vai) vomitar",   "WORD_ORIGIN": "[de afinidade incerta]… |

---

## Itens com erro ou JSON inválido

| ID | Fase | Erro | KIMI resposta |
|----|------|------|---------------|
| 62864 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries from En… |
| 62863 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries from En… |
| 62862 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries to Span… |
| 62861 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries from En… |
| 62860 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries to Span… |
| 62802 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries to Port… |
| 62801 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries from En… |
| 62800 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries to Port… |
| 62799 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries from En… |
| 62798 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries from En… |
| 62854 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries to Span… |
| 62853 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries from En… |
| 62852 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries from En… |
| 62851 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries from En… |
| 62850 | bible_translate_lexicon | JSON inválido | The user wants me to translate Greek lexicon entries from En… |
| 67049 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate a biblical lexicon entry from… |
| 67048 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate a biblical lexicon entry from… |
| 67047 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate specific lexicon fields from … |
| 67046 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate specific lexicon fields from … |
| 67045 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate specific lexicon fields from … |
| 67044 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate a biblical lexicon entry from… |
| 67043 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate a biblical lexicon entry from… |
| 67042 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate specific lexicon fields from … |
| 67041 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate specific lexicon fields from … |
| 67040 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate a biblical lexicon entry from… |
| 67098 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate a biblical lexicon entry from… |
| 67097 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate a biblical lexicon entry from… |
| 67096 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate a biblical lexicon entry from… |
| 67095 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate a biblical lexicon entry from… |
| 67099 | bible_translate_enrichment_greek | JSON inválido | The user wants me to translate a biblical lexicon entry from… |

---

## Observações automáticas

### Padrão crítico identificado: "Thinking preamble"

O modelo `kimi-k2p5` é um **modelo de raciocínio** — ele externaliza seu processo de pensamento
antes de retornar a resposta. Isso explica o **0% JSON válido** no léxico e enrichment:
a resposta começa com `"The user wants me to translate..."` (raciocínio interno) e o JSON
real (quando existe) vem depois — fora do alcance do parser simples.

**Exemplos do padrão observado:**

- KIMI resposta léxico (ID 62864):
  > `"The user wants me to translate Greek lexicon entries from English to Spanish. I need to follow the format... [G2574] SHORT: camello FULL: ..."`
- Claude resposta léxico (ID 62864):
  > `"[G2574] SHORT: camello FULL: κάμηλος..."`

No enrichment, itens 67095–67099 mostram que a resposta do Claude **é JSON** (6 campos),
enquanto o KIMI insere raciocínio antes — o JSON real provavelmente está no fim da resposta.

### Implicações

| Fase | Problema real | Possível solução |
|------|--------------|------------------|
| Glosses (LOW) | Linhas OK = 65% — preamble reduz a contagem de linhas | Extrair só as linhas com `word: tradução` |
| Léxico (MEDIUM) | 0% JSON válido por causa do preamble | `response_format: {type:"json_object"}` ou regex mais agressivo |
| Enrichment (MEDIUM) | 0% JSON válido — mesmo motivo | `response_format: {type:"json_object"}` ou regex mais agressivo |

### Qualidade de tradução (observação nos truncados)

Apesar do formato incorreto, as traduções em si parecem **semanticamente corretas**:
- Glosses: "sabéis" / "se ha acercado" / "designó" (ES) e "sabeis" / "se aproximou" / "designou" (PT) — parecem adequados para texto bíblico.
- Léxico: "camello", "José", "Jesé", "amamantar" — corretos no conteúdo.
- Enrichment: as traduções visíveis (KJV_TRANSLATION, WORD_ORIGIN) parecem precisas.

**Conclusão preliminar:** O problema é de **formato de saída**, não de qualidade de tradução.
Com `response_format: {type:"json_object"}` ou prompt adaptado, o modelo pode ser viável.

---

## Spot check manual (preencher)

| ID | Fase | Qualidade KIMI (1-10) | Qualidade Claude (1-10) | Observação |
|----|------|----------------------|------------------------|------------|
| | | | | |

---

## Decisão

- [ ] **Cenário A** — ≥ 90% JSON válido, qualidade boa → implementar `LlmDirectWorker`
- [ ] **Cenário B** — 80–90% válido, qualidade aceitável → ajustar modelo ou validação extra
- [ ] **Cenário C** — < 80% válido ou qualidade ruim → manter `/run-llm` para MEDIUM

**Decisão:** _preencher após análise_  
**Motivo:** _preencher após análise_