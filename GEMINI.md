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

---

## 🧪 테스트 전략 (Testing Standards)

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

*문서 최종 수정일: 2026-03-24*