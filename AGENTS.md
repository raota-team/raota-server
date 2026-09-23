# Raota Server — Agent Guide

라멘 커뮤니티 **라오타**의 백엔드. Java 25, Spring Boot 4.1, Spring Modulith 2.1 기반 모듈러 모놀리스.
MySQL 8 + Flyway, Redis, Spring Security(OAuth2 + JWT), Spring AI(Oracle Vector Store).

현재 목표: 웹 v1(`com.raota.web`)과 별개로 모바일 앱 기준 v2 도메인(`com.raota.mobile`)을 새로 구축한다.

## 명령

```bash
./gradlew format                               # Spring Java Format 자동 적용 (커밋 전)
./gradlew checkFormat                          # 포맷 검사 (CI·CD에서 실행)
./gradlew compileJava compileTestJava          # 빠른 컴파일 확인
./gradlew test --tests 'com.raota.system.ModulithArchitectureTest'
./gradlew test --tests '<FQCN>'                # 변경 범위 테스트
./gradlew test                                 # 전체 테스트
```

- 테스트는 `test` 프로필로 실행되며 통합 테스트(`BaseIntegrationTest`)가 Testcontainers로 MySQL 8 · Redis를 띄운다. **Docker가 실행 중이어야 한다.**
- 패키지 이동, `@NamedInterface` 변경, 새 모듈 추가 뒤에는 전체 테스트를 실행한다.
- CI는 `.github/workflows/format-check.yml`(`checkFormat`)과 `test.yml`(전체 테스트)로 나뉘어 GitHub 호스팅 러너에서 병렬 실행된다. `main` push는 CD가 두 워크플로를 모두 통과한 뒤에만 배포한다.

## 아키텍처

@docs/architecture.md

## v2(mobile) 규칙

- 경로 `/api/v2/**`, 테이블 접두어 `tb_v2_*`.
- 새 도메인은 `com.raota.mobile.<domain>`에 두고 그 루트 `package-info.java`에 `@ApplicationModule`을 선언한다. `com.raota.mobile` 자체에는 붙이지 않는다.
- 구현보다 먼저 모듈 등록과 `ModulithArchitectureTest`(예상 모듈 목록) 갱신을 한다.
- `web`과 `mobile`은 서로 참조하지 않는다. v1 데이터가 필요하면 `web` 클래스가 아니라 v1 테이블을 직접 읽는다.
- v2 오류는 `mobile.common.error.MobileException(MobileErrorCode, message)`으로 던진다. `ResponseStatusException`, `IllegalArgumentException`, `EntityNotFoundException` 등은 v2에서 500 `INTERNAL_ERROR`로 처리된다.
- v2 오류 응답은 발생 단계별로 이미 처리된다: 보안 필터(401·403)는 `RestSecurityErrorWriter`, 컨트롤러 결정 전(없는 경로·405)은 `MobileFallbackExceptionResolver`, 컨트롤러 결정 후는 `MobileExceptionAdvice`. v2 도메인 코드는 새 advice나 resolver를 만들지 않고 `MobileException`만 던진다.
- v2 응답 JSON: ID 필드는 `String`, 시각은 `Instant`(UTC ISO-8601, 끝에 `Z`), 날짜는 `LocalDate`(`YYYY-MM-DD`). v2 응답에 `LocalDateTime`은 쓰지 않는다(운영 JVM이 `Asia/Seoul`이라 KST 표시 없는 값이 나간다). null 필드는 생략하지 않는다. 전역 Jackson·JVM 타임존 설정은 v1에 영향을 주므로 바꾸지 않는다.

## API 접근 정책 (fail-closed)

endpoint를 추가하거나 메서드·경로를 바꾸면 함께 처리한다.

1. `web/account/infrastructure/config/EndpointAccessPolicy`에 `PUBLIC` / `AUTHENTICATED` / `ADMIN` 중 하나로 등록.
2. `ApiAccessPolicyInventoryTest`의 endpoint 수 갱신.
3. PUBLIC 또는 ADMIN 예외를 추가했다면 익명·USER·ADMIN 동작 테스트 추가.

`Long` ID 동적 경로는 `[0-9]+`로 제한한다.

## DB 마이그레이션

- 위치: `src/main/resources/db/migration/V{n}__{snake_case_설명}.sql`. 다음 번호는 기존 최대 버전 + 1.
- 이미 커밋된 마이그레이션 파일은 절대 수정하지 않는다. 변경은 새 버전으로 추가한다.
- 운영은 `ddl-auto=validate`다. Entity 매핑과 마이그레이션 스키마가 정확히 일치해야 한다.

## 작업 원칙

- 최소하고 리뷰 가능한 diff를 우선한다. 요청과 직접 관련된 코드만 수정하고, 무관한 리팩터링이나 추측성 추상화를 하지 않는다.
- 요구사항이 모호하면 코딩 전에 확인한다. 구현 전 검증 기준을 정한다.
- 기존 프로젝트 패턴과 맞는 가장 단순한 해법을 고른다.
- Spring 구조를 일관되게 유지한다: controller는 얇게, 비즈니스 로직은 service에, 생성자 주입.
- JPA fetch 전략(N+1, LAZY 로딩 범위)과 트랜잭션 경계를 주의한다.

## Git과 배포

- `main`에 push되면 GitHub Actions가 곧바로 운영 배포한다(`.github/workflows/cd-prod-compose.yml`).
- 사용자가 명시적으로 요청하지 않으면 commit, push, 브랜치 삭제, `reset --hard`를 하지 않는다.
