#!/bin/bash
# drain-safe.sh — wrapper seguro do drain_llm_prompt_queue.py.
# Para uso em cron ou interativo. Nao reintenta em rate-limit; respeita /tmp/claude_rate_limit_until.
#
# Uso:
#   ./scripts/drain-safe.sh                 # backend auto (Anthropic se ANTHROPIC_API_KEY, senao OpenAI)
#   LLM_BACKEND=anthropic ./scripts/drain-safe.sh
#   LLM_BACKEND=openai ./scripts/drain-safe.sh
#   DRAIN_MAX_BATCHES=10 ./scripts/drain-safe.sh  # limita rodadas
#
# Crontab recomendado (a cada 15 min, respeitando rate-limit):
#   */15 * * * * cd /Users/user1/Documents/GitHub/manuscriptumatlas && ./scripts/drain-safe.sh >> /tmp/drain.log 2>&1

set -u

REPO="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO"

# Carrega .env se existir
if [ -f deploy/.env ]; then
  set -a; . deploy/.env; set +a
fi

# Defaults
export LLM_QUEUE_BASE_URL="${LLM_QUEUE_BASE_URL:-http://localhost:8080}"
export LLM_QUEUE_EMAIL="${LLM_QUEUE_EMAIL:-dev@manuscriptum.local}"
export QUEUE_UNSTICK_MINUTES="${QUEUE_UNSTICK_MINUTES:-10}"

# Tenta tambem extrair OPENAI_API_KEY do container se nao setada
if [ -z "${OPENAI_API_KEY:-}" ] && command -v docker >/dev/null 2>&1; then
  K=$(docker exec deploy-app-1 sh -c 'printf %s "$OPENAI_API_KEY"' 2>/dev/null || true)
  [ -n "$K" ] && export OPENAI_API_KEY="$K"
fi

# Timestamp iso
ts() { date -u +"%Y-%m-%dT%H:%M:%SZ"; }

echo "{\"event\":\"wrapper_start\",\"ts\":\"$(ts)\",\"backend\":\"${LLM_BACKEND:-auto}\"}"

python3 scripts/drain_llm_prompt_queue.py
RC=$?

echo "{\"event\":\"wrapper_end\",\"ts\":\"$(ts)\",\"exit_code\":$RC}"
exit $RC
