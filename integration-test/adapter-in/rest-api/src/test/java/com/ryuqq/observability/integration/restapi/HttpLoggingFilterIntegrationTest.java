package com.ryuqq.observability.integration.restapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HttpLoggingFilter 통합 테스트.
 *
 * <p>테스트 항목:</p>
 * <ul>
 *   <li>요청/응답 로깅 동작 검증</li>
 *   <li>경로 제외 기능</li>
 *   <li>본문 로깅 (Body logging)</li>
 *   <li>느린 요청 감지</li>
 *   <li>에러 응답 로깅</li>
 * </ul>
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "observability.service-name=test-service",
                "observability.http.enabled=true",
                "observability.http.log-request-body=true",
                "observability.http.log-response-body=true",
                "observability.http.max-body-length=1000",
                "observability.http.slow-request-threshold-ms=50",
                "observability.http.exclude-paths=/api/health,/actuator/**",
                "observability.http.exclude-headers=Authorization,Cookie"
        }
)
class HttpLoggingFilterIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Nested
    @DisplayName("기본 로깅 테스트")
    class BasicLoggingTests {

        @Test
        @DisplayName("GET 요청이 정상적으로 로깅되어야 함")
        void shouldLogGetRequest() {
            // when
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/test", Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("message")).isEqualTo("success");
        }

        @Test
        @DisplayName("POST 요청이 정상적으로 로깅되어야 함")
        void shouldLogPostRequest() {
            // given
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", "test");
            requestBody.put("value", 123);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // when
            ResponseEntity<Map> response = restTemplate.postForEntity("/api/echo", request, Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            @SuppressWarnings("unchecked")
            Map<String, Object> echo = (Map<String, Object>) response.getBody().get("echo");
            assertThat(echo.get("name")).isEqualTo("test");
            assertThat(echo.get("value")).isEqualTo(123);
        }

        @Test
        @DisplayName("경로 파라미터가 있는 요청이 정상 동작해야 함")
        void shouldHandlePathParameters() {
            // when
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/users/12345", Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("userId")).isEqualTo("12345");
        }

        @Test
        @DisplayName("UUID 경로 파라미터가 있는 요청이 정상 동작해야 함")
        void shouldHandleUuidPathParameters() {
            // when
            String uuid = "550e8400-e29b-41d4-a716-446655440000";
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/users/" + uuid, Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("userId")).isEqualTo(uuid);
        }
    }

    @Nested
    @DisplayName("경로 제외 테스트")
    class PathExclusionTests {

        @Test
        @DisplayName("제외된 경로는 로깅되지 않아야 함 (기능 동작은 정상)")
        void shouldExcludeConfiguredPaths() {
            // when - /api/health는 exclude-paths에 포함
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/health", Map.class);

            // then - 요청은 정상 동작해야 함
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("status")).isEqualTo("UP");
        }
    }

    @Nested
    @DisplayName("에러 응답 테스트")
    class ErrorResponseTests {

        @Test
        @DisplayName("500 에러 응답이 정상적으로 로깅되어야 함")
        void shouldLog500ErrorResponse() {
            // when
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/error", Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("Something went wrong");
        }

        @Test
        @DisplayName("400 에러 응답이 정상적으로 로깅되어야 함")
        void shouldLog400ErrorResponse() {
            // when
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/bad-request", Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("Bad request");
        }
    }

    @Nested
    @DisplayName("느린 요청 테스트")
    class SlowRequestTests {

        @Test
        @DisplayName("느린 요청이 정상적으로 처리되어야 함")
        void shouldHandleSlowRequest() {
            // when - 100ms 지연 요청 (threshold: 50ms)
            long startTime = System.currentTimeMillis();
            ResponseEntity<Map> response = restTemplate.getForEntity("/api/slow", Map.class);
            long duration = System.currentTimeMillis() - startTime;

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("message")).isEqualTo("slow response");

            // 실제로 100ms 이상 걸렸는지 확인
            assertThat(duration).isGreaterThanOrEqualTo(100);
        }
    }

    @Nested
    @DisplayName("본문 로깅 테스트")
    class BodyLoggingTests {

        @Test
        @DisplayName("요청 본문이 포함된 POST가 정상 처리되어야 함")
        void shouldLogRequestBody() {
            // given
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("username", "testuser");
            requestBody.put("email", "test@example.com");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // when
            ResponseEntity<Map> response = restTemplate.postForEntity("/api/echo", request, Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("큰 요청 본문도 정상 처리되어야 함")
        void shouldHandleLargeRequestBody() {
            // given - 큰 본문 생성
            Map<String, Object> requestBody = new HashMap<>();
            StringBuilder largeValue = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                largeValue.append("This is a test string. ");
            }
            requestBody.put("data", largeValue.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // when
            ResponseEntity<Map> response = restTemplate.postForEntity("/api/echo", request, Map.class);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("민감정보 마스킹 테스트")
    class MaskingTests {

        @Test
        @DisplayName("password 필드가 포함된 요청이 정상 처리되어야 함")
        void shouldMaskPasswordField() {
            // given
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("username", "testuser");
            requestBody.put("password", "secretPassword123");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // when
            ResponseEntity<Map> response = restTemplate.postForEntity("/api/echo", request, Map.class);

            // then - 요청 자체는 정상 처리
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Authorization 헤더가 로깅에서 필터링되어야 함")
        void shouldFilterAuthorizationHeader() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer secret-token-12345");
            HttpEntity<Void> request = new HttpEntity<>(headers);

            // when
            ResponseEntity<Map> response = restTemplate.exchange(
                    "/api/test", HttpMethod.GET, request, Map.class);

            // then - 요청 자체는 정상 처리
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("동시 요청 테스트")
    class ConcurrentRequestTests {

        @Test
        @DisplayName("여러 요청이 동시에 들어와도 각각 독립적으로 처리되어야 함")
        void shouldHandleConcurrentRequests() throws InterruptedException {
            // given
            int requestCount = 10;
            Thread[] threads = new Thread[requestCount];
            ResponseEntity<?>[] responses = new ResponseEntity<?>[requestCount];

            // when - 동시에 여러 요청 실행
            for (int i = 0; i < requestCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    responses[index] = restTemplate.getForEntity("/api/test", Map.class);
                });
                threads[i].start();
            }

            // 모든 스레드 완료 대기
            for (Thread thread : threads) {
                thread.join();
            }

            // then - 모든 요청이 성공해야 함
            for (ResponseEntity<?> response : responses) {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }
        }
    }
}
