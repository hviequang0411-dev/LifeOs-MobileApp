#!/usr/bin/env bash
set -Eeuo pipefail

required=(RENDER_API_KEY RENDER_MIGRATION_SERVICE_ID RENDER_RUNTIME_SERVICE_ID RUNTIME_IMAGE HEALTH_URL)
for name in "${required[@]}"; do
  [[ -n "${!name:-}" ]] || { echo "missing $name" >&2; exit 1; }
done
if [[ "${RUN_MIGRATION:-true}" == true ]]; then
  [[ -n "${MIGRATION_IMAGE:-}" ]] || { echo 'missing MIGRATION_IMAGE' >&2; exit 1; }
fi
command -v curl >/dev/null
command -v jq >/dev/null

api=https://api.render.com/v1
auth="Authorization: Bearer $RENDER_API_KEY"

request() {
  method=$1 url=$2 data=${3:-}
  for attempt in 1 2 3 4 5; do
    if [[ -n "$data" ]]; then
      response=$(curl --silent --show-error --fail-with-body --request "$method" --url "$url" \
        --header "$auth" --header 'accept: application/json' --header 'content-type: application/json' \
        --data "$data") && { printf '%s' "$response"; return; }
    else
      response=$(curl --silent --show-error --fail-with-body --request "$method" --url "$url" \
        --header "$auth" --header 'accept: application/json') && { printf '%s' "$response"; return; }
    fi
    sleep $((attempt * attempt))
  done
  return 1
}

wait_deploy() {
  service=$1 deploy=$2
  for _ in $(seq 1 180); do
    body=$(request GET "$api/services/$service/deploys/$deploy")
    status=$(jq -r '.status' <<<"$body")
    case "$status" in
      live) echo "render_deploy_live service=$service deploy=$deploy"; return 0 ;;
      build_failed|update_failed|pre_deploy_failed|canceled|deactivated)
        echo "render_deploy_failed service=$service deploy=$deploy status=$status" >&2; return 1 ;;
    esac
    sleep 10
  done
  echo "render_deploy_timeout service=$service deploy=$deploy" >&2
  return 1
}

deploy_image() {
  service=$1 image=$2
  payload=$(jq -cn --arg image "$image" '{imageUrl:$image,clearCache:"do_not_clear"}')
  body=$(request POST "$api/services/$service/deploys" "$payload")
  deploy=$(jq -er '.id' <<<"$body")
  wait_deploy "$service" "$deploy"
}

if [[ "${RUN_MIGRATION:-true}" == true ]]; then
  deploy_image "$RENDER_MIGRATION_SERVICE_ID" "$MIGRATION_IMAGE"
  job_payload=$(jq -cn '{startCommand:"migrate"}')
  job_body=$(request POST "$api/services/$RENDER_MIGRATION_SERVICE_ID/jobs" "$job_payload")
  job=$(jq -er '.id' <<<"$job_body")
  for _ in $(seq 1 180); do
    body=$(request GET "$api/services/$RENDER_MIGRATION_SERVICE_ID/jobs/$job")
    status=$(jq -r '.status' <<<"$body")
    case "$status" in
      succeeded) echo "render_migration_succeeded job=$job"; break ;;
      failed|canceled) echo "render_migration_failed job=$job status=$status" >&2; exit 1 ;;
    esac
    sleep 10
  done
  [[ "$status" == succeeded ]] || { echo "render_migration_timeout job=$job" >&2; exit 1; }
fi

deploy_image "$RENDER_RUNTIME_SERVICE_ID" "$RUNTIME_IMAGE"
for _ in $(seq 1 60); do
  if curl --silent --show-error --fail "$HEALTH_URL/actuator/health/readiness" | grep -q '"status":"UP"'; then
    echo 'render_health_ready=true'
    exit 0
  fi
  sleep 5
done
echo 'render_health_ready=false' >&2
exit 1
