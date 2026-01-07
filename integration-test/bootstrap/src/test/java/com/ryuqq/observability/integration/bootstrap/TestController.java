package com.ryuqq.observability.integration.bootstrap;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.logging.annotation.Loggable;
import com.ryuqq.observability.logging.annotation.Loggable.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Bootstrap 통합 테스트용 컨트롤러.
 */
@RestController
@RequestMapping("/api")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    /**
     * TraceId 검증용 엔드포인트.
     */
    @GetMapping("/trace")
    public Map<String, String> getTrace() {
        log.info("Trace endpoint called");
        Map<String, String> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("mdcTraceId", MDC.get("traceId"));
        result.put("userId", TraceIdHolder.getUserId());
        result.put("tenantId", TraceIdHolder.getTenantId());
        result.put("organizationId", TraceIdHolder.getOrganizationId());
        return result;
    }

    /**
     * @Loggable 어노테이션 테스트용 엔드포인트.
     */
    @GetMapping("/loggable")
    @Loggable(level = LogLevel.INFO, includeArgs = true, includeResult = true)
    public Map<String, Object> loggableEndpoint(@RequestParam(value = "name", defaultValue = "test") String name) {
        log.info("Loggable endpoint called with name: {}", name);
        Map<String, Object> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("greeting", "Hello, " + name + "!");
        return result;
    }

    /**
     * 마스킹 테스트용 엔드포인트.
     */
    @PostMapping("/masking")
    @Loggable(level = LogLevel.INFO, includeArgs = true, includeResult = true)
    public Map<String, String> maskingEndpoint(@RequestBody Map<String, String> request) {
        log.info("Masking endpoint called with request: {}", request);
        Map<String, String> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("message", "Data received");
        return result;
    }

    /**
     * 느린 요청 테스트용 엔드포인트.
     */
    @GetMapping("/slow")
    @Loggable(level = LogLevel.WARN, slowThreshold = 50, includeExecutionTime = true)
    public Map<String, String> slowEndpoint() throws InterruptedException {
        log.info("Slow endpoint called");
        Thread.sleep(100); // 100ms delay
        Map<String, String> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("message", "slow response");
        return result;
    }

    /**
     * 에러 응답 테스트용 엔드포인트.
     */
    @GetMapping("/error")
    @Loggable(level = LogLevel.INFO, errorLevel = LogLevel.ERROR)
    public ResponseEntity<Map<String, String>> errorEndpoint() {
        log.error("Error endpoint called");
        Map<String, String> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("error", "Something went wrong");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 경로 파라미터 테스트용 엔드포인트.
     */
    @GetMapping("/users/{userId}/orders/{orderId}")
    public Map<String, String> complexPath(
            @PathVariable("userId") String userId,
            @PathVariable("orderId") String orderId) {
        log.info("Complex path endpoint called: userId={}, orderId={}", userId, orderId);
        Map<String, String> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("userId", userId);
        result.put("orderId", orderId);
        return result;
    }

    /**
     * Health check (로깅 제외 대상).
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "UP");
        return result;
    }
}
