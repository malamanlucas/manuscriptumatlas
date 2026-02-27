#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# NT Manuscriptum Atlas — PostgreSQL Backup
# Generates a custom-format dump suitable for efficient restore via pg_restore.
# Compatible with Linux and macOS.
# ---------------------------------------------------------------------------

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${YELLOW}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; exit 1; }

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-nt_coverage}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

export PGPASSWORD="$DB_PASSWORD"

if ! command -v pg_dump &>/dev/null; then
    fail "pg_dump not found. Install PostgreSQL client tools first."
fi

BACKUP_DIR="backups"
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="ntatlas_${TIMESTAMP}.dump"
FILEPATH="${BACKUP_DIR}/${FILENAME}"

info "Starting backup of database '${DB_NAME}' on ${DB_HOST}:${DB_PORT}..."

pg_dump -Fc \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    -f "$FILEPATH"

SIZE=$(du -h "$FILEPATH" | cut -f1)
ok "Backup complete: ${FILEPATH} (${SIZE})"
