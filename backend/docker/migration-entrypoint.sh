#!/bin/sh
set -eu

command=${1:-idle}
case "$command" in
  idle)
    exec tail -f /dev/null
    ;;
  migrate|validate|info)
    exec /flyway/flyway "$@"
    ;;
  *)
    echo "unsupported migration command" >&2
    exit 64
    ;;
esac
