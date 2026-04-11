#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEPLOY_DIR="$PROJECT_ROOT/deploy"

echo "==> Parando containers..."
docker compose -f "$DEPLOY_DIR/docker-compose.yml" down --remove-orphans
echo "==> Containers parados."
