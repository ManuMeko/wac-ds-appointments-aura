#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
  echo "Falta deploy/.env" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

if [[ -z "${POSTGRES_USER:-}" || -z "${POSTGRES_DB:-}" ]]; then
  echo "POSTGRES_USER y POSTGRES_DB son obligatorios" >&2
  exit 1
fi

backup_dir="${BACKUP_DIR:-./backups}"
mkdir -p "$backup_dir"

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
outfile="$backup_dir/appointments-${stamp}.sql.gz"

umask 077

docker compose exec -T postgres \
  pg_dump --no-owner --no-acl -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  | gzip -c > "$outfile"

find "$backup_dir" -type f -name 'appointments-*.sql.gz' -mtime +7 -delete

echo "Copia escrita en $outfile"
