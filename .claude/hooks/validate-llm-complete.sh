#!/bin/bash
# PostToolUse hook: valida JSON do responseContent antes de permitir POST /admin/llm/queue/{id}/complete.
# Bloqueia com exit 2 se label sugere JSON mas responseContent nao parseia como JSON.
# Lido via stdin conforme spec de hooks Claude Code: input JSON com tool_name, tool_input, etc.
#
# Aciona apenas quando:
#   tool_name == "Bash"
#   tool_input.command contem "admin/llm/queue/" e "/complete"
#
# Outras chamadas Bash passam direto (exit 0).

set -u

# Le payload do hook
PAYLOAD=$(cat)
TOOL=$(echo "$PAYLOAD" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null || echo "")
if [ "$TOOL" != "Bash" ]; then
  exit 0
fi

CMD=$(echo "$PAYLOAD" | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(d.get('tool_input', {}).get('command', ''))
" 2>/dev/null || echo "")

# So age em curl que chama /complete do admin queue
if ! echo "$CMD" | grep -q "admin/llm/queue/"; then
  exit 0
fi
if ! echo "$CMD" | grep -q "/complete"; then
  exit 0
fi

# Tenta extrair o body (-d '...' ou -d @file)
BODY=$(python3 << 'PYEOF'
import sys, re, json, os
cmd = os.environ.get("CMD_RAW","")
# pega ultimo -d valor
m = re.search(r"-d\s+(@[^\s]+|'[^']*'|\"[^\"]*\")", cmd)
if not m:
    sys.exit(0)
raw = m.group(1)
if raw.startswith("@"):
    path = raw[1:]
    try:
        print(open(path).read())
    except Exception as e:
        sys.exit(0)
else:
    # strip quotes
    if raw[0] in "'\"":
        raw = raw[1:-1]
    print(raw)
PYEOF
)
export CMD_RAW="$CMD"

# Extrai responseContent do body JSON
VALID=$(python3 << 'PYEOF'
import sys, json, os, re
body = os.environ.get("BODY_RAW","")
if not body.strip():
    print("SKIP")
    sys.exit(0)
try:
    data = json.loads(body)
except Exception:
    print("SKIP")
    sys.exit(0)
resp = data.get("responseContent")
if not isinstance(resp, str) or not resp.strip():
    print("FAIL:empty_response")
    sys.exit(0)
# Tenta detectar se deveria ser JSON: se a URL/label suggests
# Heuristic simples: se responseContent comeca com { ou [, deve parsear
s = resp.strip()
if s.startswith("```"):
    # remove code fences
    s = re.sub(r"^```(json)?\s*", "", s)
    s = re.sub(r"\s*```\s*$", "", s)
if s.startswith("{") or s.startswith("["):
    try:
        json.loads(s)
        print("OK")
    except Exception as e:
        print(f"FAIL:invalid_json:{e}")
else:
    # Nao JSON esperado (ex: bloco [STRONG]...) — so valida nao-vazio
    print("OK")
PYEOF
)
export BODY_RAW="$BODY"

# Re-rodar python com BODY_RAW no env
VALID=$(BODY_RAW="$BODY" python3 << 'PYEOF'
import sys, json, os, re
body = os.environ.get("BODY_RAW","")
if not body.strip():
    print("SKIP"); sys.exit(0)
try:
    data = json.loads(body)
except Exception:
    print("SKIP"); sys.exit(0)
resp = data.get("responseContent")
if not isinstance(resp, str) or not resp.strip():
    print("FAIL:empty_response"); sys.exit(0)
s = resp.strip()
if s.startswith("```"):
    s = re.sub(r"^```(?:json)?\s*", "", s)
    s = re.sub(r"\s*```\s*$", "", s)
if s.startswith("{") or s.startswith("["):
    try:
        json.loads(s); print("OK")
    except Exception as e:
        print(f"FAIL:invalid_json:{str(e)[:80]}")
else:
    print("OK")
PYEOF
)

case "$VALID" in
  OK|SKIP|"") exit 0 ;;
  FAIL:*)
    echo "validate-llm-complete: blocked ($VALID)" >&2
    exit 2
    ;;
  *) exit 0 ;;
esac
