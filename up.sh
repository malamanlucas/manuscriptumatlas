#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Uso:"
echo "  ./up.dev.sh   — Dev: build com cache, PG exposto, re-ingestão completa"
echo "  ./up.prod.sh  — Prod: build limpo, JVM limitada, skip-if-populated, PG interno"
echo ""

read -rp "Executar qual? [dev/prod] (default: dev): " choice
choice="${choice:-dev}"

case "$choice" in
  prod) exec "$SCRIPT_DIR/up.prod.sh" ;;
  *)    exec "$SCRIPT_DIR/up.dev.sh" ;;
esac
