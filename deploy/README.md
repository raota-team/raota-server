# Production Deployment

배포 관련 파일은 모두 `deploy/` 아래에 둔다.

## Layout

- `deploy/compose/app.yml`
- `deploy/compose/redis.yml`
- `deploy/scripts/bootstrap-host.sh`
- `deploy/scripts/deploy-app.sh`
- `deploy/scripts/deploy-redis.sh`

## Current production facts

- OCI LB backend set: `bs_raota_prod`
- Worker backends:
  - `10.0.1.213:8080`
  - `10.0.1.144:8080`
  - `10.0.1.87:8080`
- Redis fixed host: `10.0.1.44`

## GitHub Actions

- Compose rolling deploy:
  - `.github/workflows/cd-prod-compose.yml`
  - `main` push 시 자동 실행
  - 배포 직전에 최신 `app.yml`, `bootstrap-host.sh`, `deploy-app.sh`를 각 worker의 `/opt/raota`에 덮어씀
  - 그 다음 각 worker에서 `/opt/raota/deploy-app.sh` 실행
- Legacy Swarm workflow:
  - `.github/workflows/cd-prod.yml`
  - manual fallback only

## Canary behavior

1. target worker를 Swarm에서 `drain`
2. LB health check가 기존 backend down을 감지
3. Compose app 기동
4. readiness `UP`
5. LB health check가 backend를 다시 `OK`로 편입

## Server-side commands

App deploy script:

```bash
APP_IMAGE=<image> \
OCI_VAULT_ID=<vault-id> \
SPRING_DATA_REDIS_HOST=10.0.1.44 \
SPRING_DATA_REDIS_PORT=6379 \
OCIR_REGISTRY=<registry> \
OCIR_USERNAME=<username> \
OCIR_AUTH_TOKEN=<token> \
/opt/raota/deploy-app.sh
```

Redis deploy script:

```bash
REMOTE_DIR=/opt/raota-redis /opt/raota-redis/deploy-redis.sh
```
