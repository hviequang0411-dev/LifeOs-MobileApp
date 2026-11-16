#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

required=(RESTORE_PGHOST RESTORE_PGPORT RESTORE_PGDATABASE RESTORE_PGUSER RESTORE_PGPASSWORD RESTORE_PGSSLMODE RESTORE_S3_OBJECT RESTORE_WORK_DIR RESTORE_CONFIRM_NEW_DATABASE)
for name in "${required[@]}"; do
  [[ -n "${!name:-}" ]] || { echo "missing $name" >&2; exit 1; }
done
if [[ -n "${PROMETHEUS_TEXTFILE_DIR:-}" ]]; then
  [[ -d "$PROMETHEUS_TEXTFILE_DIR" && -w "$PROMETHEUS_TEXTFILE_DIR" ]] \
    || { echo 'PROMETHEUS_TEXTFILE_DIR must be a writable directory' >&2; exit 1; }
fi
[[ "$RESTORE_CONFIRM_NEW_DATABASE" == "$RESTORE_PGDATABASE" ]] || { echo 'confirmation must equal target database name' >&2; exit 1; }
case "$RESTORE_WORK_DIR" in ''|'/'|'/var'|'/var/lib') echo 'unsafe RESTORE_WORK_DIR' >&2; exit 1 ;; esac
command -v pg_restore >/dev/null
command -v psql >/dev/null
command -v aws >/dev/null
command -v sha256sum >/dev/null

[[ "$RESTORE_PGSSLMODE" == require || "$RESTORE_PGSSLMODE" == verify-ca || "$RESTORE_PGSSLMODE" == verify-full ]] \
  || { echo 'RESTORE_PGSSLMODE must require TLS' >&2; exit 1; }
export PGHOST=$RESTORE_PGHOST PGPORT=$RESTORE_PGPORT PGDATABASE=$RESTORE_PGDATABASE PGUSER=$RESTORE_PGUSER PGPASSWORD=$RESTORE_PGPASSWORD PGSSLMODE=$RESTORE_PGSSLMODE

existing=$(psql -XAtqc "select count(*) from information_schema.tables where table_schema='public'")
[[ "$existing" == 0 ]] || { echo 'target database is not empty; refusing restore' >&2; exit 1; }

work=$(mktemp -d "${RESTORE_WORK_DIR%/}/lifeos-restore.XXXXXX")
trap 'rm -rf -- "$work"' EXIT
object_name=${RESTORE_S3_OBJECT##*/}
[[ "$object_name" == *.dump ]] || { echo 'restore object must end in .dump' >&2; exit 1; }
dump="$work/$object_name"
checksum="$dump.sha256"
started=$(date -u +%s)

aws s3 cp "$RESTORE_S3_OBJECT" "$dump" --only-show-errors
aws s3 cp "${RESTORE_S3_OBJECT}.sha256" "$checksum" --only-show-errors
(cd "$work" && sha256sum --check "$(basename "$checksum")")
pg_restore --exit-on-error --no-owner --no-acl --dbname="$RESTORE_PGDATABASE" "$dump"

psql -Xv ON_ERROR_STOP=1 -Atqc "select version from flyway_schema_history where success order by installed_rank desc limit 1"
psql -Xv ON_ERROR_STOP=1 -Atqc "select count(*) from users"
psql -Xv ON_ERROR_STOP=1 -Atqc "select count(*) from data_exports"
ended=$(date -u +%s)
echo "restore_verified database=$RESTORE_PGDATABASE duration_seconds=$((ended-started))"
if [[ -n "${PROMETHEUS_TEXTFILE_DIR:-}" ]]; then
  metric_tmp=$(mktemp "${PROMETHEUS_TEXTFILE_DIR%/}/lifeos-restore.XXXXXX")
  {
    printf 'lifeos_restore_last_success_timestamp_seconds %s\n' "$ended"
    printf 'lifeos_restore_last_duration_seconds %s\n' "$((ended-started))"
  } > "$metric_tmp"
  mv -f -- "$metric_tmp" "${PROMETHEUS_TEXTFILE_DIR%/}/lifeos-restore.prom"
fi
