# Spring AI 기반 추천 기능 구현 계획

## 목표

라오타 서버에 다음 3가지 AI 기능을 추가한다.

1. 라멘 취향 테스트
2. 두 매장 심층 비교기
3. 다중 리뷰 3줄 요약

전제 조건은 다음과 같다.

- 서버: Spring Boot 4.x
- AI: Spring AI 2.0 milestone/beta 계열
- 벡터 저장소: Oracle Database 23ai
- 리뷰 코퍼스: 네이버 리뷰 + 라오타 커뮤니티 글/댓글 + 인증샷 설명

## 아키텍처 개요

AI 기능은 기존 `ramenShop`, `community` 도메인과 분리된 `domain.ai` 모듈로 추가한다.

- `shop_profile` 인덱스
  - 취향 추천용
  - 가게 설명, 태그, 대표 메뉴, 분위기, 운영 특성
- `shop_fact` 인덱스
  - 매장 비교용
  - 주소, 영업시간, 대표 메뉴, 가격대, 방문/북마크 수, 주차 등 구조화 정보
- `shop_review_chunk` 인덱스
  - 리뷰 요약용
  - 네이버 리뷰, 커뮤니티 글/댓글, 인증샷 설명을 청크화한 비정형 문서

## 기능별 설계

### 1. 라멘 취향 테스트

- 입력: 국물, 분위기, 우선순위, 자유 텍스트
- 처리: 입력을 질의 문장으로 정규화 후 `shop_profile` 유사도 검색
- 출력: 추천 매장, 추천 이유, 주의 포인트, 대표 메뉴

### 2. 두 매장 심층 비교기

- 입력: `shopAId`, `shopBId`
- 처리: `shop_fact`에서 비교 항목을 가져오고, `shop_review_chunk`를 보조 근거로 사용
- 출력: 표 형태 비교 결과, 요약, 근거
- 제약: 근거 없는 내용 생성 금지, 정보가 없으면 비워서 반환

### 3. 다중 리뷰 3줄 요약

- 입력: `shopId`
- 처리: `shop_review_chunk`에서 해당 매장 리뷰를 필터링 검색
- 출력:
  - 장점 1줄
  - 주의점 1줄
  - 추천 메뉴 1줄
  - 표본 수 및 출처 비중

## 데이터 적재 계획

### 내부 데이터

- `tb_ramen_shop` -> `shop_profile`, `shop_fact`
- `tb_post` -> `shop_review_chunk`
- `tb_comment` -> `shop_review_chunk`
- `tb_ramen_proof_picture.description` -> `shop_review_chunk`

### 외부 데이터

- 네이버 리뷰 크롤링 결과를 정규화 후 `shop_review_chunk`로 적재
- 매장 매핑은 `naver_map_id` 우선 전략으로 처리
- 중복 제거는 본문 해시와 매장 기준으로 수행

## PR 단위 작업 계획

### PR-0. 기술 스파이크

- Spring AI 2.0 + Boot 4.x 기동 확인
- Oracle 23ai Vector Store 연결 확인
- 임베딩 모델과 차원 수 확정

### PR-1. AI 모듈 구조 추가

- `domain.ai.config`, `controller`, `service`, `indexing`, `retrieval`, `prompt`, `dto`
- `ChatClient`, `EmbeddingModel`, `VectorStore` 공통 설정

### PR-2. 내부 데이터 인덱싱

- 매장/커뮤니티/인증샷 데이터 추출기 작성
- 청킹, 메타데이터 표준화, 중복 제거
- 전체/매장 단위 재인덱싱 지원

### PR-3. 검색/RAG 서비스

- 취향 추천 서비스
- 매장 비교 서비스
- 리뷰 요약 서비스
- 메타데이터 필터 기반 검색 로직

### PR-4. API 공개

- `POST /ai/recommendations/taste`
- `POST /ai/comparisons/shops`
- `GET /ai/shops/{shopId}/review-summary`
- 관리용 재인덱싱 API

### PR-5. 프론트 연동

- `raota-front` 추천 페이지 더미 제거
- 실제 AI API 연동
- 로딩/에러/빈 결과 처리

### PR-6. 네이버 리뷰 수집기

- 초기 데이터 적재용 크롤링 파이프라인
- 매장 매핑, 중복 제거, 적재 자동화

### PR-7. 운영 안정화

- 정기 재인덱싱/정기 수집
- 실패 로깅
- 응답 시간, 토큰 비용, 검색 성능 관측

## 우선순위

MVP는 외부 리뷰 없이도 먼저 완성한다.

1. 내부 데이터 기반 추천/비교/요약
2. 프론트 연동
3. 네이버 리뷰 추가로 품질 강화

## 주요 리스크

- Spring AI milestone과 Boot 4 조합의 호환성
- Oracle 벡터 컬럼 차원 변경 시 재생성 필요
- 네이버 리뷰 수집 안정성 및 운영 정책
- 비교 기능의 hallucination 방지
