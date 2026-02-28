#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# NT Manuscriptum Atlas — PostgreSQL Restore (via Docker)
# Uses pg_restore from inside the postgres container to avoid version mismatch.
# Usage: ./restore.sh [backup_file]
#   If no file is given, lists available backups to choose from.
# ---------------------------------------------------------------------------

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${YELLOW}[INFO]${NC}  $*"; }
ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
fail()  { echo -e "${RED}[FAIL]${NC}  $*"; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

DB_NAME="${DB_NAME:-nt_coverage}"
DB_USER="${DB_USER:-postgres}"
CONTAINER="${COMPOSE_PROJECT_NAME:-manuscriptumatlas}-postgres-1"
BACKUP_DIR="backups"

if ! docker inspect "$CONTAINER" &>/dev/null; then
    fail "Container '$CONTAINER' não encontrado. O docker compose está rodando?"
fi

# --- Select backup file ---------------------------------------------------

if [ -n "${1:-}" ]; then
    FILEPATH="$1"
else
    if [ ! -d "$BACKUP_DIR" ] || [ -z "$(ls -A "$BACKUP_DIR"/*.dump 2>/dev/null)" ]; then
        fail "Nenhum backup encontrado em $BACKUP_DIR/"
    fi

    echo ""
    info "Backups disponíveis:"
    echo ""
    mapfile -t FILES < <(ls -t "$BACKUP_DIR"/*.dump)
    for i in "${!FILES[@]}"; do
        SIZE=$(du -h "${FILES[$i]}" | cut -f1)
        echo "  [$i] $(basename "${FILES[$i]}") ($SIZE)"
    done
    echo ""
    read -rp "Escolha o número do backup (default: 0 = mais recente): " choice
    choice="${choice:-0}"
    FILEPATH="${FILES[$choice]}"
fi

if [ ! -f "$FILEPATH" ]; then
    fail "Arquivo não encontrado: $FILEPATH"
fi

FILENAME=$(basename "$FILEPATH")
SIZE=$(du -h "$FILEPATH" | cut -f1)

echo ""
info "Restaurando: $FILENAME ($SIZE)"
info "Database: $DB_NAME"
echo ""
read -rp "Isso vai APAGAR todos os dados atuais. Continuar? [y/N]: " confirm
if [[ ! "$confirm" =~ ^[yYsS]$ ]]; then
    info "Cancelado."
    exit 0
fi

# --- Kill connections and recreate database --------------------------------

info "Encerrando conexões ativas..."
docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='$DB_NAME' AND pid <> pg_backend_pid();" \
    >/dev/null 2>&1 || true

info "Recriando database '$DB_NAME'..."
docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;" >/dev/null
docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;" >/dev/null

# --- Restore ---------------------------------------------------------------

info "Restaurando dump..."
docker exec -i "$CONTAINER" \
    pg_restore -U "$DB_USER" -d "$DB_NAME" --clean --if-exists --no-owner \
    < "$FILEPATH"

ok "Restore concluído com sucesso!"
echo ""
info "Reinicie o backend para reconectar:"
echo "    docker compose restart app"
