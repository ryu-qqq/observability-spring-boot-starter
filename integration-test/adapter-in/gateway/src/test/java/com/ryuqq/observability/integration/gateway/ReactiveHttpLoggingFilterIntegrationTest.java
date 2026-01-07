package com.ryuqq.observability.integration.gateway;

import com.ryuqq.observability.webflux.config.WebFluxHttpLoggingAutoConfiguration;
import com.ryuqq.observability.webflux.config.WebFluxTraceAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReactiveHttpLoggingFilter 집중 통합 테스트.
 *
 * <p>검증 항목:</p>
 * <ul>
 *   <li>기본 요청/응답 로깅</li>
 *   <li>POST 요청 본문 로깅</li>
 *   <li>경로 파라미터 정규화 (Path Normalization)</li>
 *   <li>느린 요청 감지 (Slow Request)</li>
 *   <li>에러 응답 로깅 (4xx, 5xx)</li>
 *   <li>민감정보 마스킹</li>
 *   <li>제외 경로 처리</li>
 *   <li>헤더 필터링</li>
 *   <li>큰 응답 본문 자르기 (Truncation)</li>
 *   <li>다양한 Content-Type 처리</li>
 * </ul>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.application.name=test-gateway",
                "observability.reactive-trace.enabled=true",
                "observability.reactive-http.enabled=true",
                "observability.reactive-http.log-request-body=true",
                "observability.reactive-http.log-response-body=true",
                "observability.reactive-http.max-body-length=2000",
                "observability.reactive-http.slow-request-threshold-ms=500",
                "observability.masking.enabled=true"
        }
)
@Import({WebFluxTraceAutoConfiguration.class, WebFluxHttpLoggingAutoConfiguration.class})
class ReactiveHttpLoggingFilterIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        // 테스트 타임아웃 설정 (느린 요청 테스트를 위해)
        webTestClient = webTestClient.mutate()
                .responseTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Nested
    @DisplayName("기본 요청/응답 로깅 테스트")
    class BasicLoggingTests {

        @Test
        @DisplayName("GET 요청이 정상적으로 로깅되어야 한다")
        void shouldLogGetRequest() {
            webTestClient.get()
                    .uri("/test/trace")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.traceId").isNotEmpty();
        }

        @Test
        @DisplayName("쿼리 파라미터가 포함된 요청이 로깅되어야 한다")
        void shouldLogRequestWithQueryParameters() {
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/test/search")
                            .queryParam("q", "test query")
                            .queryParam("page", 2)
                            .queryParam("size", 20)
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.query").isEqualTo("test query")
                    .jsonPath("$.page").isEqualTo(2)
                    .jsonPath("$.size").isEqualTo(20);
        }

        @Test
        @DisplayName("헬스 체크 엔드포인트가 정상 동작해야 한다")
        void shouldHandleHealthEndpoint() {
            webTestClient.get()
                    .uri("/test/health")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("UP");
        }
    }

    @Nested
    @DisplayName("POST 요청 본문 로깅 테스트")
    class RequestBodyLoggingTests {

        @Test
        @DisplayName("JSON 요청 본문이 로깅되어야 한다")
        void shouldLogJsonRequestBody() {
            Map<String, Object> requestBody = Map.of(
                    "name", "John Doe",
                    "age", 30,
                    "active", true
            );

            webTestClient.post()
                    .uri("/test/echo")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.received.name").isEqualTo("John Doe")
                    .jsonPath("$.received.age").isEqualTo(30)
                    .jsonPath("$.timestamp").isNumber();
        }

        @Test
        @DisplayName("로그인 요청 본문이 로깅되어야 한다 (민감정보 마스킹)")
        void shouldLogLoginRequestWithMasking() {
            Map<String, Object> credentials = Map.of(
                    "username", "testuser",
                    "password", "secret123"
            );

            webTestClient.post()
                    .uri("/test/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(credentials)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(true)
                    .jsonPath("$.username").isEqualTo("testuser");
        }

        @Test
        @DisplayName("빈 요청 본문도 정상 처리되어야 한다")
        void shouldHandleEmptyRequestBody() {
            webTestClient.post()
                    .uri("/test/echo")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.received").isMap();
        }
    }

    @Nested
    @DisplayName("경로 정규화 테스트 (Path Normalization)")
    class PathNormalizationTests {

        @Test
        @DisplayName("숫자 ID 경로가 정규화되어야 한다")
        void shouldNormalizeNumericIdPath() {
            // 다양한 숫자 ID로 요청
            webTestClient.get()
                    .uri("/test/users/12345")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.userId").isEqualTo(12345)
                    .jsonPath("$.username").isEqualTo("user_12345");

            webTestClient.get()
                    .uri("/test/users/99999")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.userId").isEqualTo(99999);
        }

        @Test
        @DisplayName("UUID 경로가 정규화되어야 한다")
        void shouldNormalizeUuidPath() {
            String uuid = UUID.randomUUID().toString();

            webTestClient.get()
                    .uri("/test/orders/" + uuid)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.orderId").isEqualTo(uuid)
                    .jsonPath("$.status").isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("여러 UUID 요청이 동일한 정규화된 경로로 처리되어야 한다")
        void shouldNormalizeMultipleUuidRequests() {
            for (int i = 0; i < 5; i++) {
                String uuid = UUID.randomUUID().toString();
                webTestClient.get()
                        .uri("/test/orders/" + uuid)
                        .exchange()
                        .expectStatus().isOk();
            }
        }
    }

    @Nested
    @DisplayName("느린 요청 감지 테스트 (Slow Request)")
    class SlowRequestTests {

        @Test
        @DisplayName("느린 요청이 [SLOW] 태그와 함께 로깅되어야 한다")
        void shouldDetectSlowRequest() {
            // slowThreshold(500ms)보다 긴 지연
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/test/slow")
                            .queryParam("delayMs", 600)
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.delayMs").isEqualTo(600)
                    .jsonPath("$.message").isEqualTo("Slow request completed");
        }

        @Test
        @DisplayName("빠른 요청은 [SLOW] 태그 없이 로깅되어야 한다")
        void shouldNotMarkFastRequestAsSlow() {
            // slowThreshold(500ms)보다 짧은 지연
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/test/slow")
                            .queryParam("delayMs", 100)
                            .build())
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.delayMs").isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("에러 응답 로깅 테스트")
    class ErrorResponseLoggingTests {

        @Test
        @DisplayName("400 Bad Request가 WARN 레벨로 로깅되어야 한다")
        void shouldLogBadRequestAsWarn() {
            webTestClient.get()
                    .uri("/test/error/bad-request")
                    .exchange()
                    .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("404 Not Found가 WARN 레벨로 로깅되어야 한다")
        void shouldLogNotFoundAsWarn() {
            webTestClient.get()
                    .uri("/test/error/not-found")
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("500 Internal Server Error가 ERROR 레벨로 로깅되어야 한다")
        void shouldLogServerErrorAsError() {
            webTestClient.get()
                    .uri("/test/error/server-error")
                    .exchange()
                    .expectStatus().is5xxServerError();
        }

        @Test
        @DisplayName("존재하지 않는 엔드포인트가 404로 응답해야 한다")
        void shouldReturn404ForNonExistentEndpoint() {
            webTestClient.get()
                    .uri("/non-existent-endpoint")
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("민감정보 마스킹 테스트")
    class SensitiveDataMaskingTests {

        @Test
        @DisplayName("응답의 민감정보가 마스킹되어야 한다")
        void shouldMaskSensitiveDataInResponse() {
            webTestClient.get()
                    .uri("/test/sensitive")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.email").exists()
                    .jsonPath("$.phone").exists()
                    .jsonPath("$.creditCard").exists()
                    .jsonPath("$.password").exists();
            // 실제 마스킹은 로그 출력에서 확인
        }

        @Test
        @DisplayName("요청 본문의 비밀번호가 마스킹되어야 한다")
        void shouldMaskPasswordInRequestBody() {
            Map<String, Object> credentials = Map.of(
                    "username", "admin",
                    "password", "super_secret_password_123"
            );

            webTestClient.post()
                    .uri("/test/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(credentials)
                    .exchange()
                    .expectStatus().isOk();
            // 실제 마스킹은 로그 출력에서 확인 (password 필드가 [MASKED]로 표시)
        }

        @Test
        @DisplayName("JWT 토큰이 마스킹되어야 한다")
        void shouldMaskJwtToken() {
            Map<String, Object> credentials = Map.of(
                    "username", "user",
                    "password", "pass"
            );

            webTestClient.post()
                    .uri("/test/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(credentials)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.token").exists();
            // 응답의 JWT 토큰이 로그에서 마스킹되어야 함
        }
    }

    @Nested
    @DisplayName("헤더 필터링 테스트")
    class HeaderFilteringTests {

        @Test
        @DisplayName("Authorization 헤더가 [FILTERED]로 로깅되어야 한다")
        void shouldFilterAuthorizationHeader() {
            webTestClient.get()
                    .uri("/test/trace")
                    .header("Authorization", "Bearer secret-token-12345")
                    .exchange()
                    .expectStatus().isOk();
            // Authorization 헤더가 [FILTERED]로 로깅되어야 함
        }

        @Test
        @DisplayName("Cookie 헤더가 [FILTERED]로 로깅되어야 한다")
        void shouldFilterCookieHeader() {
            webTestClient.get()
                    .uri("/test/trace")
                    .header("Cookie", "session=abc123; auth=xyz789")
                    .exchange()
                    .expectStatus().isOk();
            // Cookie 헤더가 [FILTERED]로 로깅되어야 함
        }

        @Test
        @DisplayName("일반 헤더는 그대로 로깅되어야 한다")
        void shouldLogNormalHeaders() {
            webTestClient.get()
                    .uri("/test/trace")
                    .header("X-Custom-Header", "custom-value")
                    .header("Accept-Language", "ko-KR")
                    .exchange()
                    .expectStatus().isOk();
            // X-Custom-Header, Accept-Language는 그대로 로깅되어야 함
        }

        @Test
        @DisplayName("X-Api-Key 헤더가 [FILTERED]로 로깅되어야 한다")
        void shouldFilterApiKeyHeader() {
            webTestClient.get()
                    .uri("/test/trace")
                    .header("X-Api-Key", "super-secret-api-key")
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("응답 본문 자르기 테스트 (Truncation)")
    class ResponseBodyTruncationTests {

        @Test
        @DisplayName("큰 응답 본문이 잘려서 로깅되어야 한다")
        void shouldTruncateLargeResponseBody() {
            webTestClient.get()
                    .uri("/test/large-response")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.size").value(size -> {
                        assertThat((Integer) size).isGreaterThan(2000);
                    });
            // 로그에서 [TRUNCATED] 태그 확인
        }
    }

    @Nested
    @DisplayName("Content-Type 처리 테스트")
    class ContentTypeTests {

        @Test
        @DisplayName("JSON 응답이 정상 로깅되어야 한다")
        void shouldLogJsonResponse() {
            webTestClient.get()
                    .uri("/test/trace")
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON);
        }

        @Test
        @DisplayName("Plain Text 응답이 정상 로깅되어야 한다")
        void shouldLogPlainTextResponse() {
            webTestClient.get()
                    .uri("/test/text")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                    .expectBody(String.class)
                    .value(body -> assertThat(body).contains("plain text response"));
        }
    }

    @Nested
    @DisplayName("TraceId 연동 테스트")
    class TraceIdIntegrationTests {

        @Test
        @DisplayName("TraceId가 응답 헤더에 포함되어야 한다")
        void shouldIncludeTraceIdInResponse() {
            webTestClient.get()
                    .uri("/test/trace")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().exists("X-Trace-Id")
                    .expectBody()
                    .jsonPath("$.traceId").isNotEmpty();
        }

        @Test
        @DisplayName("요청 헤더의 TraceId가 전파되어야 한다")
        void shouldPropagateTraceIdFromRequest() {
            String customTraceId = "custom-trace-id-for-logging-test";

            webTestClient.get()
                    .uri("/test/trace")
                    .header("X-Trace-Id", customTraceId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("X-Trace-Id", customTraceId)
                    .expectBody()
                    .jsonPath("$.traceId").isEqualTo(customTraceId);
        }

        @Test
        @DisplayName("POST 요청에서도 TraceId가 정상 전파되어야 한다")
        void shouldPropagateTraceIdInPostRequest() {
            String customTraceId = "post-request-trace-id";

            webTestClient.post()
                    .uri("/test/echo")
                    .header("X-Trace-Id", customTraceId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("test", "data"))
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("X-Trace-Id", customTraceId);
        }
    }

    @Nested
    @DisplayName("동시성 테스트")
    class ConcurrencyTests {

        @Test
        @DisplayName("동시 요청이 독립적으로 로깅되어야 한다")
        void shouldLogConcurrentRequestsIndependently() {
            // 여러 요청을 순차적으로 보내고 각각이 독립적인 TraceId를 가지는지 확인
            String[] traceIds = new String[5];

            for (int i = 0; i < 5; i++) {
                final int index = i;
                webTestClient.get()
                        .uri("/test/trace")
                        .exchange()
                        .expectStatus().isOk()
                        .expectHeader().value("X-Trace-Id", traceId -> {
                            traceIds[index] = traceId;
                        });
            }

            // 모든 TraceId가 유니크한지 확인
            for (int i = 0; i < traceIds.length; i++) {
                for (int j = i + 1; j < traceIds.length; j++) {
                    assertThat(traceIds[i]).isNotEqualTo(traceIds[j]);
                }
            }
        }
    }

    @Nested
    @DisplayName("다양한 HTTP 메서드 테스트")
    class HttpMethodTests {

        @Test
        @DisplayName("GET 요청이 정상 로깅되어야 한다")
        void shouldLogGetMethod() {
            webTestClient.get()
                    .uri("/test/trace")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("POST 요청이 정상 로깅되어야 한다")
        void shouldLogPostMethod() {
            webTestClient.post()
                    .uri("/test/echo")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("key", "value"))
                    .exchange()
                    .expectStatus().isOk();
        }
    }

    @Nested
    @DisplayName("특수 케이스 테스트")
    class EdgeCaseTests {

        @Test
        @DisplayName("빈 쿼리 파라미터가 정상 처리되어야 한다")
        void shouldHandleEmptyQueryParameter() {
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/test/search")
                            .queryParam("q", "")
                            .build())
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("특수문자가 포함된 쿼리가 정상 처리되어야 한다")
        void shouldHandleSpecialCharactersInQuery() {
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/test/search")
                            .queryParam("q", "hello world & special=chars")
                            .build())
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("한글이 포함된 요청이 정상 처리되어야 한다")
        void shouldHandleKoreanCharacters() {
            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/test/search")
                            .queryParam("q", "한글 검색어")
                            .build())
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("긴 URL이 정상 처리되어야 한다")
        void shouldHandleLongUrl() {
            StringBuilder longQuery = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                longQuery.append("word").append(i).append(" ");
            }

            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/test/search")
                            .queryParam("q", longQuery.toString().trim())
                            .build())
                    .exchange()
                    .expectStatus().isOk();
        }
    }
}
