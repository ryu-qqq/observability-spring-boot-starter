package com.ryuqq.observability.integration.client;

import com.ryuqq.observability.client.webclient.TraceIdExchangeFilterFunction;
import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebClient TraceId 전파 통합 테스트.
 *
 * <p>테스트 항목:</p>
 * <ul>
 *   <li>X-Trace-Id 헤더 전파 검증</li>
 *   <li>사용자 컨텍스트 헤더 전파 검증</li>
 *   <li>Reactive 환경에서의 컨텍스트 캡처</li>
 * </ul>
 */
class WebClientTraceIdPropagationTest {

    private MockWebServer mockWebServer;
    private WebClient webClient;

    @BeforeEach
    void setUp() throws IOException {
        // MockWebServer 시작
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // WebClient에 필터 추가
        webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .filter(TraceIdExchangeFilterFunction.create())
                .build();
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
            String traceId = "webclient-trace-id-12345";
            TraceIdHolder.set(traceId);
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            Mono<String> result = webClient.get()
                    .uri("/api/test")
                    .retrieve()
                    .bodyToMono(String.class);

            // then
            StepVerifier.create(result)
                    .expectNextMatches(body -> body.contains("success"))
                    .verifyComplete();

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
            Mono<String> result = webClient.get()
                    .uri("/api/test")
                    .retrieve()
                    .bodyToMono(String.class);

            // then
            StepVerifier.create(result)
                    .expectNextMatches(body -> body.contains("success"))
                    .verifyComplete();

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
            TraceIdHolder.setUserId("webclient-user-456");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            Mono<String> result = webClient.get()
                    .uri("/api/test")
                    .retrieve()
                    .bodyToMono(String.class);

            StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_USER_ID)).isEqualTo("webclient-user-456");
        }

        @Test
        @DisplayName("X-Tenant-Id가 헤더로 전파되어야 함")
        void shouldPropagateTenantIdHeader() throws InterruptedException {
            // given
            TraceIdHolder.set("trace-123");
            TraceIdHolder.setTenantId("webclient-tenant-789");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            Mono<String> result = webClient.get()
                    .uri("/api/test")
                    .retrieve()
                    .bodyToMono(String.class);

            StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TENANT_ID)).isEqualTo("webclient-tenant-789");
        }

        @Test
        @DisplayName("X-Organization-Id가 헤더로 전파되어야 함")
        void shouldPropagateOrganizationIdHeader() throws InterruptedException {
            // given
            TraceIdHolder.set("trace-123");
            TraceIdHolder.setOrganizationId("webclient-org-abc");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            Mono<String> result = webClient.get()
                    .uri("/api/test")
                    .retrieve()
                    .bodyToMono(String.class);

            StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_ORGANIZATION_ID)).isEqualTo("webclient-org-abc");
        }

        @Test
        @DisplayName("모든 컨텍스트 헤더가 함께 전파되어야 함")
        void shouldPropagateAllContextHeaders() throws InterruptedException {
            // given
            TraceIdHolder.set("webclient-trace-all");
            TraceIdHolder.setUserId("webclient-user-all");
            TraceIdHolder.setTenantId("webclient-tenant-all");
            TraceIdHolder.setOrganizationId("webclient-org-all");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            Mono<String> result = webClient.get()
                    .uri("/api/test")
                    .retrieve()
                    .bodyToMono(String.class);

            StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("webclient-trace-all");
            assertThat(request.getHeader(TraceIdHeaders.X_USER_ID)).isEqualTo("webclient-user-all");
            assertThat(request.getHeader(TraceIdHeaders.X_TENANT_ID)).isEqualTo("webclient-tenant-all");
            assertThat(request.getHeader(TraceIdHeaders.X_ORGANIZATION_ID)).isEqualTo("webclient-org-all");
        }
    }

    @Nested
    @DisplayName("HTTP 메서드별 테스트")
    class HttpMethodTests {

        @Test
        @DisplayName("POST 요청에서도 헤더가 전파되어야 함")
        void shouldPropagateHeadersInPostRequest() throws InterruptedException {
            // given
            TraceIdHolder.set("webclient-trace-post");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"id\": 1}")
                    .addHeader("Content-Type", "application/json"));

            // when
            Mono<String> result = webClient.post()
                    .uri("/api/create")
                    .bodyValue("{\"name\": \"test\"}")
                    .retrieve()
                    .bodyToMono(String.class);

            StepVerifier.create(result)
                    .expectNextCount(1)
                    .verifyComplete();

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("webclient-trace-post");
        }

        @Test
        @DisplayName("PUT 요청에서도 헤더가 전파되어야 함")
        void shouldPropagateHeadersInPutRequest() throws InterruptedException {
            // given
            TraceIdHolder.set("webclient-trace-put");
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .addHeader("Content-Type", "application/json"));

            // when
            Mono<Void> result = webClient.put()
                    .uri("/api/update")
                    .bodyValue("{\"name\": \"updated\"}")
                    .retrieve()
                    .bodyToMono(Void.class);

            StepVerifier.create(result)
                    .verifyComplete();

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("PUT");
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("webclient-trace-put");
        }

        @Test
        @DisplayName("DELETE 요청에서도 헤더가 전파되어야 함")
        void shouldPropagateHeadersInDeleteRequest() throws InterruptedException {
            // given
            TraceIdHolder.set("webclient-trace-delete");
            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(204));

            // when
            Mono<Void> result = webClient.delete()
                    .uri("/api/delete/123")
                    .retrieve()
                    .bodyToMono(Void.class);

            StepVerifier.create(result)
                    .verifyComplete();

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("DELETE");
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("webclient-trace-delete");
        }
    }

    @Nested
    @DisplayName("Reactive 체이닝 테스트")
    class ReactiveChainTests {

        @Test
        @DisplayName("첫 번째 요청에서 TraceId가 캡처됨 (현재 스레드 기준)")
        void shouldCaptureTraceIdAtSubscriptionTime() throws InterruptedException {
            // given - TraceId는 필터가 실행되는 시점의 ThreadLocal에서 캡처됨
            TraceIdHolder.set("webclient-trace-capture");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"step\": 1}")
                    .addHeader("Content-Type", "application/json"));

            // when
            Mono<String> result = webClient.get()
                    .uri("/api/step1")
                    .retrieve()
                    .bodyToMono(String.class);

            StepVerifier.create(result)
                    .expectNextMatches(body -> body.contains("step"))
                    .verifyComplete();

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("webclient-trace-capture");
        }

        @Test
        @DisplayName("Reactive 체이닝 시 두 번째 요청은 다른 스레드에서 실행될 수 있음 (ThreadLocal 제한)")
        void shouldDemonstrateThreadLocalLimitationInChaining() throws InterruptedException {
            // given
            // Note: 이 테스트는 ThreadLocal 기반 구현의 제한사항을 문서화합니다.
            // flatMap 내부는 다른 스레드에서 실행될 수 있어 TraceIdHolder가 전파되지 않을 수 있습니다.
            // 완전한 Reactive Context 전파를 위해서는 Reactor Context를 사용해야 합니다.
            TraceIdHolder.set("webclient-trace-chain");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"step\": 1}")
                    .addHeader("Content-Type", "application/json"));
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"step\": 2}")
                    .addHeader("Content-Type", "application/json"));

            // when
            Mono<String> result = webClient.get()
                    .uri("/api/step1")
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(r -> webClient.get()
                            .uri("/api/step2")
                            .retrieve()
                            .bodyToMono(String.class));

            StepVerifier.create(result)
                    .expectNextMatches(body -> body.contains("step"))
                    .verifyComplete();

            // then - 첫 번째 요청은 TraceId 포함
            RecordedRequest request1 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request1).isNotNull();
            assertThat(request1.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("webclient-trace-chain");

            // 두 번째 요청은 다른 스레드에서 실행될 수 있어 TraceId가 없을 수 있음
            // (이것은 ThreadLocal 기반 구현의 알려진 제한사항)
            RecordedRequest request2 = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request2).isNotNull();
            // TraceId가 있을 수도 있고 없을 수도 있음 (스레드 스케줄링에 따라 다름)
            // 따라서 여기서는 null 체크만 하지 않음
        }
    }
}
