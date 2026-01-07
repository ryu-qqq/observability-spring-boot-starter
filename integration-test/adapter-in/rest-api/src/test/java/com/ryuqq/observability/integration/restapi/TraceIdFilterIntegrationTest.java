package com.ryuqq.observability.integration.restapi;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TraceIdFilter 통합 테스트.
 *
 * <p>테스트 항목:</p>
 * <ul>
 *   <li>TraceId 자동 생성</li>
 *   <li>요청 헤더에서 TraceId 추출</li>
 *   <li>응답 헤더에 TraceId 포함</li>
 *   <li>MDC 전파 검증</li>
 *   <li>사용자 컨텍스트 헤더 추출</li>
 * </ul>
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "observability.service-name=test-service",
                "observability.trace.enabled=true",
                "observability.trace.include-in-response=true",
                "observability.trace.generate-if-missing=true"
        }
)
class TraceIdFilterIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Nested
    @DisplayName("TraceId 생성 테스트")
    class TraceIdGenerationTests {

        @Test
        @DisplayName("요청에 TraceId가 없으면 자동 생성되어야 함")
        void shouldGenerateTraceIdWhenMissing() {
            // when
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/test", Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            // 응답 헤더에 TraceId 포함
            String traceIdHeader = response.getHeaders().getFirst("X-Trace-Id");
            assertThat(traceIdHeader).isNotNull().isNotEmpty();

            // 응답 body에도 TraceId 포함
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("traceId")).isEqualTo(traceIdHeader);
            assertThat(body.get("mdcTraceId")).isEqualTo(traceIdHeader);
        }

        @Test
        @DisplayName("요청 헤더에 TraceId가 있으면 그대로 사용해야 함")
        void shouldUseExistingTraceId() {
            // given
            String existingTraceId = "test-trace-id-12345";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Trace-Id", existingTraceId);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/test", HttpMethod.GET, request, Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            String traceIdHeader = response.getHeaders().getFirst("X-Trace-Id");
            assertThat(traceIdHeader).isEqualTo(existingTraceId);

            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("traceId")).isEqualTo(existingTraceId);
        }

        @Test
        @DisplayName("X-Request-Id 헤더도 TraceId로 추출되어야 함")
        void shouldExtractTraceIdFromXRequestId() {
            // given
            String requestId = "request-id-67890";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Request-Id", requestId);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/test", HttpMethod.GET, request, Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("traceId")).isEqualTo(requestId);
        }

        @Test
        @DisplayName("W3C traceparent 헤더에서 TraceId 추출")
        void shouldExtractTraceIdFromTraceparent() {
            // given - W3C Trace Context format: version-trace_id-parent_id-flags
            String traceId = "0af7651916cd43dd8448eb211c80319c";
            String traceparent = "00-" + traceId + "-b7ad6b7169203331-01";

            HttpHeaders headers = new HttpHeaders();
            headers.set("traceparent", traceparent);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/test", HttpMethod.GET, request, Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("traceId")).isEqualTo(traceId);
        }
    }

    @Nested
    @DisplayName("사용자 컨텍스트 헤더 테스트")
    class UserContextTests {

        @Test
        @DisplayName("X-User-Id 헤더가 MDC에 설정되어야 함")
        void shouldExtractUserId() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set(TraceIdHeaders.X_USER_ID, "user-123");
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/context", HttpMethod.GET, request, Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("userId")).isEqualTo("user-123");
        }

        @Test
        @DisplayName("X-Tenant-Id 헤더가 MDC에 설정되어야 함")
        void shouldExtractTenantId() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set(TraceIdHeaders.X_TENANT_ID, "tenant-456");
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/context", HttpMethod.GET, request, Map.class);

            // then
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("tenantId")).isEqualTo("tenant-456");
        }

        @Test
        @DisplayName("X-Organization-Id 헤더가 MDC에 설정되어야 함")
        void shouldExtractOrganizationId() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set(TraceIdHeaders.X_ORGANIZATION_ID, "org-789");
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/context", HttpMethod.GET, request, Map.class);

            // then
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("organizationId")).isEqualTo("org-789");
        }

        @Test
        @DisplayName("X-User-Roles 헤더가 MDC에 설정되어야 함")
        void shouldExtractUserRoles() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set(TraceIdHeaders.X_USER_ROLES, "ADMIN,USER");
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/context", HttpMethod.GET, request, Map.class);

            // then
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("userRoles")).isEqualTo("ADMIN,USER");
        }

        @Test
        @DisplayName("모든 사용자 컨텍스트 헤더가 함께 추출되어야 함")
        void shouldExtractAllUserContextHeaders() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set(TraceIdHeaders.X_USER_ID, "user-123");
            headers.set(TraceIdHeaders.X_TENANT_ID, "tenant-456");
            headers.set(TraceIdHeaders.X_ORGANIZATION_ID, "org-789");
            headers.set(TraceIdHeaders.X_USER_ROLES, "ADMIN");
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/context", HttpMethod.GET, request, Map.class);

            // then
            Map<String, String> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("userId")).isEqualTo("user-123");
            assertThat(body.get("tenantId")).isEqualTo("tenant-456");
            assertThat(body.get("organizationId")).isEqualTo("org-789");
            assertThat(body.get("userRoles")).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("응답 헤더 테스트")
    class ResponseHeaderTests {

        @Test
        @DisplayName("응답 헤더에 X-Trace-Id가 포함되어야 함")
        void shouldIncludeTraceIdInResponseHeader() {
            // when
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/test", Map.class);

            // then
            assertThat(response.getHeaders().containsKey("X-Trace-Id")).isTrue();
            assertThat(response.getHeaders().getFirst("X-Trace-Id")).isNotEmpty();
        }

        @Test
        @DisplayName("POST 요청에서도 응답 헤더에 TraceId 포함")
        void shouldIncludeTraceIdInPostResponse() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>("{\"key\":\"value\"}", headers);

            // when
            ResponseEntity<Map> response = restTemplate.postForEntity("/api/echo", request, Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getFirst("X-Trace-Id")).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("MDC 정리 테스트")
    class MdcCleanupTests {

        @Test
        @DisplayName("각 요청마다 새로운 TraceId가 생성되어야 함 (MDC 정리 확인)")
        void shouldClearMdcBetweenRequests() {
            // when - 첫 번째 요청
            ResponseEntity<Map> response1 = restTemplate.getForEntity("/api/test", Map.class);
            String traceId1 = response1.getHeaders().getFirst("X-Trace-Id");

            // when - 두 번째 요청
            ResponseEntity<Map> response2 = restTemplate.getForEntity("/api/test", Map.class);
            String traceId2 = response2.getHeaders().getFirst("X-Trace-Id");

            // then - 각 요청은 서로 다른 TraceId를 가져야 함
            assertThat(traceId1).isNotNull();
            assertThat(traceId2).isNotNull();
            assertThat(traceId1).isNotEqualTo(traceId2);
        }
    }
}
