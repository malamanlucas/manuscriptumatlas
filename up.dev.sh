#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DOCKER_NODE_IMAGE="node:20-alpine"

echo "==> [DEV] Parando containers antigos..."
docker compose down --remove-orphans 2>/dev/null || true

echo ""
echo "==> [DEV] Compilando backend (JAR)..."
./gradlew build -x test

echo ""
echo "==> [DEV] Sincronizando package-lock.json com Node $DOCKER_NODE_IMAGE..."
docker run --rm -v "$SCRIPT_DIR/frontend":/app -w /app "$DOCKER_NODE_IMAGE" \
  sh -c "npm install --package-lock-only --ignore-scripts 2>/dev/null && echo 'Lock file atualizado.' || echo 'Lock file já está em dia.'"

echo ""
echo "==> [DEV] Build Docker (com cache)..."
docker compose build

echo ""
echo "==> [DEV] Subindo containers..."
docker compose up -d

echo ""
echo "==> [DEV] Pronto! Aguarde a ingestão (~2-5 min com NTVMR)."
echo "    Backend:  http://localhost:8080"
echo "    Frontend: http://localhost:3000"
echo "    Postgres: localhost:5432 (user: postgres / pass: postgres)"
echo ""
echo "    Logs: docker compose logs -f"
