#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# NT Manuscriptum Atlas — PostgreSQL Backup (via Docker)
# Uses pg_dump from inside the postgres container to avoid version mismatch.
# Backs up both databases: nt_coverage and bible_db.
# ---------------------------------------------------------------------------

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${YELLOW}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

DB_USER="${DB_USER:-postgres}"
CONTAINER="${COMPOSE_PROJECT_NAME:-deploy}-postgres-1"

DATABASES=("nt_coverage" "bible_db")

BACKUP_DIR="backups"
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)

if ! docker inspect "$CONTAINER" &>/dev/null; then
    fail "Container '$CONTAINER' não encontrado. O docker compose está rodando?"
fi

PG_VERSION=$(docker exec "$CONTAINER" pg_dump --version | grep -oE '[0-9]+\.[0-9]+')
info "Backup via container (pg_dump $PG_VERSION)..."

for DB_NAME in "${DATABASES[@]}"; do
    FILENAME="${DB_NAME}_${TIMESTAMP}.dump"
    FILEPATH="${BACKUP_DIR}/${FILENAME}"

    info "Database: $DB_NAME"

    docker exec "$CONTAINER" \
        pg_dump -Fc -U "$DB_USER" -d "$DB_NAME" \
        > "$FILEPATH"

    SIZE=$(du -h "$FILEPATH" | cut -f1)
    ok "Backup: ${FILEPATH} (${SIZE})"
done

echo ""
ok "Backup completo — ${#DATABASES[@]} databases."
