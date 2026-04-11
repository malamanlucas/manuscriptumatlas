#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEPLOY_DIR="$PROJECT_ROOT/deploy"
BACKEND_DIR="$PROJECT_ROOT/backend"

REQUIRED_JAVA=21
DOCKER_NODE_IMAGE="node:20-alpine"
COMPOSE_FILES="-f $DEPLOY_DIR/docker-compose.yml -f $DEPLOY_DIR/docker-compose.prod.yml"

echo "==> [PROD] Verificando Java $REQUIRED_JAVA..."
if java -version 2>&1 | grep -q "version \"$REQUIRED_JAVA"; then
  echo "    Java $REQUIRED_JAVA encontrado."
else
  echo "    Java $REQUIRED_JAVA não encontrado. Instalando..."
  apt-get update -qq
  apt-get install -y -qq openjdk-${REQUIRED_JAVA}-jdk-headless > /dev/null
  echo "    Java $REQUIRED_JAVA instalado com sucesso."
fi

export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
echo "    JAVA_HOME=$JAVA_HOME"

echo ""
echo "==> [PROD] Parando containers antigos..."
docker compose $COMPOSE_FILES down --remove-orphans 2>/dev/null || true

echo ""
echo "==> [PROD] Compilando backend (JAR)..."
cd "$BACKEND_DIR" && ./gradlew clean build -x test

echo ""
echo "==> [PROD] Sincronizando package-lock.json com Node $DOCKER_NODE_IMAGE..."
docker run --rm -v "$PROJECT_ROOT/frontend":/app -w /app "$DOCKER_NODE_IMAGE" \
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
