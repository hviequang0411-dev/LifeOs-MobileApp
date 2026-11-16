#!/bin/sh
set -eu

env_file=${1:?usage: preflight.sh /path/to/.env}
test -f "$env_file" || { echo "environment file not found" >&2; exit 1; }

required='LIFEOS_IMAGE LIFEOS_MIGRATION_IMAGE DB_URL DB_USER DB_PASSWORD MIGRATION_DB_URL MIGRATION_DB_USER MIGRATION_DB_PASSWORD JWT_SECRET JWT_KID NOTIFICATION_UNSUBSCRIBE_SECRET APP_BASE_URL API_BASE_URL TRUSTED_PROXIES SMTP_HOST SMTP_USER SMTP_PASSWORD MAIL_FROM'

for name in $required; do
  value=$(sed -n "s/^${name}=//p" "$env_file" | tail -n 1)
  test -n "$value" || { echo "missing $name" >&2; exit 1; }
  case "$value" in
    *'<'*'>'*|*change-me*|*replace-me*|*example.com*) echo "placeholder $name" >&2; exit 1 ;;
  esac
done

jwt=$(sed -n 's/^JWT_SECRET=//p' "$env_file" | tail -n 1)
unsubscribe=$(sed -n 's/^NOTIFICATION_UNSUBSCRIBE_SECRET=//p' "$env_file" | tail -n 1)
proxies=$(sed -n 's/^TRUSTED_PROXIES=//p' "$env_file" | tail -n 1)
test "${#jwt}" -ge 32 || { echo 'JWT_SECRET shorter than 32 characters' >&2; exit 1; }
test "${#unsubscribe}" -ge 32 || { echo 'NOTIFICATION_UNSUBSCRIBE_SECRET shorter than 32 characters' >&2; exit 1; }
test "$jwt" != "$unsubscribe" || { echo 'JWT and unsubscribe secrets must differ' >&2; exit 1; }
case "$proxies" in *'.*'*|*0.0.0.0*) echo 'TRUSTED_PROXIES is too broad' >&2; exit 1 ;; esac

echo 'Preflight passed; values were not printed.'
