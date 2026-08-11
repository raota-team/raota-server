# 아키텍처 규칙

라오타 서버는 Spring Modulith의 `EXPLICITLY_ANNOTATED` 감지 전략을 사용하는 기능 중심 모듈러 모놀리스다.

## 모듈 구조

최상위 패키지는 기술 계층이 아니라 기능 소유권을 나타낸다.

```text
com.raota
├── account       # 인증, 회원, 관리자 회원 관리
├── agent         # AI 검색, 추천, RAG, 벡터 인덱싱
├── community     # 게시글, 댓글, 좋아요
├── discovery     # 홈/탐색 화면 조합 조회
├── ramenlog      # 라멘 로그, 인증 사진
├── ramenshop     # 라멘 가게, 메뉴, 북마크, 신고
└── global        # 기능에 종속되지 않는 공통 기술 코드
```

각 기능 모듈 내부에서는 필요한 계층만 사용한다.

```text
com.raota.<module>
├── presentation
├── application
├── domain
└── infrastructure
```

- `presentation`: HTTP 요청·응답과 외부 진입점을 처리한다.
- `application`: 유스케이스, 작업 흐름과 트랜잭션 경계를 담당한다.
- `domain`: 비즈니스 모델, 규칙과 저장소 interface를 표현한다.
- `infrastructure`: DB, 메시징, 외부 시스템과 프레임워크 adapter를 구현한다.

모든 계층을 억지로 만들지 않는다. 기능에 필요하지 않은 패키지는 생략한다.

## 모듈 등록

기능 모듈 루트의 `package-info.java`에 `@ApplicationModule`을 선언한다.

```java
@org.springframework.modulith.ApplicationModule
package com.raota.account;
```

`global`은 과도기 공통 기반이므로 OPEN 모듈로 등록한다.

```java
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package com.raota.global;
```

`EXPLICITLY_ANNOTATED` 전략에서는 `@ApplicationModule`이 없는 최상위 패키지를 모듈로 검사하지 않는다. 새 기능을 추가할 때는 구현보다 먼저 모듈 등록과 검증 테스트를 추가한다.

## 의존성 규칙

모듈 내부의 기본 의존 방향은 다음과 같다.

```text
presentation -> application -> domain
infrastructure -> application/domain interface
```

모듈 간에는 다음 규칙을 지킨다.

- 모든 기능 모듈은 `global`을 사용할 수 있다.
- `global`은 어떤 기능 모듈도 참조하지 않는다.
- 다른 기능 모듈은 상대 모듈이 `@NamedInterface`로 공개한 패키지만 참조한다.
- 모듈 간 순환 의존성을 만들지 않는다.
- 다른 모듈의 controller, JPA 구현체와 내부 설정을 직접 참조하지 않는다.
- 조회 조합이 여러 기능에 걸치면 데이터를 소유한 모듈의 공개 query/result interface를 사용한다.
- 하나의 HTTP 경로 아래 있더라도 게시글·댓글처럼 소유권이 명확한 기능은 해당 기능 모듈이 처리한다.

공개 패키지는 `package-info.java`에 이름을 명시한다.

```java
@org.springframework.modulith.NamedInterface("member-application")
package com.raota.account.application.member;
```

`@NamedInterface`는 다른 모듈이 사용해야 하는 안정된 interface에만 둔다. JPA entity나 repository 전체 공개는 과도기 호환에 한정하고 query, result 또는 facade로 점진적으로 축소한다.

## 패키지 규칙

### Presentation

```text
com.raota.<module>.presentation
com.raota.<module>.presentation.contract
com.raota.<module>.presentation.request
com.raota.<module>.presentation.response
```

- `contract`: API 문서화 interface가 필요할 때 사용한다.
- `request`: HTTP 요청 DTO를 둔다.
- `response`: HTTP 응답 DTO를 둔다.
- 간단한 controller는 별도 contract 없이 Swagger annotation을 직접 둘 수 있다.

### Application

```text
com.raota.<module>.application.command
com.raota.<module>.application.query
com.raota.<module>.application.result
com.raota.<module>.application.service
com.raota.<module>.application.port
```

- `command`: 쓰기 유스케이스 입력.
- `query`: 읽기 조건.
- `result`: 유스케이스 출력.
- `service`: 유스케이스 구현과 트랜잭션 경계.
- `port`: 외부 기능 또는 다른 구현이 필요한 seam의 interface.

### Domain

```text
com.raota.<module>.domain.model
com.raota.<module>.domain.repository
com.raota.<module>.domain.service
com.raota.<module>.domain.event
```

- `model`: 도메인 상태와 행위.
- `repository`: 도메인 저장소 interface.
- `service`: 특정 모델에 넣기 어려운 도메인 규칙.
- `event`: 다른 모듈에 전달할 도메인 사건.

### Infrastructure

```text
com.raota.<module>.infrastructure.persistence
com.raota.<module>.infrastructure.messaging
com.raota.<module>.infrastructure.external
com.raota.<module>.infrastructure.scheduler
```

기능에 종속된 persistence, listener, scheduler와 외부 연동은 해당 기능 모듈에 둔다. 여러 기능이 사용하는 범용 캐시, 파일, 로그, Redis와 프레임워크 설정만 `global`에 둔다.

## DTO 규칙

Presentation DTO는 API 표현을 담당한다.

- request/response DTO는 소유 모듈의 `presentation` 아래에 둔다.
- validation, Swagger, JSON naming과 controller binding 관련 코드를 둘 수 있다.
- `application` 또는 `domain`에서 presentation DTO를 import하지 않는다.
- application command/query로 변환하는 메서드는 둘 수 있다.
- 검색 조건이 단순한 query parameter 1~2개라면 별도 request DTO를 만들지 않는다.

Application DTO는 유스케이스 입출력을 담당한다.

- 쓰기 입력은 `application.command`, 읽기 입력은 `application.query`를 사용한다.
- 유스케이스 출력은 `application.result`를 사용한다.
- HTTP, JSON, Swagger와 presentation response type에 의존하지 않는다.
- API 표현과 application result가 동일하면 controller가 result를 그대로 직렬화할 수 있다.
- 필드 복사만 하는 response DTO는 만들지 않는다.

Domain에는 API DTO, Swagger annotation과 controller 관심사를 두지 않는다.

## Port와 Adapter 규칙

Application 코드가 저장소나 외부 기능을 필요로 하면 소유 모듈의 port에 의존한다.

```text
ramenshop.application.port.FileUrlPort
agent.application.ramenshop.port.RamenShopSearchDocumentPort
```

구현체는 소유 모듈의 infrastructure에 둔다. 여러 모듈이 함께 사용하는 범용 구현일 때만 `global` adapter가 port를 구현한다.

interface 하나와 구현체 하나뿐이고 교체 가능성이 실제로 없다면 불필요한 port를 만들지 않는다.

## 검증

`ModulithArchitectureTest`는 다음을 배포 전 강제한다.

- 예상한 7개 모듈이 모두 감지되는지 확인한다.
- `ApplicationModules.verify()`로 순환 의존성과 비공개 패키지 접근을 검사한다.
- API 접근 정책 테스트로 등록된 모든 endpoint가 PUBLIC, AUTHENTICATED, ADMIN 중 하나에만 속하는지 검사한다.

패키지 이동이나 `@NamedInterface` 변경 뒤에는 반드시 전체 테스트를 실행한다.
