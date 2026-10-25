#!/usr/bin/env bash
# 운영 이미지를 AOT 캐시와 함께 만든다(#68). build/libs의 bootJar가 먼저 있어야 한다.
#
#   scripts/build-image.sh <이미지 이름:태그> [커밋 SHA]
#
# 1. Dockerfile의 app 단계(캐시 없는 이미지)를 만든다.
# 2. 임시 MySQL·Redis를 띄우고 그 이미지로 운영 프로필 서버를 컨텍스트 refresh까지만 실행해 AOT 캐시를 학습한다.
#    Flyway·JPA 검증·Redis Stream 소비자 그룹이 기동 중에 DB·Redis에 붙으므로 docker build 안에서는 학습할 수 없다.
# 3. 캐시를 얹은 최종 이미지를 만든다. 캐시는 JDK·클래스패스가 같아야 쓰이므로 1의 이미지 위에서만 만든다.
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "usage: $0 <image:tag> [git-commit]" >&2
  exit 2
fi

image="$1"
commit="${2:-unknown}"
root="$(cd "$(dirname "$0")/.." && pwd)"
run_id="raota-aot-$$"
app_image="${run_id}:app"
mysql="${run_id}-mysql"
redis="${run_id}-redis"

cleanup() {
  docker rm -f "${mysql}" "${redis}" >/dev/null 2>&1 || true
  docker network rm "${run_id}" >/dev/null 2>&1 || true
  docker rmi "${app_image}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker build --build-arg GIT_COMMIT="${commit}" --target app -t "${app_image}" "${root}"

docker network create "${run_id}" >/dev/null
# 127.0.0.1로 ping해야 초기화용 임시 서버(--skip-networking)가 아닌 실제 서버가 뜬 뒤에 healthy가 된다.
docker run -d --name "${mysql}" --network "${run_id}" \
  -e MYSQL_ROOT_PASSWORD=password -e MYSQL_DATABASE=raota \
  --health-cmd "mysqladmin ping -h 127.0.0.1 -ppassword --silent" --health-interval 2s --health-retries 60 \
  mysql:8.0 >/dev/null
docker run -d --name "${redis}" --network "${run_id}" redis:7.2-alpine >/dev/null

for _ in $(seq 1 90); do
  [[ "$(docker inspect -f '{{.State.Health.Status}}' "${mysql}")" == "healthy" ]] && break
  sleep 2
done
if [[ "$(docker inspect -f '{{.State.Health.Status}}' "${mysql}")" != "healthy" ]]; then
  echo "MySQL did not become healthy" >&2
  exit 1
fi

aot_dir="${root}/build/aot"
rm -rf "${aot_dir}"
mkdir -p "${aot_dir}"
# 러너 사용자 권한으로 실행해야 다음 빌드의 clean이 캐시 파일을 지울 수 있다.
docker run --rm --network "${run_id}" --user "$(id -u):$(id -g)" \
  --env-file "${root}/scripts/aot-training.env" \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://${mysql}:3306/raota" \
  -e SPRING_DATA_REDIS_HOST="${redis}" \
  -e JAVA_TOOL_OPTIONS="-Xmx2g -XX:AOTCacheOutput=/aot/app.aot -Dspring.context.exit=onRefresh" \
  -v "${aot_dir}:/aot" \
  "${app_image}"

docker build --build-arg GIT_COMMIT="${commit}" -t "${image}" "${root}"
