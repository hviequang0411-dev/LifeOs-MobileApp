#!/usr/bin/env bash
set -Eeuo pipefail

required=(EXPORT_DIR PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD PGSSLMODE)
for name in "${required[@]}"; do
  [[ -n "${!name:-}" ]] || { echo "missing $name" >&2; exit 1; }
done
case "$EXPORT_DIR" in ''|'/'|'/var'|'/var/lib'|'/var/lib/lifeos') echo 'unsafe EXPORT_DIR' >&2; exit 1 ;; esac
[[ -d "$EXPORT_DIR" ]] || { echo 'EXPORT_DIR is not a directory' >&2; exit 1; }

resolved=$(cd "$EXPORT_DIR" && pwd -P)
[[ "$resolved" == "$EXPORT_DIR" ]] || { echo 'EXPORT_DIR must be an absolute canonical path' >&2; exit 1; }

export PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD
[[ "$PGSSLMODE" == require || "$PGSSLMODE" == verify-ca || "$PGSSLMODE" == verify-full ]] \
  || { echo 'PGSSLMODE must require TLS' >&2; exit 1; }
export PGSSLMODE
refs=$(mktemp)
trap 'rm -f -- "$refs"' EXIT
psql -XAtqc "select id::text from data_exports where file_ref is not null" | sort -u > "$refs"

found=0
deleted=0
while IFS= read -r -d '' file; do
  [[ ! -L "$file" && -f "$file" ]] || continue
  id=$(basename "$file" .zip)
  [[ "$id" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$ ]] || continue
  if ! grep -Fqx "$id" "$refs"; then
    found=$((found+1))
    if [[ "${APPLY:-false}" == true ]]; then
      rm -- "$file"
      deleted=$((deleted+1))
    else
      echo "would_delete=$(basename "$file")"
    fi
  fi
done < <(find "$resolved" -maxdepth 1 -type f -name '*.zip' -mmin +1560 -print0)

echo "orphan_candidates=$found deleted=$deleted apply=${APPLY:-false}"
