# 아키텍처 규칙

이 문서는 라오타 서버의 패키지 구조와 계층 의존성 규칙을 정리한다.

## 계층 구조

라오타는 네 가지 주요 계층을 사용한다.

```text
com.raota
├── presentation
├── application
├── domain
└── infrastructure
```

- `presentation`: 외부 요청과 응답을 처리하고, 사용자 또는 클라이언트와 맞닿는 진입점을 담당한다.
- `application`: 사용자의 목적을 유스케이스로 조합하고, 작업 흐름과 트랜잭션 경계를 담당한다.
- `domain`: 비즈니스 개념과 핵심 규칙을 표현하고, 도메인의 상태와 행위를 담당한다.
- `infrastructure`: 데이터 저장소, 외부 시스템, 프레임워크 등 기술 세부사항과의 연결을 담당한다.

## 의존성 규칙

허용하는 의존성 방향:

```text
presentation -> application
application -> domain
infrastructure -> application/domain port
```

## 패키지 규칙

각 계층은 도메인별로 나눌 수 있다.

```text
presentation.api.<domain>
application.<domain>
domain.<domain>
infrastructure.persistence.<domain>
```

### `presentation` 계층의 패키지 목록

```text
presentation.api.<domain>
presentation.admin.<domain>
presentation.api.<domain>.contract
presentation.api.<domain>.request
presentation.api.<domain>.response
presentation.common
```

- `api`: 클라이언트가 사용하는 HTTP API 진입점.
- `contract`: API 문서화 인터페이스.
- `request`: HTTP 요청 DTO.
- `response`: HTTP 응답 DTO.
- `admin`: 관리자 화면과 관리자 API 진입점.
- `common`: presentation 계층에서 공통으로 사용하는 응답, 예외, 웹 관련 객체.

### `application` 계층의 패키지 목록

```text
application.<domain>.command
application.<domain>.query
application.<domain>.result
application.<domain>.service
application.<domain>.port
```

- `command`: 쓰기 유스케이스 입력.
- `query`: 읽기 유스케이스 입력 또는 조회용 application 객체.
- `result`: application 유스케이스 출력.
- `service`: 유스케이스 구현체.
- `port`: application 코드가 외부 기능을 사용하기 위해 필요한 interface.

### `domain` 계층의 패키지 목록

```text
domain.<domain>.model
domain.<domain>.repository
domain.<domain>.service
```

- `model`: 도메인의 상태와 행위.
- `repository`: 도메인 저장소 interface.
- `service`: 특정 모델에 넣기 어려운 도메인 규칙.

### `infrastructure` 계층의 패키지 목록

```text
infrastructure.persistence.<domain>
infrastructure.persistence.<domain>.entity
infrastructure.persistence.<domain>.repository
infrastructure.persistence.<domain>.query
infrastructure.cache
infrastructure.file
infrastructure.messaging
infrastructure.config
```

- `persistence`: DB 접근 구현.
- `entity`: JPA entity.
- `repository`: repository adapter 또는 Spring Data repository.
- `query`: 조회 전용 repository.
- `cache`: 캐시 설정과 캐시 무효화 구현.
- `file`: 파일 저장소와 파일 접근 URL 처리.
- `messaging`: 메시지 발행과 구독 구현.
- `config`: 프레임워크와 외부 기술 설정.

## DTO 규칙

Presentation DTO는 API 표현을 담당한다.

- request DTO는 `presentation.api.<domain>.request` 또는 `presentation.admin.<domain>.request`에 둔다.
- response DTO는 `presentation.api.<domain>.response` 또는 `presentation.admin.<domain>.response`에 둔다.
- validation annotation, Swagger annotation, JSON naming, controller binding 관련 코드를 둘 수 있다.
- `application` 또는 `domain`에서 import하지 않는다.
- application command/query로 변환하는 메서드는 둘 수 있다.

Application DTO는 유스케이스 입출력을 담당한다.

- 쓰기 입력은 `application.<domain>.command`를 사용한다.
- 읽기 입력은 `application.<domain>.query`를 사용한다.
- 유스케이스 출력은 presentation DTO가 API 관심사를 누수할 때 `application.<domain>.result`를 사용한다.
- HTTP, JSON, Swagger, controller annotation, presentation response type에 의존하지 않는다.

Domain에는 API DTO를 두지 않는다.

- Domain에는 model, value object, repository interface, domain service를 둔다.
- JPA projection과 API response DTO는 `domain`에 두지 않는다.

## Port와 Adapter 규칙

Application 코드가 외부 기능을 필요로 할 때는 port에 의존한다.

예시:

```text
application.common.port.CacheInvalidationPort
application.common.port.FileStoragePort
application.ramenshop.port.RamenShopCacheInvalidationPort
```

구현체는 `infrastructure`에 둔다.

```text
infrastructure.cache.RedisCacheInvalidationAdapter
infrastructure.file.FileStorageAdapter
```
