#!/bin/bash
# Visualizar logs do Docker Compose em tempo real
# Uso:
#   ./scripts/logs.sh           — todos os serviços
#   ./scripts/logs.sh app       — só o backend
#   ./scripts/logs.sh frontend  — só o frontend
#   ./scripts/logs.sh postgres  — só o banco

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$SCRIPT_DIR/../deploy"

cd "$DEPLOY_DIR"

if [ -n "$1" ]; then
  docker compose logs -f --tail 100 "$@"
else
  docker compose logs -f --tail 100
fi
