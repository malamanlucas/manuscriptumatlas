---
name: queue-status
description: Mostra stats da llm_prompt_queue, detecta stale claims, estima ETA e flagra usage-limit ativo — read-only, não aciona nada
user_invocable: true
---

# Queue Status

Exibe visão rapida da `llm_prompt_queue` antes de decidir iniciar um drain. **Não claima, não processa, não aplica.**

## Passos

### 1. Token

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/dev-login?email=dev@manuscriptum.local" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
```

### 2. Rate-limit ativo?

```bash
RATE_FILE=/tmp/claude_rate_limit_until
if [ -f "$RATE_FILE" ]; then
  UNTIL=$(cat "$RATE_FILE" | tr -d '[:space:]')
  NOW=$(date +%s)
  if [ -n "$UNTIL" ] && [ "$NOW" -lt "$UNTIL" ]; then
    echo "Usage limit ativo ate $(date -r "$UNTIL" 2>/dev/null || date -d @"$UNTIL")"
    echo "Restam $((UNTIL - NOW))s"
  else
    echo "Rate-limit file existe mas timestamp ja passou (OK pra drenar)"
  fi
else
  echo "Rate-limit: OK"
fi
```

### 3. Stats da fila

```bash
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/llm/queue/stats | python3 -m json.tool
```

Exibir: `totalPending`, `totalProcessing`, `totalCompleted`, `totalApplied`, `totalFailed` e por fase.

### 4. Stale claims (items em `processing` ha > 10 min)

Via postgres MCP (`mcp__postgres-nt__query`):

```sql
SELECT phase_name, COUNT(*) AS stale
FROM llm_prompt_queue
WHERE status='processing'
  AND claimed_at IS NOT NULL
  AND claimed_at < NOW() - INTERVAL '10 minutes'
GROUP BY phase_name;
```

Se > 0 → recomendar `POST /admin/llm/queue/unstick?staleMinutes=10`.

### 5. ETA estimada

- Throughput observado: ~50 items/min com 5 sub-agents Haiku (drenagens anteriores).
- ETA = `totalPending / 50` minutos.

Exemplo de output:
```
Pending: 2790
Processing: 0
Applied: 33953
Failed: 0
ETA drain @ 50 items/min com 5 agents: ~56 min
```

### 6. Regras (quando NAO drenar)

- Rate-limit ativo → esperar reset 17:00 America/Sao_Paulo.
- Stale > 50 items → rodar `unstick` primeiro.
- `totalFailed > 0` de fase crítica → investigar com `SELECT id, label, error_message FROM llm_prompt_queue WHERE status='failed' LIMIT 10` antes de retry.

## Uso

Invocar via `/queue-status` antes de `/run-llm` ou `/drain-queue` para decidir se vale a pena iniciar a rodada.
