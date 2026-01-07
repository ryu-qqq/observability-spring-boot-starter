# Integration Tests

observability-spring-boot-starter 통합 테스트 모듈입니다.

각 모듈은 독립적인 테스트 환경으로 격리되어 있으며, 특정 어댑터/기능에 대한 통합 테스트를 수행합니다.

## 모듈 구조

```
integration-test/
├── adapter-in/          # 인바운드 어댑터 (요청 수신)
│   ├── rest-api/        # Servlet 기반 REST API
│   ├── gateway/         # WebFlux/Netty 기반 Gateway
│   ├── sqs-in/          # SQS 메시지 리스너
│   └── redis-in/        # Redis 메시지 리스너
├── adapter-out/         # 아웃바운드 어댑터 (요청 발신)
│   ├── http-client/     # HTTP 클라이언트 (RestTemplate, WebClient)
│   ├── sqs-out/         # SQS 메시지 발행
│   └── redis-out/       # Redis 메시지 발행
└── bootstrap/           # 전체 AutoConfiguration 통합 테스트
```

---

## adapter-in (인바운드 어댑터)

### rest-api

Servlet 기반 REST API 환경에서의 HTTP 요청/응답 로깅 및 TraceId 처리를 테스트합니다.

**테스트 클래스:**
- `TraceIdFilterIntegrationTest` - TraceId 필터 동작 검증
- `HttpLoggingFilterIntegrationTest` - HTTP 로깅 필터 동작 검증
- `PathNormalizerTest` - 경로 정규화 기능 검증

**커버 시나리오:**

| 카테고리 | 시나리오 | 설명 |
|---------|---------|------|
| 기본 로깅 | GET/POST 요청 로깅 | HTTP 메서드별 요청/응답 로깅 |
| | 경로 파라미터 처리 | `/users/{id}`, `/orders/{uuid}` 형태 처리 |
| 경로 제외 | 제외 경로 필터링 | `/actuator/**`, `/health` 등 로깅 제외 |
| 에러 응답 | 4xx/5xx 에러 로깅 | 400, 500 에러 응답 로깅 검증 |
| 느린 요청 | 지연 요청 처리 | 응답 시간이 긴 요청 정상 처리 |
| 본문 로깅 | 요청 본문 로깅 | JSON 요청 본문 캡처 및 로깅 |
| | 대용량 본문 처리 | 큰 요청 본문 정상 처리 |
| 민감정보 | 패스워드 마스킹 | `password` 필드 마스킹 처리 |
| | Authorization 헤더 필터링 | 인증 헤더 로깅 제외 |

---

### gateway

WebFlux/Netty 기반 리액티브 환경에서의 HTTP 요청/응답 로깅 및 TraceId 처리를 테스트합니다.

**테스트 클래스:**
- `ReactiveTraceIdFilterIntegrationTest` - 리액티브 TraceId 필터 검증
- `ReactiveHttpLoggingFilterIntegrationTest` - 리액티브 HTTP 로깅 필터 검증
- `ReactivePathNormalizerTest` - 리액티브 경로 정규화 검증
- `MdcContextLifterTest` - MDC 컨텍스트 전파 검증

**커버 시나리오:**

| 카테고리 | 시나리오 | 설명 |
|---------|---------|------|
| 기본 로깅 | GET 요청 로깅 | 리액티브 환경 GET 요청 처리 |
| | 쿼리 파라미터 로깅 | URL 쿼리 스트링 포함 로깅 |
| | 헬스 체크 처리 | `/actuator/health` 정상 동작 |
| POST 본문 | JSON 본문 로깅 | 리액티브 요청 본문 캡처 |
| | 민감정보 마스킹 | 로그인 요청 패스워드 마스킹 |
| | 빈 본문 처리 | 빈 요청 본문 정상 처리 |
| 경로 정규화 | 숫자 ID 정규화 | `/users/123` → `/users/{id}` |
| | UUID 정규화 | `/orders/{uuid}` 패턴 처리 |
| MDC 전파 | Scannable 구현 | Reactor Context-MDC 연동 |
| | onNext/onError 전파 | 신호별 MDC 복원 |
| | Context 반환 | delegate 컨텍스트 정상 반환 |

---

### sqs-in

AWS SQS 메시지 리스너에서 TraceId 추출 및 MDC 설정을 테스트합니다.

**테스트 클래스:**
- `SqsListenerTraceIdExtractionTest` - SQS 메시지 TraceId 추출 검증

**커버 시나리오:**

| 카테고리 | 시나리오 | 설명 |
|---------|---------|------|
| TraceId 추출 | 헤더에서 TraceId 추출 | `X-Trace-Id` 헤더 값 추출 |
| | TraceId 자동 생성 | 헤더 없을 시 신규 TraceId 생성 |
| 사용자 컨텍스트 | UserId 추출 | `X-User-Id` 헤더 추출 |
| | TenantId 추출 | `X-Tenant-Id` 헤더 추출 |
| | OrganizationId 추출 | `X-Organization-Id` 헤더 추출 |
| | 전체 컨텍스트 추출 | 모든 사용자 헤더 동시 추출 |
| MDC 정리 | 처리 완료 후 정리 | 정상 처리 후 MDC 클리어 |
| | 예외 발생 후 정리 | 에러 발생 시에도 MDC 클리어 |
| MessageContext | SQS 소스 컨텍스트 | `source=SQS` 컨텍스트 생성 |
| | 시작 시간 자동 설정 | `startTimeMillis` 자동 기록 |
| | 처리 시간 계산 | `calculateDuration()` 동작 |

---

### redis-in

Redis Pub/Sub 및 Stream 메시지 리스너에서 TraceId 추출 및 MDC 설정을 테스트합니다.

**테스트 클래스:**
- `RedisListenerTraceIdExtractionTest` - Redis 메시지 TraceId 추출 검증

**커버 시나리오:**

| 카테고리 | 시나리오 | 설명 |
|---------|---------|------|
| Pub/Sub | TraceId 추출 | Pub/Sub 메시지에서 TraceId 추출 |
| | TraceId 자동 생성 | TraceId 없을 시 신규 생성 |
| Stream | TraceId 추출 | Stream 레코드에서 TraceId 추출 |
| | MessageId 포함 | Stream MessageId 컨텍스트 포함 |
| 사용자 컨텍스트 | UserId 추출 | Redis 메시지에서 UserId 추출 |
| | TenantId 추출 | Redis 메시지에서 TenantId 추출 |
| | OrganizationId 추출 | Redis 메시지에서 OrganizationId 추출 |
| | 전체 컨텍스트 추출 | 모든 사용자 헤더 동시 추출 |
| MDC 정리 | Pub/Sub 처리 후 정리 | Pub/Sub 완료 후 MDC 클리어 |
| | Stream 처리 후 정리 | Stream 완료 후 MDC 클리어 |
| | 예외 발생 후 정리 | 에러 발생 시에도 MDC 클리어 |
| 소스별 컨텍스트 | REDIS_PUBSUB 소스 | Pub/Sub 소스 컨텍스트 생성 |
| | REDIS_STREAM 소스 | Stream 소스 컨텍스트 생성 |

---

## adapter-out (아웃바운드 어댑터)

### http-client

RestTemplate, WebClient를 통한 HTTP 요청 시 TraceId 헤더 전파를 테스트합니다.

**테스트 클래스:**
- `RestTemplateTraceIdPropagationTest` - RestTemplate TraceId 전파 검증
- `WebClientTraceIdPropagationTest` - WebClient TraceId 전파 검증

**커버 시나리오:**

| 카테고리 | 시나리오 | 설명 |
|---------|---------|------|
| RestTemplate | TraceId 헤더 전파 | `X-Trace-Id` 헤더 자동 추가 |
| | TraceId 없을 시 미전파 | MDC 비어있으면 헤더 미추가 |
| | GET/POST/PUT/DELETE | HTTP 메서드별 전파 검증 |
| | 사용자 컨텍스트 전파 | `X-User-Id`, `X-Tenant-Id`, `X-Organization-Id` 전파 |
| WebClient | TraceId 헤더 전파 | 리액티브 환경 TraceId 전파 |
| | TraceId 없을 시 미전파 | MDC 비어있으면 헤더 미추가 |
| | GET/POST/PUT/DELETE | HTTP 메서드별 전파 검증 |
| | 사용자 컨텍스트 전파 | 모든 사용자 헤더 전파 |
| Reactive 체이닝 | 첫 번째 요청 TraceId 캡처 | 현재 스레드 기준 TraceId 캡처 |
| | 다른 스레드 실행 시 제한 | ThreadLocal 제한 사항 검증 |

---

### sqs-out

SQS 메시지 발행 시 현재 컨텍스트의 TraceId를 메시지 속성에 주입하는 기능을 테스트합니다.

**테스트 클래스:**
- `SqsMessageTraceIdPropagationTest` - SQS 메시지 발행 TraceId 전파 검증

**커버 시나리오:**

| 카테고리 | 시나리오 | 설명 |
|---------|---------|------|
| TraceId 주입 | TraceId 메시지 속성 추가 | 현재 컨텍스트 TraceId → MessageAttributes |
| | TraceId 없을 시 미포함 | 컨텍스트 비어있으면 속성 미추가 |
| 사용자 컨텍스트 | UserId 속성 추가 | `X-User-Id` 메시지 속성 주입 |
| | TenantId 속성 추가 | `X-Tenant-Id` 메시지 속성 주입 |
| | OrganizationId 속성 추가 | `X-Organization-Id` 메시지 속성 주입 |
| 전체 컨텍스트 | 모든 속성 전파 | 전체 사용자 컨텍스트 동시 전파 |
| | 부분 컨텍스트 처리 | 일부만 설정 시 정상 처리 |
| 유틸리티 | 빈 컨텍스트 처리 | 빈 컨텍스트에서 빈 맵 반환 |
| | null 값 필터링 | null 값은 속성에 미포함 |

---

### redis-out

Redis Pub/Sub 및 Stream 메시지 발행 시 현재 컨텍스트의 TraceId를 메시지에 주입하는 기능을 테스트합니다.

**테스트 클래스:**
- `RedisMessageTraceIdPropagationTest` - Redis 메시지 발행 TraceId 전파 검증

**커버 시나리오:**

| 카테고리 | 시나리오 | 설명 |
|---------|---------|------|
| Pub/Sub 전파 | TraceId 메시지 포함 | Pub/Sub 메시지에 TraceId 주입 |
| | 사용자 컨텍스트 포함 | 전체 사용자 컨텍스트 메시지 주입 |
| Stream 전파 | TraceId 필드 추가 | Stream 레코드 필드에 TraceId 추가 |
| | 사용자 컨텍스트 필드 추가 | 사용자 헤더 필드로 추가 |
| 전체 컨텍스트 | 모든 컨텍스트 전파 | 전체 사용자 컨텍스트 동시 전파 |
| | 부분 컨텍스트 처리 | 일부만 설정 시 정상 처리 |
| | 빈 컨텍스트 처리 | 빈 컨텍스트에서 빈 맵 반환 |
| 채널별 발행 | 다중 채널 동일 컨텍스트 | 다른 채널에도 동일 컨텍스트 전파 |

---

## bootstrap

전체 AutoConfiguration 통합 및 End-to-End 흐름을 테스트합니다.

**테스트 클래스:**
- `AutoConfigurationTest` - 모든 Bean 자동 등록 검증
- `ClientPropagationIntegrationTest` - 클라이언트 TraceId 전파 검증
- `FullIntegrationFlowTest` - E2E 전체 흐름 검증

**커버 시나리오:**

| 카테고리 | 시나리오 | 설명 |
|---------|---------|------|
| Core AutoConfig | LogMasker Bean 등록 | `LogMasker` 빈 자동 등록 |
| | 마스킹 패턴 동작 | 설정된 패턴 마스킹 동작 |
| Web AutoConfig | TraceIdProvider 등록 | `TraceIdProvider` 빈 등록 |
| | PathNormalizer 등록 | `PathNormalizer` 빈 등록 |
| | TraceIdFilter 등록 | `FilterRegistrationBean` 등록 |
| | HttpLoggingFilter 등록 | `FilterRegistrationBean` 등록 |
| Client AutoConfig | RestTemplate Interceptor | `TraceIdRestTemplateInterceptor` 등록 |
| | RestClient Interceptor | `TraceIdRestClientInterceptor` 등록 |
| | WebClient Filter | `TraceIdExchangeFilterFunction` 등록 |
| Logging AutoConfig | LoggableAspect 등록 | `@Loggable` AOP 빈 등록 |
| | BusinessLogAspect 등록 | `@BusinessLog` AOP 빈 등록 |
| | BusinessEventListener 등록 | 이벤트 리스너 빈 등록 |
| 설정 검증 | 서비스명 로드 | `application.yml` 서비스명 로드 |
| | 기본 패턴 포함 | PathNormalizer 기본 패턴 확인 |
| RestTemplate 전파 | TraceId 헤더 전파 | 실제 HTTP 요청 TraceId 전파 |
| | 사용자 컨텍스트 전파 | 모든 사용자 헤더 전파 |
| | 빈 컨텍스트 미전파 | TraceIdHolder 비어있으면 미전파 |

---

## 테스트 실행

### 전체 테스트 실행

```bash
./gradlew test
```

### 모듈별 테스트 실행

```bash
# adapter-in 테스트
./gradlew :integration-test:adapter-in:rest-api:test
./gradlew :integration-test:adapter-in:gateway:test
./gradlew :integration-test:adapter-in:sqs-in:test
./gradlew :integration-test:adapter-in:redis-in:test

# adapter-out 테스트
./gradlew :integration-test:adapter-out:http-client:test
./gradlew :integration-test:adapter-out:sqs-out:test
./gradlew :integration-test:adapter-out:redis-out:test

# bootstrap 테스트
./gradlew :integration-test:bootstrap:test
```

### 테스트 리포트

각 모듈의 테스트 리포트는 다음 경로에서 확인할 수 있습니다:

```
integration-test/{module}/build/reports/tests/test/index.html
```

---

## 의존성

각 모듈은 테스트 대상 기능에 필요한 최소한의 의존성만 포함합니다:

| 모듈 | 주요 의존성 |
|------|-----------|
| rest-api | `spring-boot-starter-web`, `spring-boot-starter-test` |
| gateway | `spring-boot-starter-webflux`, `reactor-test` |
| sqs-in | `observability-message`, `spring-cloud-aws-starter-sqs` |
| redis-in | `observability-message`, `spring-boot-starter-data-redis` |
| http-client | `observability-client`, `okhttp3-mockwebserver` |
| sqs-out | `observability-core` |
| redis-out | `observability-core` |
| bootstrap | 모든 observability 모듈 |

---

## 개선 예정 (v1.3.0)

현재 `sqs-in`, `sqs-out`, `redis-in`, `redis-out` 모듈의 테스트는 실제 인프라(SQS, Redis)와 연동하지 않고 인터셉터/유틸리티 메서드만 직접 호출하는 방식입니다.

**v1.3.0**에서는 Testcontainers를 활용한 **실제 통합 테스트**로 개선될 예정입니다:

- **LocalStack**: AWS SQS 에뮬레이션
- **Testcontainers Redis**: 실제 Redis Pub/Sub 및 Stream 테스트

자세한 개선 계획은 [Testcontainers 통합 계획 문서](../docs/testcontainers-integration-plan.md)를 참조하세요.
