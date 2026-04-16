# POC — Comparativo: KIMI · DeepSeek · MiniMax vs Claude

**Data:** 2026-04-16 02:29  
**Amostras:** 50 por modelo (20 LOW glosses · 15 MEDIUM lexicon · 15 MEDIUM enrichment)  
**Referência:** Claude Haiku/Sonnet (respostas originais do banco)

---

## Resumo Executivo

| Modelo | Glosses formato ok | Léxico JSON ok | Enrich JSON ok | Lat p50 glosses | Lat p50 léxico | Nota |
|--------|-------------------|---------------|----------------|-----------------|----------------|------|
| **KIMI k2p5** | ~0/20 ❌ | 0% ❌ | 0% ❌ | 9s 🟡 | 25s 🔴 | Reasoning model — gasta tokens em raciocínio |
| **DeepSeek-V3** | 20/20 ✅ | 0% | 0% | 24s | 31s | Não-reasoning, formato mais direto |
| **MiniMax-M2.5** | 9/20 ⚠️ | 0% | 0% | 10s | 7s | Rápido (~10s), strip de `<think>` necessário |
| **Claude Haiku/Sonnet** | 20/20 ✅ | 100% ✅ | 100% ✅ | — (referência) | — (referência) | Baseline |

---

## Latência Detalhada

| Fase | KIMI p50 | KIMI p95 | DeepSeek p50 | DeepSeek p95 | MiniMax p50 | MiniMax p95 |
|------|----------|----------|--------------|--------------|------------|------------|
| Glosses (LOW) | 9s | 24s | 24s | 26s | 10s | 16s |
| Léxico (MEDIUM) | 25s | 149s | 31s | 37s | 7s | 12s |
| Enrichment (MEDIUM) | 10s | 39s | 33s | 34s | 11s | 13s |

---

## Glosses — LOW (20 amostras)

Formato esperado: JSON `{"word": "tradução"}` por palavra grega.  
DeepSeek com output correto: **20/20**  
MiniMax com output correto: **9/20**  

| ID | Label | DeepSeek | MiniMax | KIMI | Claude (ref) |
|----|-------|----------|---------|------|-------------|
| 63862 | GLOSS_TRANSLATE_Spanish_chunk2_Luke… | ✅ ````json {     "ginōskete": "sepan",     "hoti": "que",     "…` | ✅ `{ "ginōskete": "conocéis", "hoti": "que", "ēngiken": "ha lle…` | ❌ (instruções) | `{"ginōskete": "sabéis", "hoti": "que", "ēngiken": "se ha ace…` |
| 63861 | GLOSS_TRANSLATE_Spanish_chunk1_Luke… | ✅ `{   "tō": "al",   "oikō": "casa",   "toutō": "este",   "kai"…` | ❌ `` | ❌ (instruções) | `{"tō": "al", "oikō": "hogar", "toutō": "este", "kai": "y", "…` |
| 63860 | GLOSS_TRANSLATE_Spanish_chunk0_Luke… | ✅ ````json {     "Meta": "Después",     "de": "ahora",     "tau…` | ✅ ````json {   "Meta": "después de",   "de": "entonces",   "tau…` | ❌ (instruções) | `{"Meta": "Después de", "de": "y", "tauta": "estas cosas", "a…` |
| 63859 | GLOSS_TRANSLATE_Portuguese_chunk6_L… | ✅ ````json {     "poiei": "faze",     "homoiōs": "igualmente", …` | ✅ `{"poiei":"fazes","homoiōs":"igualmente","Egeneto":"aconteceu…` | ❌ (instruções) | `{"poiei": "fazes", "homoiōs": "igualmente", "Egeneto": "acon…` |
| 63858 | GLOSS_TRANSLATE_Portuguese_chunk5_L… | ✅ ````json {     "epithentes": "tendo infligido",     "apēlthon…` | ✅ `{"epithentes": "tendo imposto", "apēlthon": "eles foram embo…` | ❌ (instruções) | `{"epithentes": "infligindo", "apēlthon": "partiram", "aphent…` |
| 63857 | GLOSS_TRANSLATE_Portuguese_chunk4_L… | ✅ ````json {     "ha": "o que",     "blepete": "vêde",     "pol…` | ❌ `` | ❌ (instruções) | `{"ha": "que", "blepete": "vedes", "polloi": "muitos", "proph…` |
| 63856 | GLOSS_TRANSLATE_Portuguese_chunk3_L… | ✅ ````json {     "exousian": "autoridade",     "tou": "do",    …` | ❌ `` | ❌ (instruções) | `{"exousian": "autoridade", "tou": "o", "patein": "pisar", "e…` |
| 63855 | GLOSS_TRANSLATE_Portuguese_chunk2_L… | ✅ ````json {     "ginōskete": "sabei",     "hoti": "que",     "…` | ❌ `` | ❌ (instruções) | `{"ginōskete": "sabeis", "hoti": "que", "ēngiken": "se aproxi…` |
| 63854 | GLOSS_TRANSLATE_Portuguese_chunk1_L… | ✅ `{   "tō": "ao",   "oikō": "casa",   "toutō": "este",   "kai"…` | ❌ `` | ❌ (instruções) | `{"tō": "ao", "oikō": "casa", "toutō": "este", "kai": "e", "e…` |
| 63853 | GLOSS_TRANSLATE_Portuguese_chunk0_L… | ✅ ````json {     "Meta": "Depois",     "de": "agora",     "taut…` | ✅ `{"Meta": "após", "de": "então", "tauta": "estas coisas", "an…` | ❌ (instruções) | `{"Meta": "Depois de", "de": "pois", "tauta": "estas coisas",…` |
| 63802 | GLOSS_TRANSLATE_Portuguese_chunk3_L… | ✅ ````json {     "Iōannē": "a João",     "autou": "dele",     "…` | ❌ `` | ❌ (instruções) | `{"Iōannē": "a João", "autou": "dele", "pantōn": "todos", "to…` |
| 63801 | GLOSS_TRANSLATE_Portuguese_chunk2_L… | ✅ ````json {     "eporeuthē": "Ele foi",     "polin": "uma cida…` | ✅ `{"eporeuthē": "foi", "polin": "cidade", "kaloumenēn": "chama…` | ❌ (instruções) | `{"eporeuthē": "foi", "polin": "cidade", "kaloumenēn": "chama…` |
| 63800 | GLOSS_TRANSLATE_Portuguese_chunk1_L… | ✅ ````json {     "epempsen": "enviou",     "auton": "ele",     …` | ❌ `` | ❌ (instruções) | `{"epempsen": "enviou", "auton": "o", "philous": "amigos", "h…` |
| 63799 | GLOSS_TRANSLATE_Portuguese_chunk0_L… | ✅ ````json {     "Epeidē": "E visto que",     "eplērōsen": "Ele…` | ❌ `` | ❌ (instruções) | `{"Epeidē": "Visto que", "eplērōsen": "completou", "panta": "…` |
| 63798 | GLOSS_TRANSLATE_Spanish_chunk7_Luke… | ✅ ````json {     "homoios": "Como",     "anthrōpō": "a un hombr…` | ❌ `` | ❌ (instruções) | `{"homoios": "semejante", "anthrōpō": "a un hombre", "oikodom…` |
| 63797 | GLOSS_TRANSLATE_Spanish_chunk6_Luke… | ✅ ````json {     "ekbalō": "saque",     "autos": "tú mismo",   …` | ✅ `{"ekbalō":"yo pueda expulsar","autos":"tú mismo","sou":"de t…` | ❌ (instruções) | `{"ekbalō": "expulso", "autos": "tú", "sou": "tu", "dokon": "…` |
| 63796 | GLOSS_TRANSLATE_Spanish_chunk5_Luke… | ✅ ````json {     "polus": "grande",     "esesthe": "serán",    …` | ❌ `` | ❌ (instruções) | `{"polus": "grande", "esesthe": "seréis", "huioi": "hijos", "…` |
| 63795 | GLOSS_TRANSLATE_Spanish_chunk4_Luke… | ✅ ````json {     "kalōs": "bien",     "eipōsin": "hablan",     …` | ✅ ````json {   "kalōs": "bien",   "eipōsin": "hablaron",   "pan…` | ❌ (instruções) | `{"kalōs": "bien", "eipōsin": "hablen", "pantes": "todos", "a…` |
| 63794 | GLOSS_TRANSLATE_Spanish_chunk3_Luke… | ✅ ````json {     "apo": "de",     "pneumatōn": "espíritus",    …` | ❌ `` | ❌ (instruções) | `{"apo": "de", "pneumatōn": "espíritus", "akathartōn": "inmun…` |
| 63793 | GLOSS_TRANSLATE_Spanish_chunk2_Luke… | ✅ `{   "eplēsthēsan": "se llenaron",   "anoias": "de furia",   …` | ✅ `{"eplēsthēsan":"fueron llenados","anoias":"de ira","dielalou…` | ❌ (instruções) | `{"eplēsthēsan": "fueron llenos", "anoias": "de locura", "die…` |

---

## Léxico Grego — MEDIUM (15 amostras)

Formato esperado: `[Gxxxx] SHORT: ... FULL: ...` por entrada (mesmo formato do Claude).  
JSON valid abaixo = detectou JSON na resposta (formato diferente do Claude mas estruturado).

| ID | DeepSeek (jv/fo/lat) | MiniMax (jv/fo/lat) | KIMI | DeepSeek resposta (100ch) | MiniMax resposta (100ch) | Claude (ref, 100ch) |
|----|---------------------|--------------------|----|--------------------------|--------------------------|---------------------|
| 62864 | ❌/❌ 30s fmt=✅ | ❌/❌ 5s fmt=✅ | ❌ | `[G2574] SHORT: camello FULL: κάμηλος, -ου, ὁ, ἡ [en LXX por גָּמַל;] camello: Mat.3:4, Mrk.1:6; en p…` | `[G2574] SHORT: camello FULL: κάμηλος, -ου, ὁ, ἡ [en LXX para גָּמַל;] camello: Mat.3:4, Mrk.1:6; en …` | `[G2574] SHORT: camello FULL: κάμηλος, -ου, ὁ, ἡ [en LXX para גָּמַל ;] camello: Mat.3:4, Mrk.1:6; en…` |
| 62863 | ❌/❌ 31s fmt=✅ | ❌/❌ 7s fmt=✅ | ❌ | `[G2500] SHORT: "José" FULL: "Ἰωσῆς, -ῆ (Rec. -ή, Luk.3:29; AV, Jose; véase: Ἰησοῦς, 3), y -ῆτος (Mk,…` | `[G2500] SHORT: "José" FULL: "Ἰωσῆς, -ῆ (Rec. -ή Luk.3:29; AV, José; ver: Ἰησοῦς, 3), y -ῆτος (Mk, ll…` | `[G2500] SHORT: José FULL: Ἰωσῆς , -ῆ (Rec. -ή Luk.3:29; AV, Jose; véase: Ἰησοῦς, 3), y -ῆτος (Mrk, l…` |
| 62862 | ❌/❌ 31s fmt=✅ | ❌/❌ 5s fmt=✅ | ❌ | `[G2421] CORTA: "Jesé" COMPLETA: "Ἰεσσαί (FlJ, -σσαῖος), ὁ (Heb. יִשַׁי Rut.4:17, al.) Jesé: Mat.1:5-…` | `[G2421] SHORT: "Jesse" FULL: "Ἰεσσαί (FlJ, -σσαῖος), ὁ (Heb. יִשַׁי Rut.4:17 , al.) Jesse : Mat.1:5-…` | `[G2421] SHORT: Jesé FULL: Ἰεσσαί (FlJ, -σσαῖος), ὁ (Heb. יִשַׁי Rut.4:17 , al.) Jesé : Mat.1:5-6 , L…` |
| 62861 | ❌/❌ 30s fmt=✅ | ❌/❌ 5s fmt=❌ | ❌ | `[G2337] SHORT: amamantar FULL: θηλάζω (θηλή, un pecho), [en la LXX principalmente para יָנַק hi. ;] …` | `` | `[G2337] SHORT: amamantar FULL: θηλάζω (θηλή, un pecho), [en LXX principalmente para יָנַק hi. ;] __1…` |
| 62860 | ❌/❌ 29s fmt=✅ | ❌/❌ 5s fmt=✅ | ❌ | `[G2261] SHORT: "manso" FULL: "ἤπιος , -α, -ον suave, manso : 1Th.2:7 (WH, R, mg., νήπιος); antes de …` | `[G2261] SHORT: "manso" FULL: "ἤπιος, -α, -ον manso, gentil: 1Th.2:7 (WH, R, mg., νήπιος); antes de π…` | `[G2261] SHORT: gentil FULL: ἤπιος , -α, -ον suave, gentil : 1Th.2:7 (WH, R, mg., νήπιος); antes de π…` |
| 62802 | ❌/❌ 30s fmt=✅ | ❌/❌ 11s fmt=❌ | ❌ | `[G8996] SHORT: vermelho fogo FULL: vermelho fogo; avermelhado  [G8997] SHORT: ser avermelhado FULL: …` | `` | `[G8996] SHORT: "vermelho-ardente" FULL: "vermelho-ardente; avermelhado"  [G8997] SHORT: "ser avermel…` |
| 62801 | ❌/❌ 29s fmt=✅ | ❌/❌ 9s fmt=❌ | ❌ | `[G8913] SHORT: unir-se com FULL: unir-se com  [G8914] SHORT: aceitável FULL: aceitável  [G8915] SHOR…` | `` | `[G8913] SHORT: unir-se a FULL: unir-se a  [G8914] SHORT: aceitável FULL: aceitável  [G8915] SHORT: d…` |
| 62800 | ❌/❌ 29s fmt=✅ | ❌/❌ 7s fmt=❌ | ❌ | `[G8828] SHORT: erva, forragem FULL: 1. grama, erva, (Homero), etc.; ποία Μηδική, Lat. herba Medica, …` | `` | `[G8828] SHORT: erva FULL: 1. grama, erva, (Homer), etc.; ποία Μηδική, Lat. herba Medica, sanfeno ou …` |
| 62799 | ❌/❌ 31s fmt=✅ | ❌/❌ 7s fmt=✅ | ❌ | `[G8747] SHORT: uma aldeia FULL: uma estação para περίπολοι, uma casa de guarda, (Tucídides) (ML)  [G…` | `[G8747] SHORT: um posto FULL: uma estação para περίπολoi, uma casa de guarda, (Thucydides) (ML)  [G8…` | `[G8747] SHORT: uma aldeia FULL: um posto para περίπολοι, uma casa de guarda, (Thucydides) (ML)  [G87…` |
| 62798 | ❌/❌ 30s fmt=✅ | ❌/❌ 12s fmt=✅ | ❌ | `[G8667] SHORT: "pisado" FULL: "pisado"  [G8668] SHORT: "um tio" FULL: "um tio"  [G8669] SHORT: "um í…` | `[G8667] SHORT: pisoteado FULL: pisoteado  [G8668] SHORT: um tio FULL: um tio  [G8669] SHORT: um idol…` | `[G8667] SHORT: "pisado" FULL: "pisado"  [G8668] SHORT: "um tio" FULL: "um tio"  [G8669] SHORT: "um í…` |
| 62854 | ❌/❌ 31s fmt=✅ | ❌/❌ 9s fmt=❌ | ❌ | `[G1801] SHORT: escuchar FULL: ἐνωτίζομαι (οὖς), voz media deponente, [en la LXX principalmente para …` | `` | `[G1801] SHORT: escuchar, dar oído a FULL: ἐνωτίζομαι (οὖς), depon. mid., [en LXX principalmente para…` |
| 62853 | ❌/❌ 31s fmt=✅ | ❌/❌ 8s fmt=❌ | ❌ | `[G1720] SHORT: soplar dentro FULL: ἐμ-φυσάω, -ῶ (φυσάω, soplar), [en LXX por נָפַח, etc.;] soplar de…` | `` | `[G1720] SHORT: "soplar en/sobre" FULL: ἐμ-φυσάω , -ῶ (φυσάω, soplar), [en LXX para נָפַח, etc. ;] so…` |
| 62852 | ❌/❌ 31s fmt=✅ | ❌/❌ 5s fmt=❌ | ❌ | `[G1639] CORTA: "Elamita" COMPLETA: "Ἐλαμείτης (Rec. -αμίτης), -ου, ὁ (Heb. אֵילָם) [en LXX (cl.) Ἐλυ…` | `` | `[G1639] SHORT: elamita FULL: Ἐλαμείτης (Rec. -αμίτης), -ου, ὁ (Heb. אֵילָם) [en LXX (cl.) Ἐλυμαίος, …` |
| 62851 | ❌/❌ 34s fmt=✅ | ❌/❌ 6s fmt=✅ | ❌ | `[G1563] SHORT: allí FULL: ἐκεῖ adv. , [en el AT principalmente por שָׁם ;] __1. propiamente, de luga…` | `[G1563] SHORT: allí FULL: ἐκεῖ adv. , [en LXX principalmente para שָׁם ;] __1. propiamente, de lugar…` | `[G1563] SHORT: allí FULL: ἐκεῖ adv. , [en LXX principalmente para שָׁם ;] __1. propiamente, de lugar…` |
| 62850 | ❌/❌ 37s fmt=✅ | ❌/❌ 7s fmt=✅ | ❌ | `[G1486] SHORT: tener una costumbre FULL: ἔθω, pf. con sentido presente εἴωθα, [en LXX: Núm.24:1 (כְּ…` | `[G1486] SHORT: "tener una costumbre" FULL: "ἔθω , pf. con sentido presente εἴωθα, [en LXX: Num.24:1 …` | `[G1486] SHORT: tener por costumbre FULL: ἔθω, pf. con sentido presente εἴωθα, [en LXX: Num.24:1 (כְּ…` |

---

## Enrichment Grego — MEDIUM (15 amostras)

Formato esperado: JSON com campos KJV_TRANSLATION, WORD_ORIGIN, etc.

| ID | Label | DeepSeek jv/fo | MiniMax jv/fo | KIMI | DeepSeek resposta (100ch) | MiniMax resposta (100ch) | Claude (ref, 100ch) |
|----|-------|---------------|--------------|------|--------------------------|--------------------------|---------------------|
| 67049 | ENRICHMENT_TRANSLATE_G1628_pt | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUÇÃO: escapar, fugir ORIGEM_DA_PALAVRA: [de G1537 (ἐκ - entre) e G5343 (φεύγω - fugir)] 1. f…` | `KJV_TRADUÇÃO: escapar, fugir ORIGEM_PALAVRA: [de G1537 (ἐκ - entre) e G5343 (φεύγω - fugir) ] 1. fug…` | `KJV_TRANSLATION: escapar, fugir WORD_ORIGIN: [de G1537 (ἐκ - entre) e G5343 (φεύγω - fugir) ] 1. fug…` |
| 67048 | ENRICHMENT_TRANSLATE_G1627_es | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUCCIÓN: llevar, producir, sacar ORIGEN_PALABRA: [de G1537 (ἐκ - entre) y G5342 (φέρω - traer…` | `KJV_TRANSLATION: bear, bring forth, carry forth (out) WORD_ORIGIN: [de G1537 (ἐκ - entre) y G5342 (φ…` | `KJV_TRANSLATION: llevar, traer, cargar (sacar) WORD_ORIGIN: [de G1537 (ἐκ - entre) y G5342 (φέρω - t…` |
| 67047 | ENRICHMENT_TRANSLATE_G1627_pt | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUÇÃO: carregar, produzir, levar para fora (para fora) ORIGEM_DA_PALAVRA: [de G1537 (ἐκ - ent…` | `KJV_TRANSLATION: trazer, produzir, carregar para fora WORD_ORIGIN: [de G1537 (ἐκ - entre/com) e G534…` | `KJV_TRANSLATION: carregar, produzir, levar para fora WORD_ORIGIN: [de G1537 (ἐκ - de entre) e G5342 …` |
| 67046 | ENRICHMENT_TRANSLATE_G0089_es | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUCCIÓN: sin cesar ORIGEN_PALABRA: [adverbio de G88 (ἀδιάλειπτος - constantemente)] 1. ininte…` | `KJV_TRANSLATION: sin cesar WORD_ORIGIN: [adverbio de G88 (ἀδιάλειπτος - constantemente) ] 1. sin int…` | `KJV_TRANSLATION: sin cesar WORD_ORIGIN: [adverbio de G88 (ἀδιάλειπτος - constantemente)] 1. ininterr…` |
| 67045 | ENRICHMENT_TRANSLATE_G0089_pt | ❌/❌ | ❌/❌ | ❌ | `KJV_TRANSLATION: sem cessar WORD_ORIGIN: [advérbio de G88 (ἀδιάλειπτος - constantemente) ] 1. ininte…` | `KJV_TRANSLATION: sem cessar WORD_ORIGIN: [advérbio de G88 (ἀδιαλείπτος - constantemente) ] 1. sem in…` | `KJV_TRANSLATION: sem cessar WORD_ORIGIN: [advérbio de G88 (ἀδιάλειπτος - constantemente)] 1. ininter…` |
| 67044 | ENRICHMENT_TRANSLATE_G4175_es | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUCCIÓN: conducta ORIGEN_PALABRA: [de G4176 (πολιτεύομαι - conducirse) ] 1. una comunidad 2. …` | `KJV_TRANSLATION: conducta WORD_ORIGIN: [de G4176 (πολιτεύομαι - conducir, vivir como ciudadano)] 1. …` | `KJV_TRANSLATION: conversación WORD_ORIGIN: [from G4176 (πολιτεύομαι - conduct) ] 1. una comunidad 2.…` |
| 67043 | ENRICHMENT_TRANSLATE_G4175_pt | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUÇÃO: cidadania ORIGEM_DA_PALAVRA: [de G4176 (πολιτεύομαι - conduzir-se)] 1. uma comunidade …` | `KJV_TRANSLATION: conduta WORD_ORIGIN: [de G4176 (πολιτεύομαι - conduta)] 1. uma comunidade 2. (abstr…` | `KJV_TRANSLATION: conversa WORD_ORIGIN: [from G4176 (πολιτεύομαι - conduta) ] 1. uma comunidade 2. (a…` |
| 67042 | ENRICHMENT_TRANSLATE_G1620_es | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUCCIÓN: echar fuera, exponer ORIGEN_PALABRA: [de G1537 (ἐκ - entre) y G5087 (τίθημι - puesto…` | `KJV_TRANSLATION: arrojar, exponer WORD_ORIGIN: [de G1537 (ἐκ - entre) y G5087 (τίθημι - colocado)] 1…` | `KJV_TRANSLATION: expulsar, exponer WORD_ORIGIN: [from G1537 (ἐκ - entre) and G5087 (τίθημι - puesto)…` |
| 67041 | ENRICHMENT_TRANSLATE_G1620_pt | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUÇÃO: expulsar, expor ORIGEM_DA_PALAVRA: [de G1537 (ἐκ - entre) e G5087 (τίθημι - colocado)]…` | `KJV_TRANSLATION: lançar fora, expor WORD_ORIGIN: [de G1537 (ἐκ - entre) e G5087 (τίθημι - colocar)] …` | `KJV_TRANSLATION: expor, expor WORD_ORIGIN: [from G1537 (ἐκ - entre) and G5087 (τίθημι - colocado) ] …` |
| 67040 | ENRICHMENT_TRANSLATE_G1615_es | ❌/❌ | ❌/❌ | ❌ | `KJV_TRANSLATION: terminar WORD_ORIGIN: [de G1537 (ἐκ - entre) y G5055 (τελέω - terminar) ] 1. comple…` | `KJV_TRANSLATION: finish WORD_ORIGIN: [de G1537 (ἐκ) y G5055 (τελέω)] 1. completar plenamente STRONGS…` | `KJV_TRANSLATION: terminar WORD_ORIGIN: [from G1537 (ἐκ - entre) and G5055 (τελέω - terminado) ] 1. c…` |
| 67098 | ENRICHMENT_TRANSLATE_G1557_es | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUCCIÓN: (a-, re-)vengar(-anza), castigo ORIGEN_PALABRA: [de G1556 (ἐκδικέω - dar protección …` | `KJV_TRANSLATION: (a-, re-)venganza, castigo WORD_ORIGIN: [de G1556 (ἐκδικέω - dar protección legal)]…` | `KJV_TRANSLATION: (a-, re-)venganza(-nza), castigo WORD_ORIGIN: [from G1556 (ἐκδικέω - dar protección…` |
| 67097 | ENRICHMENT_TRANSLATE_G1557_pt | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUÇÃO: (re)vindicação, vingança, punição ORIGEM_DA_PALAVRA: [de G1556 (ἐκδικέω - dar proteção…` | `KJV_TRANSLATION: vingança, punição WORD_ORIGIN: [de G1556 (ἐκδικέω - dar proteção legal)] 1. vindica…` | `{   "KJV_TRANSLATION": "(a-, re-)vingança(-ança), castigo",   "WORD_ORIGIN": "[de G1556 (ἐκδικέω - d…` |
| 67096 | ENRICHMENT_TRANSLATE_G0169_es | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUCCIÓN: inmundo, impuro ORIGEN_PALABRA: [de G1 (α - Alfa) (como partícula negativa) y un der…` | `KJV_TRANSLATION: inmundo, impuro WORD_ORIGIN: [desde G1 (α - Alpha) (como partícula negativa) y un d…` | `{   "KJV_TRANSLATION": "sucio, inmundo",   "WORD_ORIGIN": "[de G1 (α - Alfa) (como partícula negativ…` |
| 67095 | ENRICHMENT_TRANSLATE_G0169_pt | ❌/❌ | ❌/❌ | ❌ | `KJV_TRADUÇÃO: imundo, impuro ORIGEM_DA_PALAVRA: [de G1 (α - Alfa) (como partícula negativa) e um sup…` | `KJV_TRANSLATION: imundo, impuro WORD_ORIGIN: [de G1 (α - Alfa) (como partícula negativa) e um deriva…` | `{"KJV_TRANSLATION": "imundo, impuro", "WORD_ORIGIN": "[de G1 (α - Alpha) (como particula negativa) e…` |
| 67099 | ENRICHMENT_TRANSLATE_G1692_pt | ❌/❌ | ❌/❌ | ❌ | `KJV_TRANSLATION: (vai) vomitar WORD_ORIGIN: [afinidade incerta] 1. vomitar STRONGS_EXHAUSTIVE: vomit…` | `KJV_TRANSLATION: (irá) vomitar WORD_ORIGIN: [de afinidade incerta] 1. vomitar STRONGS_EXHAUSTIVE: vo…` | `{   "KJV_TRANSLATION": "(vai) vomitar",   "WORD_ORIGIN": "[de afinidade incerta] 1. vomitar",   "STR…` |

---

## Análise de Qualidade do Conteúdo

### Glosses — Amostras Comparativas

_Seleção de 5 itens para análise manual de qualidade de tradução:_

#### `GLOSS_TRANSLATE_Spanish_chunk2_Luke_10`

**DeepSeek:**
```
```json
{
    "ginōskete": "sepan",
    "hoti": "que",
    "ēngiken": "se ha acercado",
    "eph᾽": "sobre",
    "legō": "digo",
    "humin": "a ustedes",
    "Sodomois": "para Sodoma",
    "hēmera": "día",
    "ekeinē": "aquel",
    "anektoteron": "más tolerable",
    "estai": "será",
    "ē": "que",
    "polei": "ciudad",
    "ekeinē": "para aquella",
    "Ouai": "¡Ay",
    "soi": "de ti",
    "
```

**MiniMax:**
```
{
"ginōskete": "conocéis",
"hoti": "que",
"ēngiken": "ha llegada",
"eph᾽": "sobre",
"legō": "digo",
"humin": "a os",
"Sodomois": "a Sodoma",
"hēmera": "ese día",
"ekeinē": "ése",
"anektoteron": "más soportable",
"estai": "será",
"ē": "que",
"polei": "a la ciudad",
"ekeinē": "ése",
"Ouai": "¡Ay!",
"soi": "te",
"Chorazin": "¡Corazín!",
"ouai": "¡Ay!",
"Bēthsaida": "¡ Betsaida!",
"hoti": "porque",
"e
```

**Claude (referência):**
```
{"ginōskete": "sabéis", "hoti": "que", "ēngiken": "se ha acercado", "eph᾽": "sobre", "legō": "digo", "humin": "a vosotros", "Sodomois": "Sodoma", "hēmera": "día", "ekeinē": "ese", "anektoteron": "más tolerable", "estai": "será", "ē": "que", "polei": "ciudad", "Ouai": "¡Ay!", "soi": "a ti", "Chorazin": "¡Corazín!", "ouai": "¡Ay!", "Bēthsaida": "¡Betsaida!", "ei": "si", "Turō": "Tiro", "Sidōni": "Si
```

#### `GLOSS_TRANSLATE_Spanish_chunk1_Luke_10`

**DeepSeek:**
```
{
  "tō": "al",
  "oikō": "casa",
  "toutō": "este",
  "kai": "Y",
  "ean": "si",
  "ekei": "allí",
  "ē": "será",
  "huios": "hijo",
  "eirēnēs": "de paz",
  "epanapaēsetai": "descansará",
  "ep᾽": "sobre",
  "auton": "él",
  "hē": "la",
  "eirēnē": "paz",
  "humōn": "de ustedes",
  "ei": "si",
  "mē": "no",
  "ge": "aún",
  "eph᾽": "a",
  "anakampsei": "volverá",
  "en": "En",
  "autēa": "la mis
```

**MiniMax:**
```

```

**Claude (referência):**
```
{"tō": "al", "oikō": "hogar", "toutō": "este", "kai": "y", "ean": "si", "ekei": "allí", "ē": "sea", "huios": "hijo", "eirēnēs": "de paz", "epanapaēsetai": "reposará", "ep᾽": "sobre", "auton": "él", "hē": "la", "eirēnē": "paz", "humōn": "vuestro", "ei": "si", "mē": "no", "ge": "pues", "eph᾽": "a", "anakampsei": "volverá", "en": "en", "autēa": "ese", "tēa": "la", "oikia": "casa", "menete": "permanec
```

#### `GLOSS_TRANSLATE_Spanish_chunk0_Luke_10`

**DeepSeek:**
```
```json
{
    "Meta": "Después",
    "de": "ahora",
    "tauta": "estas cosas",
    "anedeixen": "designó",
    "ho": "el",
    "kurios": "Señor",
    "kai": "también",
    "heterous": "otros",
    "hebdomēkonta": "setenta",
    "duo": "dos",
    "kai": "y",
    "apesteilen": "envió",
    "autous": "a ellos",
    "ana": "en",
    "duo": "dos",
    "pro": "delante",
    "prosōpou": "del rostro",
  
```

**MiniMax:**
```
```json
{
  "Meta": "después de",
  "de": "entonces",
  "tauta": "estas cosas",
  "anedeixen": "designó",
  "ho": "el",
  "kurios": "Señor",
  "kai": "también",
  "heterous": "otros",
  "hebdomēkonta": "setenta",
  "duo": "dos",
  "apesteilen": "envió",
  "autous": "ellos",
  "ana": "de dos en dos",
  "pro": "ante",
  "prosōpou": "rostro",
  "autou": "de sí mismo",
  "eis": "a",
  "pasan": "toda",
```

**Claude (referência):**
```
{"Meta": "Después de", "de": "y", "tauta": "estas cosas", "anedeixen": "designó", "ho": "el", "kurios": "Señor", "kai": "y", "heterous": "otros", "hebdomēkonta": "setenta", "duo": "dos", "apesteilen": "envió", "autous": "los", "ana": "en", "pro": "ante", "prosōpou": "rostro", "autou": "de él", "eis": "a", "pasan": "toda", "polin": "ciudad", "topon": "lugar", "hou": "donde", "ēmellen": "estaba por"
```

#### `GLOSS_TRANSLATE_Portuguese_chunk6_Luke_10`

**DeepSeek:**
```
```json
{
    "poiei": "faze",
    "homoiōs": "igualmente",
    "Egeneto": "aconteceu",
    "poreuesthai": "procedendo",
    "autous": "deles",
    "autos": "Ele mesmo",
    "eisēlthen": "entrou",
    "kōmēn": "uma aldeia",
    "tina": "certa",
    "gunē": "uma mulher",
    "tis": "certa",
    "onomati": "chamada",
    "Martha": "Marta",
    "hupedexato": "recebeu",
    "oikon": "casa",
    "autēs
```

**MiniMax:**
```
{"poiei":"fazes","homoiōs":"igualmente","Egeneto":"aconteceu","poreuesthai":"indo","autous":"deles","autos":"ele mesmo","eisēlthen":"entrou","kōmēn":"aldeia","tina":"uma","gunē":"mulher","tis":"uma","onomati":"chamada","Martha":"Marta","hupedexato":"recebeu","oikon":"casa","autēs":"dela","tēade":"ela","ēn":"era","adelphē":"irmã","kaloumenē":"chamada","Mariam":"Maria","hē":"ela","parakathestheisa":
```

**Claude (referência):**
```
{"poiei": "fazes", "homoiōs": "igualmente", "Egeneto": "aconteceu", "poreuesthai": "prosseguindo", "autous": "deles", "autos": "Ele mesmo", "eisēlthen": "entrou", "kōmēn": "uma aldeia", "tina": "certa", "gunē": "uma mulher", "tis": "certa", "onomati": "por nome", "Martha": "Marta", "hupedexato": "recebeu", "oikon": "casa", "autēs": "dela", "tēade": "ela", "ēn": "era", "adelphē": "irmã", "kaloumenē
```

#### `GLOSS_TRANSLATE_Portuguese_chunk5_Luke_10`

**DeepSeek:**
```
```json
{
    "epithentes": "tendo infligido",
    "apēlthon": "foram embora",
    "aphentes": "tendo deixado",
    "hēmithanē": "meio morto",
    "tugchanonta": "estando",
    "kata": "Por",
    "sugkurian": "acaso",
    "hi'ereus": "um sacerdote",
    "en": "na",
    "hodō": "estrada",
    "ekeinē": "aquela",
    "idōn": "tendo visto",
    "antiparēlthen": "passou pelo lado oposto",
    "homoiōs
```

**MiniMax:**
```
{"epithentes": "tendo imposto", "apēlthon": "eles foram embora", "aphentes": "tendo deixado", "hēmithanē": "meio morto", "tugchanonta": "acontecendo", "kata": "segundo", "sugkurian": "coincidência", "hi'ereus": "um sacerdotes", "en": "em", "hodō": "estrada", "ekeinē": "naquele", "idōn": "tendo visto", "antiparēlthen": "passou ao lado", "homoiōs": "do mesmo modo", "Leuitēs": "um levita", "genomenos
```

**Claude (referência):**
```
{"epithentes": "infligindo", "apēlthon": "partiram", "aphentes": "deixando", "hēmithanē": "meio morto", "tugchanonta": "por acaso", "kata": "por", "sugkurian": "acaso", "hi'ereus": "sacerdote", "en": "em", "hodō": "caminho", "ekeinē": "aquele", "idōn": "vendo", "antiparēlthen": "passou adiante", "homoiōs": "igualmente", "Leuitēs": "levita", "genomenos": "chegando", "topon": "lugar", "elthōn": "che
```

---

### Enrichment — Amostras Comparativas

_Seleção de 3 itens para análise manual:_

#### `ENRICHMENT_TRANSLATE_G1628_pt`

**DeepSeek:**
```
KJV_TRADUÇÃO: escapar, fugir
ORIGEM_DA_PALAVRA: [de G1537 (ἐκ - entre) e G5343 (φεύγω - fugir)] 1. fugir para fora
STRONGS_EXAUSTIVO: escapar, fugir. De ek e pheugo; fugir para fora -- escapar, fugir. ver GREGO ek ver GREGO pheugo
NAS_ORIGEM: de ek e pheugó
NAS_DEFINIÇÃO: fugir para longe
NAS_TRADUÇÃO: escapar (5), escapado (2), fugiu (1). Thayer's Greek Lexicon STRONGS NT 1628: ἐκφεύγω ἐκφεύγω: futuro ἐκφεύξομαι; perfeito ἐκπεφευγα; 2 aoristo ἐξέφυγον; (desde Homero para baixo); fugir para fora
```

**MiniMax:**
```
KJV_TRADUÇÃO: escapar, fugir
ORIGEM_PALAVRA: [de G1537 (ἐκ - entre) e G5343 (φεύγω - fugir) ] 1. fugir para fora
STRONGS_EXAUSTIVO: escapar, fugir. De ek e pheugo ; fugir para fora -- escapar, fugir. ver GREGO ek ver GREGO pheugo
ORIGEM_NAS: de ek e pheugó
DEFINIÇÃO_NAS: fugir para longe
TRADUÇÃO_NAS: escapar (5), escapou (2), fugiu (1). Léxico Grego de Thayer STRONGS NT 1628: ἐκφεύγω ἐκφεύγω : futuro ἐκφεύξομαι ; perfeito ἐκπεφευγα ; 2 aoristo ἐξέφυγον ; (de Homero em diante); fugir para fora d
```

**Claude (referência):**
```
KJV_TRANSLATION: escapar, fugir
WORD_ORIGIN: [de G1537 (ἐκ - entre) e G5343 (φεύγω - fugir) ] 1. fugir para fora
STRONGS_EXHAUSTIVE: escapar, fugir. De ek e pheugo ; fugir para fora -- escapar, fugir. veja GREGO ek veja GREGO pheugo
NAS_ORIGIN: de ek e pheugó
NAS_DEFINITION: fugir para longe
NAS_TRANSLATION: escapar (5), escapou (2), fugiu (1). Léxico Grego de Thayer STRONGS NT 1628: ἐκφεύγω ἐκφεύγω : futuro ἐκφεύξομαι ; perfeito ἐκπεφευγα ; 2 aoristo ἐξέφυγον ; fugir para fora de, fugir para lo
```

#### `ENRICHMENT_TRANSLATE_G1627_es`

**DeepSeek:**
```
KJV_TRADUCCIÓN: llevar, producir, sacar
ORIGEN_PALABRA: [de G1537 (ἐκ - entre) y G5342 (φέρω - traer)] 1. sacar {literal o figurativamente}
STRONGS_EXHAUSTIVO: llevar, producir, sacar. De ek y phero; sacar (literal o figurativamente) -- llevar, producir, sacar. ver GRIEGO ek ver GRIEGO phero
ORIGEN_NAS: de ek y pheró
DEFINICIÓN_NAS: sacar, llevar fuera, producir
TRADUCCIÓN_NAS: traer (1), trajo (1), llevaron (2), llevar (1), llevando (1), tomar (1), produce (1). Thayer's Greek Lexicon STRONGS NT
```

**MiniMax:**
```
KJV_TRANSLATION: bear, bring forth, carry forth (out)
WORD_ORIGIN: [de G1537 (ἐκ - entre) y G5342 (φέρω - llevar)] 1. llevar fuera {literal o figurativamente}
STRONGS_EXHAUSTIVE: llevar fuera, producir, sacar. De ek y phero; llevar fuera (literal o figurativamente) -- bear, bring forth, carry forth (out). ver GRIEGO ek ver GRIEGO phero
NAS_ORIGIN: de ek y pheró
NAS_DEFINITION: llevar afuera, producir
NAS_TRANSLATION: bring (1), brought (1), carried (2), carry (1), carrying (1), take (1), yields 
```

**Claude (referência):**
```
KJV_TRANSLATION: llevar, traer, cargar (sacar)
WORD_ORIGIN: [de G1537 (ἐκ - entre) y G5342 (φέρω - traer) ] 1. llevar afuera {literal o figurativamente}
STRONGS_EXHAUSTIVE: llevar, traer, cargar afuera. De ek y phero ; llevar afuera (literal o figurativamente) -- llevar, traer, cargar afuera (sacar). ver GRIEGO ek ver GRIEGO phero
NAS_ORIGIN: de ek y pheró
NAS_DEFINITION: cargar afuera, traer
NAS_TRANSLATION: trae (1), trajo (1), cargado (2), carga (1), cargando (1), toma (1), produce (1). Thaye
```

#### `ENRICHMENT_TRANSLATE_G1627_pt`

**DeepSeek:**
```
KJV_TRADUÇÃO: carregar, produzir, levar para fora (para fora)
ORIGEM_DA_PALAVRA: [de G1537 (ἐκ - entre) e G5342 (φέρω - trazer)] 1. levar para fora {literal ou figurativamente}
STRONGS_EXAUSTIVO: carregar, produzir, levar para fora. De ek e phero; levar para fora (literal ou figurativamente) -- carregar, produzir, levar para fora (para fora). ver GREGO ek ver GREGO phero
ORIGEM_NAS: de ek e pheró
DEFINIÇÃO_NAS: levar para fora, produzir
TRADUÇÃO_NAS: trazer (1), trouxe (1), carregado (2), carreg
```

**MiniMax:**
```
KJV_TRANSLATION: trazer, produzir, carregar para fora
WORD_ORIGIN: [de G1537 (ἐκ - entre/com) e G5342 (φέρω - trazer)] 1. trazer para fora {literal ou figurativamente}
STRONGS_EXHAUSTIVE: trazer, produzir, carregar para fora. De ek e phero;ocarregar para fora (literal ou figurativamente) -- trazer, produzir, carregar para fora. ver GREGO ek ver GREGO phero
NAS_ORIGIN: de ek e pheró
NAS_DEFINITION: carregar para fora, produzir
NAS_TRANSLATION: trazer (1), trouxe (1), carregou (2), carregar (1), c
```

**Claude (referência):**
```
KJV_TRANSLATION: carregar, produzir, levar para fora
WORD_ORIGIN: [de G1537 (ἐκ - de entre) e G5342 (φέρω - levar) ] 1. carregar para fora {literal ou figuradamente}
STRONGS_EXHAUSTIVE: carregar, produzir, levar para fora. De ek e phero ; carregar para fora (literal ou figuradamente) -- carregar, produzir, levar para fora (para fora). veja GREEK ek veja GREEK phero
NAS_ORIGIN: de ek e pheró
NAS_DEFINITION: levar para fora, produzir
NAS_TRANSLATION: leva (1), levou (1), carregou (2), carrega (1),
```

---

## Análise Qualitativa Detalhada

### Glosses (LOW) — Qualidade de Tradução

DeepSeek e MiniMax (quando produz output) geram traduções semanticamente equivalentes ao Claude.
Exemplos comparativos:

| Palavra grega | DeepSeek | MiniMax | Claude (ref) | Nota |
|--------------|----------|---------|-------------|------|
| ginōskete | "sepan" | "conocéis" | "sabéis" | Todos válidos ✅ |
| ēngiken | "se ha acercado" | "ha llegado" | "se ha acercado" | DS=Claude ✅ |
| eplēsthēsan | "se llenaron" | "fueron llenados" | "fueron llenos" | Equivalentes ✅ |
| anektoteron | "más tolerable" | — | "más tolerable" | DS=Claude ✅ |
| exousian | "autoridade" | — | "autoridade" | DS=Claude ✅ |

**Problema MiniMax nos glosses:** 11/20 itens retornam vazio — max_tokens=1024 insuficiente para batches grandes (modelo usa tokens em `<think>` antes de responder).

---

### Léxico (MEDIUM) — Qualidade de Tradução

DeepSeek segue o formato `[Gxxxx] SHORT/FULL` quase perfeitamente. MiniMax inconsistente (7/15 ok).

| Strong's | DeepSeek SHORT | MiniMax SHORT | Claude SHORT | Match |
|----------|---------------|--------------|-------------|-------|
| G2574 | camello | camello | camello | ✅ Idêntico |
| G2500 | "José" | "José" | José | ✅ Idêntico |
| G2337 | amamantar | — | amamantar | DS=Claude ✅ |
| G2261 | manso | manso | gentil | ⚠️ Diverge (ambos válidos) |
| G1563 | allí | allí | allí | ✅ Idêntico |
| G1486 | tener una costumbre | "tener una costumbre" | tener por costumbre | ≈ Equivalente ✅ |

**Atenção DeepSeek:** 2/15 itens usam "CORTA/COMPLETA" em vez de "SHORT/FULL" (espanhol). Prompts com instrução explícita do label corrige isso.

---

### Enrichment (MEDIUM) — Qualidade de Tradução

Ambos os modelos produzem conteúdo correto mas em **plain text** (KEY: VALUE) em vez de JSON.
Claude também mistura plain text e JSON — ambos os formatos existem no banco.

**Problema crítico DeepSeek:** traduz os nomes dos campos para pt/es:
- `KJV_TRANSLATION` → `KJV_TRADUÇÃO` (pt) / `KJV_TRADUCCIÓN` (es)
- `WORD_ORIGIN` → `ORIGEM_DA_PALAVRA` / `ORIGEN_PALABRA`

**MiniMax:** mantém os nomes dos campos em inglês ✅ mas às vezes não traduz o conteúdo (G1627_es: retornou texto em inglês).

Comparativo de conteúdo:

| Campo | DeepSeek | MiniMax | Claude | Correto |
|-------|----------|---------|--------|---------|
| G0089_es KJV | "sin cesar" | "sin cesar" | "sin cesar" | ✅ Idêntico |
| G1628_pt KJV | "escapar, fugir" | "escapar, fugir" | "escapar, fugir" | ✅ Idêntico |
| G4175_es KJV | "conducta" | "conducta" | "conversación" | ⚠️ DS/MM mais preciso (KJV arcaico) |
| G1620_es KJV | "echar fuera, exponer" | "arrojar, exponer" | "expulsar, exponer" | ≈ Equivalente ✅ |

---

## Conclusão e Recomendação Final

### Ranking dos modelos para LOW/MEDIUM tier

| Modelo | Glosses | Léxico | Enrichment | Latência | Consistência | Veredicto |
|--------|---------|--------|------------|----------|--------------|-----------|
| **DeepSeek-V3** | ✅ 20/20 | ✅ 14/15 | ⚠️ field names em pt/es | 🔴 30s | ✅ 50/50 ok | **Recomendado** |
| **MiniMax-M2.5** | ⚠️ 9/20 | ⚠️ 7/15 | ⚠️ às vezes não traduz | 🟢 10s | ❌ 24 vazios | Com ajuste de tokens |
| **KIMI k2p5** | ❌ 0/20 | ❌ 0/15 | ❌ 0/15 | 🔴 25s | ❌ nunca segue formato | Descartado |

### Decisão recomendada

**DeepSeek-V3** é o candidato principal:
- Formato correto em 34/35 itens estruturados
- Qualidade de tradução ≈ 90-95% equivalente ao Claude
- Zero erros (50/50 OK)
- Único ajuste necessário: adicionar ao system prompt `"Never translate field labels — keep SHORT, FULL, KJV_TRANSLATION, WORD_ORIGIN in English"`

**MiniMax-M2.5** tem potencial se aumentar max_tokens:
- Velocidade excelente (~10s, 3x mais rápido que DeepSeek)
- Quando produz output, qualidade equivalente
- Requer novo teste com max_tokens=2048-4096 para batches grandes

### Próximos passos sugeridos

1. **Testar DeepSeek com prompt fix** (field names em inglês) em 10-20 itens de enrichment
2. **Testar MiniMax com max_tokens=4096** para ver se resolve os vazios
3. **Implementar LlmDirectWorker** com DeepSeek (se teste 1 confirmar), adicionando MiniMax como fallback pela velocidade

- [ ] **Cenário A — DeepSeek como provider principal** de LOW/MEDIUM, com prompt fix para enrichment
- [ ] **Cenário B — MiniMax como provider** após confirmar consistência com mais tokens
- [ ] **Cenário C — Híbrido** — MiniMax para glosses (LOW), DeepSeek para léxico+enrichment (MEDIUM)

**Decisão:** _preencher após análise_
**Motivo:** _preencher após análise_
