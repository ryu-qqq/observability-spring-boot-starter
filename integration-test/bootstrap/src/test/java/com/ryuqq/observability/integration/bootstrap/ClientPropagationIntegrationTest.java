package com.ryuqq.observability.integration.bootstrap;

import com.ryuqq.observability.client.rest.TraceIdRestTemplateInterceptor;
import com.ryuqq.observability.client.webclient.TraceIdExchangeFilterFunction;
import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HTTP Client TraceId 전파 통합 테스트.
 *
 * <p>observability-starter가 제공하는 인터셉터/필터가
 * RestTemplate과 WebClient에서 올바르게 동작하는지 검증합니다.</p>
 */
@SpringBootTest(classes = BootstrapTestApplication.class)
class ClientPropagationIntegrationTest {

    @Autowired
    private TraceIdRestTemplateInterceptor restTemplateInterceptor;

    @Autowired
    private TraceIdExchangeFilterFunction webClientFilterFunction;

    private MockWebServer mockWebServer;
    private RestTemplate restTemplate;
    private WebClient webClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // 주입받은 인터셉터를 사용하여 RestTemplate 구성
        restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(restTemplateInterceptor);

        // 주입받은 필터 함수를 사용하여 WebClient 구성
        webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .filter(webClientFilterFunction)
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        TraceIdHolder.clear();
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("RestTemplate TraceId 전파")
    class RestTemplateTests {

        @Test
        @DisplayName("RestTemplate이 TraceId를 헤더로 전파해야 함")
        void shouldPropagateTraceIdViaRestTemplate() throws InterruptedException {
            // given
            String traceId = "bootstrap-rest-trace-123";
            TraceIdHolder.set(traceId);
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            String url = mockWebServer.url("/api/external").toString();
            restTemplate.getForEntity(url, String.class);

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo(traceId);
        }

        @Test
        @DisplayName("RestTemplate이 사용자 컨텍스트를 헤더로 전파해야 함")
        void shouldPropagateUserContextViaRestTemplate() throws InterruptedException {
            // given
            TraceIdHolder.set("context-trace-123");
            TraceIdHolder.setUserId("user-bootstrap");
            TraceIdHolder.setTenantId("tenant-bootstrap");
            TraceIdHolder.setOrganizationId("org-bootstrap");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            String url = mockWebServer.url("/api/external").toString();
            restTemplate.getForEntity(url, String.class);

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("context-trace-123");
            assertThat(request.getHeader(TraceIdHeaders.X_USER_ID)).isEqualTo("user-bootstrap");
            assertThat(request.getHeader(TraceIdHeaders.X_TENANT_ID)).isEqualTo("tenant-bootstrap");
            assertThat(request.getHeader(TraceIdHeaders.X_ORGANIZATION_ID)).isEqualTo("org-bootstrap");
        }

        @Test
        @DisplayName("TraceIdHolder가 비어있으면 헤더가 전파되지 않아야 함")
        void shouldNotPropagateWhenTraceIdHolderEmpty() throws InterruptedException {
            // given - TraceIdHolder is empty
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            String url = mockWebServer.url("/api/external").toString();
            restTemplate.getForEntity(url, String.class);

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isNull();
        }
    }

    @Nested
    @DisplayName("WebClient TraceId 전파")
    class WebClientTests {

        @Test
        @DisplayName("WebClient가 TraceId를 헤더로 전파해야 함")
        void shouldPropagateTraceIdViaWebClient() throws InterruptedException {
            // given
            String traceId = "bootstrap-webclient-trace-123";
            TraceIdHolder.set(traceId);
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            StepVerifier.create(webClient.get()
                            .uri("/api/external")
                            .retrieve()
                            .bodyToMono(String.class))
                    .expectNextMatches(body -> body.contains("success"))
                    .verifyComplete();

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo(traceId);
        }

        @Test
        @DisplayName("WebClient가 사용자 컨텍스트를 헤더로 전파해야 함")
        void shouldPropagateUserContextViaWebClient() throws InterruptedException {
            // given
            TraceIdHolder.set("webclient-context-trace-123");
            TraceIdHolder.setUserId("user-webclient");
            TraceIdHolder.setTenantId("tenant-webclient");
            TraceIdHolder.setOrganizationId("org-webclient");
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            StepVerifier.create(webClient.get()
                            .uri("/api/external")
                            .retrieve()
                            .bodyToMono(String.class))
                    .expectNextCount(1)
                    .verifyComplete();

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isEqualTo("webclient-context-trace-123");
            assertThat(request.getHeader(TraceIdHeaders.X_USER_ID)).isEqualTo("user-webclient");
            assertThat(request.getHeader(TraceIdHeaders.X_TENANT_ID)).isEqualTo("tenant-webclient");
            assertThat(request.getHeader(TraceIdHeaders.X_ORGANIZATION_ID)).isEqualTo("org-webclient");
        }

        @Test
        @DisplayName("WebClient에서 TraceIdHolder가 비어있으면 헤더가 전파되지 않아야 함")
        void shouldNotPropagateWhenTraceIdHolderEmptyInWebClient() throws InterruptedException {
            // given - TraceIdHolder is empty
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"success\": true}")
                    .addHeader("Content-Type", "application/json"));

            // when
            StepVerifier.create(webClient.get()
                            .uri("/api/external")
                            .retrieve()
                            .bodyToMono(String.class))
                    .expectNextCount(1)
                    .verifyComplete();

            // then
            RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getHeader(TraceIdHeaders.X_TRACE_ID)).isNull();
        }
    }

    @Nested
    @DisplayName("AutoConfiguration Bean 주입 검증")
    class BeanInjectionTests {

        @Test
        @DisplayName("TraceIdRestTemplateInterceptor가 정상 주입되어야 함")
        void shouldInjectRestTemplateInterceptor() {
            assertThat(restTemplateInterceptor).isNotNull();
        }

        @Test
        @DisplayName("TraceIdExchangeFilterFunction이 정상 주입되어야 함")
        void shouldInjectWebClientFilterFunction() {
            assertThat(webClientFilterFunction).isNotNull();
        }
    }
}
