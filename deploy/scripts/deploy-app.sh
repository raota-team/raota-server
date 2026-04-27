#!/usr/bin/env bash
set -euo pipefail

: "${APP_IMAGE:?APP_IMAGE is required}"
: "${OCI_VAULT_ID:?OCI_VAULT_ID is required}"
: "${SPRING_DATA_REDIS_HOST:?SPRING_DATA_REDIS_HOST is required}"
: "${SPRING_DATA_REDIS_PORT:?SPRING_DATA_REDIS_PORT is required}"
: "${OCIR_REGISTRY:?OCIR_REGISTRY is required}"
: "${OCIR_USERNAME:?OCIR_USERNAME is required}"
: "${OCIR_AUTH_TOKEN:?OCIR_AUTH_TOKEN is required}"

REMOTE_DIR="${REMOTE_DIR:-/opt/raota}"
COMPOSE_FILE="${REMOTE_DIR}/docker-compose.prod.yml"
ENV_FILE="${REMOTE_DIR}/.env.prod"
JAVA_TOOL_OPTIONS_VALUE="${JAVA_TOOL_OPTIONS:--Xms2g -Xmx4g}"
READINESS_URL="${READINESS_URL:-http://127.0.0.1:8080/actuator/health/readiness}"

previous_image=""
if docker container inspect raota-app >/dev/null 2>&1; then
  previous_image="$(docker inspect --format '{{.Config.Image}}' raota-app)"
fi

write_env_file() {
  local image="$1"
  cat > "${ENV_FILE}" <<EOF
APP_IMAGE=${image}
OCI_VAULT_ID=${OCI_VAULT_ID}
SPRING_DATA_REDIS_HOST=${SPRING_DATA_REDIS_HOST}
SPRING_DATA_REDIS_PORT=${SPRING_DATA_REDIS_PORT}
JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS_VALUE}
EOF
}

rollback() {
  if [[ -n "${previous_image}" && "${previous_image}" != "${APP_IMAGE}" ]]; then
    echo "[deploy] readiness failed, rolling back to ${previous_image}"
    write_env_file "${previous_image}"
    docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d --remove-orphans
  fi
}

trap rollback ERR

mkdir -p "${REMOTE_DIR}"
printf '%s' "${OCIR_AUTH_TOKEN}" | docker login "${OCIR_REGISTRY}" -u "${OCIR_USERNAME}" --password-stdin

write_env_file "${APP_IMAGE}"
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" pull
docker compose -f "${COMPOSE_FILE}" --env-file "${ENV_FILE}" up -d --remove-orphans

for attempt in $(seq 1 36); do
  if curl --fail --silent "${READINESS_URL}" >/dev/null; then
    trap - ERR
    echo "[deploy] readiness check passed"
    exit 0
  fi

  echo "[deploy] waiting for readiness (${attempt}/36)"
  sleep 5
done

echo "[deploy] readiness check timed out"
exit 1
