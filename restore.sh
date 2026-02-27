#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# NT Manuscriptum Atlas — PostgreSQL Restore
# Restores a custom-format dump created by backup.sh.
# Compatible with Linux and macOS.
#
# Usage: ./restore.sh backups/ntatlas_YYYYMMDD_HHMMSS.dump
# ---------------------------------------------------------------------------

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${YELLOW}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; exit 1; }

if [ $# -lt 1 ]; then
    fail "Usage: $0 <dump-file>"
fi

DUMP_FILE="$1"

if [ ! -f "$DUMP_FILE" ]; then
    fail "File not found: ${DUMP_FILE}"
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-nt_coverage}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

export PGPASSWORD="$DB_PASSWORD"

if ! command -v pg_restore &>/dev/null; then
    fail "pg_restore not found. Install PostgreSQL client tools first."
fi

if ! command -v psql &>/dev/null; then
    fail "psql not found. Install PostgreSQL client tools first."
fi

info "Terminating active connections to '${DB_NAME}'..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='${DB_NAME}' AND pid <> pg_backend_pid();" \
    >/dev/null 2>&1 || true

info "Dropping database '${DB_NAME}' (if exists)..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c \
    "DROP DATABASE IF EXISTS \"${DB_NAME}\";"

info "Creating database '${DB_NAME}'..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c \
    "CREATE DATABASE \"${DB_NAME}\";"

info "Restoring from ${DUMP_FILE}..."
pg_restore \
    --clean \
    --if-exists \
    --verbose \
    -h "$DB_HOST" \
    -p "$DB_PORT" \
    -U "$DB_USER" \
    -d "$DB_NAME" \
    "$DUMP_FILE"

ok "Restore complete."
