#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "==> Compilando backend (JAR)..."
./gradlew build -x test

echo ""
echo "==> Build Docker sem cache..."
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
