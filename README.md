# Raota Server

> 라멘 맛집을 찾고, 기록하고, 공유하는 커뮤니티 플랫폼 **라오타**의 백엔드 서버입니다.

![Java](https://img.shields.io/badge/Java-25-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-cache%20%7C%20token-DC382D?style=flat-square&logo=redis&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-migration-CC0200?style=flat-square&logo=flyway&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?style=flat-square&logo=docker&logoColor=white)

## 소개

Raota Server는 라멘 매장 정보, 방문 인증샷, 메뉴 투표, 북마크, 커뮤니티, 회원 활동, AI 추천 기능을 제공하는 Spring Boot 기반 REST API 서버입니다.

주요 기능은 다음과 같습니다.

- 라멘 매장 목록/상세 조회, 북마크, 정보 오류 제보
- 라멘 매장별 메뉴 투표와 투표 현황 조회
- 방문 인증샷 등록/조회/삭제 및 업로드 티켓 발급
- 커뮤니티 게시글, 좋아요, 스레드 댓글
- OAuth2 기반 소셜 로그인과 JWT Access/Refresh Token 인증
- 마이페이지 활동 내역 조회
- AI 기반 취향 추천, 매장 비교, 리뷰 요약, 후속 채팅
- `ADMIN` 권한으로 보호되는 라멘 매장 CRUD, 회원/제보 조회 및 검색/RAG 인덱싱 API

## 문서

- [아키텍처 규칙](docs/architecture.md)

## 기술 스택

### Back-end

![Java](https://img.shields.io/badge/Java-25-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0--M6-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![JPA](https://img.shields.io/badge/JPA-Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)

### Database & Infra

![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)
![Oracle Cloud](https://img.shields.io/badge/Oracle%20Cloud-F80000?style=for-the-badge&logo=oracle&logoColor=white)
![Cloudinary](https://img.shields.io/badge/Cloudinary-3448C5?style=for-the-badge&logo=cloudinary&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

### Test & Tools

![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![Testcontainers](https://img.shields.io/badge/Testcontainers-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![RestAssured](https://img.shields.io/badge/RestAssured-6.0.0-6DB33F?style=for-the-badge)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

## 프로젝트 아키텍처

<img width="747" height="486" alt="image" src="https://github.com/user-attachments/assets/c6e2fafe-e5bd-4993-80db-338835615b45" />

### 패키지 구조

```text
src/main/java/com/raota
├── presentation  # REST API, Admin Controller, 공통 응답/예외 처리
├── application   # 유스케이스 서비스와 트랜잭션 경계
├── domain        # 도메인 모델, 정책, 저장소 인터페이스
└── infrastructure# 인증, Redis, 파일 업로드, 캐시, 벡터 검색, 외부 연동
```

## 배포

`main` 브랜치에 push되면 GitHub Actions가 다음 순서로 운영 배포를 진행합니다.

1. JDK 25 환경에서 `./gradlew clean bootJar -x test` 실행
2. Docker 이미지 빌드
3. OCIR(Oracle Cloud Infrastructure Registry)에 이미지 push
4. `raota-team/raota-infra` 저장소의 배포 워크플로 dispatch
5. canary 또는 full 배포 진행 상태 확인

## 기술적 이슈와 해결 과정

- Refresh Token 저장소를 JPA/Redis 구현으로 분리해 테스트와 운영 환경의 선택지를 확보
- Redis 기반 캐시 무효화 pub/sub으로 라멘 매장 상세/목록 캐시 정합성 관리
- Flyway 마이그레이션과 `ddl-auto=validate` 조합으로 운영 DB 스키마 변경 안정성 확보
- 파일 용도별 업로드 라우팅으로 프로필, 배경, 매장, 커뮤니티, 인증샷 이미지 경로 분리
- Oracle Vector Store와 Spring AI를 활용한 매장 검색/RAG 인덱싱 기반 마련

## 프로젝트 팀원

|                                                              Backend                                                               |
|:----------------------------------------------------------------------------------------------------------------------------------:|
|                                                                박희태                                                                 |
| <img width="200" height="200" alt="image" src="https://github.com/user-attachments/assets/48ace07a-44a2-4477-a79e-af255dc204ff" /> |
