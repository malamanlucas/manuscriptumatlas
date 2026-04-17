---
name: drain-queue
description: Wrapper safe para drenar a llm_prompt_queue — checa rate-limit, dispatcha para queue-dispatcher com batch=50 parallelism=5
user_invocable: true
---

# Drain Queue

Wrapper de alto nivel que invoca `queue-dispatcher` com defaults seguros. Substitui o uso ad-hoc de `/run-llm` para drenar grandes volumes.

## Passos

### 1. Pre-flight

```bash
# Rate limit ativo?
RATE_FILE=/tmp/claude_rate_limit_until
if [ -f "$RATE_FILE" ]; then
  UNTIL=$(cat "$RATE_FILE" | tr -d '[:space:]')
  NOW=$(date +%s)
  if [ -n "$UNTIL" ] && [ "$NOW" -lt "$UNTIL" ]; then
    echo "ABORT: usage limit until $(date -r "$UNTIL" 2>/dev/null || date -d @"$UNTIL")"
    exit 0
  fi
fi

# Token admin
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/dev-login?email=dev@manuscriptum.local" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
echo "$TOKEN" > /tmp/atlas_token.txt

# Stats
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/llm/queue/stats | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(f'Pending: {d[\"totalPending\"]}  Processing: {d[\"totalProcessing\"]}  Completed: {d[\"totalCompleted\"]}')
if d['totalPending'] == 0 and d['totalCompleted'] == 0:
    print('SKIP: nada para drenar')
    sys.exit(0)
"
```

Se `totalPending==0`, sair.

### 2. Unstick stale (seguranca)

```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/unstick?staleMinutes=10"
```

### 3. Invocar `queue-dispatcher`

Via Task tool com subagent_type=`queue-dispatcher`. Parametros obrigatorios:
- `batchSize: 50`
- `parallelism: 5` (HARD CAP)
- `maxWaves: 20` (limita para sessao nao estourar context — total ~1000 items por invocacao)

### 4. Apply por fase

Apos o dispatcher retornar, aplicar para cada fase com items completed:

```bash
TOKEN=$(cat /tmp/atlas_token.txt)
for PHASE in bible_translate_glosses bible_translate_hebrew_lexicon bible_translate_lexicon bible_translate_enrichment_greek bible_translate_enrichment_hebrew; do
  curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/apply/$PHASE" | python3 -m json.tool
done
```

### 5. Reportar stats finais

```bash
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/llm/queue/stats | python3 -m json.tool
```

## Quando usar

- Volume > 100 items pending E < ~2000 (sessao unica).
- Quer drenar com total controle de parallelism.
- Para volumes > 2000 items, preferir **headless**: `./scripts/drain-safe.sh`.

## Regras

- Parallelism **SEMPRE** 5. Nunca ultrapassar.
- Se qualquer wave detectar `usage limit`, o dispatcher aborta e escreve `/tmp/claude_rate_limit_until`.
- Sem retry em loop.
