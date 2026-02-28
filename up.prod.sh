#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DOCKER_NODE_IMAGE="node:20-alpine"
COMPOSE_FILES="-f docker-compose.yml -f docker-compose.prod.yml"

echo "==> [PROD] Parando containers antigos..."
docker compose $COMPOSE_FILES down --remove-orphans 2>/dev/null || true

echo ""
echo "==> [PROD] Compilando backend (JAR)..."
./gradlew build -x test

echo ""
echo "==> [PROD] Sincronizando package-lock.json com Node $DOCKER_NODE_IMAGE..."
docker run --rm -v "$SCRIPT_DIR/frontend":/app -w /app "$DOCKER_NODE_IMAGE" \
  sh -c "npm install --package-lock-only --ignore-scripts 2>/dev/null && echo 'Lock file atualizado.' || echo 'Lock file já está em dia.'"

echo ""
echo "==> [PROD] Build Docker (sem cache)..."
docker compose $COMPOSE_FILES build --no-cache

echo ""
echo "==> [PROD] Subindo containers..."
docker compose $COMPOSE_FILES up -d

echo ""
echo "==> [PROD] Deploy concluído!"
echo "    Frontend: http://localhost:35855"
echo "    Backend:  http://localhost:35856"
echo "    Postgres: localhost:35857 (user: postgres)"
echo ""
echo "    Logs:    docker compose $COMPOSE_FILES logs -f"
echo "    Status:  docker compose $COMPOSE_FILES ps"
echo "    Parar:   docker compose $COMPOSE_FILES down"
