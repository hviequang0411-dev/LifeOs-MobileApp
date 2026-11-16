#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

required=(PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD PGSSLMODE BACKUP_WORK_DIR BACKUP_S3_URI BACKUP_KMS_KEY_ID)
for name in "${required[@]}"; do
  [[ -n "${!name:-}" ]] || { echo "missing $name" >&2; exit 1; }
done
if [[ -n "${PROMETHEUS_TEXTFILE_DIR:-}" ]]; then
  [[ -d "$PROMETHEUS_TEXTFILE_DIR" && -w "$PROMETHEUS_TEXTFILE_DIR" ]] \
    || { echo 'PROMETHEUS_TEXTFILE_DIR must be a writable directory' >&2; exit 1; }
fi

case "$BACKUP_WORK_DIR" in ''|'/'|'/var'|'/var/lib') echo 'unsafe BACKUP_WORK_DIR' >&2; exit 1 ;; esac
command -v pg_dump >/dev/null
command -v aws >/dev/null
command -v sha256sum >/dev/null
[[ "$PGSSLMODE" == require || "$PGSSLMODE" == verify-ca || "$PGSSLMODE" == verify-full ]] \
  || { echo 'PGSSLMODE must require TLS' >&2; exit 1; }

work=$(mktemp -d "${BACKUP_WORK_DIR%/}/lifeos-backup.XXXXXX")
trap 'rm -rf -- "$work"' EXIT
stamp=$(date -u +%Y%m%dT%H%M%SZ)
dump="$work/lifeos-${stamp}.dump"
checksum="$dump.sha256"

pg_dump --format=custom --compress=9 --no-owner --no-acl --file="$dump"
(cd "$work" && sha256sum "$(basename "$dump")" > "$(basename "$checksum")")

base="${BACKUP_S3_URI%/}/daily/$(date -u +%Y/%m)/$(basename "$dump")"
aws s3 cp "$dump" "$base" --only-show-errors --sse aws:kms --sse-kms-key-id "$BACKUP_KMS_KEY_ID"
aws s3 cp "$checksum" "${base}.sha256" --only-show-errors --sse aws:kms --sse-kms-key-id "$BACKUP_KMS_KEY_ID"

# Weekly/monthly are extra object keys; lifecycle policy enforces 4 weekly and 6 monthly copies.
if [[ $(date -u +%u) == 7 ]]; then
  aws s3 cp "$base" "${BACKUP_S3_URI%/}/weekly/$(date -u +%G-W%V)/$(basename "$dump")" --only-show-errors --copy-props metadata-directive
  aws s3 cp "${base}.sha256" "${BACKUP_S3_URI%/}/weekly/$(date -u +%G-W%V)/$(basename "$checksum")" --only-show-errors --copy-props metadata-directive
fi
if [[ $(date -u +%d) == 01 ]]; then
  aws s3 cp "$base" "${BACKUP_S3_URI%/}/monthly/$(date -u +%Y-%m)/$(basename "$dump")" --only-show-errors --copy-props metadata-directive
  aws s3 cp "${base}.sha256" "${BACKUP_S3_URI%/}/monthly/$(date -u +%Y-%m)/$(basename "$checksum")" --only-show-errors --copy-props metadata-directive
fi

echo "backup_uploaded_at=$stamp object=$(basename "$dump")"
if [[ -n "${PROMETHEUS_TEXTFILE_DIR:-}" ]]; then
  metric_tmp=$(mktemp "${PROMETHEUS_TEXTFILE_DIR%/}/lifeos-backup.XXXXXX")
  printf 'lifeos_backup_last_success_timestamp_seconds %s\n' "$(date -u +%s)" > "$metric_tmp"
  mv -f -- "$metric_tmp" "${PROMETHEUS_TEXTFILE_DIR%/}/lifeos-backup.prom"
fi
