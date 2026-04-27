#!/usr/bin/env bash
set -euo pipefail

wait_for_apt_lock() {
  while sudo fuser /var/lib/dpkg/lock-frontend >/dev/null 2>&1 || sudo fuser /var/lib/apt/lists/lock >/dev/null 2>&1; do
    sleep 2
  done
}

if ! command -v docker >/dev/null 2>&1; then
  wait_for_apt_lock
  sudo apt-get update
  sudo apt-get install -y docker.io
fi

if ! docker compose version >/dev/null 2>&1; then
  wait_for_apt_lock
  sudo apt-get update
  sudo apt-get install -y docker-compose-v2
fi

if ! command -v curl >/dev/null 2>&1; then
  wait_for_apt_lock
  sudo apt-get update
  sudo apt-get install -y curl
fi

sudo systemctl enable --now docker
sudo mkdir -p /opt/raota
sudo chown -R "$USER":"$USER" /opt/raota
