# Logging 표준 가이드

모든 프로젝트에서 일관된 로깅 설정을 위한 표준 가이드입니다.

---

## 1. 버전 및 의존성 표준

### 1.1 필수 의존성

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| **logstash-logback-encoder** | `8.0` 이상 | JSON 구조화 로깅 |
| **observability-starter** | `v1.3.0` 이상 | TraceId 관리 + 구조화 로깅 SDK |
| **slf4j-api** | Spring Boot 관리 | 로깅 추상화 |
| **logback-classic** | Spring Boot 관리 | 로깅 구현체 |

### 1.2 Version Catalog 설정 (libs.versions.toml)

```toml
[versions]
# Logging
logstashLogback = "8.0"

# Observability SDK
observabilityStarter = "v1.3.0"

[libraries]
# JSON Structured Logging
logstash-logback-encoder = { module = "net.logstash.logback:logstash-logback-encoder", version.ref = "logstashLogback" }

# Observability SDK (TraceId 관리)
observability-starter = { module = "com.github.ryu-qqq.observability-spring-boot-starter:observability-starter", version.ref = "observabilityStarter" }

[bundles]
# Observability Bundle (로깅 + 모니터링)
observability = [
    "spring-boot-starter-actuator",
    "micrometer-prometheus",
    "logstash-logback-encoder"
]
```

### 1.3 build.gradle 설정

```gradle
dependencies {
    // Observability SDK
    implementation libs.observability.starter

    // JSON Logging
    implementation libs.logstash.logback.encoder

    // Sentry (옵션)
    implementation libs.sentry.spring.boot
    implementation libs.sentry.logback
}
```

---

## 2. 로그 포맷 표준

### 2.1 Console 포맷 (Development)

```text
{timestamp} {level} [{thread}] {logger} - {message} [traceId={traceId}, userId={userId}, requestId={requestId}]
```

**예시:**
```text
2024-01-06 10:30:45.123 INFO  [http-nio-8080-exec-1] c.e.s.OrderService - 주문 생성 완료: orderId=ORD-123 [traceId=abc-123, userId=user-001, requestId=req-456]
```

### 2.2 JSON 포맷 (Production)

```json
{
  "@timestamp": "2024-01-06T10:30:45.123+09:00",
  "level": "INFO",
  "logger_name": "com.example.service.OrderService",
  "thread_name": "http-nio-8080-exec-1",
  "message": "주문 생성 완료: orderId=ORD-123",
  "traceId": "abc-123",
  "spanId": null,
  "userId": "user-001",
  "tenantId": "tenant-001",
  "requestId": "req-456",
  "service": "order-api",
  "environment": "prod"
}
```

### 2.3 필수 MDC 필드

| 필드 | 설정 주체 | 용도 | OpenSearch 필터 |
|------|----------|------|-----------------|
| `traceId` | SDK TraceIdFilter | 분산 추적 | ✅ 필수 |
| `requestId` | RequestLoggingFilter | 요청 그룹핑 | ✅ 필수 |
| `userId` | UserContextFilter | 사용자 추적 | ✅ 권장 |
| `tenantId` | UserContextFilter | 테넌트 분리 | 선택 |
| `spanId` | OpenTelemetry | 스팬 추적 | 선택 |

---

## 3. Logback 설정 표준

### 3.1 표준 logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">

    <!-- ═══════════════════════════════════════════════════════════════════
         Properties
         ═══════════════════════════════════════════════════════════════════ -->
    <springProperty name="SERVICE_NAME" source="spring.application.name" defaultValue="unknown"/>
    <springProperty name="LOG_LEVEL" source="logging.level.root" defaultValue="INFO"/>

    <!-- MDC 패턴 (Console용) -->
    <property name="MDC_PATTERN" value="traceId=%X{traceId:-N/A}, userId=%X{userId:-N/A}, requestId=%X{requestId:-N/A}"/>

    <!-- Console 패턴 -->
    <property name="CONSOLE_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg [${MDC_PATTERN}]%n"/>

    <!-- ═══════════════════════════════════════════════════════════════════
         Console Appender (Development)
         ═══════════════════════════════════════════════════════════════════ -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- ═══════════════════════════════════════════════════════════════════
         JSON Appender (Production)
         ═══════════════════════════════════════════════════════════════════ -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- 타임스탬프 형식 (ISO-8601) -->
            <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSSXXX</timestampPattern>

            <!-- MDC 필드 포함 (명시적 지정) -->
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>tenantId</includeMdcKeyName>
            <includeMdcKeyName>requestId</includeMdcKeyName>

            <!-- 서비스 메타데이터 -->
            <customFields>{"service":"${SERVICE_NAME}","environment":"${SPRING_PROFILES_ACTIVE:-unknown}"}</customFields>

            <!-- 예외 스택트레이스 형식 -->
            <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
                <maxDepthPerThrowable>30</maxDepthPerThrowable>
                <maxLength>2048</maxLength>
                <shortenedClassNameLength>20</shortenedClassNameLength>
                <rootCauseFirst>true</rootCauseFirst>
            </throwableConverter>

            <!-- 불필요한 필드 제외 -->
            <fieldNames>
                <levelValue>[ignore]</levelValue>
            </fieldNames>
        </encoder>
    </appender>

    <!-- ═══════════════════════════════════════════════════════════════════
         Async Appender (Production 성능 최적화)
         ═══════════════════════════════════════════════════════════════════ -->
    <appender name="ASYNC_JSON" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="JSON"/>
        <queueSize>1024</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <includeCallerData>false</includeCallerData>
        <neverBlock>true</neverBlock>
    </appender>

    <!-- ═══════════════════════════════════════════════════════════════════
         Sentry Appender
         ═══════════════════════════════════════════════════════════════════ -->
    <appender name="SENTRY" class="io.sentry.logback.SentryAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <minimumBreadcrumbLevel>INFO</minimumBreadcrumbLevel>
        <minimumEventLevel>ERROR</minimumEventLevel>
    </appender>

    <!-- ═══════════════════════════════════════════════════════════════════
         Profile: Local/Development
         ═══════════════════════════════════════════════════════════════════ -->
    <springProfile name="local,dev,default">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>

        <!-- 개발 시 SQL 로깅 -->
        <logger name="org.hibernate.SQL" level="DEBUG"/>
        <logger name="org.hibernate.orm.jdbc.bind" level="TRACE"/>

        <!-- 개발 시 Spring 상세 로깅 -->
        <logger name="org.springframework.web" level="DEBUG"/>
    </springProfile>

    <!-- ═══════════════════════════════════════════════════════════════════
         Profile: Staging
         ═══════════════════════════════════════════════════════════════════ -->
    <springProfile name="staging">
        <root level="INFO">
            <appender-ref ref="ASYNC_JSON"/>
            <appender-ref ref="SENTRY"/>
        </root>

        <!-- Staging에서 상세 로깅 -->
        <logger name="com.example" level="DEBUG"/>
    </springProfile>

    <!-- ═══════════════════════════════════════════════════════════════════
         Profile: Production
         ═══════════════════════════════════════════════════════════════════ -->
    <springProfile name="prod,production">
        <root level="INFO">
            <appender-ref ref="ASYNC_JSON"/>
            <appender-ref ref="SENTRY"/>
        </root>

        <!-- Production 노이즈 억제 -->
        <logger name="org.hibernate" level="WARN"/>
        <logger name="org.springframework" level="WARN"/>
        <logger name="org.apache" level="WARN"/>
        <logger name="io.netty" level="WARN"/>

        <!-- 애플리케이션 로그는 INFO 유지 -->
        <logger name="com.example" level="INFO"/>
    </springProfile>

</configuration>
```

---

## 4. 로그 작성 표준

### 4.1 로그 레벨 사용 기준

| 레벨 | 용도 | 예시 |
|------|------|------|
| **ERROR** | 즉시 대응 필요한 오류 | DB 연결 실패, 외부 API 장애 |
| **WARN** | 주의 필요하나 서비스 정상 | 재시도 성공, 폴백 적용 |
| **INFO** | 주요 비즈니스 이벤트 | 주문 생성, 결제 완료 |
| **DEBUG** | 개발/디버깅용 상세 정보 | 메서드 진입/종료, 중간 값 |
| **TRACE** | 매우 상세한 추적 정보 | 루프 내부, 저수준 연산 |

### 4.2 올바른 로그 작성법

#### ✅ 권장

```java
// 1. 구조화된 파라미터 사용
log.info("주문 생성 완료: orderId={}, amount={}, userId={}", orderId, amount, userId);

// 2. 비즈니스 의미가 명확한 메시지
log.info("결제 처리 시작: paymentId={}, method={}", paymentId, paymentMethod);

// 3. 예외는 마지막 파라미터로 전달 (스택트레이스 자동 포함)
log.error("결제 실패: paymentId={}", paymentId, exception);

// 4. 조건부 로깅 (성능 최적화)
if (log.isDebugEnabled()) {
    log.debug("상세 처리 정보: data={}", expensiveToString(data));
}

// 5. 시작/종료 쌍으로 로깅
log.info("외부 API 호출 시작: url={}", url);
// ... 처리 ...
log.info("외부 API 호출 완료: url={}, status={}, duration={}ms", url, status, duration);
```

#### ❌ 비권장

```java
// 1. 문자열 연결 (성능 저하)
log.info("주문 생성: " + orderId + ", 금액: " + amount);  // ❌

// 2. 중복된 MDC 정보 수동 추가
log.info("[traceId={}] 처리 중", MDC.get("traceId"));  // ❌ 자동 포함됨

// 3. 스택트레이스 누락
log.error("에러 발생: " + e.getMessage());  // ❌ 스택트레이스 없음

// 4. 민감 정보 로깅
log.info("사용자 정보: {}", user.toString());  // ❌ PII 노출

// 5. 과도한 로깅
for (Item item : items) {
    log.info("아이템 처리: {}", item);  // ❌ 대량 로그 발생
}

// 6. 의미 없는 로깅
log.info("here");  // ❌
log.debug("test");  // ❌
```

### 4.3 구조화 로깅 (Structured Logging)

#### 문자열 로깅 vs 구조화 로깅

일반적인 문자열 로깅은 모든 데이터가 `message` 필드에 문자열로 들어갑니다:

```java
// 일반 문자열 로깅
log.info("주문 생성 완료: orderId={}, amount={}", orderId, amount);
```

**출력 결과 (JSON):**
```json
{
  "message": "주문 생성 완료: orderId=123, amount=50000",
  "traceId": "abc-123"
}
```
→ `orderId`로 직접 쿼리 불가능 (message 필드 내 문자열 검색만 가능)

**구조화 로깅**은 각 데이터 필드를 JSON 최상위 필드로 출력합니다:

```json
{
  "message": "주문 생성 완료",
  "orderId": "123",
  "amount": 50000,
  "traceId": "abc-123"
}
```
→ OpenSearch에서 `orderId:123` 으로 직접 쿼리 가능

#### SDK 어노테이션 기반 구조화 로깅 (v1.3.0+)

SDK의 `@BusinessLog`와 `@Loggable` 어노테이션은 **자동으로 구조화 로깅**을 지원합니다.

**@BusinessLog - 비즈니스 이벤트 로깅:**

```java
@BusinessLog(
    action = "ORDER_CREATED",
    entity = "Order",
    entityId = "#command.orderId",
    context = {"amount=#command.amount", "userId=#command.userId"}
)
public void createOrder(CreateOrderCommand command) {
    // 비즈니스 로직
}
```

**출력 결과 (JSON):**
```json
{
  "message": "[BUSINESS] action=ORDER_CREATED",
  "action": "ORDER_CREATED",
  "entity": "Order",
  "entityId": "ORD-123",
  "amount": 50000,
  "userId": "user-001",
  "success": true,
  "traceId": "abc-123"
}
```

**@Loggable - 메서드 실행 로깅:**

```java
@Loggable(
    value = "OrderService.createOrder",
    includeArgs = true,
    includeResult = true,
    includeExecutionTime = true
)
public Order createOrder(CreateOrderCommand command) {
    return orderRepository.save(order);
}
```

**출력 결과 (JSON):**
```json
{
  "message": "OrderService.createOrder completed in 45ms",
  "method": "OrderService.createOrder",
  "phase": "completed",
  "duration": 45,
  "result": "Order{id=123, ...}",
  "traceId": "abc-123"
}
```

#### 직접 구조화 로깅 사용

SDK 어노테이션 외에 직접 구조화 로깅을 사용할 수 있습니다:

```java
import static net.logstash.logback.argument.StructuredArguments.kv;
import static net.logstash.logback.argument.StructuredArguments.v;

// 방법 1: kv() - key=value 형식으로 메시지에도 표시
log.info("주문 생성 완료: {}, {}", kv("orderId", orderId), kv("amount", amount));
// 메시지: "주문 생성 완료: orderId=123, amount=50000"
// JSON 필드: orderId, amount 별도 추가

// 방법 2: v() - value만 메시지에 표시
log.info("주문 생성 완료: orderId={}", v("orderId", orderId));
// 메시지: "주문 생성 완료: orderId=123"
// JSON 필드: orderId 별도 추가

// 방법 3: Markers 사용 (다수 필드)
import net.logstash.logback.marker.Markers;

Map<String, Object> logData = Map.of(
    "orderId", orderId,
    "amount", amount,
    "userId", userId
);
log.info(Markers.appendEntries(logData), "주문 생성 완료");
// 메시지: "주문 생성 완료"
// JSON 필드: orderId, amount, userId 모두 별도 추가
```

#### 구조화 로깅 선택 기준

| 상황 | 권장 방식 |
|------|----------|
| 비즈니스 이벤트 (주문, 결제 등) | `@BusinessLog` |
| 메서드 실행 추적 | `@Loggable` |
| 일회성 상세 로깅 | `StructuredArguments.kv()` |
| 다수 필드 로깅 | `Markers.appendEntries()` |
| 단순 디버그 로그 | 일반 문자열 로깅 |

### 4.4 민감 정보 처리

```java
// 마스킹 유틸리티
public class LogMasker {

    public static String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "***-****-" + phone.substring(phone.length() - 4);
    }

    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}

// 사용 예시
log.info("사용자 인증: email={}", LogMasker.maskEmail(email));
// 출력: 사용자 인증: email=te***@example.com
```

---

## 5. 로그 필터 구현 표준

### 5.1 Filter 실행 순서

```text
요청 수신
    ↓
TraceIdFilter (Order=0, SDK)     ← traceId 설정
    ↓
RequestLoggingFilter (Order=5)   ← requestId 설정, 요청/응답 로깅
    ↓
UserContextFilter (Order=10)     ← userId, tenantId 설정
    ↓
비즈니스 로직
    ↓
응답 반환
```

### 5.2 Filter Order 상수

```java
package com.example.common.filter;

/**
 * Filter 순서 상수.
 * 낮은 숫자가 먼저 실행됩니다.
 */
public final class FilterOrder {

    private FilterOrder() {}

    /** SDK TraceIdFilter (자동 구성) */
    public static final int TRACE_ID_FILTER = 0;

    /** 요청/응답 로깅 필터 */
    public static final int REQUEST_LOGGING_FILTER = 5;

    /** 사용자 컨텍스트 필터 */
    public static final int USER_CONTEXT_FILTER = 10;

    /** 보안 필터 (Spring Security) */
    public static final int SECURITY_FILTER = 100;

    /** 비즈니스 필터 */
    public static final int BUSINESS_FILTER = 200;
}
```

---

## 6. OpenSearch 연동

### 6.1 인덱스 템플릿

```json
{
  "index_patterns": ["logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1
    },
    "mappings": {
      "properties": {
        "@timestamp": { "type": "date" },
        "level": { "type": "keyword" },
        "logger_name": { "type": "keyword" },
        "message": { "type": "text" },
        "traceId": { "type": "keyword" },
        "spanId": { "type": "keyword" },
        "userId": { "type": "keyword" },
        "tenantId": { "type": "keyword" },
        "requestId": { "type": "keyword" },
        "service": { "type": "keyword" },
        "environment": { "type": "keyword" },
        "stack_trace": { "type": "text" }
      }
    }
  }
}
```

### 6.2 주요 검색 쿼리

```json
// traceId로 요청 전체 흐름 조회
GET logs-*/_search
{
  "query": { "term": { "traceId": "abc-123" } },
  "sort": [{ "@timestamp": "asc" }]
}

// 특정 사용자의 에러 조회
GET logs-*/_search
{
  "query": {
    "bool": {
      "must": [
        { "term": { "userId": "user-001" } },
        { "term": { "level": "ERROR" } }
      ]
    }
  }
}

// 서비스별 에러 통계
GET logs-*/_search
{
  "size": 0,
  "query": { "term": { "level": "ERROR" } },
  "aggs": {
    "by_service": {
      "terms": { "field": "service" }
    }
  }
}
```

---

## 7. 적용 평가 체크리스트

### 7.1 의존성 체크 (10점)

| 항목 | 배점 | 확인 방법 |
|------|------|----------|
| observability-starter | 5점 | `grep observability build.gradle` |
| logstash-logback-encoder | 5점 | `grep logstash build.gradle` |

### 7.2 Logback 설정 체크 (40점)

| 항목 | 배점 | 확인 방법 |
|------|------|----------|
| Console Appender (dev) | 5점 | `grep CONSOLE logback-spring.xml` |
| JSON Appender (prod) | 10점 | `grep LogstashEncoder logback-spring.xml` |
| Async Appender | 5점 | `grep AsyncAppender logback-spring.xml` |
| MDC 필드 포함 (traceId) | 10점 | `grep includeMdcKeyName logback-spring.xml` |
| 프로파일별 분기 | 5점 | `grep springProfile logback-spring.xml` |
| 서비스 메타데이터 | 5점 | `grep customFields logback-spring.xml` |

### 7.3 Filter 체크 (30점)

| 항목 | 배점 | 확인 방법 |
|------|------|----------|
| TraceIdFilter (SDK) | 10점 | SDK 의존성 확인 |
| RequestLoggingFilter | 10점 | requestId MDC 설정 확인 |
| UserContextFilter | 10점 | userId MDC 설정 확인 |

### 7.4 로그 품질 체크 (20점)

| 항목 | 배점 | 확인 방법 |
|------|------|----------|
| 구조화된 파라미터 사용 | 5점 | `grep -r "log\." --include="*.java"` |
| 예외 스택트레이스 포함 | 5점 | ERROR 로그 확인 |
| 민감 정보 마스킹 | 5점 | PII 노출 검사 |
| 적절한 로그 레벨 | 5점 | 레벨 분포 확인 |

---

## 8. 트러블슈팅

### 8.1 JSON 로그가 출력되지 않음

```yaml
# 체크리스트
1. 프로파일 확인
   - SPRING_PROFILES_ACTIVE=prod 설정 확인

2. logback-spring.xml 확인
   - springProfile name="prod" 내에 JSON appender 있는지

3. 의존성 확인
   - logstash-logback-encoder 의존성 포함 여부
```

### 8.2 MDC 필드가 로그에 없음

```yaml
# 체크리스트
1. Filter 순서 확인
   - TraceIdFilter가 가장 먼저 실행되는지

2. Logback 설정 확인
   - includeMdcKeyName 에 필드가 포함되어 있는지

3. MDC 값 설정 확인
   - 로그 직전에 MDC.get("traceId") 값 확인
```

### 8.3 로그가 너무 많이 발생함

```yaml
# 해결 방법
1. 루프 내 로깅 제거
   - 루프 외부에서 요약 로깅

2. 로그 레벨 조정
   - 상세 로그는 DEBUG/TRACE로 변경

3. 샘플링 적용 (선택)
   - TurboFilter로 로그 샘플링
```

### 8.4 성능 저하

```yaml
# 해결 방법
1. AsyncAppender 사용
   - 동기 로깅 → 비동기 로깅 전환

2. 조건부 로깅
   - if (log.isDebugEnabled()) 사용

3. 파라미터화된 로깅
   - 문자열 연결(+) → {} 플레이스홀더
```
