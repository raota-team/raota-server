# LangGraph4j 호환성 검증

라오타 제품 모듈에 의존성을 추가하기 전에 Spring AI와 LangGraph4j의 조합을 격리해서 확인하는 프로젝트입니다.

## 검증 조합

- Java 25
- Spring Boot 4.1.0
- Spring AI 2.0.0
- LangGraph4j 1.9.0-beta7

## 실행

서버 저장소 루트에서 다음 명령을 실행합니다.

```bash
./gradlew -p compatibility-tests/langgraph4j test
```

현재 검증은 `load → normalize → validate → valid/invalid` 조건부 그래프, Spring 컨텍스트 빈 주입, 상태 JSON 값 왕복, 예외 입력 경로, `MemorySaver` 체크포인트 조회를 포함합니다.

## 판정

2026-09-15 기준 위 조합의 컴파일과 5개 테스트가 통과했습니다. LangGraph4j의 Spring AI 모듈 POM이 Spring AI 2.0.1을 기본으로 요청하지만 이 프로젝트의 BOM은 2.0.0을 선택하도록 고정했고 테스트에서 동작을 확인했습니다. 런타임 의존성에는 Spring AI 2.x와 Jackson 3.x가 사용되며, Jackson 3의 호환 의존성으로 `com.fasterxml.jackson.core:jackson-annotations:2.21`이 함께 보입니다. 제품 모듈에 도입하기 전에는 이 트리를 CI에서 다시 확인합니다.

제품 런타임에는 아직 LangGraph4j 의존성을 추가하지 않습니다. 호환성 테스트가 깨지거나 Spring AI 버전을 올려야 하는 경우 별도 업그레이드 이슈를 먼저 처리합니다.
