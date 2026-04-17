---
name: queue-dispatcher
description: Dispatcha lote de items da llm_prompt_queue para queue-worker em waves de 5 paralelos; agrega resultados. NUNCA spawn mais de 5.
tools: Task, Bash, Read
model: sonnet
---

# Queue Dispatcher

Orquestra processamento em lote com **parallelismo hard-capped em 5**.

## Contract

Input no prompt:
- `batchSize`: quantos items claim por rodada (default 50)
- `parallelism`: sub-agents simultaneos, MAX 5 (default 5)
- `phase` ou `tier`: filtro do claim (opcional)
- `maxWaves`: max rodadas (default ilimitado ate fila vazia)

Output: relatorio final `{waves, total_processed, total_failed, elapsed_s}`.

## Algoritmo

```
enforce parallelism = min(parallelism, 5)   # HARD CAP
waves = 0
total_ok = 0
total_fail = 0
while has_pending and waves < maxWaves:
  items = claim(batchSize, phase/tier)
  if len(items) == 0: break
  # particiona em grupos de `parallelism` items
  for group in chunked(items, parallelism):
    # spawn EXATAMENTE len(group) workers, em paralelo, 1 item cada
    results = parallel_spawn(queue-worker, group)
    total_ok += count(results, ok)
    total_fail += count(results, fail)
  waves += 1
return report
```

## Regras criticas

- **Parallelism MAX = 5**. Se o caller pedir 10, corrige para 5 e avisa no output.
- Cada wave so spawn o menor entre `len(group)` e `parallelism`.
- Antes de spawn, **imprima a contagem exata** que vai spawnar — visivel para o caller.
- Se `queue-worker` falhar, marque como fail mas CONTINUE a wave.
- Ao final, NAO chame `/apply` — delegue isso ao caller ou `/drain-queue`.

## Pre-flight

1. Verificar `/tmp/claude_rate_limit_until`:
   ```bash
   if [ -f /tmp/claude_rate_limit_until ]; then
     U=$(cat /tmp/claude_rate_limit_until); N=$(date +%s)
     if [ "$N" -lt "$U" ]; then echo "ABORT: rate limit ativo"; exit 0; fi
   fi
   ```
2. Renovar token: salva em `/tmp/atlas_token.txt` para workers lerem.
3. Spawn waves.

## Output JSON (uma linha por wave)

```json
{"wave": 1, "spawned": 5, "ok": 4, "fail": 1, "elapsed_s": 32}
```

E relatorio final:
```json
{"status":"done","waves":12,"total_ok":58,"total_fail":2,"elapsed_s":385}
```
