# 라멘로그 API 명세

기준일: 2026-06-18

## 1. 개요

기존 라멘 인증샷을 라멘로그로 승격한다.

- 기존 테이블 `tb_ramen_proof_picture`는 V16에서 `tb_ramen_log`로 변경된다.
- 기존 인증샷의 ID, 작성자, 가게, 메뉴명, 이미지, 설명, 작성일, 삭제 상태는 유지된다.
- 신규 라멘로그 API는 `/ramen-logs`를 사용한다.
- 기존 `/photos` API는 같은 `tb_ramen_log` 데이터를 사용하는 호환 API로 유지한다.

기존 인증샷 데이터의 신규 필드 기본값:

| 필드 | 기본값 |
| --- | --- |
| `ramenType` | `기타` |
| `tasteNotes` | 모든 항목 빈 배열 |
| `revisit` | `SOMETIMES` |
| `public` | `true` |
| `likeCount` | `0` |

## 2. 공통 응답

```json
{
  "status": "SUCCESS",
  "message": null,
  "data": {}
}
```

실패 응답:

```json
{
  "status": "FAIL",
  "message": "오류 메시지"
}
```

페이지 응답:

```json
{
  "status": "SUCCESS",
  "data": {
    "items": [],
    "page": {
      "number": 0,
      "size": 8,
      "totalElements": 0,
      "totalPages": 0,
      "hasNext": false,
      "hasPrevious": false
    }
  }
}
```

## 3. 데이터 형식

### 라멘로그

```json
{
  "id": 101,
  "author": {
    "id": 12,
    "name": "멘마수집가",
    "imageUrl": "https://..."
  },
  "shop": {
    "id": 1,
    "name": "멘야 하루",
    "location": "서울 마포구"
  },
  "menuName": "특제 돈코츠",
  "ramenType": "돈코츠",
  "imageUrl": "https://...",
  "createdAt": "2026-06-18T12:30:00",
  "note": "진하지만 끝맛이 깔끔했다.",
  "tasteNotes": {
    "broth": ["진해요"],
    "noodle": ["단단해요"],
    "seasoning": ["딱 좋아요"],
    "topping": ["차슈 좋아요"]
  },
  "revisit": "DEFINITELY",
  "likeCount": 1,
  "liked": true,
  "public": true,
  "mine": true
}
```

### 재방문 의사

| 값 | 화면 문구 |
| --- | --- |
| `DEFINITELY` | 자주 감 |
| `SOMETIMES` | 가끔 생각남 |
| `ONCE_ENOUGH` | 한번이면 충분 |

## 4. API

### 4.1 공개 피드

```http
GET /ramen-logs?page=0&size=8&sort=LATEST&shopId=1&keyword=돈코츠
```

- 인증 선택
- 공개 로그만 반환
- `sort`: `LATEST` 또는 `POPULAR`
- `keyword`: 가게명, 메뉴명, 라멘 타입, 기록 내용 검색
- 로그인 시 `liked`, `mine`을 계산한다.

### 4.2 상세 조회

```http
GET /ramen-logs/{logId}
```

- 공개 로그는 누구나 조회 가능
- 비공개 로그는 작성자만 조회 가능

### 4.3 작성

```http
POST /ramen-logs
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "shopId": 1,
  "menuName": "특제 돈코츠",
  "ramenType": "돈코츠",
  "imageUrl": "proof/ramen-log.webp",
  "note": "진하지만 끝맛이 깔끔했다.",
  "tasteNotes": {
    "broth": ["진해요"],
    "noodle": ["단단해요"],
    "seasoning": ["딱 좋아요"],
    "topping": ["차슈 좋아요"]
  },
  "revisit": "DEFINITELY",
  "public": true
}
```

필수 필드:

- `shopId`
- `menuName`
- `ramenType`
- `imageUrl`
- `revisit`
- `public`

`note`는 최대 200자다. `tasteNotes` 또는 각 배열이 없으면 빈 배열로 처리한다.

### 4.4 수정

```http
PATCH /ramen-logs/{logId}
Authorization: Bearer {accessToken}
```

- 요청 형식은 작성과 동일
- 작성자만 수정 가능
- 가게가 변경되면 방문 통계도 함께 변경된다.

### 4.5 삭제

```http
DELETE /ramen-logs/{logId}
Authorization: Bearer {accessToken}
```

- 작성자만 삭제 가능
- `is_deleted=true`로 소프트 삭제
- 연결된 좋아요와 방문·사진 통계를 함께 정리

### 4.6 좋아요 토글

```http
POST /ramen-logs/{logId}/likes
Authorization: Bearer {accessToken}
```

```json
{
  "status": "SUCCESS",
  "data": {
    "liked": true,
    "likeCount": 2
  }
}
```

### 4.7 내 라멘로그

```http
GET /users/me/ramen-logs?page=0&size=8&shopId=1
Authorization: Bearer {accessToken}
```

- 공개·비공개 로그 모두 반환
- 최신순

### 4.8 사용자 라멘로그

```http
GET /users/{userId}/ramen-logs?page=0&size=8&shopId=1
```

- 다른 사용자가 조회하면 공개 로그만 반환
- 본인이 조회하면 비공개 로그도 반환

### 4.9 로그가 존재하는 가게 목록

```http
GET /users/me/ramen-logs/shops
GET /users/{userId}/ramen-logs/shops
```

```json
{
  "status": "SUCCESS",
  "data": [
    {
      "id": 1,
      "name": "멘야 하루",
      "logCount": 3
    }
  ]
}
```

## 5. 프로필 통계

`GET /users/me/profile`, `GET /users/{userId}/profile`:

```json
{
  "stats": {
    "visited_restaurant_count": 5,
    "total_photo_count": 12,
    "total_log_count": 12,
    "total_bookmark_count": 4,
    "post_count": 3,
    "comment_count": 8
  }
}
```

현재 인증샷과 라멘로그가 같은 데이터이므로 `total_photo_count`와 `total_log_count`는 동일하다.

## 6. 기존 인증샷 호환 API

다음 API는 제거하지 않고 동일한 라멘로그 데이터를 사용한다.

```http
GET    /ramen-shops/{shopId}/photos
POST   /ramen-shops/{shopId}/photos
DELETE /ramen-shops/{shopId}/photos/{photoId}
GET    /users/me/photos
GET    /users/{userId}/photos
```

기존 인증샷 등록은 다음 기본값의 공개 라멘로그를 생성한다.

- `ramenType`: `기타`
- `revisit`: `SOMETIMES`
- `public`: `true`
- 취향 태그: 빈 배열

## 7. 상태 코드

| 상황 | 상태 코드 |
| --- | --- |
| 성공 | `200` |
| 잘못된 요청·enum·검증 실패 | `400` |
| 로그인 필요 | `401` |
| 다른 사용자의 로그 수정·삭제 | `403` |
| 로그·가게·사용자 없음 | `404` |
