# Observability SDK 통합 평가 커맨드 v2.1

외부 프로젝트의 Observability SDK 통합 상태를 **실제 활용 중심**으로 평가합니다.

> **v2.1 변경사항** (SDK 1.3.0 대응):
> - `Reactor Context` → `Context Propagation`으로 평가 항목 변경
> - `MdcContextLifterHook` deprecated 경고 및 감점 추가
> - Context Propagation 체크 스크립트 추가
> - SDK 버전 기준 업데이트 (1.3.0)

---

## 사용법

```bash
/evaluate {프로젝트_경로} [--type servlet|webflux|worker|gateway]
```

**예시**:
```bash
/evaluate /Users/sangwon-ryu/fileflow --type servlet
/evaluate /Users/sangwon-ryu/connectly-gateway --type gateway
/evaluate ~/my-worker-service --type worker
```

> 💡 `--type` 생략 시 자동 감지 (WebFlux 의존성, SQS/Kafka 의존성 등으로 판단)

---

## 평가 철학

```
┌─────────────────────────────────────────────────────────────┐
│  📊 평가 핵심 원칙                                           │
├─────────────────────────────────────────────────────────────┤
│  ❌ 설정 파일 복붙 = 높은 점수 (기존 문제)                    │
│  ✅ 실제 동작 검증 + 올바른 활용 = 높은 점수 (개선 방향)      │
├─────────────────────────────────────────────────────────────┤
│  • 설정은 시작일 뿐, 실제 활용이 핵심                        │
│  • 테스트로 검증되지 않은 설정은 신뢰할 수 없음               │
│  • 운영 환경에서 디버깅 가능해야 진정한 Observability        │
└─────────────────────────────────────────────────────────────┘
```

---

## 평가 체계 v2.0

> **총점: 100점** (프로젝트 유형별 가중치 적용)

| 영역 | 배점 | 핵심 변경 | 평가 초점 |
|------|------|----------|----------|
| 기본 설정 | **15점** | ⬇️ 축소 | 의존성 + SDK/Logback/Sentry 설정 통합 |
| **런타임 검증** | **30점** | ⬆️ 확대 | TraceId 전파, 실제 동작 확인 ⭐ |
| **로그 활용** | **25점** | ⬆️ 확대 | @Loggable, 민감정보, 구조화 ⭐ |
| **테스트 커버리지** | **20점** | 🆕 신규 | SDK 관련 테스트 존재 여부 |
| **운영 품질** | **10점** | 🆕 신규 | 에러 컨텍스트, 검색 가능성 |

---

## 프로젝트 유형별 가중치

| 평가 항목 | Servlet | WebFlux | Worker | Gateway |
|----------|---------|---------|--------|---------|
| TraceId Filter | 1.0x | 1.5x | 0.5x | 1.5x |
| WebClient 전파 | 1.0x | 1.0x | 0.5x | 1.5x |
| 메시지 헤더 전파 | 0.5x | 0.5x | **2.0x** | 0.5x |
| **Context Propagation** | N/A | **1.5x** | 1.0x | **1.5x** |
| GlobalFilter | N/A | N/A | N/A | **2.0x** |
| deprecated Hook 미사용 | N/A | **1.0x** | 1.0x | **1.0x** |

> ⚠️ 가중치 적용 후 해당 영역 만점 초과 시 만점으로 제한
> ⚠️ **v1.3.0**: `Reactor Context` → `Context Propagation`으로 변경 (Micrometer 기반)

---

## 상세 평가 항목

### 1. 기본 설정 (15점)

> 설정은 복붙으로 해결 가능하므로 배점 축소

#### 1.1 의존성 (5점)

| 항목 | 배점 | 체크 내용 | 자동 검증 |
|------|------|----------|----------|
| observability-starter | 2점 | 최신 - 2 minor 이내 | ✅ |
| sentry-spring-boot-starter-jakarta | 1.5점 | 최신 - 1 minor 이내 | ✅ |
| logstash-logback-encoder | 1.5점 | 7.x 이상 | ✅ |

**자동 측정 스크립트**:
```bash
# libs.versions.toml 또는 build.gradle에서 버전 추출
grep -E "observability|sentry|logstash" gradle/libs.versions.toml 2>/dev/null || \
grep -E "observability|sentry|logstash" build.gradle
```

#### 1.2 SDK 설정 (5점)

| 항목 | 배점 | 체크 내용 |
|------|------|----------|
| service-name 설정 | 1점 | `observability.service-name` 명시 |
| trace 설정 | 1점 | header-names, include-in-response 등 |
| http 설정 | 1.5점 | exclude-paths, slow-request-threshold-ms |
| masking 설정 | 1.5점 | 커스텀 mask-fields 정의 |

**부분 점수 기준**:
- 설정 블록 존재 + 커스터마이징: **100%**
- 설정 블록 존재 + 기본값만: **50%**
- 완전 미설정: **0%**

#### 1.3 Logback/Sentry 설정 (5점)

| 항목 | 배점 | 체크 내용 |
|------|------|----------|
| Console Appender (MDC 포함) | 1점 | traceId/spanId 패턴 |
| JSON Appender | 1점 | LogstashEncoder 설정 |
| Sentry Appender | 1점 | ERROR 레벨 필터 |
| 프로파일 분기 | 1점 | springProfile local/prod |
| DSN 환경변수화 | 1점 | `${SENTRY_DSN:}` 형태 |

---

### 2. 런타임 검증 (30점) ⭐ 핵심

> 실제로 TraceId가 전파되고 동작하는지 검증

#### 2.1 TraceId Filter 동작 (10점)

| 항목 | 배점 | Servlet | WebFlux | 검증 방법 |
|------|------|---------|---------|----------|
| Filter 존재 | 3점 | TraceIdFilter | ReactiveTraceIdFilter | 클래스 검색 |
| MDC 설정 | 3점 | MDC.put() 호출 | contextWrite() 사용 | 코드 분석 |
| Response 헤더 | 2점 | X-Trace-Id 반환 | X-Trace-Id 반환 | 테스트 확인 |
| 헤더 추출 | 2점 | 요청 헤더 → MDC | 요청 헤더 → Context | 코드 분석 |

**자동 측정 스크립트**:
```bash
# TraceId Filter 존재 확인
find . -name "*.java" -exec grep -l "TraceIdFilter\|ReactiveTraceIdFilter" {} \;

# MDC 사용 확인
grep -r "MDC.put\|MDC.get" --include="*.java" | wc -l
```

#### 2.2 서비스 간 전파 (12점)

| 항목 | 배점 | 체크 내용 | 프로젝트 유형 가중치 |
|------|------|----------|---------------------|
| WebClient 전파 | 4점 | TraceIdExchangeFilterFunction 적용 | WebFlux 1.0x, Gateway 1.5x |
| RestTemplate 전파 | 3점 | ClientHttpRequestInterceptor 적용 | Servlet 1.0x |
| Feign 전파 | 2점 | RequestInterceptor 적용 | Servlet 1.0x |
| 메시지 큐 전파 | 3점 | SQS/Kafka 헤더 인터셉터 | Worker **2.0x** |

**WebClient 검증 코드 패턴**:
```java
// 필수 패턴 (있어야 함)
WebClient.builder()
    .filter(TraceIdExchangeFilterFunction())  // ← 이것
    .build()
```

#### 2.3 Context Propagation (8점) - WebFlux/Gateway만 해당 ⭐ v1.3.0 변경

> **v1.3.0 주요 변경**: `Hooks.onEachOperator()` 방식은 Netty ByteBuf 메모리 누수 및 Prometheus 엔드포인트 문제를 유발합니다.
> **권장**: Micrometer Context Propagation (`Hooks.enableAutomaticContextPropagation()`)

| 항목 | 배점 | 체크 내용 |
|------|------|----------|
| SDK 1.3.0+ 사용 | 3점 | Context Propagation 지원 버전 |
| CP 자동 구성 활성화 | 3점 | `enableAutomaticContextPropagation()` 동작 |
| ⚠️ deprecated Hook 미사용 | 2점 | `MdcContextLifterHook.install()` 직접 호출 없음 |

**마이그레이션 상태 평가**:

| 상태 | 점수 | 설명 |
|------|------|------|
| ✅ SDK 1.3.0+ & CP 활성화 | 8점 | 권장 방식 |
| ⚠️ SDK 1.2.x (Legacy Hook) | 4점 | 마이그레이션 권장 |
| ❌ Legacy Hook + 이슈 보고 | 0점 | 즉시 마이그레이션 필수 |
| ❌ MDC 동기화 없음 | 0점 | 필수 설정 누락 |

**자동 측정 스크립트**:
```bash
# ✅ 권장 패턴 확인 (v1.3.0+)
echo "=== Context Propagation 설정 확인 ==="
grep -rE "enableAutomaticContextPropagation|ThreadLocalAccessor|context-propagation" --include="*.java" --include="*.yml" --include="*.yaml"

# ⚠️ deprecated 패턴 경고
echo "=== ⚠️ Deprecated 패턴 검사 ==="
DEPRECATED=$(grep -rE "MdcContextLifterHook\.install|Hooks\.onEachOperator.*MDC" --include="*.java" 2>/dev/null)
if [ -n "$DEPRECATED" ]; then
  echo "🔴 WARNING: deprecated MdcContextLifterHook 사용 감지!"
  echo "   → v1.3.0의 Context Propagation으로 마이그레이션 필요"
  echo "   → 메모리 누수 및 Prometheus 이슈 발생 가능"
  echo "$DEPRECATED"
fi

# Reactor Context 사용 확인 (여전히 유효)
grep -rE "contextWrite|deferContextual" --include="*.java" | wc -l
```

---

### 3. 로그 활용 (25점) ⭐ 핵심

> 올바른 로깅 패턴 적용 여부

#### 3.1 @Loggable 적용률 (10점)

| 적용률 | 점수 | 기준 |
|--------|------|------|
| 80% 이상 | 10점 | 핵심 Service/UseCase 메서드 |
| 60-79% | 7점 | 대부분 적용 |
| 40-59% | 5점 | 절반 적용 |
| 20-39% | 3점 | 일부 적용 |
| 20% 미만 | 1점 | 거의 미적용 |
| 0% | 0점 | 완전 미사용 |

**정량적 측정 스크립트**:
```bash
#!/bin/bash
# @Loggable 적용률 자동 계산

# 대상 메서드 수 (public 메서드 in Service/UseCase)
TARGET_METHODS=$(grep -rE "public\s+\w+\s+\w+\s*\(" \
  --include="*Service.java" --include="*UseCase.java" \
  --include="*ServiceImpl.java" | wc -l)

# @Loggable 적용 수
LOGGABLE_COUNT=$(grep -rB1 "@Loggable" --include="*.java" | \
  grep -E "public\s+\w+\s+\w+\s*\(" | wc -l)

# 적용률 계산
if [ $TARGET_METHODS -gt 0 ]; then
  RATE=$((LOGGABLE_COUNT * 100 / TARGET_METHODS))
  echo "적용률: ${RATE}% (${LOGGABLE_COUNT}/${TARGET_METHODS})"
else
  echo "대상 메서드 없음"
fi
```

#### 3.2 민감정보 처리 (10점) 🔴 Critical

| 등급 | 점수 | 상태 | 조치 |
|------|------|------|------|
| 안전 | 10점 | 노출 없음, LogMasker 활용 | - |
| 주의 | 6점 | 일부 위험 패턴 존재 | 개선 권장 |
| 위험 | 3점 | 민감정보 로깅 가능성 | 개선 필수 |
| **Critical** | 0점 | 평문 노출 확인 | **등급 1단계 하향** |

**민감정보 패턴 정의**:
```yaml
CRITICAL (평문 노출 시 0점 + 등급 하향):
  - password, passwd, pwd
  - creditCard, cardNumber
  - ssn, 주민등록번호
  - accessToken, refreshToken, apiKey, secretKey

HIGH (마스킹 권장):
  - email (부분 마스킹: a***@example.com)
  - phoneNumber (부분 마스킹: 010-****-5678)
  - accountNumber (부분 마스킹)

MEDIUM (선택적 마스킹):
  - address, 주소
  - birthDate, 생년월일
```

**자동 탐지 스크립트**:
```bash
#!/bin/bash
# 민감정보 평문 노출 탐지

CRITICAL_PATTERNS="password|passwd|pwd|creditCard|cardNumber|ssn|accessToken|refreshToken|apiKey|secretKey"

# 로그 출력에서 민감정보 검색 (위험 패턴)
VIOLATIONS=$(grep -rE "log\.(info|debug|warn|error).*($CRITICAL_PATTERNS)" \
  --include="*.java" | grep -v "@Mask\|LogMasker\|masked\|\*\*\*" | wc -l)

if [ $VIOLATIONS -gt 0 ]; then
  echo "🔴 CRITICAL: 민감정보 노출 위험 ${VIOLATIONS}건"
  grep -rE "log\.(info|debug|warn|error).*($CRITICAL_PATTERNS)" \
    --include="*.java" | grep -v "@Mask\|LogMasker\|masked\|\*\*\*"
else
  echo "✅ 민감정보 노출 없음"
fi
```

#### 3.3 구조화 로깅 (5점)

| 항목 | 배점 | 체크 내용 |
|------|------|----------|
| JSON 필드 일관성 | 2점 | 동일한 이벤트는 동일한 필드명 |
| 검색 가능 키워드 | 2점 | orderId, userId 등 식별자 포함 |
| 적절한 로그 레벨 | 1점 | DEBUG/INFO/WARN/ERROR 올바른 사용 |

---

### 4. 테스트 커버리지 (20점) 🆕

> SDK 관련 기능이 테스트로 검증되어 있는가?

#### 4.1 TraceId 전파 테스트 (8점)

| 항목 | 배점 | 체크 내용 |
|------|------|----------|
| HTTP 요청 TraceId 전파 | 3점 | 요청 헤더 → 응답 헤더 검증 |
| WebClient 전파 테스트 | 3점 | 외부 호출 시 헤더 포함 검증 |
| 메시지 큐 전파 테스트 | 2점 | 메시지 헤더에 traceId 포함 검증 |

**테스트 존재 확인 스크립트**:
```bash
# TraceId 관련 테스트 검색
grep -rE "X-Trace-Id|traceId|TraceIdFilter" \
  --include="*Test.java" --include="*IT.java" | wc -l
```

#### 4.2 @Loggable 동작 테스트 (5점)

| 항목 | 배점 | 체크 내용 |
|------|------|----------|
| AOP 프록시 동작 확인 | 2점 | @Loggable 메서드 호출 시 로그 출력 |
| 실행 시간 측정 검증 | 2점 | slow-execution-threshold 동작 |
| 예외 로깅 검증 | 1점 | Exception 발생 시 에러 로그 |

#### 4.3 LogMasker 테스트 (4점)

| 항목 | 배점 | 체크 내용 |
|------|------|----------|
| 기본 마스킹 패턴 테스트 | 2점 | password, email 등 마스킹 확인 |
| 커스텀 패턴 테스트 | 2점 | 프로젝트 정의 패턴 동작 확인 |

#### 4.4 통합 테스트 (3점)

| 항목 | 배점 | 체크 내용 |
|------|------|----------|
| E2E TraceId 흐름 | 2점 | 요청 → 서비스 → 응답 전체 추적 |
| 에러 시나리오 | 1점 | Exception 발생 시 traceId 포함 확인 |

---

### 5. 운영 품질 (10점) 🆕

> 실제 운영 환경에서 디버깅/모니터링 가능한가?

#### 5.1 에러 컨텍스트 (5점)

| 항목 | 배점 | 체크 내용 |
|------|------|----------|
| Exception에 traceId 포함 | 2점 | 에러 로그에 traceId 필수 포함 |
| 요청 정보 포함 | 2점 | HTTP method, path, userId 등 |
| 스택트레이스 적절성 | 1점 | 불필요한 프레임워크 스택 제외 |

**확인 방법**:
```java
// 올바른 에러 로깅 패턴
log.error("Order failed - traceId: {}, orderId: {}, userId: {}",
    MDC.get("traceId"), orderId, userId, exception);
```

#### 5.2 검색 가능성 (3점)

| 항목 | 배점 | 체크 내용 |
|------|------|----------|
| 일관된 로그 포맷 | 1점 | JSON 필드명 통일 |
| 식별자 인덱싱 | 1점 | orderId, userId 등 검색 가능 |
| 타임스탬프 정확성 | 1점 | ISO 8601 형식, 타임존 명시 |

#### 5.3 메트릭 연동 (2점)

| 항목 | 배점 | 체크 내용 |
|------|------|----------|
| Micrometer 연동 | 1점 | 기본 메트릭 수집 |
| 커스텀 메트릭 | 1점 | 비즈니스 메트릭 정의 (선택) |

---

## 등급 판정

| 등급 | 점수 | 상태 | 운영 준비도 |
|------|------|------|------------|
| **S** | 95-100 | 모범 사례 | 🟢 즉시 운영 가능, 레퍼런스 프로젝트 |
| **A** | 85-94 | 우수 | 🟢 운영 가능 |
| **B+** | 75-84 | 양호 | 🟡 일부 개선 후 운영 |
| **B** | 65-74 | 기본 충족 | 🟡 개선 권장 |
| **C** | 50-64 | 미흡 | 🟠 개선 필수 |
| **D** | 30-49 | 부족 | 🔴 상당한 개선 필요 |
| **F** | 0-29 | 재구현 필요 | 🔴 전면 재검토 |

### Critical 조건 (등급 하향)

| 조건 | 페널티 |
|------|--------|
| 민감정보 평문 노출 | **등급 1단계 하향** |
| TraceId Filter 완전 미동작 | **등급 1단계 하향** |
| 테스트 0% (운영 배포 예정) | **등급 1단계 하향** |
| ⚠️ **MdcContextLifterHook 직접 사용** (v1.3.0+) | **경고 + 5점 감점** |

> **v1.3.0 신규**: `MdcContextLifterHook.install()` 직접 호출 시 경고
> - Netty ByteBuf 메모리 누수 유발
> - Prometheus/Actuator 엔드포인트 오류 발생
> - → `ContextPropagationConfiguration.install()` 또는 자동 구성 사용 권장

---

## 평가 프로세스

### 1단계: 프로젝트 분석

```bash
# 프로젝트 유형 자동 감지
detect_project_type() {
  if grep -q "spring-webflux\|reactor-core" build.gradle 2>/dev/null; then
    echo "webflux"
  elif grep -q "spring-cloud-gateway" build.gradle 2>/dev/null; then
    echo "gateway"
  elif grep -q "sqs\|kafka" build.gradle 2>/dev/null; then
    echo "worker"
  else
    echo "servlet"
  fi
}
```

### 2단계: 자동 측정 실행

```bash
# 각 영역별 자동 측정 스크립트 실행
run_evaluation() {
  PROJECT_PATH=$1
  PROJECT_TYPE=$2

  echo "=== 1. 의존성 분석 ==="
  check_dependencies $PROJECT_PATH

  echo "=== 2. 런타임 검증 ==="
  check_runtime_verification $PROJECT_PATH $PROJECT_TYPE

  echo "=== 3. 로그 활용 ==="
  check_log_usage $PROJECT_PATH

  echo "=== 4. 테스트 커버리지 ==="
  check_test_coverage $PROJECT_PATH

  echo "=== 5. 운영 품질 ==="
  check_operational_quality $PROJECT_PATH
}
```

### 3단계: 가중치 적용

```bash
# 프로젝트 유형별 가중치 적용
apply_weights() {
  PROJECT_TYPE=$1
  RAW_SCORES=$2

  case $PROJECT_TYPE in
    "webflux")
      CONTEXT_PROPAGATION_WEIGHT=1.5  # v1.3.0: Reactor Context → Context Propagation
      TRACEID_FILTER_WEIGHT=1.5
      DEPRECATED_HOOK_PENALTY=1.0     # deprecated hook 사용 시 감점
      ;;
    "worker")
      MESSAGE_QUEUE_WEIGHT=2.0
      TRACEID_FILTER_WEIGHT=0.5
      CONTEXT_PROPAGATION_WEIGHT=1.0
      ;;
    "gateway")
      GLOBAL_FILTER_WEIGHT=2.0
      WEBCLIENT_WEIGHT=1.5
      CONTEXT_PROPAGATION_WEIGHT=1.5  # Gateway도 CP 필수
      DEPRECATED_HOOK_PENALTY=1.0
      ;;
    *)
      # servlet - 기본 가중치 (Context Propagation 해당 없음)
      ;;
  esac
}
```

### 4단계: 보고서 생성

```
{observability-sdk-repo}/docs/evaluations/{프로젝트명}-evaluation.md
```

---

## 출력 형식

```markdown
# {프로젝트명} - Observability SDK 통합 평가 보고서

## 개요

| 항목 | 내용 |
|------|------|
| 프로젝트 | {프로젝트명} |
| 평가일 | {날짜} |
| 프로젝트 유형 | {Servlet/WebFlux/Worker/Gateway} |
| SDK 버전 | {버전} |
| 평가 버전 | v2.1 |

## 종합 평가 결과

### 총점

| 영역 | 배점 | 획득 | 가중치 | 최종 | 상태 |
|------|------|------|--------|------|------|
| 기본 설정 | 15 | ? | 1.0x | ? | ✅/⚠️/❌ |
| 런타임 검증 | 30 | ? | ?x | ? | ✅/⚠️/❌ |
| 로그 활용 | 25 | ? | 1.0x | ? | ✅/⚠️/❌ |
| 테스트 커버리지 | 20 | ? | 1.0x | ? | ✅/⚠️/❌ |
| 운영 품질 | 10 | ? | 1.0x | ? | ✅/⚠️/❌ |
| **총점** | **100** | - | - | **?** | **등급** |

### Critical 체크

| 항목 | 상태 | 영향 |
|------|------|------|
| 민감정보 평문 노출 | ✅/🔴 | 등급 하향 여부 |
| TraceId Filter 동작 | ✅/🔴 | 등급 하향 여부 |
| 테스트 존재 (운영 배포 시) | ✅/🔴 | 등급 하향 여부 |
| ⚠️ deprecated Hook 미사용 (v1.3.0+) | ✅/⚠️ | 5점 감점 여부 |

## 정량적 측정 결과

### @Loggable 적용률
```
대상 메서드: {N}개
적용 메서드: {M}개
적용률: {M/N * 100}%
```

### 민감정보 스캔 결과
```
CRITICAL 패턴 노출: {N}건
HIGH 패턴 노출: {M}건
LogMasker 적용: {K}건
```

### 테스트 커버리지
```
TraceId 관련 테스트: {N}개
@Loggable 테스트: {M}개
LogMasker 테스트: {K}개
통합 테스트: {L}개
```

## 상세 평가

### 1. 기본 설정 (15점)
{영역별 상세 분석}

### 2. 런타임 검증 (30점)
{영역별 상세 분석 - 프로젝트 유형별 가중치 적용 결과 포함}

### 3. 로그 활용 (25점)
{영역별 상세 분석 - 민감정보 스캔 결과 포함}

### 4. 테스트 커버리지 (20점)
{테스트 존재 여부 및 품질 분석}

### 5. 운영 품질 (10점)
{에러 컨텍스트, 검색 가능성 분석}

## 개선 권장 사항

### 🔴 Critical (즉시 조치)
{등급 하향 요인 또는 보안 위험}

### 🟠 High (1주 내 조치)
{운영 전 필수 개선}

### 🟡 Medium (권장)
{개선하면 좋은 항목}

### 🟢 Low (선택)
{있으면 좋은 항목}

## 개선 코드 예시

### {문제 1}: {제목}
```java
// Before (문제)
{문제 코드}

// After (개선)
{개선 코드}
```

## 결론

{종합 평가 및 다음 단계 권장사항}

### 운영 준비도
- [ ] Critical 이슈 해결
- [ ] High 이슈 해결
- [ ] 테스트 커버리지 확보
- [ ] 운영 모니터링 연동

### 예상 개선 효과
- 디버깅 시간: {현재} → {개선 후}
- 장애 탐지 시간: {현재} → {개선 후}
- 로그 검색 효율: {현재} → {개선 후}
```

---

## 프로젝트 유형별 필수 체크

### Servlet (Spring MVC)

| 필수 항목 | 가중치 | 체크 |
|----------|--------|------|
| TraceIdFilter (Servlet Filter) | 1.0x | □ |
| RestTemplate Interceptor | 1.0x | □ |
| Feign Interceptor (사용 시) | 1.0x | □ |

### WebFlux (Reactive) ⭐ v1.3.0 변경

| 필수 항목 | 가중치 | 체크 |
|----------|--------|------|
| ReactiveTraceIdFilter (WebFilter) | **1.5x** | □ |
| **Context Propagation 활성화** | **1.5x** | □ |
| SDK 1.3.0+ 버전 사용 | 1.0x | □ |
| ⚠️ deprecated Hook 미사용 | 1.0x | □ |

> **v1.3.0 변경사항**:
> - ❌ `MDC-Context 동기화 (Hooks.onEachOperator)` → deprecated (메모리 누수)
> - ✅ `Context Propagation (Hooks.enableAutomaticContextPropagation)` → 권장

### Spring Cloud Gateway ⭐ v1.3.0 변경

| 필수 항목 | 가중치 | 체크 |
|----------|--------|------|
| GlobalFilter 구현 | **2.0x** | □ |
| 라우팅 전/후 TraceId 유지 | **1.5x** | □ |
| 다운스트림 헤더 전파 | **1.5x** | □ |
| **Context Propagation 활성화** | **1.5x** | □ |
| ⚠️ deprecated Hook 미사용 | 1.0x | □ |

### Worker (SQS/Kafka)

| 필수 항목 | 가중치 | 체크 |
|----------|--------|------|
| 메시지 헤더 TraceId 추출 | **2.0x** | □ |
| SqsTraceIdInterceptor | **2.0x** | □ |
| `observability.message` 설정 | 1.0x | □ |
| 배치 처리 시 개별 TraceId | 1.0x | □ |

---

## 버전 기준 (동적)

```yaml
# 버전 체크 정책: 상대적 기준 적용

observability-starter:
  strategy: "latest - 1 minor"
  current_latest: "1.3.0"  # Context Propagation 지원 버전
  acceptable: ["1.3.x", "1.2.x"]
  migration_note: "1.3.0부터 Micrometer Context Propagation 자동 구성"
  deprecated_warning: "1.2.x 이하에서 MdcContextLifterHook 사용 시 메모리 누수 주의"

sentry-spring-boot-starter-jakarta:
  strategy: "latest - 1 minor"
  current_latest: "8.29.0"
  acceptable: ["8.29.x", "8.28.x"]

logstash-logback-encoder:
  strategy: "major version"
  minimum: "7.0"
  acceptable: ["7.x", "8.x"]
```

---

## 인자 설명

**$ARGUMENTS**: `{프로젝트_경로} [--type {타입}]`

| 인자 | 필수 | 설명 | 예시 |
|------|------|------|------|
| 프로젝트_경로 | ✅ | 절대/상대 경로 | `/Users/dev/myproject` |
| --type | ❌ | 프로젝트 유형 (자동 감지) | `servlet`, `webflux`, `worker`, `gateway` |

---

## 실행 예시

```bash
# Servlet 프로젝트 평가
/evaluate /Users/sangwon-ryu/order-service

# WebFlux 프로젝트 평가 (타입 명시)
/evaluate /Users/sangwon-ryu/reactive-api --type webflux

# Gateway 프로젝트 평가
/evaluate /Users/sangwon-ryu/api-gateway --type gateway

# Worker 프로젝트 평가
/evaluate /Users/sangwon-ryu/order-worker --type worker
```

---

## 평가 도구 실행

평가 시 아래 자동화 스크립트들이 순차 실행됩니다:

```bash
# 1. 의존성 체크 (SDK 버전 포함)
scripts/check-dependencies.sh {project_path}

# 2. @Loggable 적용률 측정
scripts/measure-loggable-coverage.sh {project_path}

# 3. 민감정보 스캔
scripts/scan-sensitive-data.sh {project_path}

# 4. 테스트 커버리지 분석
scripts/analyze-test-coverage.sh {project_path}

# 5. 런타임 검증 항목 체크
scripts/verify-runtime-features.sh {project_path} {project_type}

# 6. ⭐ Context Propagation 상태 체크 (v1.3.0+)
scripts/check-context-propagation.sh {project_path} {project_type}
```

### Context Propagation 체크 스크립트 (v1.3.0+)

```bash
#!/bin/bash
# scripts/check-context-propagation.sh

PROJECT_PATH=$1
PROJECT_TYPE=$2

echo "=== Context Propagation 상태 확인 (v1.3.0) ==="

# WebFlux/Gateway만 해당
if [[ "$PROJECT_TYPE" != "webflux" && "$PROJECT_TYPE" != "gateway" ]]; then
  echo "ℹ️ Servlet/Worker 프로젝트 - Context Propagation 해당 없음"
  exit 0
fi

# 1. SDK 버전 확인
SDK_VERSION=$(grep -E "observability.*=.*[0-9]" $PROJECT_PATH/gradle/libs.versions.toml 2>/dev/null | \
  grep -oE "[0-9]+\.[0-9]+\.[0-9]+" | head -1)
echo "SDK 버전: ${SDK_VERSION:-미확인}"

# 2. deprecated Hook 사용 여부 (경고)
DEPRECATED=$(grep -rE "MdcContextLifterHook\.install|Hooks\.onEachOperator.*MDC" \
  --include="*.java" $PROJECT_PATH/src/ 2>/dev/null)

if [ -n "$DEPRECATED" ]; then
  echo "🔴 WARNING: deprecated MdcContextLifterHook 사용 감지!"
  echo "   → v1.3.0의 Context Propagation으로 마이그레이션 필요"
  echo "   → 메모리 누수 및 Prometheus 이슈 발생 가능"
  echo "파일 목록:"
  echo "$DEPRECATED"
  echo "점수: 0/8 (마이그레이션 필수)"
  exit 1
fi

# 3. Context Propagation 설정 확인
CP_ENABLED=$(grep -rE "context-propagation|enableAutomaticContextPropagation" \
  --include="*.java" --include="*.yml" --include="*.yaml" $PROJECT_PATH/ 2>/dev/null)

if [ -n "$CP_ENABLED" ]; then
  echo "✅ Context Propagation 적용 확인"
  echo "점수: 8/8"
else
  echo "⚠️ Context Propagation 설정 미확인"
  echo "   → SDK 1.3.0 이상 사용 시 자동 구성됨"
  echo "점수: 3/8 (설정 확인 필요)"
fi
```

> 💡 스크립트 위치: `{observability-sdk-repo}/scripts/evaluation/`
