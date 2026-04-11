#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEPLOY_DIR="$PROJECT_ROOT/deploy"

COMPOSE_FILES="-f $DEPLOY_DIR/docker-compose.yml -f $DEPLOY_DIR/docker-compose.prod.yml"

echo "==> [DB] Parando containers antigos..."
docker compose $COMPOSE_FILES down --remove-orphans 2>/dev/null || true

echo ""
echo "==> [DB] Subindo apenas o Postgres..."
docker compose $COMPOSE_FILES up -d postgres

echo ""
echo "==> [DB] Banco de dados em execução!"
echo "    Postgres: localhost:35857 (user: postgres, db: nt_coverage)"
echo ""
echo "    Logs:    docker compose $COMPOSE_FILES logs -f postgres"
echo "    Status:  docker compose $COMPOSE_FILES ps"
echo "    Parar:   docker compose $COMPOSE_FILES down"
