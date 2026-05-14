# AI Ingestion Plan

## 범위

이 PR은 벡터 검색에 넣을 문서를 어떻게 만들고 적재할지에 집중한다.

- 내부 데이터 추출
- 네이버 리뷰 초기 수집 전략
- 청킹
- 메타데이터 표준화
- 중복 제거
- 재인덱싱 운영 전략

## 인덱스 구분

### `shop_profile`

- 취향 추천용
- 가게 설명, 태그, 대표 메뉴, 분위기, 운영 특성

### `shop_fact`

- 비교용
- 주소, 영업시간, 메뉴, 통계, 주차, 방문 관련 객관 정보

### `shop_review_chunk`

- 리뷰 요약용
- 네이버 리뷰, 커뮤니티 글/댓글, 인증샷 설명을 청크화한 문서

## 데이터 소스

### 내부

- `tb_ramen_shop`
- `tb_post`
- `tb_comment`
- `tb_ramen_proof_picture.description`

### 외부

- 네이버 리뷰 크롤링 결과

## 문서 메타데이터 표준

- `shopId`
- `source`
- `sourceId`
- `documentType`
- `createdAt`
- `region`
- `tags`
- `menuNames`
- `rating`

## 작업 항목

### 1. 내부 데이터 추출기

- 매장 -> `shop_profile`, `shop_fact`
- 게시글 -> `shop_review_chunk`
- 댓글 -> `shop_review_chunk`
- 인증샷 설명 -> `shop_review_chunk`

### 2. 외부 리뷰 수집기

- `naver_map_id` 기반 매장 매핑
- 리뷰 정규화 DTO
- 수집 실패/재시도 전략

### 3. 청킹/정제

- 길이 기준 청킹
- 공백/불필요 텍스트 제거
- 너무 짧은 리뷰 제외

### 4. 중복 제거

- 동일 매장 + 본문 해시
- 유사 텍스트 제거 기준 검토

### 5. 인덱싱 운영

- 전체 재인덱싱
- 매장 단위 재인덱싱
- source 단위 재인덱싱

## 완료 조건

- 내부 데이터만으로도 세 인덱스 적재 가능
- 네이버 리뷰를 source 구분된 형태로 적재 가능
- 재실행 시 중복 정책이 일관됨

## 후속 PR

- 검색/RAG 서비스
- API 응답 설계
- 프론트 연동
