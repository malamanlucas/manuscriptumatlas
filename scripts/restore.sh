#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# NT Manuscriptum Atlas — PostgreSQL Restore (via Docker)
# Uses pg_restore from inside the postgres container to avoid version mismatch.
# Restores both databases: nt_coverage and bible_db.
#
# Usage:
#   ./scripts/restore.sh                — Lists available backup timestamps to choose from
#   ./scripts/restore.sh 20260317_015300 — Restores specific timestamp (both databases)
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
BACKUP_DIR="backups"
DATABASES=("nt_coverage" "bible_db")

if ! docker inspect "$CONTAINER" &>/dev/null; then
    fail "Container '$CONTAINER' não encontrado. O docker compose está rodando?"
fi

# --- Select backup timestamp ------------------------------------------------

if [ -n "${1:-}" ]; then
    TIMESTAMP="$1"
else
    if [ ! -d "$BACKUP_DIR" ] || [ -z "$(ls -A "$BACKUP_DIR"/*.dump 2>/dev/null)" ]; then
        fail "Nenhum backup encontrado em $BACKUP_DIR/"
    fi

    echo ""
    info "Timestamps disponíveis:"
    echo ""
    mapfile -t TIMESTAMPS < <(ls -t "$BACKUP_DIR"/*.dump | sed -E 's/.*_([0-9]{8}_[0-9]{6})\.dump/\1/' | sort -ru)
    for i in "${!TIMESTAMPS[@]}"; do
        ts="${TIMESTAMPS[$i]}"
        FILES_FOR_TS=$(ls "$BACKUP_DIR"/*_"${ts}".dump 2>/dev/null | xargs -I{} basename {} | tr '\n' ', ' | sed 's/,$//')
        echo "  [$i] $ts — $FILES_FOR_TS"
    done
    echo ""
    read -rp "Escolha o número (default: 0 = mais recente): " choice
    choice="${choice:-0}"
    TIMESTAMP="${TIMESTAMPS[$choice]}"
fi

echo ""
info "Timestamp selecionado: $TIMESTAMP"
echo ""

# --- Check which dumps exist for this timestamp -----------------------------

FOUND=()
for DB_NAME in "${DATABASES[@]}"; do
    FILEPATH="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.dump"
    if [ -f "$FILEPATH" ]; then
        SIZE=$(du -h "$FILEPATH" | cut -f1)
        info "Encontrado: $(basename "$FILEPATH") ($SIZE)"
        FOUND+=("$DB_NAME")
    else
        info "Não encontrado: ${DB_NAME}_${TIMESTAMP}.dump (pulando)"
    fi
done

if [ ${#FOUND[@]} -eq 0 ]; then
    fail "Nenhum dump encontrado para o timestamp $TIMESTAMP"
fi

echo ""
read -rp "Isso vai APAGAR os dados dos databases: ${FOUND[*]}. Continuar? [y/N]: " confirm
if [[ ! "$confirm" =~ ^[yYsS]$ ]]; then
    info "Cancelado."
    exit 0
fi

# --- Restore each database --------------------------------------------------

for DB_NAME in "${FOUND[@]}"; do
    FILEPATH="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.dump"
    echo ""
    info "=== Restaurando $DB_NAME ==="

    info "Encerrando conexões ativas..."
    docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c \
        "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='$DB_NAME' AND pid <> pg_backend_pid();" \
        >/dev/null 2>&1 || true

    info "Recriando database '$DB_NAME'..."
    docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;" >/dev/null
    docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;" >/dev/null

    info "Restaurando dump..."
    docker exec -i "$CONTAINER" \
        pg_restore -U "$DB_USER" -d "$DB_NAME" --clean --if-exists --no-owner \
        < "$FILEPATH"

    ok "$DB_NAME restaurado."
done

echo ""
ok "Restore completo — ${#FOUND[@]} database(s)."
echo ""
info "Reinicie o backend para reconectar:"
echo "    docker restart deploy-app-1"
