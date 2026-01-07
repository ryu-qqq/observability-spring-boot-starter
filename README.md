# Observability Spring Boot Starter

[![](https://jitpack.io/v/ryu-qqq/observability-spring-boot-starter.svg)](https://jitpack.io/#ryu-qqq/observability-spring-boot-starter)

Spring Boot 애플리케이션을 위한 경량 Observability SDK입니다.

## 📋 요구사항

- **Java**: 21+
- **Spring Boot**: 3.5.x+
- **Gradle**: 8.x+ (권장)

## ✨ 특징

- **자동 TraceId 전파**: HTTP 요청 간 TraceId 자동 생성 및 전파
- **Gateway 사용자 컨텍스트 지원**: X-User-Id, X-Tenant-Id, X-Organization-Id 자동 추출
- **HTTP 자동 로깅**: 요청/응답 자동 로깅 (Body 포함 선택)
- **WebFlux/Netty 지원**: Spring WebFlux, Spring Cloud Gateway 환경 지원 (v1.1.0+)
- **Reactor Context ↔ MDC 전파**: 리액티브 스트림에서 자동 MDC 전파
- **메시지 큐 자동 로깅**: SQS, Redis 리스너 자동 로깅
- **민감정보 마스킹**: 비밀번호, 카드번호 등 자동 마스킹
- **JSON 구조화 로깅**: ELK/CloudWatch 연동에 최적화
- **제로 설정**: 의존성 추가만으로 즉시 작동

## 🚀 빠른 시작

### 1. 의존성 추가

**Gradle (Groovy DSL)**
```groovy
// settings.gradle 또는 build.gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

// build.gradle
dependencies {
    implementation 'com.github.ryu-qqq:observability-spring-boot-starter:v1.1.0'
}
```

**Gradle (Kotlin DSL)**
```kotlin
// settings.gradle.kts 또는 build.gradle.kts
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

// build.gradle.kts
dependencies {
    implementation("com.github.ryu-qqq:observability-spring-boot-starter:v1.1.0")
}
```

**Gradle Version Catalog (libs.versions.toml) - 권장**
```toml
[versions]
observabilityStarter = "v1.1.0"

[libraries]
observability-starter = { module = "com.github.ryu-qqq:observability-spring-boot-starter", version.ref = "observabilityStarter" }
```
```groovy
// build.gradle
dependencies {
    implementation libs.observability.starter
}
```

**Maven**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.ryu-qqq</groupId>
    <artifactId>observability-spring-boot-starter</artifactId>
    <version>v1.1.0</version>
</dependency>
```

### 2. 설정 (선택사항)

```yaml
# application.yml
observability:
  enabled: true
  service-name: my-service
  environment: local

  trace:
    include-in-response: true

  http:
    enabled: true
    log-request-body: false
    log-response-body: false
    exclude-paths:
      - /actuator/**
      - /health

  message:
    enabled: true
    log-payload: false
    max-payload-length: 500
```

### 3. 실행

설정 없이도 기본값으로 즉시 작동합니다!

```
2024-01-05 12:00:00.123 [http-nio-8080-exec-1] [abc123] INFO  observability.http - HTTP Request: GET /api/users
2024-01-05 12:00:00.456 [http-nio-8080-exec-1] [abc123] INFO  observability.http - HTTP Response: GET /api/users | status=200 | duration=333ms
```

## 🔗 Gateway 연동

### TraceId 전파

SDK는 다음 헤더에서 TraceId를 자동으로 추출합니다 (우선순위 순):

1. `X-Trace-Id` (Gateway 기본 헤더)
2. `X-Request-Id`
3. W3C `traceparent`
4. AWS X-Ray `X-Amzn-Trace-Id`

### 사용자 컨텍스트 헤더

Gateway에서 전달하는 사용자 컨텍스트 헤더를 자동으로 추출하여 MDC에 저장합니다:

| 헤더 | MDC 키 | 설명 |
|------|--------|------|
| `X-User-Id` | `userId` | 사용자 ID |
| `X-Tenant-Id` | `tenantId` | 테넌트 ID |
| `X-Organization-Id` | `organizationId` | 조직 ID |
| `X-User-Roles` | `userRoles` | 사용자 역할 |

애플리케이션 코드에서 사용자 컨텍스트에 접근할 수 있습니다:

```java
// 사용자 ID 조회
String userId = TraceIdHolder.getUserId();
String tenantId = TraceIdHolder.getTenantId();
String organizationId = TraceIdHolder.getOrganizationId();
```

## ⚙️ 설정 옵션

### 전체 설정 레퍼런스

```yaml
observability:
  # ─────────────────────────────────────────────
  # 기본 설정
  # ─────────────────────────────────────────────
  service-name: my-service              # 서비스 이름 (기본: unknown)

  # ─────────────────────────────────────────────
  # TraceId 설정 (Spring MVC)
  # ─────────────────────────────────────────────
  trace:
    enabled: true                       # TraceId 기능 활성화
    header-names:                       # TraceId 추출 헤더 (우선순위 순)
      - X-Trace-Id
      - X-Request-Id
      - traceparent                     # W3C Trace Context
      - X-Amzn-Trace-Id                 # AWS X-Ray
    include-in-response: true           # 응답 헤더에 TraceId 포함
    generate-if-missing: true           # 요청에 없으면 자동 생성
    response-header-name: X-Trace-Id    # 응답 헤더 이름

  # ─────────────────────────────────────────────
  # Reactive TraceId 설정 (WebFlux/Netty)
  # ─────────────────────────────────────────────
  reactive-trace:
    enabled: true
    generate-if-missing: true
    include-in-response: true
    response-header-name: X-Trace-Id

  # ─────────────────────────────────────────────
  # HTTP 로깅 설정
  # ─────────────────────────────────────────────
  http:
    enabled: true
    log-request-body: false             # 요청 본문 로깅 (⚠️ 민감정보 주의)
    log-response-body: false            # 응답 본문 로깅
    max-body-length: 1000               # 본문 최대 길이
    slow-request-threshold-ms: 3000     # 느린 요청 임계값 (ms)
    exclude-paths:                      # 로깅 제외 경로 (Ant 패턴)
      - /actuator/**
      - /health
      - /health/**
      - /favicon.ico
      - /swagger-ui/**
      - /v3/api-docs/**
    exclude-headers:                    # 로깅 제외 헤더
      - Authorization
      - Cookie
      - Set-Cookie
      - X-Api-Key
      - Api-Key
    path-patterns:                      # 경로 정규화 패턴
      - pattern: "/users/\\d+"
        replacement: "/users/{id}"

  # ─────────────────────────────────────────────
  # 메시지 큐 로깅 설정 (SQS, Kafka 등)
  # ─────────────────────────────────────────────
  message:
    enabled: true
    log-payload: false                  # 페이로드 로깅 (⚠️ 민감정보 주의)
    max-payload-length: 500             # 페이로드 최대 길이

  # ─────────────────────────────────────────────
  # 비즈니스 로깅 설정 (@Loggable, @BusinessLog)
  # ─────────────────────────────────────────────
  logging:
    business:
      enabled: true
      log-arguments: false              # 메서드 인자 로깅
      log-result: false                 # 메서드 결과 로깅
      log-execution-time: true          # 실행 시간 로깅
      slow-execution-threshold: 1000    # 느린 실행 임계값 (ms)

  # ─────────────────────────────────────────────
  # 민감정보 마스킹 설정
  # ─────────────────────────────────────────────
  masking:
    enabled: true
    mask-fields:                        # 마스킹할 필드명 (JSON 키)
      - password
      - passwd
      - secret
      - token
      - apiKey
      - api_key
      - accessToken
      - access_token
      - refreshToken
      - refresh_token
      - creditCard
      - credit_card
      - cardNumber
      - card_number
      - ssn
      - socialSecurityNumber
    patterns:                           # 커스텀 마스킹 패턴
      - name: credit-card
        pattern: "\\d{4}-\\d{4}-\\d{4}-\\d{4}"
        replacement: "****-****-****-****"
```

### 설정 옵션 요약

| Prefix | 용도 | 주요 옵션 |
|--------|------|----------|
| `observability` | 기본 설정 | `service-name` |
| `observability.trace` | TraceId (MVC) | `enabled`, `header-names`, `include-in-response` |
| `observability.reactive-trace` | TraceId (WebFlux) | `enabled`, `generate-if-missing` |
| `observability.http` | HTTP 로깅 | `exclude-paths`, `slow-request-threshold-ms` |
| `observability.message` | 메시지 로깅 | `log-payload`, `max-payload-length` |
| `observability.logging.business` | 비즈니스 로깅 | `log-arguments`, `log-result`, `slow-execution-threshold` |
| `observability.masking` | 마스킹 | `mask-fields`, `patterns` |

### 기본 마스킹 필드

SDK는 다음 필드명을 기본으로 마스킹합니다:

```
password, passwd, secret, token, apiKey, api_key,
accessToken, access_token, refreshToken, refresh_token,
creditCard, credit_card, cardNumber, card_number, ssn, socialSecurityNumber
```

추가 필드가 필요하면 `mask-fields`에 추가하세요.

## 🌊 WebFlux/Netty 지원 (v1.1.0+)

### Spring WebFlux 환경

Spring WebFlux, Spring Cloud Gateway 등 리액티브 환경에서 자동으로 TraceId가 전파됩니다.

**지원 환경:**
- Spring WebFlux (Netty)
- Spring Cloud Gateway
- Reactor Netty 기반 애플리케이션

**WebFlux 설정 (선택사항)**
```yaml
observability:
  reactive-trace:
    enabled: true
    generate-if-missing: true
    include-in-response: true
    response-header-name: X-Trace-Id
```

### 자동 MDC 전파

WebFlux 환경에서는 스레드가 계속 변경되기 때문에 ThreadLocal 기반의 MDC가 자동 전파되지 않습니다.
SDK는 Reactor Context와 MDC를 자동으로 동기화하여 로깅이 올바르게 동작하도록 합니다.

```java
// WebFlux Controller에서도 TraceId 자동 설정
@RestController
public class OrderController {

    @GetMapping("/orders/{id}")
    public Mono<Order> getOrder(@PathVariable String id) {
        // 로그에 자동으로 TraceId 포함
        log.info("Fetching order: {}", id);
        return orderService.findById(id);
    }
}
```

### Spring Cloud Gateway 연동

Gateway에서 생성한 TraceId가 downstream 서비스로 자동 전파됩니다.

```java
// Gateway에서 TraceId 설정
// observability-starter가 자동으로 처리 (추가 설정 불필요)
```

### 커스텀 Reactive TraceId Provider

```java
@Bean
public ReactiveTraceIdProvider customReactiveTraceIdProvider() {
    return new ReactiveTraceIdProvider() {
        @Override
        public String generate() {
            return "gateway-" + UUID.randomUUID();
        }
        @Override
        public String extractFromExchange(ServerWebExchange exchange) {
            return exchange.getRequest().getHeaders()
                .getFirst("X-Custom-Trace");
        }
    };
}
```

## 📨 메시지 큐 로깅

### SQS 리스너 자동 로깅

`@SqsListener` 어노테이션이 붙은 메서드는 자동으로 로깅됩니다.

```java
@SqsListener("order-events")
public void handleOrderEvent(OrderEvent event) {
    // 비즈니스 로직
    // TraceId가 자동으로 MDC에 설정됩니다
}
```

**출력 예시:**
```
2024-01-05 12:00:00.123 [sqs-listener-1] [abc123] INFO  observability.message - Message Received: SQS | queue=order-events | messageId=msg-123
2024-01-05 12:00:00.456 [sqs-listener-1] [abc123] INFO  observability.message - Message Processed: SQS | queue=order-events | duration=333ms
```

### Redis 리스너 자동 로깅

Redis Pub/Sub 및 Stream 리스너도 자동으로 로깅됩니다.

```java
@Component
public class OrderEventListener implements MessageListener {
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 비즈니스 로직
    }
}
```

### 메시지에서 TraceId 전파

메시지 속성(헤더)에 `X-Trace-Id`가 포함되어 있으면 자동으로 추출됩니다:

```java
// SQS 메시지 발송 시 TraceId 포함
Map<String, MessageAttributeValue> attributes = Map.of(
    "X-Trace-Id", MessageAttributeValue.builder()
        .dataType("String")
        .stringValue(TraceIdHolder.get())
        .build()
);
```

## 🔧 커스터마이징

### 커스텀 TraceId Provider

```java
@Bean
public TraceIdProvider customTraceIdProvider() {
    return new TraceIdProvider() {
        @Override
        public String generate() {
            return "custom-" + UUID.randomUUID();
        }
        @Override
        public String extractFromRequest(HttpServletRequest request) {
            return request.getHeader("X-Custom-Trace");
        }
    };
}
```

### 커스텀 마스킹 패턴

```yaml
observability:
  masking:
    patterns:
      - pattern: "(계좌번호\\s*[=:]\\s*)\\d{10,14}"
        replacement: "$1**********"
    mask-fields:
      - cardNumber
      - accountNumber
      - ssn
```

### 경로 정규화 패턴

```yaml
observability:
  http:
    path-patterns:
      - pattern: "/orders/ORD-[A-Z0-9]+"
        replacement: "/orders/{orderId}"
```

## 📊 로그 출력 예시

### 기본 로그 형식

```
2024-01-05 12:00:00.123 [http-nio-8080-exec-1] [abc123-def456] INFO  observability.http - HTTP Request: GET /api/users/123
2024-01-05 12:00:00.456 [http-nio-8080-exec-1] [abc123-def456] INFO  observability.http - HTTP Response: GET /api/users/123 | status=200 | duration=333ms
```

### JSON 구조화 로그 (운영 환경)

```json
{
  "@timestamp": "2024-01-05T12:00:00.123Z",
  "level": "INFO",
  "logger": "observability.http",
  "traceId": "abc123-def456",
  "userId": "USR-12345",
  "tenantId": "TNT-001",
  "service": "user-service",
  "environment": "prod",
  "message": "HTTP Request: GET /api/users/123",
  "http": {
    "method": "GET",
    "uri": "/api/users/123",
    "normalizedUri": "/api/users/{id}"
  }
}
```

## 📦 Logback 설정

SDK는 기본 Logback 설정을 제공합니다.

```xml
<!-- logback-spring.xml -->
<configuration>
    <!-- Observability 기본 설정 포함 -->
    <include resource="logback/observability-defaults.xml"/>

    <root level="INFO">
        <appender-ref ref="OBSERVABILITY_CONSOLE"/>
    </root>
</configuration>
```

JSON 출력을 위해서는 `logstash-logback-encoder` 의존성을 추가하세요:

```kotlin
implementation("net.logstash.logback:logstash-logback-encoder:8.0")
```

## 🛡️ 기본 마스킹 패턴

| 패턴 | 예시 | 마스킹 결과 |
|------|------|-------------|
| 이메일 | `user@example.com` | `us***@example.com` |
| 신용카드 | `1234-5678-9012-3456` | `****-****-****-3456` |
| 한국 전화번호 | `010-1234-5678` | `010-****-5678` |
| 주민등록번호 | `900101-1234567` | `900101-*******` |
| Bearer 토큰 | `Bearer eyJ...` | `Bearer [MASKED]` |
| 비밀번호 (JSON) | `"password":"secret"` | `"password":"[MASKED]"` |

## 🎯 설계 철학

### "진입점은 자동, 비즈니스 로직은 명시적"

- **Entry Points**: HTTP 요청/응답, SQS/Redis 메시지 등은 SDK가 자동 로깅
- **Business Logic**: 애플리케이션 로직에서는 명시적으로 필요한 곳만 로깅

### "의견이 담긴 기본값, 유연한 확장"

- 설정 없이도 즉시 작동하는 합리적인 기본값
- 필요 시 세밀한 커스터마이징 가능

### "성능 우선"

- 불필요한 Body 로깅 기본 비활성화
- 느린 요청 자동 감지 및 경고
- 효율적인 마스킹 패턴 적용 순서

## 📁 프로젝트 구조

```
observability-spring-boot-starter/
├── observability-core/          # 핵심 모듈 - TraceId, MDC, 마스킹
│   └── trace/                   # TraceIdHolder, TraceIdProvider
│   └── masking/                 # LogMasker, MaskingPatterns
│   └── context/                 # RequestContext, UserContext
│
├── observability-logging/       # 로깅 모듈 - JSON 구조화 로깅
│   └── config/                  # Logback 설정
│   └── encoder/                 # JSON 인코더
│
├── observability-web/           # 웹 모듈 - HTTP 요청/응답 로깅 (Servlet)
│   └── filter/                  # TraceIdFilter, HttpLoggingFilter
│   └── interceptor/             # RestTemplate/WebClient 인터셉터
│
├── observability-webflux/       # 웹플럭스 모듈 - Reactive HTTP (WebFlux/Netty)
│   └── trace/                   # ReactiveTraceIdFilter (WebFilter)
│   └── context/                 # MdcContextLifter (Reactor Context ↔ MDC)
│
├── observability-client/        # 클라이언트 모듈 - 외부 호출 로깅
│   └── webclient/               # WebClient TraceId 전파
│   └── feign/                   # Feign Client TraceId 전파
│
├── observability-message/       # 메시지 모듈 - SQS/Redis 로깅
│   └── sqs/                     # SQS Listener AOP 로깅
│   └── redis/                   # Redis MessageListener 로깅
│
└── observability-starter/       # 통합 스타터 (이 모듈만 의존하면 전체 기능 사용)
    └── autoconfigure/           # Spring Boot AutoConfiguration
```

### 선택적 의존성

전체 기능이 필요하지 않다면 개별 모듈만 사용할 수 있습니다:

```groovy
// 전체 기능 (권장)
implementation 'com.github.ryu-qqq:observability-spring-boot-starter:v1.1.0'

// 또는 필요한 모듈만 선택
implementation 'com.github.ryu-qqq.observability-spring-boot-starter:observability-core:v1.1.0'
implementation 'com.github.ryu-qqq.observability-spring-boot-starter:observability-web:v1.1.0'
implementation 'com.github.ryu-qqq.observability-spring-boot-starter:observability-webflux:v1.1.0'  // WebFlux/Netty 환경
```

## 📜 라이선스

MIT License

## 🤝 기여

Issue와 Pull Request를 환영합니다!
