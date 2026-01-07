package com.ryuqq.observability.integration.gateway;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.webflux.trace.ReactiveTraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * WebFlux 통합 테스트용 컨트롤러.
 *
 * <p>HTTP 로깅 테스트를 위한 다양한 엔드포인트를 제공합니다:</p>
 * <ul>
 *   <li>기본 GET/POST 요청</li>
 *   <li>경로 파라미터가 있는 요청 (Path Normalization 테스트)</li>
 *   <li>느린 요청 (Slow Request 테스트)</li>
 *   <li>에러 응답 (4xx, 5xx)</li>
 *   <li>민감정보 포함 응답 (Masking 테스트)</li>
 * </ul>
 */
@RestController
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    /**
     * TraceId와 MDC 값을 반환하는 엔드포인트.
     *
     * <p>Mono.deferContextual()을 사용하여 Reactor Context에서 값을 가져옵니다.
     * MdcContextLifter가 정상 동작하면 MDC에서도 값을 읽을 수 있습니다.</p>
     */
    @GetMapping("/test/trace")
    public Mono<Map<String, String>> getTrace() {
        return Mono.deferContextual(ctx -> {
            Map<String, String> result = new HashMap<>();

            // Reactor Context에서 직접 TraceId 확인
            String contextTraceId = ctx.getOrDefault(ReactiveTraceIdFilter.TRACE_ID_CONTEXT_KEY, null);
            result.put("traceId", contextTraceId != null ? contextTraceId : "null");

            // MDC에서 값 확인 (MdcContextLifter가 전파한 값)
            String mdcTraceId = MDC.get("traceId");
            result.put("mdcTraceId", mdcTraceId != null ? mdcTraceId : "null");

            // Reactor Context에서 서비스 이름 확인
            String serviceName = ctx.getOrDefault(ReactiveTraceIdFilter.SERVICE_NAME_CONTEXT_KEY, null);
            result.put("serviceName", serviceName != null ? serviceName : "null");

            log.info("Test endpoint called - traceId: {}, mdcTraceId: {}", contextTraceId, mdcTraceId);

            return Mono.just(result);
        });
    }

    /**
     * 지연 응답 엔드포인트 (비동기 처리 테스트용).
     *
     * <p>Mono.delay() 후에도 Context가 유지되는지 확인합니다.</p>
     */
    @GetMapping("/test/delay")
    public Mono<Map<String, String>> getDelayedTrace() {
        return Mono.delay(java.time.Duration.ofMillis(100))
                .flatMap(tick -> Mono.deferContextual(ctx -> {
                    Map<String, String> result = new HashMap<>();

                    String contextTraceId = ctx.getOrDefault(ReactiveTraceIdFilter.TRACE_ID_CONTEXT_KEY, null);
                    result.put("traceId", contextTraceId != null ? contextTraceId : "null");

                    String mdcTraceId = MDC.get("traceId");
                    result.put("mdcTraceId", mdcTraceId != null ? mdcTraceId : "null");

                    log.info("Delayed endpoint - traceId after delay: {}", contextTraceId);

                    return Mono.just(result);
                }));
    }

    /**
     * 간단한 헬스 체크 엔드포인트.
     */
    @GetMapping("/test/health")
    public Mono<Map<String, String>> health() {
        return Mono.just(Map.of("status", "UP"));
    }

    // ===== HTTP 로깅 테스트용 엔드포인트 =====

    /**
     * POST 요청 본문을 에코하는 엔드포인트.
     * Request Body 로깅 테스트용.
     */
    @PostMapping(value = "/test/echo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> echo(@RequestBody Map<String, Object> body) {
        log.info("Echo endpoint called with body: {}", body);
        Map<String, Object> response = new HashMap<>();
        response.put("received", body);
        response.put("timestamp", System.currentTimeMillis());
        return Mono.just(response);
    }

    /**
     * 경로 파라미터가 있는 엔드포인트.
     * Path Normalization 테스트용 (숫자 ID).
     */
    @GetMapping("/test/users/{userId}")
    public Mono<Map<String, Object>> getUserById(@PathVariable Long userId) {
        log.info("Get user by ID: {}", userId);
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("username", "user_" + userId);
        response.put("email", "user" + userId + "@example.com");
        return Mono.just(response);
    }

    /**
     * UUID 경로 파라미터 엔드포인트.
     * Path Normalization 테스트용 (UUID).
     */
    @GetMapping("/test/orders/{orderId}")
    public Mono<Map<String, Object>> getOrderById(@PathVariable String orderId) {
        log.info("Get order by ID: {}", orderId);
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("status", "COMPLETED");
        response.put("amount", 99.99);
        return Mono.just(response);
    }

    /**
     * 느린 요청 시뮬레이션 엔드포인트.
     * Slow Request 로깅 테스트용.
     *
     * @param delayMs 지연 시간 (밀리초). 기본값 4000ms (slowThreshold 초과)
     */
    @GetMapping("/test/slow")
    public Mono<Map<String, Object>> slowRequest(@RequestParam(defaultValue = "4000") long delayMs) {
        log.info("Slow request started with delay: {}ms", delayMs);
        return Mono.delay(Duration.ofMillis(delayMs))
                .map(tick -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("delayMs", delayMs);
                    response.put("message", "Slow request completed");
                    return response;
                });
    }

    /**
     * 400 Bad Request 에러 엔드포인트.
     * 4xx 에러 로깅 테스트용.
     */
    @GetMapping("/test/error/bad-request")
    public Mono<Map<String, Object>> badRequest() {
        log.warn("Bad request endpoint called");
        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request parameters"));
    }

    /**
     * 404 Not Found 에러 엔드포인트.
     * 4xx 에러 로깅 테스트용.
     */
    @GetMapping("/test/error/not-found")
    public Mono<Map<String, Object>> notFound() {
        log.warn("Not found endpoint called");
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));
    }

    /**
     * 500 Internal Server Error 엔드포인트.
     * 5xx 에러 로깅 테스트용.
     */
    @GetMapping("/test/error/server-error")
    public Mono<Map<String, Object>> serverError() {
        log.error("Server error endpoint called");
        return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
    }

    /**
     * 민감정보가 포함된 응답 엔드포인트.
     * Masking 테스트용.
     */
    @GetMapping("/test/sensitive")
    public Mono<Map<String, Object>> sensitiveData() {
        log.info("Sensitive data endpoint called");
        Map<String, Object> response = new HashMap<>();
        response.put("username", "john_doe");
        response.put("email", "john.doe@example.com");
        response.put("phone", "010-1234-5678");
        response.put("creditCard", "1234-5678-9012-3456");
        response.put("password", "secret123");
        response.put("ssn", "900101-1234567");
        return Mono.just(response);
    }

    /**
     * POST 요청에 민감정보가 포함된 엔드포인트.
     * Request Body Masking 테스트용.
     */
    @PostMapping(value = "/test/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> login(@RequestBody Map<String, Object> credentials) {
        log.info("Login endpoint called");
        String username = (String) credentials.get("username");
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("username", username);
        response.put("token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U");
        return Mono.just(response);
    }

    /**
     * 쿼리 파라미터 테스트 엔드포인트.
     * Query String 로깅 테스트용.
     */
    @GetMapping("/test/search")
    public Mono<Map<String, Object>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Search endpoint called: q={}, page={}, size={}", q, page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("query", q);
        response.put("page", page);
        response.put("size", size);
        response.put("totalResults", 42);
        return Mono.just(response);
    }

    /**
     * 큰 응답 본문 엔드포인트.
     * Response Body Truncation 테스트용.
     */
    @GetMapping("/test/large-response")
    public Mono<Map<String, Object>> largeResponse() {
        log.info("Large response endpoint called");
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            largeContent.append("This is line ").append(i).append(" of the large response content. ");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("content", largeContent.toString());
        response.put("size", largeContent.length());
        return Mono.just(response);
    }

    /**
     * Content-Type이 text/plain인 엔드포인트.
     * 다양한 Content-Type 로깅 테스트용.
     */
    @GetMapping(value = "/test/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> textResponse() {
        log.info("Text response endpoint called");
        return Mono.just("This is a plain text response for logging test.");
    }
}
