package com.ryuqq.observability.integration.bootstrap;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전체 통합 흐름 테스트.
 *
 * <p>observability-starter의 모든 기능이 함께 동작하는지 검증합니다.</p>
 *
 * <ul>
 *   <li>TraceId 필터 → MDC 전파 → 응답 헤더</li>
 *   <li>사용자 컨텍스트 헤더 추출 및 전파</li>
 *   <li>HTTP 로깅 (요청/응답)</li>
 *   <li>@Loggable 어노테이션 동작</li>
 *   <li>마스킹 동작</li>
 * </ul>
 */
@SpringBootTest(
        classes = BootstrapTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class FullIntegrationFlowTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Nested
    @DisplayName("TraceId 전체 흐름 테스트")
    class TraceIdFullFlowTests {

        @Test
        @DisplayName("TraceId가 없으면 새로 생성되어 응답 헤더에 포함되어야 함")
        void shouldGenerateTraceIdWhenNotProvided() {
            // when
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/trace", Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().get(TraceIdHeaders.X_TRACE_ID)).isNotNull();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("traceId")).isNotNull();
        }

        @Test
        @DisplayName("요청에 TraceId가 있으면 그대로 사용되어야 함")
        void shouldUseProvidedTraceId() {
            // given
            String providedTraceId = "provided-trace-id-" + UUID.randomUUID();
            HttpHeaders headers = new HttpHeaders();
            headers.set(TraceIdHeaders.X_TRACE_ID, providedTraceId);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/trace",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst(TraceIdHeaders.X_TRACE_ID)).isEqualTo(providedTraceId);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("traceId")).isEqualTo(providedTraceId);
            assertThat(response.getBody().get("mdcTraceId")).isEqualTo(providedTraceId);
        }
    }

    @Nested
    @DisplayName("사용자 컨텍스트 전체 흐름 테스트")
    class UserContextFullFlowTests {

        @Test
        @DisplayName("모든 사용자 컨텍스트 헤더가 추출되어야 함")
        void shouldExtractAllUserContextHeaders() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set(TraceIdHeaders.X_TRACE_ID, "context-trace-123");
            headers.set(TraceIdHeaders.X_USER_ID, "user-456");
            headers.set(TraceIdHeaders.X_TENANT_ID, "tenant-789");
            headers.set(TraceIdHeaders.X_ORGANIZATION_ID, "org-abc");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/trace",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("traceId")).isEqualTo("context-trace-123");
            assertThat(body.get("userId")).isEqualTo("user-456");
            assertThat(body.get("tenantId")).isEqualTo("tenant-789");
            assertThat(body.get("organizationId")).isEqualTo("org-abc");
        }

        @Test
        @DisplayName("일부 컨텍스트 헤더만 있어도 동작해야 함")
        void shouldHandlePartialContextHeaders() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set(TraceIdHeaders.X_TRACE_ID, "partial-trace-123");
            headers.set(TraceIdHeaders.X_USER_ID, "user-only");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/trace",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("traceId")).isEqualTo("partial-trace-123");
            assertThat(body.get("userId")).isEqualTo("user-only");
            assertThat(body.get("tenantId")).isNull();
        }
    }

    @Nested
    @DisplayName("@Loggable 통합 테스트")
    class LoggableIntegrationTests {

        @Test
        @DisplayName("@Loggable 엔드포인트가 정상 동작해야 함")
        void shouldWorkWithLoggableEndpoint() {
            // when
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "/api/loggable?name=IntegrationTest",
                    Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("greeting")).isEqualTo("Hello, IntegrationTest!");
            assertThat(body.get("traceId")).isNotNull();
        }

        @Test
        @DisplayName("느린 요청에서도 @Loggable이 동작해야 함")
        void shouldWorkWithSlowEndpoint() {
            // when
            long start = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/slow", Map.class);
            long elapsed = System.currentTimeMillis() - start;

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(elapsed).isGreaterThanOrEqualTo(100); // 100ms delay
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("message")).isEqualTo("slow response");
        }
    }

    @Nested
    @DisplayName("마스킹 통합 테스트")
    class MaskingIntegrationTests {

        @Test
        @DisplayName("민감한 데이터가 포함된 요청이 처리되어야 함")
        void shouldHandleRequestWithSensitiveData() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(TraceIdHeaders.X_TRACE_ID, "masking-trace-123");

            Map<String, String> requestBody = Map.of(
                    "username", "testuser",
                    "password", "secretPassword123",
                    "email", "test@example.com"
            );
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/masking",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("message")).isEqualTo("Data received");
            assertThat(body.get("traceId")).isEqualTo("masking-trace-123");
            // 마스킹은 로깅에서 적용되므로 응답 자체에는 영향 없음
        }
    }

    @Nested
    @DisplayName("경로 정규화 통합 테스트")
    class PathNormalizationTests {

        @Test
        @DisplayName("숫자 ID 경로가 정상 처리되어야 함")
        void shouldHandleNumericIdPath() {
            // when
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "/api/users/12345/orders/67890",
                    Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("userId")).isEqualTo("12345");
            assertThat(body.get("orderId")).isEqualTo("67890");
            // 경로 정규화는 로깅에서 /api/users/{id}/orders/{id}로 기록됨
        }

        @Test
        @DisplayName("UUID 경로가 정상 처리되어야 함")
        void shouldHandleUuidPath() {
            // given
            String uuid = UUID.randomUUID().toString();

            // when
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "/api/users/" + uuid + "/orders/12345",
                    Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("userId")).isEqualTo(uuid);
            // 경로 정규화는 로깅에서 /api/users/{uuid}/orders/{id}로 기록됨
        }
    }

    @Nested
    @DisplayName("에러 응답 통합 테스트")
    class ErrorResponseTests {

        @Test
        @DisplayName("5xx 에러 응답에도 TraceId가 포함되어야 함")
        void shouldIncludeTraceIdIn5xxResponse() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set(TraceIdHeaders.X_TRACE_ID, "error-trace-123");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/error",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getHeaders().getFirst(TraceIdHeaders.X_TRACE_ID)).isEqualTo("error-trace-123");
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("traceId")).isEqualTo("error-trace-123");
        }
    }

    @Nested
    @DisplayName("Health Check (로깅 제외) 테스트")
    class HealthCheckTests {

        @Test
        @DisplayName("Health check 엔드포인트가 정상 동작해야 함")
        void shouldWorkHealthCheckEndpoint() {
            // when
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/health", Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("status")).isEqualTo("UP");
        }
    }
}
