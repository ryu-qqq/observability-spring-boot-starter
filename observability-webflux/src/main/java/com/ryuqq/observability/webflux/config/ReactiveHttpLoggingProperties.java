package com.ryuqq.observability.webflux.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * WebFlux HTTP 요청/응답 로깅 설정.
 *
 * <p>Spring MVC의 HttpLoggingProperties와 동일한 설정을 지원하며,
 * WebFlux/Netty 환경에 최적화되어 있습니다.</p>
 *
 * <pre>
 * observability:
 *   reactive-http:
 *     enabled: true
 *     log-request-body: false
 *     log-response-body: false
 *     max-body-length: 1000
 *     exclude-paths:
 *       - /actuator/**
 *       - /health
 *     exclude-headers:
 *       - Authorization
 *       - Cookie
 *     slow-request-threshold-ms: 3000
 * </pre>
 */
@ConfigurationProperties(prefix = "observability.reactive-http")
public class ReactiveHttpLoggingProperties {

    /**
     * HTTP 로깅 활성화 여부
     */
    private boolean enabled = true;

    /**
     * 요청 본문 로깅 여부 (민감정보 주의).
     * WebFlux에서는 DataBuffer를 캐싱해야 하므로 메모리 사용량 증가에 유의.
     */
    private boolean logRequestBody = false;

    /**
     * 응답 본문 로깅 여부 (민감정보 주의).
     * WebFlux에서는 DataBuffer를 캐싱해야 하므로 메모리 사용량 증가에 유의.
     */
    private boolean logResponseBody = false;

    /**
     * 본문 로깅 시 최대 길이 (bytes).
     * 초과 시 잘림 처리.
     */
    private int maxBodyLength = 1000;

    /**
     * 로깅 제외 경로 패턴 (Ant 패턴 또는 PathPattern)
     */
    private List<String> excludePaths = new ArrayList<>(List.of(
            "/actuator/**",
            "/health",
            "/health/**",
            "/favicon.ico",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**"
    ));

    /**
     * 로깅 제외 헤더 (대소문자 무관).
     * 보안상 민감한 헤더는 기본으로 제외.
     */
    private List<String> excludeHeaders = new ArrayList<>(List.of(
            "Authorization",
            "Cookie",
            "Set-Cookie",
            "X-Api-Key",
            "Api-Key",
            "X-Auth-Token"
    ));

    /**
     * 경로 정규화 패턴 목록.
     * 메트릭 폭발 방지를 위해 동적 경로 파라미터를 일반화.
     */
    private List<PathPattern> pathPatterns = new ArrayList<>();

    /**
     * 느린 요청으로 판단할 임계값 (ms).
     * 이 시간을 초과하면 WARN 레벨로 [SLOW] 태그와 함께 로깅.
     */
    private long slowRequestThresholdMs = 3000;

    /**
     * Content-Type별 본문 로깅 허용 목록.
     * 지정하지 않으면 기본값 사용 (json, xml, text, form-urlencoded).
     */
    private List<String> loggableContentTypes = new ArrayList<>(List.of(
            "application/json",
            "application/xml",
            "text/plain",
            "text/xml",
            "text/html",
            "application/x-www-form-urlencoded"
    ));


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogRequestBody() {
        return logRequestBody;
    }

    public void setLogRequestBody(boolean logRequestBody) {
        this.logRequestBody = logRequestBody;
    }

    public boolean isLogResponseBody() {
        return logResponseBody;
    }

    public void setLogResponseBody(boolean logResponseBody) {
        this.logResponseBody = logResponseBody;
    }

    public int getMaxBodyLength() {
        return maxBodyLength;
    }

    public void setMaxBodyLength(int maxBodyLength) {
        this.maxBodyLength = maxBodyLength;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public List<String> getExcludeHeaders() {
        return excludeHeaders;
    }

    public void setExcludeHeaders(List<String> excludeHeaders) {
        this.excludeHeaders = excludeHeaders;
    }

    public List<PathPattern> getPathPatterns() {
        return pathPatterns;
    }

    public void setPathPatterns(List<PathPattern> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    public long getSlowRequestThresholdMs() {
        return slowRequestThresholdMs;
    }

    public void setSlowRequestThresholdMs(long slowRequestThresholdMs) {
        this.slowRequestThresholdMs = slowRequestThresholdMs;
    }

    public List<String> getLoggableContentTypes() {
        return loggableContentTypes;
    }

    public void setLoggableContentTypes(List<String> loggableContentTypes) {
        this.loggableContentTypes = loggableContentTypes;
    }


    /**
     * 경로 정규화 패턴 정의.
     *
     * <p>예시:</p>
     * <pre>
     * pattern: "/users/\d+"
     * replacement: "/users/{id}"
     * </pre>
     */
    public static class PathPattern {
        private String pattern;
        private String replacement;

        public PathPattern() {
        }

        public PathPattern(String pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getReplacement() {
            return replacement;
        }

        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }
    }
}
