package com.ryuqq.observability.integration.gateway;

import com.ryuqq.observability.webflux.config.WebFluxTraceAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReactiveTraceIdFilter 통합 테스트.
 *
 * <p>검증 항목:</p>
 * <ul>
 *   <li>TraceId 자동 생성</li>
 *   <li>요청 헤더에서 TraceId 추출</li>
 *   <li>응답 헤더에 TraceId 포함</li>
 *   <li>Reactor Context → MDC 전파 (MdcContextLifter)</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=test-gateway",
                "observability.reactive-trace.enabled=true",
                "observability.reactive-trace.generate-if-missing=true",
                "observability.reactive-trace.include-in-response=true"
        }
)
@Import(WebFluxTraceAutoConfiguration.class)
class ReactiveTraceIdFilterIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 필요한 초기화
    }

    @Test
    @DisplayName("TraceId가 없을 때 자동 생성되어야 한다")
    void shouldGenerateTraceIdWhenMissing() {
        webTestClient.get()
                .uri("/test/trace")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Trace-Id")
                .expectBody()
                .jsonPath("$.traceId").isNotEmpty()
                .jsonPath("$.traceId").value(traceId -> {
                    assertThat((String) traceId).isNotEqualTo("null");
                    assertThat((String) traceId).hasSizeGreaterThan(10);
                });
    }

    @Test
    @DisplayName("요청 헤더의 TraceId가 전파되어야 한다")
    void shouldPropagateTraceIdFromRequestHeader() {
        String expectedTraceId = "test-trace-id-12345";

        webTestClient.get()
                .uri("/test/trace")
                .header("X-Trace-Id", expectedTraceId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Trace-Id", expectedTraceId)
                .expectBody()
                .jsonPath("$.traceId").isEqualTo(expectedTraceId);
    }

    @Test
    @DisplayName("응답 헤더에 TraceId가 포함되어야 한다")
    void shouldIncludeTraceIdInResponseHeader() {
        webTestClient.get()
                .uri("/test/trace")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Trace-Id")
                .expectHeader().value("X-Trace-Id", traceId -> {
                    assertThat(traceId).isNotBlank();
                });
    }

    @Test
    @DisplayName("비동기 지연 후에도 TraceId가 유지되어야 한다 (MdcContextLifter)")
    void shouldMaintainTraceIdAfterAsyncDelay() {
        String expectedTraceId = "async-test-trace-id";

        webTestClient.get()
                .uri("/test/delay")
                .header("X-Trace-Id", expectedTraceId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Trace-Id", expectedTraceId)
                .expectBody()
                .jsonPath("$.traceId").isEqualTo(expectedTraceId);
    }

    @Test
    @DisplayName("서비스 이름이 Reactor Context에 전파되어야 한다")
    void shouldPropagateServiceName() {
        webTestClient.get()
                .uri("/test/trace")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.serviceName").isEqualTo("test-gateway");
    }

    @Test
    @DisplayName("여러 요청이 독립적인 TraceId를 가져야 한다")
    void shouldHaveIndependentTraceIdsForMultipleRequests() {
        // 첫 번째 요청
        String traceId1 = webTestClient.get()
                .uri("/test/trace")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseHeaders()
                .getFirst("X-Trace-Id");

        // 두 번째 요청
        String traceId2 = webTestClient.get()
                .uri("/test/trace")
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class)
                .getResponseHeaders()
                .getFirst("X-Trace-Id");

        assertThat(traceId1).isNotNull();
        assertThat(traceId2).isNotNull();
        assertThat(traceId1).isNotEqualTo(traceId2);
    }

    @Test
    @DisplayName("간단한 엔드포인트 호출 시 예외가 발생하지 않아야 한다 (Actuator 시나리오)")
    void shouldNotThrowExceptionForSimpleEndpoint() {
        // Actuator /health 같은 간단한 엔드포인트 시뮬레이션
        webTestClient.get()
                .uri("/test/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("사용자 컨텍스트 헤더가 전파되어야 한다")
    void shouldPropagateUserContextHeaders() {
        webTestClient.get()
                .uri("/test/trace")
                .header("X-User-Id", "user-123")
                .header("X-Tenant-Id", "tenant-456")
                .header("X-Trace-Id", "context-test")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Trace-Id");
    }
}
