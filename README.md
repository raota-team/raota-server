# 🍜 라오타 (Raota) - 라멘 맛집 커뮤니티 플랫폼

> **"당신의 인생 라멘을 찾고, 기록하고, 공유하세요."**
> 라오타는 라멘 매니아들을 위한 맛집 정보 공유 및 커뮤니티 플랫폼입니다.

---

## ✨ 페이지별 주요 기능

### 🏠 홈 및 탐색 (Home & Search)
- **스마트 검색**: 지역별 필터링과 키워드 검색을 통해 원하는 라멘집을 빠르게 찾을 수 있습니다.
- **트렌드 탐색**: 현재 인기 있는 라멘집과 기간 한정 이벤트 메뉴를 홈 화면에서 바로 확인하세요.

### 🍜 라멘 가게 상세 (Shop Detail)
- **종합 정보**: 가게의 기본 정보(영업시간, 주소)와 전체 메뉴판을 한눈에 조회합니다.
- **메뉴 투표**: "이 집에서 꼭 먹어야 할 메뉴"에 직접 투표하여 맛집 가이드를 함께 만듭니다. (1인 1표 원칙)
- **인증샷 갤러리**: 사용자들이 직접 올린 생생한 방문 인증샷을 확인하고, 나만의 방문 기록을 남길 수 있습니다.
- **북마크 & 제보**: 마음에 드는 가게를 '찜'해두고, 정보가 틀렸을 경우(폐업, 시간 변경 등) 즉시 제보하여 데이터 정합성을 유지합니다.

### 💬 커뮤니티 (Community)
- **소통의 장**: 라멘에 대한 정보와 일상을 공유하는 자유 게시판을 운영합니다.
- **스레드 댓글**: 깊이 있는 대화를 위해 계층형 댓글(Reply) 기능을 지원합니다.
- **활동 연동**: 글과 댓글 작성 시 사용자의 활동 통계가 실시간으로 반영됩니다.

### 👤 마이페이지 (My Page)
- **활동 로그**: 내가 쓴 글, 작성한 댓글, 찜한 가게 목록을 한곳에서 관리합니다.
- **개인 통계**: 나의 라멘 탐방 기록(인증샷 수, 커뮤니티 활동 등)을 시각화하여 제공합니다.

---

## 🏗 아키텍처 및 기술적 특징

라오타는 확장성과 유지보수성을 최우선으로 고려하여 설계되었습니다.

### 🧩 도메인 주도 설계 (DDD)
- **Bounded Context**: `Member`, `RamenShop`, `Community`, `Auth` 등 도메인별로 명확한 경계를 설정하여 결합도를 낮췄습니다.
- **Rich Domain Model**: 비즈니스 로직을 엔티티와 도메인 객체에 응집시켜 서비스 레이어의 비대화를 방지했습니다.

### ⚡ CQRS (Command-Query Responsibility Segregation)
- **물리적 분리**: 상태를 변경하는 `Command`는 **JPA**를, 복잡한 조회와 성능이 중요한 `Query`는 **JOOQ**를 사용하여 효율을 극대화했습니다.
- **성능 최적화**: 다중 조인 및 페이징 처리를 JOOQ를 통해 타입 안정성을 확보하며 최적화된 SQL로 실행합니다.

### 🔐 보안 및 인프라 (Security & Infrastructure)
- **OAuth2 & JWT**: Google, Kakao 소셜 로그인을 지원하며, Access/Refresh Token 로테이션 정책으로 보안을 강화했습니다.
- **Redis Cache**: Refresh Token 저장 및 캐싱을 위해 Redis를 활용하여 빠른 인증 처리를 지원합니다.
- **Object Storage**: S3 호환 API(OCI/AWS)를 사용하여 고성능 이미지 업로드 및 Presigned URL 기반의 안전한 파일 관리를 수행합니다.

---

## 🛠 기술 스택
- **Language**: Java 25
- **Framework**: Spring Boot 4.0.2, Spring Security
- **Database**: MySQL, Redis, Flyway
- **Persistence**: JPA (Hibernate), JOOQ
- **Test**: JUnit 5, AssertJ, RestAssured, Testcontainers
- **Cloud**: Oracle Cloud Infrastructure (OCI), GitHub Actions

---

## 🛠 주요 실행 명령어
- **빌드**: `./gradlew build`
- **테스트**: `./gradlew test`
- **실행**: `./gradlew bootRun`

*문서 최종 수정일: 2026-04-14*
