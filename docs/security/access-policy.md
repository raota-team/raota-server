# API 접근 정책

이 문서는 라오타 서버 HTTP 엔드포인트의 접근 등급과 Spring Security 적용 순서를 정의한다.

## 기본 원칙

- `PUBLIC`: 인증 없이 호출할 수 있도록 메서드와 경로를 명시한 엔드포인트다.
- `AUTHENTICATED`: 유효한 Access Token과 활성 회원이 필요한 엔드포인트다.
- `ADMIN`: 현재 DB 역할이 `ADMIN`인 회원만 호출할 수 있는 엔드포인트다.
- 명시적으로 분류되지 않은 요청은 `AUTHENTICATED`로 처리한다.
- `LoginMemberArgumentResolver`는 회원 식별을 담당하며 접근 제어의 최종 수단으로 사용하지 않는다.
- 공개 엔드포인트에 잘못된 Bearer Token을 보내면 토큰 오류를 숨기지 않고 401을 반환한다.

## 애플리케이션 엔드포인트

현재 애플리케이션 컨트롤러의 81개 매핑은 `PUBLIC` 31개, `AUTHENTICATED` 35개, `ADMIN` 15개로 분류한다.

### PUBLIC

| 영역 | 메서드와 경로 | 공개 이유 |
|---|---|---|
| 인증 쿠키 흐름 | `POST /auth/refresh`, `POST /auth/logout` | Access Token 없이 Refresh Token 쿠키를 검증해야 한다. |
| 커뮤니티 조회 | `GET /community/posts`, `GET /community/posts/{postId}`, `GET /community/posts/{postId}/comments`, `GET /community/ramen-shops` | 공개 콘텐츠 조회다. |
| 커뮤니티 조회수 | `POST /community/posts/{postId}/views` | 익명 조회를 집계한다. |
| 홈 커뮤니티 | `GET /api/v1/community/posts`, `GET /api/v1/community/posts/popular` | 공개 홈 데이터다. |
| 공개 사용자 활동 | `GET /users/{userId}/profile`, `/photos`, `/visits`, `/posts`, `/comments` | 공개 범위 정책을 application에서 적용한다. |
| 라멘집 조회 | `GET /ramen-shops`, `GET /ramen-shops/{shopId}`, `GET /ramen-shops/{shopId}/menus` | 공개 매장 정보다. |
| 라멘집 조회수 | `POST /ramen-shops/{shopId}/views` | 익명 조회를 집계한다. |
| 투표·인증샷 조회 | `GET /ramen-shops/{shopId}/votes`, `GET /ramen-shops/{shopId}/photos` | 공개 매장 활동이다. |
| 홈 라멘집 | `GET /api/v1/shops/recent-verified` | 공개 홈 데이터다. |
| 라멘로그 조회 | `GET /ramen-logs`, `GET /ramen-logs/{logId}`, `GET /users/{userId}/ramen-logs`, `GET /users/{userId}/ramen-logs/shops` | 공개 범위 정책을 application에서 적용한다. |
| Discovery 조회 | `GET /api/v1/discovery/stats`, `GET /api/v1/discovery/popular-shops/today`, `GET /api/v1/discovery/today-recommendations` | 공개 탐색 데이터다. |
| 기본 상태 | `GET /`, `GET /favicon.ico` | 서비스 기본 응답이다. |
| 로컬 가짜 업로드 | `PUT /files/mock-upload-endpoint` | 로컬 presigned URL 호환용이며 데이터를 저장하지 않는다. |

공개 사용자 경로는 숫자 `userId`만 허용한다. `/users/me/**`가 공개 사용자 경로로 해석되지 않도록 별도의 인증 규칙과 회귀 테스트를 유지한다.

### AUTHENTICATED

| 영역 | 메서드와 경로 |
|---|---|
| 게시글 | `POST /community/posts`, `PATCH /community/posts/{postId}`, `DELETE /community/posts/{postId}`, `POST /community/posts/{postId}/likes` |
| 댓글 | `POST /community/posts/{postId}/comments`, `PUT /community/comments/{commentId}`, `DELETE /community/comments/{commentId}` |
| 내 계정 | `GET /users/me/summary`, `GET /users/me/profile`, `PATCH /users/me/profile`, `PATCH /users/me/email`, `GET /users/me/privacy-settings`, `PATCH /users/me/privacy-settings`, `DELETE /users/me` |
| 내 활동 | `GET /users/me/photos`, `/bookmarks`, `/visits`, `/posts`, `/comments`, `/ramen-logs`, `/ramen-logs/shops` |
| 라멘집 사용자 작업 | `POST /ramen-shops/{shopId}/bookmark`, `/reports`, `/votes/menus/{menuId}` |
| 인증샷 변경 | `POST /ramen-shops/{shopId}/photos`, `DELETE /ramen-shops/{shopId}/photos/{photoId}` |
| AI 매장 기능 | `POST /ramen-shops/ai-search`, `POST /ramen-shops/compare` |
| 라멘로그 변경 | `POST /ramen-logs`, `PATCH /ramen-logs/{logId}`, `DELETE /ramen-logs/{logId}`, `POST /ramen-logs/{logId}/likes` |
| AI 추천 기능 | `POST /recommendations/summary`, `POST /recommendations/chat` |
| 업로드 티켓 | `GET /files/upload-ticket` |

### ADMIN

| 영역 | 메서드와 경로 |
|---|---|
| 관리자 API | 모든 `/admin/**` 요청 |
| 수동 추천 생성 | `POST /api/v1/discovery/today-recommendations/generate` |

`/admin/**`에는 현재 라멘집 CRUD, 제보 조회, 회원 조회와 검색 인덱싱 API가 포함된다. 수동 추천 생성은 경로에 `admin`이 없지만 비용이 발생하고 공유 데이터를 변경하므로 `ADMIN`으로 분류한다.

## 프레임워크와 운영 엔드포인트

### PUBLIC

- `OPTIONS /**`
- ERROR dispatcher와 `/error`
- `GET /login`
- `GET /oauth2/authorization/**`
- `GET /login/oauth2/code/**`
- `GET /swagger-ui.html`, `GET /swagger-ui/**`
- `GET /v3/api-docs`, `GET /v3/api-docs/**`
- `GET /actuator/health`, `GET /actuator/health/**`

### ADMIN

- health를 제외한 모든 `/actuator/**`

Prometheus를 외부 수집기에 연결할 때는 ADMIN Bearer Token 또는 별도의 관리 네트워크 정책을 먼저 결정한다.

## 적용 순서

Spring Security는 다음 순서로 규칙을 적용한다.

1. ERROR dispatcher 허용
2. 명시된 `PUBLIC` matcher 허용
3. `ADMIN` matcher에 `ROLE_ADMIN` 요구
4. `AUTHENTICATED` matcher에 인증 요구
5. 나머지 모든 요청에 인증 요구

## 오류 응답 계약

인증·인가 실패는 UTF-8 JSON으로 응답한다.

```json
{
  "status": "FAIL",
  "message": "인증이 필요합니다.",
  "success": false
}
```

- 401은 `WWW-Authenticate: Bearer` 헤더를 포함한다.
- 일반 익명 요청의 401 메시지는 `인증이 필요합니다.`다.
- 만료·변조·탈퇴 회원처럼 서버가 판정한 JWT 실패는 해당 안전한 메시지를 유지한다.
- 403 메시지는 `접근 권한이 없습니다.`로 고정한다.

## 변경 절차

엔드포인트를 추가하거나 HTTP 메서드·경로를 변경할 때는 다음을 함께 처리한다.

1. `EndpointAccessPolicy`에 접근 등급을 추가한다.
2. 이 문서의 정책표를 갱신한다.
3. `ApiAccessPolicyInventoryTest`의 엔드포인트 수를 갱신한다.
4. 공개 또는 ADMIN 예외를 추가했다면 익명·USER·ADMIN 동작 테스트를 추가한다.

`ApiAccessPolicyInventoryTest`는 실제 Spring MVC 매핑을 읽어 미분류·중복 분류와 예상하지 않은 매핑 수 변경을 실패시킨다.

## 후속 범위

다음 항목은 별도 변경으로 처리한다.

- CORS exact origin, Refresh Token 쿠키와 CSRF 정책
- AI·업로드의 입력 크기, 호출량과 동시성 제한
- 외부 리뷰 파일의 import root, symlink, 확장자와 용량 검증
- 로컬 가짜 업로드 컨트롤러의 비운영 프로필 분리
