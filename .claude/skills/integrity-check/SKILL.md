---
name: integrity-check
description: Verifica integridade da llm_prompt_queue e tabelas destino (bible_db) depois de concluir uma fase de ingestão. Reporta violações com comandos de fix.
user_invocable: true
---

# Integrity Check

Roda os contratos de integridade do `scripts/integrity_check.py` e interpreta o resultado. Usar **depois** de concluir uma fase de ingestão (`/drain-queue`, `/run-llm`) para confirmar que aplicou direito.

## Passos

### 1. Executar o script

```bash
cd "$(git rev-parse --show-toplevel 2>/dev/null || echo /Users/user1/Documents/GitHub/manuscriptumatlas)"
./scripts/integrity_check.py --json > /tmp/integrity_$$.json
RC=$?
cat /tmp/integrity_$$.json
```

Exit code: `0` = tudo verde, `1` = há violações.

### 2. Ler e tabular

```bash
python3 -c "
import json, sys
d = json.load(open('/tmp/integrity_$$.json'))
s = d['summary']
print(f\"\\n=== Integrity: {s['passed']}/{s['total']} passed ({s['failed']} failed) ===\\n\")
for c in d['checks']:
    mark = '[OK]' if c['passed'] else '[FAIL]'
    print(f\"{mark}  {c['name']}\")
    print(f\"       {c['detail']}\")
    if not c['passed'] and c.get('fix'):
        print(f\"       fix: {c['fix']}\")
    print()
"
```

### 3. Se houver violações, detalhar cada uma

Para cada check `[FAIL]`, executar a query diagnóstica correspondente:

| Check | Query diagnóstica | Fix sugerido |
|---|---|---|
| `no_orphaned_claims` | `SELECT id, phase_name, label, claimed_at FROM llm_prompt_queue WHERE status='processing' AND claimed_at < NOW() - INTERVAL '10 minutes' LIMIT 20;` | `curl -X POST $BACKEND/admin/llm/queue/unstick?staleMinutes=10` |
| `response_content_present` | `SELECT id, phase_name, status FROM llm_prompt_queue WHERE status IN ('completed','applied') AND (response_content IS NULL OR LENGTH(response_content)=0) LIMIT 20;` | Investigar item-por-item; provavelmente bug no save. Reset com `/retry?phase=...`. |
| `callback_context_parseable` | `SELECT id, phase_name, LEFT(callback_context,200) FROM llm_prompt_queue WHERE status<>'pending' AND callback_context IS NOT NULL AND callback_context::jsonb IS NULL LIMIT 10;` | Items com JSON malformado — deletar ou resetar para pending. |
| `glosses_applied_reflects_destination` | `SELECT language, COUNT(*) FILTER (WHERE portuguese_gloss IS NOT NULL) AS pt, COUNT(*) FILTER (WHERE spanish_gloss IS NOT NULL) AS es, COUNT(*) AS total FROM interlinear_words GROUP BY language;` | Rerun `POST /admin/bible/ingestion/run/bible_translate_glosses` + apply. |
| `hebrew_pt_translations_consistent` | `SELECT hl.strongs_number, hl.lemma FROM hebrew_lexicon hl LEFT JOIN hebrew_lexicon_translations t ON t.lexicon_id=hl.id AND t.locale='pt' WHERE hl.short_definition IS NOT NULL AND t.short_definition IS NULL LIMIT 20;` | Rerun `POST /admin/bible/ingestion/run/bible_translate_hebrew_lexicon` + apply. |
| `no_duplicate_applied` | `SELECT phase_name, md5(callback_context) AS h, COUNT(*) FROM llm_prompt_queue WHERE status='applied' AND callback_context IS NOT NULL GROUP BY phase_name, md5(callback_context) HAVING COUNT(*) > 1 LIMIT 10;` | Investigar: o `translateLexiconPrepare` / `translateGlossesPrepare` filtra por estado do destino, mas itens antigos `applied` de rodadas anteriores podem bater com novos. Remover duplicatas mais antigas. |

Use o MCP `postgres-nt` e `postgres-bible` para rodar essas queries sob demanda.

### 4. Reportar ao usuário

Formato:

```
Integrity check concluído: N/6 OK

Violações encontradas:
  - hebrew_pt_translations_consistent: 991 gaps
    Amostra: [H0418, H0421, H0424, ...]
    Fix: POST /admin/bible/ingestion/run/bible_translate_hebrew_lexicon

  - no_duplicate_applied: 43 duplicatas
    Amostra: phase=bible_translate_glosses com callback_context duplicado
    Fix: investigar lógica de prepare (items antigos não são filtrados fora)
```

### 5. Limpeza

```bash
rm -f /tmp/integrity_$$.json
```

## Quando invocar

- **Depois de cada `/drain-queue` ou `/run-llm`** em fase de ingestão nova.
- **Antes de considerar uma fase "concluída"** (ex: terminar `bible_translate_glosses` 100%).
- **Periodicamente** em produção para detectar regressão silenciosa.

## Regras

- **NUNCA aplicar fix automaticamente** sem aprovação do usuário — o skill só reporta e sugere.
- **Read-only** no banco — script não executa DELETE/UPDATE.
- Respeitar tempo: se o script demorar > 60s, reportar e sair (pode indicar DB travado).
