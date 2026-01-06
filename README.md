# Observability Spring Boot Starter

Spring Boot 애플리케이션을 위한 경량 Observability SDK입니다.

## 📋 특징

- **자동 TraceId 전파**: HTTP 요청 간 TraceId 자동 생성 및 전파
- **Gateway 사용자 컨텍스트 지원**: X-User-Id, X-Tenant-Id, X-Organization-Id 자동 추출
- **HTTP 자동 로깅**: 요청/응답 자동 로깅 (Body 포함 선택)
- **메시지 큐 자동 로깅**: SQS, Redis 리스너 자동 로깅
- **민감정보 마스킹**: 비밀번호, 카드번호 등 자동 마스킹
- **JSON 구조화 로깅**: ELK/CloudWatch 연동에 최적화
- **제로 설정**: 의존성 추가만으로 즉시 작동

## 🚀 빠른 시작

### 1. 의존성 추가

```kotlin
// build.gradle.kts
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.ryu-qqq:observability-spring-boot-starter:1.0.0")
}
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

### 기본 설정

| 속성 | 기본값 | 설명 |
|------|--------|------|
| `observability.enabled` | `true` | SDK 전체 활성화 |
| `observability.service-name` | `unknown` | 서비스 이름 |
| `observability.environment` | `local` | 환경 (local/dev/prod) |

### TraceId 설정

| 속성 | 기본값 | 설명 |
|------|--------|------|
| `observability.trace.enabled` | `true` | TraceId 필터 활성화 |
| `observability.trace.include-in-response` | `true` | 응답 헤더에 TraceId 포함 |
| `observability.trace.header-names` | `[X-Trace-Id, X-Request-Id, traceparent, X-Amzn-Trace-Id]` | TraceId 추출 헤더 (우선순위 순) |
| `observability.trace.response-header-name` | `X-Trace-Id` | 응답 헤더명 |

### HTTP 로깅 설정

| 속성 | 기본값 | 설명 |
|------|--------|------|
| `observability.http.enabled` | `true` | HTTP 로깅 활성화 |
| `observability.http.log-request-body` | `false` | 요청 본문 로깅 |
| `observability.http.log-response-body` | `false` | 응답 본문 로깅 |
| `observability.http.max-body-length` | `2000` | 본문 최대 길이 |
| `observability.http.slow-request-threshold-ms` | `3000` | 느린 요청 기준 (ms) |
| `observability.http.exclude-paths` | `[]` | 로깅 제외 경로 |
| `observability.http.exclude-headers` | `[Authorization, Cookie]` | 로깅 제외 헤더 |

### 메시지 로깅 설정 (SQS, Redis)

| 속성 | 기본값 | 설명 |
|------|--------|------|
| `observability.message.enabled` | `true` | 메시지 로깅 활성화 |
| `observability.message.log-payload` | `false` | 페이로드 로깅 |
| `observability.message.max-payload-length` | `500` | 페이로드 최대 길이 |

### 마스킹 설정

| 속성 | 기본값 | 설명 |
|------|--------|------|
| `observability.masking.enabled` | `true` | 마스킹 활성화 |
| `observability.masking.mask-fields` | `[]` | 마스킹할 JSON 필드명 |

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
src/main/java/com/ryuqq/observability/
├── ObservabilityAutoConfiguration.java  # Spring Boot AutoConfiguration
├── config/                              # 설정 클래스
│   ├── ObservabilityProperties.java
│   ├── TraceProperties.java
│   ├── HttpLoggingProperties.java
│   ├── MessageLoggingProperties.java
│   └── MaskingProperties.java
├── trace/                               # TraceId 및 사용자 컨텍스트
│   ├── TraceIdFilter.java
│   ├── TraceIdHolder.java
│   ├── TraceIdHeaders.java
│   └── TraceIdProvider.java
├── logging/                             # 로깅
│   ├── http/
│   │   ├── HttpLoggingFilter.java
│   │   └── PathNormalizer.java
│   └── message/
│       ├── MessageLoggingInterceptor.java
│       ├── SqsMessageLoggingAspect.java
│       └── RedisMessageLoggingAspect.java
├── masking/                             # 민감정보 마스킹
│   ├── LogMasker.java
│   └── MaskingPatterns.java
└── support/                             # 공통 유틸
    └── LogConstants.java
```

## 📜 라이선스

MIT License

## 🤝 기여

Issue와 Pull Request를 환영합니다!
