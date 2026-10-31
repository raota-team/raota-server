# Raota Server — Agent Guide

라멘 커뮤니티 **라오타**의 백엔드. Java 25, Spring Boot 4.1, Spring Modulith 2.1 기반 모듈러 모놀리스.
MySQL 8 + Flyway, Redis, Spring Security(OAuth2 + JWT), Spring AI(Oracle Vector Store).

현재 목표: 웹 v1(`com.raota.web`)과 별개로 모바일 앱 기준 v2 도메인(`com.raota.mobile`)을 새로 구축한다.

## 명령

```bash
./gradlew spotlessApply                        # Palantir Java Format 자동 적용 (커밋 전)
./gradlew spotlessCheck                        # 포맷 검사 (CI·CD에서 실행). 손으로 맞춘 모양은 // spotless:off ~ on으로만 지킨다
./gradlew checkstyleMain checkstyleTest        # v2 코드 규칙 검사 (CI·CD에서 실행)
./gradlew compileJava compileTestJava          # 빠른 컴파일 확인
./gradlew test --tests 'com.raota.system.ModulithArchitectureTest'
./gradlew test --tests '<FQCN>'                # 변경 범위 테스트
./gradlew test                                 # 전체 테스트
```

- 테스트는 `test` 프로필로 실행되며 통합 테스트(`BaseIntegrationTest`)가 Testcontainers로 MySQL 8 · Redis를 띄운다. **Docker가 실행 중이어야 한다.**
- 패키지 이동, `@NamedInterface` 변경, 새 모듈 추가 뒤에는 전체 테스트를 실행한다.
- CI는 `.github/workflows/format-check.yml`(`spotlessCheck`, `checkstyleMain`, `checkstyleTest`)과 `test.yml`(전체 테스트)로 나뉘어 GitHub 호스팅 러너에서 병렬 실행된다. `main` push는 CD가 두 워크플로를 모두 통과한 뒤에만 배포한다.

## 아키텍처

@docs/architecture.md

## v2(mobile) 규칙

- 경로 `/api/v2/**`, 테이블 접두어 `tb_v2_*`.
- 새 도메인은 `com.raota.mobile.<domain>`에 두고 그 루트 `package-info.java`에 `@ApplicationModule`을 선언한다. `com.raota.mobile` 자체에는 붙이지 않는다.
- 구현보다 먼저 모듈 등록과 `ModulithArchitectureTest`(예상 모듈 목록) 갱신을 한다.
- `web`과 `mobile`은 서로 참조하지 않는다. v1 데이터가 필요하면 `web` 클래스가 아니라 v1 테이블을 직접 읽는다.
- v2 오류는 `mobile.common.error.MobileException(MobileErrorCode, message)`으로 던진다. `ResponseStatusException`, `IllegalArgumentException`, `EntityNotFoundException` 등은 v2에서 500 `INTERNAL_ERROR`로 처리된다.
- v2 오류 응답은 발생 단계별로 이미 처리된다: 보안 필터(401·403)는 `MobileSecurityErrorResponder`, 컨트롤러 결정 전(없는 경로·405)은 `MobileFallbackExceptionResolver`, 컨트롤러 결정 후는 `MobileExceptionAdvice`. v2 도메인 코드는 새 advice나 resolver를 만들지 않고 `MobileException`만 던진다.
- agent가 제공하는 v2 API(AI 추천, 오늘의 큐레이션)의 컨트롤러는 `agent.presentation.mobile`에 둔다. v2 응답·오류 advice가 이 패키지까지만 적용되므로 다른 agent 패키지에 두면 v1 형식으로 응답한다. 접근 규칙은 같은 패키지의 `MobileAgentAccessRules`에 등록한다.
- v2 데이터의 관리자 API는 `/admin/api/v2/**`이고 데이터를 소유한 모듈의 `presentation.admin`에 둔다(`mobile.shop`의 `MobileShopAdminController`, `mobile.ramenlog`의 `MobileContentReportAdminController`·`MobileRamenLogAdminController`). 관리자 웹(raota-front)이 웹 관리자 로그인으로 부르므로 인증·권한은 v1 `EndpointAccessPolicy`의 `/admin/**` ADMIN 규칙이 맡아 따로 등록하지 않는다(401·403은 v1 형식). 성공·오류 응답은 v2 형식이고, 번호 페이지 목록은 `mobile.common.page.OffsetPage`를 쓴다.
- `tb_v2_shop` ID는 v1 매장 ID와 같다(V29 복사). 관리자가 새로 만드는 v2 매장은 V36부터 10,000,000번대에서 시작해 이후 v1 매장 ID와 겹치지 않는다. AI 추천은 v1 RAG 색인의 매장 ID를 v2 매장으로 읽으므로 이 간격을 줄이지 않는다.
- `tb_v2_user` ID도 v1 회원 ID와 같다. v1 회원 가입·가입 완료·소셜 계정 추가는 DB 트리거(V45)가 같은 ID의 v2 회원·소셜 계정으로 복사한다(규칙은 V45 주석). 앱 가입 ID는 운영에서 10,000,000부터 시작한다(2026-10-31 수동 변경). 운영 86번은 v1(구글)과 v2(Apple)가 다른 사람이므로 v1 데이터를 회원 ID로 옮길 때 제외한다. 트리거는 v1 탈퇴·프로필 수정은 옮기지 않는다.
- 앱 업로드 사진(`v2/` 아래 객체)은 `global.file.cleanup`의 `OrphanImageCleanupScheduler`가 매일 05:10(KST) 정리한다. `StoredImageReferences`를 제공한 접두어(`mobile.ramenlog`: `v2/ramen-logs/`, `mobile.account`: `v2/profiles/`)만 보고, 마지막 수정 뒤 `app.file.orphan-cleanup.retention`(기본 7일)이 지났으며 그 기여자가 참조한다고 답하지 않은 객체를 지운다. 이 접두어의 사진 주소를 새 테이블·컬럼에 저장하게 되면 해당 기여자의 참조 쿼리에 같이 넣는다. 넣지 않으면 그 사진은 7일 뒤 지워진다. 접두어는 `mobile.common.upload.MobileImageFolder` 한 곳에서 정의하고 업로드 티켓·사진 주소 검증·정리 기여자가 함께 쓴다. 한 폴더의 사진 주소를 다른 용도의 컬럼에 받지 않는다(기록 사진은 `v2/ramen-logs/`만 받는다).
- v2 응답 JSON: ID 필드는 `String`, 시각은 `Instant`(UTC ISO-8601, 끝에 `Z`), 날짜는 `LocalDate`(`YYYY-MM-DD`). v2 응답에 `LocalDateTime`은 쓰지 않는다(운영 JVM이 `Asia/Seoul`이라 KST 표시 없는 값이 나간다). null 필드는 생략하지 않는다. 전역 Jackson·JVM 타임존 설정은 v1에 영향을 주므로 바꾸지 않는다.
- v2 DTO 이름: 서비스·포트가 돌려주는 타입은 `application/result`에 두고 `~Result`로 끝낸다(컨트롤러가 그대로 응답 JSON으로 내보낸다). 요청 바인딩은 `presentation/request`의 `~Request`다. `~Response`는 API 응답 모양이 결과와 달라 컨트롤러에서 옮겨 담아야 할 때만 `presentation/response`에 만든다. 클래스 이름을 바꿔도 JSON 필드는 바뀌지 않는다.
- v2 코드(`com.raota.mobile`, 테스트 포함)에서는 삼항 연산자(`a ? b : c`)를 쓰지 않는다. `if` 문, `switch` 식 또는 작은 메서드로 쓴다. Checkstyle `AvoidInlineConditionals`(`config/checkstyle/checkstyle.xml`)가 `mobile` 패키지만 검사한다.

## API 접근 정책 (fail-closed)

보안 필터 체인은 `global.security`에 하나만 두고 각 모듈이 인증기·오류 응답기·접근 규칙을 플러그인으로 제공한다.
v1 접근 규칙은 `web.account`의 `EndpointAccessPolicy`가 관리한다. v2 규칙은 각 모듈의 `presentation`에서 `AccessRuleContributor`로 제공한다(`mobile.account`의 `MobileAccessRules`, `mobile.shop`의 `MobileShopAccessRules`, `mobile.ramenlog`의 `MobileRamenLogAccessRules`).

endpoint를 추가하거나 메서드·경로를 바꾸면 함께 처리한다.

1. v1은 `web/account/infrastructure/config/EndpointAccessPolicy`, v2는 해당 모듈의 `presentation`에 있는 `AccessRuleContributor`에 `PUBLIC` / `AUTHENTICATED` / `ACTIVE_MEMBER` / `ADMIN` 중 하나로 등록.
2. `ApiAccessPolicyInventoryTest`의 endpoint 수 갱신.
3. PUBLIC 또는 ADMIN 예외를 추가했다면 익명·USER·ADMIN 동작 테스트 추가.
4. 새 v2 쓰기 API는 원칙적으로 `ACTIVE_MEMBER`로 등록한다. 온보딩 중 이용해야 하는 내 정보 조회·수정, 온보딩, 약관 동의(`PUT /members/me/consents`), 닉네임 중복 확인, 업로드 티켓, 로그아웃만 `AUTHENTICATED`로 등록한다.

## DB 마이그레이션

- 위치: `src/main/resources/db/migration/V{n}__{snake_case_설명}.sql`. 다음 번호는 기존 최대 버전 + 1.
- 이미 커밋된 마이그레이션 파일은 절대 수정하지 않는다. 변경은 새 버전으로 추가한다.
- 운영은 `ddl-auto=validate`다. Entity 매핑과 마이그레이션 스키마가 정확히 일치해야 한다.

## 운영 비밀값

- 운영 비밀값의 원본은 OCI Vault다. 앱은 Vault를 부르지 않는다. 배포(raota-infra)가 Vault의 ACTIVE 비밀값을 이름별 파일로 노드에 넣고, 앱은 `application-prod.yml`의 `spring.config.import: optional:configtree:/run/secrets/raota/`로 읽는다.
- 새 비밀값은 `application-prod.yml`에서 `${이름}`으로 참조하고(이름은 `^[A-Z][A-Z0-9_]*$`, 파일 이름이 그대로 속성 이름), 같은 이름의 가짜 값을 `ProdProfileStartupTest`에 넣는다. 빠뜨리면 CI가 기동에 실패한다. Vault에 실제 값을 만드는 일은 사용자에게 요청한다.
- 비밀값을 지울 때는 참조를 먼저 지워 배포한 뒤 Vault에서 지운다. 참조하는 비밀값이 없으면 운영 서버가 뜨지 않는다.

## 작업 원칙

- 최소하고 리뷰 가능한 diff를 우선한다. 요청과 직접 관련된 코드만 수정하고, 무관한 리팩터링이나 추측성 추상화를 하지 않는다.
- 요구사항이 모호하면 코딩 전에 확인한다. 구현 전 검증 기준을 정한다.
- 기존 프로젝트 패턴과 맞는 가장 단순한 해법을 고른다.
- Spring 구조를 일관되게 유지한다: controller는 얇게, 비즈니스 로직은 service에, 생성자 주입.
- JPA fetch 전략(N+1, LAZY 로딩 범위)과 트랜잭션 경계를 주의한다.

## Git과 배포

- `main`에 push되면 GitHub Actions가 곧바로 운영 배포한다(`.github/workflows/cd-prod-compose.yml`).
- 사용자가 명시적으로 요청하지 않으면 commit, push, 브랜치 삭제, `reset --hard`를 하지 않는다.
