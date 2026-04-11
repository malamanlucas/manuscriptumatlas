#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEPLOY_DIR="$PROJECT_ROOT/deploy"
BACKEND_DIR="$PROJECT_ROOT/backend"

DOCKER_NODE_IMAGE="node:20-alpine"

echo "==> [DEV] Parando containers antigos..."
docker compose -f "$DEPLOY_DIR/docker-compose.yml" down --remove-orphans 2>/dev/null || true

echo ""
echo "==> [DEV] Compilando backend (JAR incremental)..."
cd "$BACKEND_DIR" && ./gradlew build -x test --parallel

# So sincroniza lock se package.json mudou
if [ "$PROJECT_ROOT/frontend/package.json" -nt "$PROJECT_ROOT/frontend/package-lock.json" ]; then
  echo ""
  echo "==> [DEV] Sincronizando package-lock.json (package.json alterado)..."
  docker run --rm -v "$PROJECT_ROOT/frontend":/app -w /app "$DOCKER_NODE_IMAGE" \
    sh -c "npm install --package-lock-only --ignore-scripts 2>/dev/null && echo 'Lock file atualizado.' || echo 'Lock file já está em dia.'"
else
  echo ""
  echo "==> [DEV] package-lock.json em dia. Pulando sync."
fi

echo ""
echo "==> [DEV] Build Docker (com cache)..."
docker compose -f "$DEPLOY_DIR/docker-compose.yml" build

echo ""
echo "==> [DEV] Subindo containers..."
docker compose -f "$DEPLOY_DIR/docker-compose.yml" up -d

echo ""
echo "==> [DEV] Pronto!"
echo "    Backend:  http://localhost:8080"
echo "    Frontend: http://localhost:3000"
echo "    Postgres: localhost:5432"
echo ""
echo "    Monitoramento: docker compose -f $DEPLOY_DIR/docker-compose.yml --profile monitoring up -d"
echo "    Logs: docker compose -f $DEPLOY_DIR/docker-compose.yml logs -f"
