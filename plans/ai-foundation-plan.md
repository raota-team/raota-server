# AI Foundation Plan

## 범위

이 PR은 Spring AI 도입을 위한 기반 작업만 다룬다.

- Spring Boot 4.x + Spring AI 2.0 milestone/beta 조합 점검
- Oracle Database 23ai Vector Store 연결 전략 확정
- AI 전용 패키지 구조 정의
- 공통 설정 및 운영 리스크 정리

## 목표

구현 전에 다음 사항을 확정한다.

1. 사용할 Spring AI BOM 및 starter 조합
2. Oracle 23ai 벡터 저장소 설정값
3. 임베딩 모델과 차원 수
4. AI 기능을 넣을 패키지 경계

## 작업 항목

### 1. 기술 스파이크

- `spring-ai-starter-model-openai`
- `spring-ai-starter-vector-store-oracle`
- Boot 4 환경에서 최소 기동 확인
- 샘플 document add/query 테스트

### 2. Oracle Vector Store 기준값

- `index-type`
- `distance-type`
- `dimensions`
- `initialize-schema`
- 필요 시 강제 정규화 옵션 검토

## 추천 기본값

- `distance-type`: `COSINE`
- `index-type`: 초기 `NONE`, 이후 데이터 증가 시 `HNSW` 또는 `IVF`
- `dimensions`: 선택 임베딩 모델 차원에 고정

### 3. 패키지 구조

- `com.raota.domain.ai.config`
- `com.raota.domain.ai.controller`
- `com.raota.domain.ai.service`
- `com.raota.domain.ai.dto`
- `com.raota.domain.ai.indexing`
- `com.raota.domain.ai.retrieval`
- `com.raota.domain.ai.prompt`

### 4. 설정 항목

- OpenAI API key
- Oracle 23ai datasource/vector 설정
- AI 기능 on/off flag
- 인덱싱 admin endpoint 노출 여부

## 완료 조건

- 앱 기동 성공
- Oracle Vector Store에 샘플 문서 적재 및 검색 성공
- AI 전용 모듈 구조와 설정 방침이 확정됨

## 후속 PR

- 내부 데이터 인덱싱 계획
- 검색/RAG/API/프론트 연동 계획
