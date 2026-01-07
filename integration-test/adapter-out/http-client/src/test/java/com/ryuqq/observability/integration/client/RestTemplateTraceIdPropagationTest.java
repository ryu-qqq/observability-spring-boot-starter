package com.ryuqq.observability.integration.client;

import com.ryuqq.observability.client.rest.TraceIdRestTemplateInterceptor;
import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RestTemplate TraceId 전파 통합 테스트.
 *
 * <p>테스트 항목:</p>
 * <ul>
 *   <li>X-Trace-Id 헤더 전파 검증</li>
 *   <li>사용자 컨텍스트 헤더 전파 검증</li>
 *   <li>MDC에 TraceId가 없을 때 동작</li>
 * </ul>
 */
class RestTemplateTraceIdPropagationTest {

    private MockWebServer mockWebServer;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() throws IOException {
        // MockWebServer 시작
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // RestTemplate에 인터셉터 추가
        restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new TraceIdRestTemplateInterceptor());
    }

    @AfterEach
    void tearDown() throws IOException {
        // TraceIdHolder 정리
        TraceIdHolder.clear();

        // MockWebServer 종료
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("TraceId 전파 테스트")
    class TraceIdPropagationTests {

        @Test
        @DisplayName("MDC에 TraceId가 있으면 X-Trace-Id 헤더로 전파되어야 함")
        void shouldPropagateTraceIdHeader() throws InterruptedException {
            // given
            String traceId = "test-trace-id-12345";
            TraceIdHolder.set(traceId);
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            String url = mockWebServer.url("/api/test").toString();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(200);

            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo(traceId);
        }

        @Test
        @DisplayName("MDC에 TraceId가 없으면 헤더가 전파되지 않아야 함")
        void shouldNotPropagateHeaderWhenNoTraceId() throws InterruptedException {
            // given - TraceIdHolder is empty
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            String url = mockWebServer.url("/api/test").toString();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // then
            assertThat(response.getStatusCode().value()).isEqualTo(200);

            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isNull();
        }
    }

    @Nested
    @DisplayName("사용자 컨텍스트 전파 테스트")
    class UserContextPropagationTests {

        @Test
        @DisplayName("X-User-Id가 헤더로 전파되어야 함")
        void shouldPropagateUserIdHeader() throws InterruptedException {
            // given
            TraceIdHolder.set("trace-123");
            TraceIdHolder.setUserId("user-456");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            String url = mockWebServer.url("/api/test").toString();
            restTemplate.getForEntity(url, String.class);

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_USER_ID)).isEqualTo("user-456");
        }

        @Test
        @DisplayName("X-Tenant-Id가 헤더로 전파되어야 함")
        void shouldPropagateTenantIdHeader() throws InterruptedException {
            // given
            TraceIdHolder.set("trace-123");
            TraceIdHolder.setTenantId("tenant-789");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            String url = mockWebServer.url("/api/test").toString();
            restTemplate.getForEntity(url, String.class);

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TENANT_ID)).isEqualTo("tenant-789");
        }

        @Test
        @DisplayName("X-Organization-Id가 헤더로 전파되어야 함")
        void shouldPropagateOrganizationIdHeader() throws InterruptedException {
            // given
            TraceIdHolder.set("trace-123");
            TraceIdHolder.setOrganizationId("org-abc");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            String url = mockWebServer.url("/api/test").toString();
            restTemplate.getForEntity(url, String.class);

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_ORGANIZATION_ID)).isEqualTo("org-abc");
        }

        @Test
        @DisplayName("모든 컨텍스트 헤더가 함께 전파되어야 함")
        void shouldPropagateAllContextHeaders() throws InterruptedException {
            // given
            TraceIdHolder.set("trace-all");
            TraceIdHolder.setUserId("user-all");
            TraceIdHolder.setTenantId("tenant-all");
            TraceIdHolder.setOrganizationId("org-all");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            String url = mockWebServer.url("/api/test").toString();
            restTemplate.getForEntity(url, String.class);

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("trace-all");
            assertThat(request.getHeader(TraceIdHeaders.X_USER_ID)).isEqualTo("user-all");
            assertThat(request.getHeader(TraceIdHeaders.X_TENANT_ID)).isEqualTo("tenant-all");
            assertThat(request.getHeader(TraceIdHeaders.X_ORGANIZATION_ID)).isEqualTo("org-all");
        }
    }

    @Nested
    @DisplayName("HTTP 메서드별 테스트")
    class HttpMethodTests {

        @Test
        @DisplayName("POST 요청에서도 헤더가 전파되어야 함")
        void shouldPropagateHeadersInPostRequest() throws InterruptedException {
            // given
            TraceIdHolder.set("trace-post");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"id\": 1}")
                    .addHeader("Content-Type", "application/json"));

            // when
            String url = mockWebServer.url("/api/create").toString();
            restTemplate.postForEntity(url, "{\"name\": \"test\"}", String.class);

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("trace-post");
        }

        @Test
        @DisplayName("PUT 요청에서도 헤더가 전파되어야 함")
        void shouldPropagateHeadersInPutRequest() throws InterruptedException {
            // given
            TraceIdHolder.set("trace-put");
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json"));

            // when
            String url = mockWebServer.url("/api/update").toString();
            restTemplate.put(url, "{\"name\": \"updated\"}");

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("PUT");
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("trace-put");
        }

        @Test
        @DisplayName("DELETE 요청에서도 헤더가 전파되어야 함")
        void shouldPropagateHeadersInDeleteRequest() throws InterruptedException {
            // given
            TraceIdHolder.set("trace-delete");
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(204));

            // when
            String url = mockWebServer.url("/api/delete/123").toString();
            restTemplate.delete(url);

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("DELETE");
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("trace-delete");
        }
    }
}
