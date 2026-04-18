---
name: run-llm
description: Processa prompts LLM pendentes da fila do backend usando Claude Code como motor LLM
user_invocable: true
---

# Run LLM Queue

Voce e o motor LLM do Manuscriptum Atlas. Leia prompts pendentes da fila, processe-os com o modelo adequado ao tier, e salve os resultados no backend.

## Autenticacao

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/dev-login?email=dev@manuscriptum.local" | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
```

Se `SERVICE_CLIENT_ID` estiver configurado, prefira client_credentials:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
  -d "grant_type=client_credentials&client_id=${SERVICE_CLIENT_ID}&client_secret=${SERVICE_CLIENT_SECRET}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

## Tier → Modelo

| Tier | Fases | Modelo | Model ID |
|------|-------|--------|----------|
| **LOW** | `bible_translate_enrichment_*` | **Haiku** | `claude-haiku-4-5` |
| **MEDIUM** | `bible_translate_enrichment_*` | **Haiku** | `claude-haiku-4-5` |
| **MEDIUM** | `bible_translate_glosses`, `bible_translate_lexicon`, `bible_translate_hebrew_lexicon`, `council_*`, `heresy_*`, `bio_*` | **Sonnet** | `claude-sonnet-4-6` |
| **HIGH** | `bible_align_*`, `dating_*`, `apologetics_*` | **Opus** | `claude-opus-4-7` |

**Regra rapida:** `enrichment` no nome → Haiku. `tier=LOW` → Haiku. `tier=MEDIUM` (resto) → Sonnet. `tier=HIGH` → Opus direto.

## Fluxo

### 0. Pre-flight rate-limit check

Antes de qualquer coisa, verificar se já sabemos que o usage limit esta ativo:

```bash
RATE_FILE=/tmp/claude_rate_limit_until
if [ -f "$RATE_FILE" ]; then
  UNTIL=$(cat "$RATE_FILE" 2>/dev/null | tr -d '[:space:]')
  NOW=$(date +%s)
  if [ -n "$UNTIL" ] && [ "$NOW" -lt "$UNTIL" ]; then
    REMAIN=$((UNTIL - NOW))
    echo "SKIP /run-llm: usage limit until $(date -r "$UNTIL" 2>/dev/null || date -d @"$UNTIL") (${REMAIN}s restantes)" >&2
    exit 0
  fi
fi
```

**Regra:** se o arquivo existe com timestamp futuro, SAIR silencioso (exit 0). Nunca retry em loop. Quando 17:00 America/Sao_Paulo passar e o cron rodar de novo, `NOW >= UNTIL` e o fluxo segue.

### 1. Stats — ver o que tem na fila

```bash
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/llm/queue/stats
```

Se `totalPending` = 0 → fila vazia, parar.

### 2. Claim + processar

Ordem de prioridade: LOW → MEDIUM enrichment → MEDIUM lexicon → HIGH.

**Claim ate 50 itens por vez:**
```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/claim?tier=LOW&limit=50"
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/claim?tier=MEDIUM&limit=50"
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/claim?tier=HIGH&limit=10"
```

**Sessao dedicada a uma fase especifica (maximo paralelismo com 4+ terminais):**
```bash
# Terminal 1 — glosses (LOW)
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/claim?tier=LOW&limit=50"

# Terminal 2 — enrichment hebraico (MEDIUM/Haiku, maior volume)
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/claim?phase=bible_translate_enrichment_hebrew&limit=50"

# Terminal 3 — enrichment grego (MEDIUM/Haiku)
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/claim?phase=bible_translate_enrichment_greek&limit=50"

# Terminal 4 — lexicon (MEDIUM/Sonnet, volume menor)
curl -s -X POST -H "Authorization: Bearer $TOKEN" "http://localhost:8080/admin/llm/queue/claim?tier=MEDIUM&phase=bible_translate_lexicon&limit=50"
```

> Sessoes dedicadas eliminam competicao entre terminais e maximizam throughput.

### 3. Processar — UM ITEM POR VEZ por Agent

**REGRA CRITICA DE CORRECAO:** Cada Agent deve processar **exatamente 1 item** e retornar **exatamente 1 resposta**. NAO agrupe multiplos itens em um unico Agent — isso causa respostas desalinhadas (o item N recebe a resposta do item N-1).

**Para cada item claimed:**

1. **Spawne um Agent** com o modelo correto (`model: "haiku"`, `model: "sonnet"`, ou processe direto se Opus).
2. Passe o `systemPrompt` e `userContent` do item.
3. O Agent deve retornar **APENAS a resposta**, sem explicacoes, sem prefixo, sem markdown.

**Paralelismo seguro:** Spawne **multiplos Agents em paralelo** (3-5 simultaneos), mas cada um processando **1 unico item**. Assim voce tem velocidade E correcao.

Exemplo para 5 itens Haiku em paralelo:
```
Agent 1 (haiku) → item[0] → resposta para item[0].id
Agent 2 (haiku) → item[1] → resposta para item[1].id
Agent 3 (haiku) → item[2] → resposta para item[2].id
Agent 4 (haiku) → item[3] → resposta para item[3].id
Agent 5 (haiku) → item[4] → resposta para item[4].id
```

Cada Agent retorna a resposta, e voce salva com o ID correto do item que foi enviado a AQUELE Agent.

### 4. Salvar resultados

Para cada resposta de Agent, salve imediatamente com **estimativa de tokens** (aprox. `ceil(len(texto) / 4)` — padrao da industria quando nao ha metadata de API).

```bash
IN=$(( (${#SYSTEM_PROMPT} + ${#USER_CONTENT} + 3) / 4 ))
OUT=$(( (${#RESPONSE} + 3) / 4 ))
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  http://localhost:8080/admin/llm/queue/{id}/complete \
  -d "{\"id\":<id>,\"responseContent\":\"<resposta>\",\"modelUsed\":\"<model_id>\",\"inputTokens\":$IN,\"outputTokens\":$OUT}"
```

**Por que estimativa e nao zero?** Observabilidade de custo/volume. `input_tokens=0`/`output_tokens=0` inviabiliza dashboards de uso. A estimativa char/4 erra ~15% mas da ordem de grandeza correta para graficos.

Se falhar:
```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  http://localhost:8080/admin/llm/queue/{id}/fail \
  -d '{"message":"<erro>"}'
```

**Se detectar `usage limit` / `rate_limit` na resposta:** escreva o proximo reset (17:00 America/Sao_Paulo = 20:00 UTC) em `/tmp/claude_rate_limit_until` e **aborte o /run-llm**:

```bash
# proximo 17:00 America/Sao_Paulo (20:00 UTC) em epoch
NEXT_RESET=$(python3 -c "
import datetime, time
now = datetime.datetime.now(datetime.timezone.utc)
target = now.replace(hour=20, minute=0, second=0, microsecond=0)
if target <= now: target += datetime.timedelta(days=1)
print(int(target.timestamp()))
")
echo "$NEXT_RESET" > /tmp/claude_rate_limit_until
echo "ABORT: usage limit hit; next reset epoch $NEXT_RESET" >&2
exit 2
```

### 5. Apply ao banco

Apos completar todos os itens de uma fase:
```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/admin/llm/queue/apply/{phaseName}
```

### 6. Repetir

Volte ao passo 1. Processe ate a fila esvaziar ou o loop externo parar.

## Dicas de velocidade

- **Paralelismo por item:** Spawne 3-5 Agents em paralelo, cada um com 1 item. Isso e rapido E correto.
- **Nao agrupe itens em um Agent:** Isso causa off-by-one nas respostas. SEMPRE 1 item = 1 Agent.
- **Claims grandes:** Claim 50 de uma vez, depois processe em lotes de 5 paralelos.
- **Nao espere apply:** Chame apply apos salvar todos os itens de uma fase, nao apos cada item.
- **Pule stats repetido:** Apos o primeiro check, va direto pro claim.
- **Sessoes dedicadas:** Com 4+ terminais, use `?phase=` para dedicar cada terminal a uma fase. Elimina competicao.

## Recuperar itens travados (orphaned)

Se o processo foi interrompido, itens podem ficar presos em `processing`. Use:

```bash
# Reset SOMENTE itens em processing ha mais de 10 minutos (seguro com sessoes ativas)
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/llm/queue/unstick?staleMinutes=10"

# Reset de uma fase especifica (mais seguro)
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/llm/queue/unstick?phase=bible_translate_glosses&staleMinutes=10"

# Reset total (usar apenas quando NENHUMA sessao estiver rodando)
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/llm/queue/unstick"
```

## Notas

- O endpoint `/claim` usa `SELECT FOR UPDATE SKIP LOCKED` — multiplas sessoes podem rodar em paralelo sem conflito.
- O `callbackContext` contem JSON para o downstream — nao modifique.
- Reporte `modelUsed` com o ID real do modelo.
- Use `curl` via Bash tool para chamadas HTTP (WebFetch nao funciona com localhost).
- **NUNCA** agrupe multiplos itens em um Agent — respostas ficam desalinhadas.
