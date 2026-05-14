# AI Product Plan

## 범위

이 PR은 실제 사용자 기능과 API/프론트 연동 계획을 다룬다.

- 취향 추천
- 두 매장 비교
- 리뷰 3줄 요약
- 백엔드 API
- 프론트 추천 페이지 연동

## 기능 1. 취향 추천

### 입력

- `soup`
- `mood`
- `priority`
- `freeText`

### 처리

- 구조화 입력을 검색 문장으로 변환
- `shop_profile` 유사도 검색

### 출력

- 추천 매장 목록
- 추천 이유
- 주의 포인트
- 대표 메뉴

## 기능 2. 두 매장 비교

### 입력

- `shopAId`
- `shopBId`

### 처리

- `shop_fact` 기반 1차 비교
- `shop_review_chunk`는 보조 근거로 제한 사용

### 출력

- 표 형태 비교 행
- 요약
- 근거 source

### 제약

- 모르는 내용 추측 금지
- 정보 없으면 빈 값 또는 정보 없음 처리

## 기능 3. 리뷰 3줄 요약

### 입력

- `shopId`

### 처리

- `shop_review_chunk` 필터 검색
- source 비중과 표본 수 반영

### 출력

- 장점 1줄
- 주의점 1줄
- 추천 메뉴 1줄
- 표본 수
- 출처 비중

## 백엔드 API 초안

- `POST /ai/recommendations/taste`
- `POST /ai/comparisons/shops`
- `GET /ai/shops/{shopId}/review-summary`
- `POST /admin/ai/reindex`
- `POST /admin/ai/reindex/shops/{shopId}`

## 프롬프트 원칙

- 구조화 응답 우선
- 비교 기능은 표 중심
- 근거 없는 생성 금지
- 표본 수가 적으면 경고 문구 포함

## 프론트 연동

대상 화면:

- `raota-front/app/(routes)/recommend/page.tsx`

작업:

- 더미 데이터 제거
- AI 전용 API 클라이언트 추가
- react-query 연동
- 로딩/에러/빈 결과 처리

## 완료 조건

- 추천/비교/요약 API 계약이 확정됨
- 프론트가 붙을 수 있는 응답 형식이 명확함
- hallucination 방지 규칙이 문서화됨
