# Prod Compose Migration

이 문서는 `Docker Swarm`에서 `OCI Load Balancer + worker별 Docker Compose` 구조로 옮길 때의 기준 절차를 정리한다.

## 현재 기준값

- OCI LB: `ocid1.loadbalancer.oc1.ap-chuncheon-1.aaaaaaaavlscjywcxzfh5mrwiugwenmmq4jzkg3e3w3dplnxplsbwcmhwq3a`
- Backend set: `bs_raota_prod`
- Worker backends:
  - `10.0.1.213:8080`
  - `10.0.1.144:8080`
  - `10.0.1.87:8080`
- Redis fixed host: `10.0.1.44`

## 리포에 추가된 산출물

- `docker-compose.prod.yml`
- `docker-compose.redis.prod.yml`
- `scripts/bootstrap-compose-host.sh`
- `scripts/deploy-compose-node.sh`
- `scripts/deploy-redis-compose-node.sh`
- `.github/workflows/cd-prod-compose.yml`

## GitHub Actions prerequisites

- Secret `PROD_DEPLOY_SSH_PRIVATE_KEY`
  - worker 접속용 개인키 전체 내용
- Existing secrets reused:
  - `OCIR_REGISTRY`
  - `OCIR_NAMESPACE`
  - `OCIR_REPOSITORY`
  - `OCIR_USERNAME`
  - `OCIR_AUTH_TOKEN`
- Self-hosted runner prerequisites:
  - worker private IP로 SSH 가능

## 이미 반영된 인프라 값

- OCI Vault secret `SPRING_DATA_REDIS_HOST`는 `redis`에서 `10.0.1.44`로 변경됐다.
- worker instance pool은 재부팅 후 방화벽 규칙이 유지되도록 새 instance configuration을 보도록 바뀌었다.

## 배포 모드

### Canary

`workflow_dispatch`로 `deploy_mode=canary` 실행:

- 지정한 worker 1대만 대상으로 진행
- 대상 worker를 Swarm에서 `drain`
- OCI LB health check가 해당 backend를 자동 제외
- 새 이미지 배포
- readiness 확인
- health check 통과 후 자동 재편입

### Full

`workflow_dispatch`로 `deploy_mode=full` 실행:

- worker 3대를 순차 배포
- 각 worker는 cutover 시점에 Swarm에서 `drain`
- 마지막에 `raota_app` service 제거

## Cutover sequence

1. `PROD_DEPLOY_SSH_PRIVATE_KEY` GitHub secret 추가
2. `cd-prod-compose.yml` canary 실행
3. 대상 worker에서 `/opt/raota/.env.prod`, `/opt/raota/docker-compose.prod.yml` 확인
4. OCI LB 콘솔에서 해당 backend가 다시 healthy 상태인지 확인
5. full 배포 실행
6. manager에서 standalone Redis 준비
7. `raota_redis` service 제거 직후 `docker-compose.redis.prod.yml`로 Redis 기동
8. 최종 검증 후 worker/manager에서 Swarm 정리

## Hybrid canary state

canary 이후엔 다음 상태가 된다.

- canary worker: `docker compose` app
- 나머지 worker: Swarm app
- canary worker는 Swarm `drain`

이 상태는 의도된 중간 단계다.

## Swarm retirement

full cutover 검증이 끝난 뒤에만 실행한다.

### Manager

Redis standalone 전환:

```bash
mkdir -p /opt/raota-redis
cp docker-compose.redis.prod.yml /opt/raota-redis/
docker service rm raota_redis
REMOTE_DIR=/opt/raota-redis ./scripts/deploy-redis-compose-node.sh
```

그 다음:

```bash
docker node ls
```

### Workers

```bash
docker swarm leave
```

### Manager 마지막 정리

```bash
docker node rm <left-node-id>
docker swarm leave --force
```

## 주의사항

- Compose app는 host `8080`을 직접 점유하므로 같은 노드에서 Swarm app task와 동시에 띄우면 충돌한다.
- 새 workflow는 대상 worker를 먼저 `docker node update --availability drain` 하고, OCI LB health check가 기존 task down 상태를 감지할 시간을 준 뒤 Compose app를 올린다.
- `scripts/deploy-compose-node.sh`는 readiness 실패 시 직전 이미지로 자동 rollback한다.
- manager에는 `oci` CLI를 별도 설치했지만, 현재 instance principal엔 LB 수정 권한이 없어서 workflow는 LB API를 직접 호출하지 않는다.
