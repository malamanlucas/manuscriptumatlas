# Relatório de Verificação — Traduções IA (Concílios + Pais da Igreja)

> Gerado em 10/04/2026 ~18:00 BRT

---

## 1. Concílios (`council_translations`)

### Estado Atual
- **12 traduções restantes** (após deletar 100 erradas)
- Todas as 12 passaram na verificação de nomes — **OK**

### Problema Encontrado e Corrigido
As 100 traduções machine-generated estavam com **nomes trocados** — o texto do Wikipedia de um concílio mencionava outros concílios, e a IA usou esse texto para gerar o nome/resumo errado.

| Council ID | Nome Real | Nome Traduzido (ERRADO) |
|---|---|---|
| 145 | First Council of Nicaea | Sínodo de Jerusalém |
| 147 | Council of Ephesus | Concílio de Antioquia |
| 152 | Fourth Council of Constantinople | Concílio de Niceia |
| 153 | Council of Jerusalem | Concílio de Éfeso |
| 154 | Council of Rome (155) | Concílio de Trento |

**Causa:** A fase `council_summaries` passava `originalText` (Wikipedia) como campo `summary` para tradução. O texto Wikipedia frequentemente começa mencionando OUTROS concílios.

**Correção:** 100 traduções deletadas, 18 novas enfileiradas com prompt corrigido. Prompt agora enfatiza: "The 'displayName' field is the CORRECT name — translate it literally, do NOT replace it."

---

## 2. Pais da Igreja (`church_father_translations`)

### PROBLEMA CRÍTICO: Biografias Deslocadas (Off-by-One)

**70 traduções verificadas — quase TODAS com biografia do Pai ANTERIOR.**

O padrão é sistemático: cada Pai recebeu a biografia traduzida do Pai de Igreja com ID anterior na sequência.

| ID | Pai da Igreja | Biografia deveria ser sobre | Biografia recebida é sobre |
|---|---|---|---|
| 142 | Ignatius of Antioch | Inácio | **Clemente de Roma** (ID 141) |
| 143 | Polycarp of Smyrna | Policarpo pt | **Clemente de Roma** (ID 141) |
| 143 | Polycarp of Smyrna | Policarpo es | Policarpo (correto) |
| 145 | Justin Martyr | Justino pt | **Papias** (ID 144) |
| 145 | Justin Martyr | Justino es | **Papias** (ID 144) |
| 147 | Clement of Alexandria | Clemente de Alex. | **Ireneu de Lyon** (ID 146) |
| 148 | Tertullian | Tertuliano pt | **Ireneu** (ID 146) |
| 150 | Origen | Orígenes | **Hipólito** (ID 149) |
| 152 | Eusebius of Caesarea | Eusébio | **Cipriano** (ID 151) |
| 153 | Athanasius | Atanásio pt | **Cipriano** (ID 151) |
| 154 | Ephrem the Syrian | Efrém | **Atanásio** (ID 153) |
| 155 | Hilary of Poitiers | Hilário | **Atanásio** (ID 153) |
| 157 | Gregory of Nazianzus | Gregório Naz. | **Basílio** (ID 156) |
| 158 | Gregory of Nyssa | Gregório Nissa pt | **Basílio** (ID 156) |
| 159 | Ambrose of Milan | Ambrósio | **Gregório de Nissa** (ID 158) |
| 160 | John Chrysostom | Crisóstomo | **Gregório de Nissa** (ID 158) |
| 162 | Augustine of Hippo | Agostinho | **Jerônimo** (ID 161) |
| 163 | Cyril of Alexandria | Cirilo pt | **Jerônimo** (ID 161) |
| 164 | Theodoret | Teodoreto | **Cirilo** (ID 163) |
| 165 | Leo the Great | Leão Magno | **Cirilo** (ID 163) |
| 167 | Gregory the Great | Gregório Magno | **Shenoute** (ID 166) |
| 168 | Maximus the Confessor | Máximo pt | **Shenoute** (ID 166) |
| 169 | Isidore of Seville | Isidoro | **Máximo** (ID 168) |
| 170 | Isaac of Nineveh | Isaac | **Máximo** (ID 168) |
| 172 | John of Damascus | João Damasceno | **Beda** (ID 171) |
| 173 | Theodore the Studite | Teodoro pt | **Beda** (ID 171) |
| 174 | Photius | Fócio | **Teodoro** (ID 173) |
| 175 | Symeon the New Theologian | Simeão | **Teodoro** (ID 173) |

### Traduções CORRETAS (nomes OK, biografia OK)
- 141 Clement of Rome — pt e es OK
- 148 Tertullian — es OK (pt errado)
- 149 Hippolytus — pt e es OK
- 151 Cyprian — pt e es OK (nome OK, bio parece OK)
- 156 Basil of Caesarea — es OK
- 158 Gregory of Nyssa — es OK
- 161 Jerome — pt e es OK
- 166 Shenoute — pt OK
- 171 Bede — pt e es OK
- 173 Theodore Studite — es OK

### Causa Raiz Provável

O bug de off-by-one ocorre no fluxo de enqueue/apply:
1. A fase `runTranslateBiographies` no `PatristicIngestionService` itera sobre os Pais e enfileira traduções
2. O `enqueueBioTranslate` recebe `fatherId` e `text` (a biografia em inglês)
3. O `LlmResponseProcessor.applyBioTranslation()` usa o `fatherId` do `callbackContext` para inserir

**Hipótese:** O texto da biografia (`entry.biographyOriginal`) pode estar sendo lido da lista de seed data (`ChurchFathersSeedData.entries`) que está em ordem diferente dos IDs no banco. Ou o `fatherId` no callback está correto mas o `text` passado é do entry anterior no loop.

### Ação Necessária

1. **Deletar TODAS as 70 traduções machine** dos Pais da Igreja
2. **Investigar e corrigir** o bug de off-by-one no `PatristicIngestionService.runTranslateBiographies()` ou no `enqueueBioTranslate()`
3. **Re-enfileirar** e reprocessar

---

## 3. Resumo

| Domínio | Total | Corretas | Erradas | Status |
|---|---|---|---|---|
| Concílios | 12 | 12 | 0 (100 já deletadas) | OK — 18 re-enfileiradas |
| Pais da Igreja | 70 | ~12 | ~58 | CRÍTICO — off-by-one |

---

## 4. Causa Raiz Identificada — Bug no `/run-llm` (Batch Processing)

### O que aconteceu

O skill `/run-llm` agrupava **multiplos itens em um unico Agent** com o formato:
```
===ITEM {id}===
{resposta}
```

O problema: a IA retornava as respostas **desalinhadas** — a resposta do item N era atribuida ao item N+1. Isso causou o off-by-one sistematico em todas as traducoes de Pais da Igreja.

### Evidencia

| Queue Label | Input (correto) | Output (ERRADO) |
|---|---|---|
| BIO_TRANSLATE_142_pt | "Ignatius of Antioch..." | "Clemente de Roma..." (ID 141) |
| BIO_TRANSLATE_145_pt | "Justin Martyr..." | "Papias..." (ID 144) |
| BIO_TRANSLATE_150_pt | "Origen..." | "Hipólito..." (ID 149) |
| BIO_TRANSLATE_162_pt | "Augustine..." | "Jerônimo..." (ID 161) |

O `user_content` (input) na fila estava CORRETO. O `response_content` (output) estava ERRADO — continha a traducao do item anterior.

### Correcao Aplicada

1. **70 traducoes machine deletadas** de `church_father_translations`
2. **70 itens da fila resetados** para `pending` (response limpo)
3. **Skill `/run-llm` reescrito**: agora processa **1 item por Agent** (nao mais batch)
   - Cada Agent recebe exatamente 1 systemPrompt + 1 userContent
   - Retorna exatamente 1 resposta
   - O ID e atribuido sem ambiguidade
   - Paralelismo mantido via multiplos Agents simultaneos (3-5)
4. **296 traducoes de concilios** tambem resetadas e re-enfileiradas

### Estado Atual
- 70 bio_translate_prepare: `pending` (aguardando `/run-llm`)
- 18 council_translate_prepare: `pending`
- Skill corrigido — proximo `/run-llm` vai processar corretamente
