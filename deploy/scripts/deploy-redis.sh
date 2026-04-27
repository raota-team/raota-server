#!/usr/bin/env bash
set -euo pipefail

REMOTE_DIR="${REMOTE_DIR:-/opt/raota-redis}"
COMPOSE_FILE="${REMOTE_DIR}/docker-compose.redis.prod.yml"

mkdir -p "${REMOTE_DIR}"
docker compose -f "${COMPOSE_FILE}" up -d

for attempt in $(seq 1 20); do
  if docker exec raota-redis redis-cli ping | grep -q PONG; then
    echo "[deploy] redis is healthy"
    exit 0
  fi

  echo "[deploy] waiting for redis (${attempt}/20)"
  sleep 3
done

echo "[deploy] redis did not become healthy"
exit 1
