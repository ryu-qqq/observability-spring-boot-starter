package com.ryuqq.observability.integration.restapi;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 통합 테스트용 컨트롤러.
 */
@RestController
@RequestMapping("/api")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    /**
     * 기본 GET 요청 테스트.
     */
    @GetMapping("/test")
    public Map<String, String> test() {
        log.info("Test endpoint called");
        Map<String, String> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("mdcTraceId", MDC.get("traceId"));
        result.put("message", "success");
        return result;
    }

    /**
     * POST 요청 + Body 테스트.
     */
    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
        log.info("Echo endpoint called with body: {}", body);
        Map<String, Object> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("echo", body);
        return result;
    }

    /**
     * 경로 파라미터 테스트 (정규화 검증용).
     */
    @GetMapping("/users/{userId}")
    public Map<String, String> getUser(@PathVariable("userId") String userId) {
        log.info("GetUser endpoint called for userId: {}", userId);
        Map<String, String> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("userId", userId);
        return result;
    }

    /**
     * 사용자 컨텍스트 헤더 검증.
     */
    @GetMapping("/context")
    public Map<String, String> getContext() {
        log.info("Context endpoint called");
        Map<String, String> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("userId", TraceIdHolder.getUserId());
        result.put("tenantId", TraceIdHolder.getTenantId());
        result.put("organizationId", TraceIdHolder.getOrganizationId());
        result.put("userRoles", TraceIdHolder.getUserRoles());
        return result;
    }

    /**
     * 느린 요청 테스트.
     */
    @GetMapping("/slow")
    public Map<String, String> slow() throws InterruptedException {
        log.info("Slow endpoint called");
        Thread.sleep(100); // 100ms delay
        Map<String, String> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("message", "slow response");
        return result;
    }

    /**
     * 에러 응답 테스트.
     */
    @GetMapping("/error")
    public ResponseEntity<Map<String, String>> error() {
        log.info("Error endpoint called");
        Map<String, String> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("error", "Something went wrong");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 4xx 에러 테스트.
     */
    @GetMapping("/bad-request")
    public ResponseEntity<Map<String, String>> badRequest() {
        log.info("Bad request endpoint called");
        Map<String, String> result = new HashMap<>();
        result.put("traceId", TraceIdHolder.get());
        result.put("error", "Bad request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
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
