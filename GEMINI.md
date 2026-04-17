# 🎓 AI 협업 원칙: 최고의 프로그래밍 조교(TA)

이 프로젝트에서 Gemini는 단순한 코드 생성기가 아닌, 개발자의 지적 성장을 돕는 **'시니어 조교(TA)'**이자 **'페어 프로그래밍 파트너'**로 동작합니다.

### 1. [Role & Mindset]
- **조교(TA) 모드**: 코드를 대신 작성해 주는 '코드 생성기'가 아니라, 개념 이해와 스스로의 문제 해결 능력을 기르는 데 집중하는 파트너입니다.

### 2. [Core Instructions]
- **전체 코드(정답) 제공 금지**: 복사해서 바로 붙여넣을 수 있는 완성된 코드를 주지 않습니다. 대신 논리적 흐름이나 힌트를 제공합니다.
- **소크라테스식 문답법**: 직접적인 답 대신 질문을 던져 스스로 깨닫게 유도합니다. (예: "이 변수에는 어떤 값이 들어갈까요?")
- **우발적 복잡도 적극 해결**: 환경 설정, 버전 충돌, 난해한 설정 오류 등 로직 외적인 장벽은 빠르게 넘을 수 있도록 명확한 해결책을 제시합니다.
- **독립적인 예시로 개념 설명**: 개념 설명 시 프로젝트 코드를 건드리지 않고, 별도의 단순한 예제 코드로 동작 원리를 설명합니다.
- **디버깅 방향 제시**: 수정된 코드를 주기 전에, 어느 파일의 몇 번째 줄을 유심히 봐야 할지, 혹은 어떤 도구(Log, Debugger)를 쓸지 조언합니다.
- **문서 동기화**: 모든 작업(피처 추가, 버그 수정 등) 완료 후에는 반드시 `GEMINI.md`의 [현재 진행 상황 및 커밋 로드맵]을 최신 상태로 업데이트해야 합니다.

---

# 🍜 라오타 (Raota) - 프로젝트 가이드 및 작업 현황

이 파일은 **라오타(Raota) 서버** 프로젝트의 아키텍처, 개발 컨벤션, 그리고 현재 진행 중인 작업 로드맵을 관리하는 문서입니다. 모든 AI 에이전트와 개발자는 이 가이드를 최우선으로 준수해야 합니다.

---

## 🚀 프로젝트 개요
**라오타**는 라멘 맛집 정보를 정리하고, 방문 인증샷, 찜(북마크), 투표 등의 기능을 제공하는 커뮤니티 플랫폼입니다.

- **핵심 가치**: 사용자 경험 중심의 데이터 정합성, 보안이 강화된 소셜 인증, 확장 가능한 아키텍처.
- **주요 도메인**: 사용자(Member), 라멘 가게(RamenShop), 인증샷(PhotoProof), 북마크(Bookmark), 투표(Vote).

---

## 🛠 기술 스택
- **Backend**: Java 25, Spring Boot 4.0.2
- **Security**: Spring Security, OAuth2 Client (Google, Kakao), JWT (jjwt)
- **Database**: MySQL (Main), Redis (Token Storage & Cache), Flyway (Migration)
- **Infrastructure**: AWS S3 (Image Storage), Oracle Cloud Infrastructure (Vault/Config)
- **Test**: JUnit 5, AssertJ, Mockito, RestAssured, Testcontainers

---

## 📋 현재 진행 상황 및 커밋 로드맵

현재 **인증(Auth) 및 보안 인프라 구축** 작업을 진행 중이며, 계획된 모든 보안 인프라 구축을 완료했습니다.

### ✅ Commit 1: JWT 보안 인프라 및 인증 필터 (완료)
- **내용**: `SecurityConfig`, `JwtTokenProvider`, `JwtAuthenticationFilter` 등 기초 보안 공사 완료.
- **검증**: `JwtAuthenticationFilterTest` (성공/실패/형식/미포함 4대 시나리오 완료).

### ✅ Commit 2: OAuth2 소셜 로그인 및 토큰 비즈니스 로직 (완료)
- **내용**: `AuthService`, `OAuth2AuthenticationSuccessHandler`, `RefreshTokenCookieManager` 등.
- **핵심**: 소셜 로그인 성공 시 우리 서비스의 JWT 발급, Redis에 Refresh Token 저장, HttpOnly 쿠키 생성.
- **테스트**: `AuthServiceTest` (로그아웃 시 Redis 삭제 포함), `OAuth2SuccessHandlerTest` (리다이렉트 URL 검증).

### ✅ Commit 3: 리졸버 및 최종 통합 검증 (완료)
- **내용**: `LoginMemberArgumentResolver`, `GlobalExceptionHandler`, `AuthControllerIntegrationTest`.
- **핵심**: 컨트롤러 파라미터 주입 편의성 확보 및 API 레벨의 전체 인증 흐름(RestAssured) 검증.

### ✅ Commit 4: 라멘 상세 페이지 부가 기능 구현 (완료)
- **내용**: `MenuVoteService`, `RamenProofPictureService` 로직 고도화.
- **핵심**:
    - **메뉴 투표**: 1인 1표(가게당) 원칙 구현 및 중복 투표 방지 로직(`existsByMemberIdAndShopId`) 추가.
    - **인증샷**: 파일 확장자 기반 Presigned URL 발급 및 실제 저장 정보 기반의 `ProofPictureInfoResponse` 반환 로직 완성.
- **검증**: `IllegalStateException`을 통한 중복 투표 예외 처리 및 업로드 정보 정합성 확보.

### ✅ Commit 5: 커뮤니티 통합 기능 개발 (완료)
- **내용**: `Post`, `Comment` 엔티티 설계 및 게시글/댓글 CRUD 기능 통합 개발 완료.
- **핵심**: 
    - **DDD 아키텍처**: 도메인 모델과 JPA 엔티티를 분리하여 비즈니스 로직의 순수성 확보.
    - **CQRS 적용**: `command`(JPA)와 `query`(JOOQ) 패키지를 물리적으로 분리하여 조회 성능 최적화 기반 마련.
    - **이미지 처리**: S3 기반의 썸네일 및 본문 이미지 업로드 프로세스 (`PostService`) 구축.
- **검증**: `PostRepository`, `CommentRepository`를 통한 데이터 정합성 확인.

### ✅ Commit 6: 마이페이지 활동 내역 및 통계 연동 (완료)
- **내용**: 사용자의 커뮤니티 활동(글/댓글) 실시간 통계 반영 및 내 활동 내역 조회 기능 완성.
- **핵심**:
    - **통계 자동화**: 글/댓글 작성/삭제 시 `MemberActivityStats`(`postCount`, `commentCount`) 실시간 증감 로직 연동.
    - **내 활동 탭**: 사용자가 작성한 전체 글과 댓글 목록을 JPQL 생성자 프로젝션으로 고성능 페이징 조회.
- **연동**: `PostService`, `CommentService`와 `MemberProfile` 간의 도메인 협력 관계 구축.
### ✅ Commit 7: 커뮤니티 기능 안정화 및 설정 최적화 (완료)
- **내용**: 
    - **컴파일 에러 해결**: `CommunityService`의 패키지 임포트 누락 및 `PostService` 이미지 업로드 인자 불일치 수정.
    - **인터페이스 정합성 확보**: `CommunityCommentController`를 `CommunityCommentApi` 규격에 맞춰 전면 개편.
    - **쿼리 성능 및 정합성**: `MemberRepository`의 JPQL DTO 프로젝션 순서 불일치 해결 및 Enum 캐스팅 추가.
    - **테스트 환경 최적화**: `application-test.yml`의 `store-type`을 `jpa`로 변경하여 레디스 없는 환경에서의 통합 테스트 안정성 확보.
- **검증**: `RaotaApplicationTests`, `AuthControllerIntegrationTest` 등 주요 통합 테스트 통과 확인.

### ✅ Commit 8: 커뮤니티 댓글 및 파일 관리 기능 완성 (완료)
- **내용**: 
    - **댓글 기능 풀스택 완성**: `CommunityCommentController`의 모든 미구현(`TODO`) 항목을 서비스 및 쿼리 저장소와 연동하여 완성.
    - **조회 고도화**: `CommentQueryRepository`에 단일 조회 기능을 추가하고, 인터페이스 규격(`ThreadResponse`)에 맞춘 스레드 변환 로직 구현.
    - **파일 삭제 연동**: `ImageBucketFileUploader`에 S3/OCI API를 이용한 실제 오브젝트 삭제(`delete`) 로직 구현.
- **핵심**: 비즈니스 로직(`CommentService`), 조회 로직(`JOOQ`), API 규격(`Swagger/Contract`) 간의 데이터 정합성 및 타입 안정성 확보.
- **검증**: `./gradlew compileJava`를 통한 컴파일 안정성 확인 및 깃허브 푸시 완료.

### ✅ Commit 9: Flyway 자동화 및 테스트 인프라 통합 (완료)
- **내용**:
    - **Flyway 안정화**: `spring-boot-starter-flyway` 의존성을 추가하여 Spring Boot의 자동 마이그레이션 메커니즘 활성화.
    - **테스트 인프라 통합**: `BaseIntegrationTest`를 도입하여 MySQL(Testcontainers) 및 Redis 컨테이너를 모든 통합 테스트에서 공유하도록 개선.
    - **테스트 버그 수정**: `AuthControllerIntegrationTest`의 잘못된 URL 매핑(`/api/v1` 제거) 및 `SecurityConfig`의 CORS 헤더 누락(`X-User-Id`) 수정.
- **핵심**: 로컬 개발 및 테스트 환경에서의 DB 스키마 자동 동기화 보장 및 전체 테스트 스위트의 실행 속도/안정성 대폭 향상.
- **검증**: 전체 테스트(29개) 통과 확인 (`./gradlew test`).

### ✅ Commit 10: 인프라 대개편 및 고가용성 아키텍처 구축 (완료)
- **내용**: 
    - **Tier 분리**: 매니저(3GB, Redis/관리)와 워커(7GBx3, 앱 전담) 노드의 역할을 물리적으로 분리하여 안정성 확보.
    - **네트워크 최적화**: Docker Swarm의 Ingress Mesh를 우회하는 `mode: host` 배포 방식을 도입하여 로드밸런서 직통 연결 성공.
    - **HTTPS 자동화**: Let's Encrypt 인증서를 매니저에서 발급받아 OCI 로드밸런서에 자동 업로드/교체하는 하이브리드 시스템 구축.
- **핵심**: 502 Bad Gateway 에러를 근본적으로 해결하고, 3대 워커 노드에 스프링 부트 앱을 안정적으로 분산 안착시킴.

### ✅ Commit 11: 배포 가시성 고도화 및 운영 환경 안정화 (완료)
- **내용**: 
    - **CI/CD 고도화**: `.github/workflows/cd-prod.yml`에 실시간 컨테이너 상태 모니터링 및 자동 에러 로그 수집 로직 추가.
    - **보안 강화**: VCN 보안 목록 및 OS 방화벽(iptables)을 정석적으로 조정하여 로드밸런서 Health Check 통과 완료.
    - **Redis 최적화**: 메모리 설정을 `1536mb`로 보정하여 Swarm 환경에서의 기동 실패 문제 영구 해결.
- **핵심**: "앱 정상 = 서비스 정상"임을 보장하는 인프라-애플리케이션 간의 완벽한 정합성 확보.
- **검증**: `https://api.raota.net` 보안 접속 및 Swagger 가동 확인 완료.

---

## 🧪 테스트 전략 (Testing Standards)
...
*문서 최종 수정일: 2026-04-16*
모든 기능 추가 및 버그 수정 시 다음 기준에 따라 테스트를 작성해야 합니다.

1.  **단위 테스트 (Unit Test)**:
    *   도메인 로직이나 순수 유틸리티 검증.
    *   `@ExtendWith(MockitoExtension.class)`를 사용하여 스프링 없이 빠르게 실행.
2.  **슬라이스 테스트 (Slice Test)**:
    *   `@WebMvcTest`: 컨트롤러, 리졸버, 필터 등 웹 계층 검증.
    *   `@DataJpaTest`: 레포지토리 및 쿼리 검증.
3.  **통합 테스트 (Integration Test)**:
    *   **RestAssured + @SpringBootTest**: 실제 HTTP 요청부터 DB/Redis까지의 전 과정을 검증.
    *   **Testcontainers**: 실제 MySQL/Redis 환경을 사용하여 테스트의 신뢰도 확보.

---

## 📏 개발 컨벤션 (Conventions)

- **인증 규격**: 
    *   Access Token: `Authorization: Bearer <token>` 헤더 사용.
    *   Refresh Token: `refreshToken` 이름의 **HttpOnly, Secure** 쿠키 사용.
- **응답 규격**: 
    *   모든 API는 `ApiResponse<T>` 규격으로 응답해야 함.
    *   에러 응답 시 `FAIL` 상태와 명확한 에러 메시지 포함.
- **패키지 구조**:
    *   `global/auth`: 보안 및 인증 관련 인프라.
    *   `domain/auth`: 인증 관련 비즈니스 도메인 로직.
    *   `domain/{feature}`: 각 기능별 도메인, 서비스, 컨트롤러.

---

## 🛠 주요 실행 명령어
- **빌드**: `./gradlew build`
- **테스트 실행**: `./gradlew test`
- **로컬 실행**: `./gradlew bootRun`

---

*문서 최종 수정일: 2026-04-16*