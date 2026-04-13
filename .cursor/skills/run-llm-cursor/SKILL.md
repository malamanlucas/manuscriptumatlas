---
name: run-llm-cursor
description: Process Manuscriptum Atlas `llm_prompt_queue` items in a loop using Cursor subagents, tier-aware model routing, and the backend `claim -> complete/fail -> apply` flow. Use when the user mentions run-llm, LLM queue, prompt queue, claim, apply, retry, unstick, or wants to drain pending LLM prompts from Cursor.
---

# Run LLM Queue (Cursor)

Use this skill when Cursor should act as the async worker for `llm_prompt_queue`.

## Quick Start

- Use `Composer 2` or `Auto` only for orchestration: auth, stats, claim, dispatch, complete/fail, apply, retry, `unstick`.
- Route each claimed item to exactly one subagent:
  - `llm-queue-low` -> `gpt-5-mini`
  - `llm-queue-medium` -> `gpt-5`
  - `llm-queue-high` -> `gpt-5.4`
- Hard rule: `1 claimed item = 1 subagent = 1 response`.

## Tier -> Subagent -> Model

| Queue scope | Use | Subagent | Model |
|---|---|---|---|
| `tier=LOW` | glosses and other simple 1:1 tasks | `llm-queue-low` | `gpt-5-mini` |
| `phaseName` contains `bible_translate_enrichment_` | simple MEDIUM enrichment | `llm-queue-low` | `gpt-5-mini` |
| `tier=MEDIUM` for `bible_translate_lexicon`, `bible_translate_hebrew_lexicon`, `BIO_*`, `COUNCIL_*`, `HERESY_*`, `council_*`, `heresy_*`, `bio_*` | structured MEDIUM work | `llm-queue-medium` | `gpt-5` |
| `tier=HIGH`, `phaseName` starts with `bible_align_`, `DatingEnrichment:`, `dating_`, `apologetics_` | high-precision structured work | `llm-queue-high` | `gpt-5.4` |
| repeated schema failures in a structured phase | manual rescue escalation | `llm-queue-high` | `gpt-5.4` |

Fast variants are not the default. They cost much more for modest speed gains.

## Authentication

Default dev token:

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/dev-login?email=dev@manuscriptum.local" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
```

If `SERVICE_CLIENT_ID` is configured, prefer client credentials:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
  -d "grant_type=client_credentials&client_id=${SERVICE_CLIENT_ID}&client_secret=${SERVICE_CLIENT_SECRET}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

## Queue Workflow

### 1. Inspect the queue

```bash
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/llm/queue/stats
```

If the requested scope has no `pending` items, stop.

### 2. Recover only when needed

- If the user asked to resume after an interruption, inspect `processing`.
- Prefer stale-only recovery first:

```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/llm/queue/unstick?staleMinutes=10"
```

- Prefer phase-scoped recovery when the user named a phase:

```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/llm/queue/unstick?phase=bible_translate_glosses&staleMinutes=10"
```

- Never run full `unstick` while other sessions may still be active unless the user explicitly wants that.

### 3. Claim in priority order

Default order:
1. `LOW`
2. `MEDIUM` enrichment
3. `MEDIUM` structured
4. `HIGH`

Suggested claim sizes:

```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/claim?tier=LOW&limit=50"
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/claim?phase=bible_translate_enrichment_greek&limit=50"
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/claim?tier=MEDIUM&phase=bible_translate_lexicon&limit=50"
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/claim?tier=HIGH&limit=10"
```

For user-requested scope, claim only that scope.

### 4. Dispatch exactly one item per subagent

For each claimed item:

1. Choose the subagent from the routing table.
2. Pass `id`, `phaseName`, `label`, `systemPrompt`, and `userContent`.
3. Instruct the subagent to return only the raw response content.
4. Run small parallel batches, usually `3-4` items at a time.

Use this prompt template when invoking a worker:

```text
Process exactly one Manuscriptum Atlas queue item and return only the final response content.
Do not add markdown, prefixes, explanations, or quotes.

id: <id>
phaseName: <phaseName>
label: <label>

systemPrompt:
<systemPrompt>

userContent:
<userContent>
```

Never send multiple queue items to the same subagent.

### 5. Save each result immediately

- On success, call `/complete` for that item.
- On failure, call `/fail` for that item and continue.
- Always JSON-encode `responseContent` with Python before POSTing. Never interpolate raw model text directly into shell JSON.
- Report the real model ID in `modelUsed`.

Success payload shape:

```json
{
  "id": 123,
  "responseContent": "<raw model response>",
  "modelUsed": "gpt-5-mini",
  "inputTokens": 0,
  "outputTokens": 0
}
```

Failure payload shape:

```json
{
  "message": "<error>"
}
```

### 6. Apply by phase

After finishing the claimed items for a phase in the current round:

```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/llm/queue/apply/<phaseName>
```

Do not apply after every single item.

### 7. Repeat

Repeat stats/claim/dispatch/complete/apply until:

- the requested phase is empty, or
- the requested tier is empty, or
- the whole queue is drained in the selected priority order.

## Failure Handling

- Use `/fail` for a bad single item in the current round.
- Use `/retry?phase=...` when the user wants to requeue previously failed items for a phase.
- If structured output keeps failing in a MEDIUM phase, escalate that phase manually to `llm-queue-high`.
- Do not modify `callbackContext`.

## Parallel Sessions

For maximum throughput without runaway cost, prefer `2-3` chats:

- chat 1: `LOW` or `bible_translate_glosses`
- chat 2: `bible_translate_enrichment_greek` or `bible_translate_enrichment_hebrew`
- chat 3: `bible_translate_lexicon` or another structured MEDIUM phase

This works because `/claim` uses `SELECT FOR UPDATE SKIP LOCKED`.

## Response Quality Rules

- Preserve the exact format requested by the queue prompt.
- If the prompt asks for JSON, return valid JSON only.
- If the prompt expects line-oriented output, keep line counts aligned.
- Never add fences, commentary, headers, or summaries.

## How To Use

### Aggressive parallel drain (host script)

Cursor subagents (`llm-queue-*`) cannot run `docker`/`psql` in Ask mode; for **high parallelism with minimal orchestration**, use the repo script (same tier/phase → model routing as this skill, **one OpenAI call per queue item**, no temp JSON files):

```bash
set -a; source deploy/.env; set +a   # OPENAI_API_KEY + model envs
# Optional: QUEUE_UNSTICK_MINUTES=10  — only stale “processing” rows
python3 scripts/drain_llm_prompt_queue.py
```

Default execution policy in the script: **LOW** claim 120 / up to **8** workers; **MEDIUM enrichment** claim 100 / up to **6**; **MEDIUM structured** claim 50 / up to **4**; **HIGH** claim 10 / up to **2**. Priority: LOW → enrichment phases (greek, hebrew) → any remaining MEDIUM → HIGH. Stats are refreshed only when a full claim round finds no work (end-of-scope). Set `STRICT_JSON_VALIDATE=1` only when debugging malformed JSON.

Environment knobs: `DRAIN_MAX_BATCHES`, `OPENAI_MAX_429_RETRIES`, `OPENAI_429_BACKOFF_SEC`, `OPENAI_HTTP_TIMEOUT`.

### Cursor-only (subagents)

Standard full drain:

```text
Use the run-llm-cursor skill to process the LLM queue in a loop until it is empty.
Prioritize LOW, then MEDIUM enrichment, then MEDIUM structured, then HIGH.
Claim 50 items at a time and process up to 4 workers in parallel per round.
```

Single phase:

```text
Use the run-llm-cursor skill to process only the phase bible_translate_glosses in a loop.
Use the default model routing and stop when that phase has no more pending items.
```

Recovery:

```text
Use the run-llm-cursor skill to unstick stale processing items for bible_translate_glosses with staleMinutes=10 and then resume the loop.
```
