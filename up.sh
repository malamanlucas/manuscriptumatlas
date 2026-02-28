#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DOCKER_NODE_IMAGE="node:20-alpine"

echo "==> Parando containers antigos..."
docker compose down --remove-orphans 2>/dev/null || true

echo ""
echo "==> Compilando backend (JAR)..."
./gradlew build -x test

echo ""
echo "==> Sincronizando package-lock.json com Node $DOCKER_NODE_IMAGE..."
docker run --rm -v "$SCRIPT_DIR/frontend":/app -w /app "$DOCKER_NODE_IMAGE" \
  sh -c "npm install --package-lock-only --ignore-scripts 2>/dev/null && echo 'Lock file atualizado.' || echo 'Lock file já está em dia.'"

echo ""
echo "==> Build Docker (sem cache)..."
docker compose build --no-cache

echo ""
echo "==> Subindo containers..."
docker compose up -d

echo ""
echo "==> Pronto! Aguarde a ingestão de dados (~1-2 min)."
echo "    Backend:  http://localhost:8080"
echo "    Frontend: http://localhost:3000"
echo ""
echo "    Logs: docker compose logs -f"
