package com.ryuqq.observability.web.config;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP 요청/응답 로깅 설정.
 *
 * <pre>
 * observability:
 *   http:
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
 * </pre>
 */
public class HttpLoggingProperties {

    /**
     * HTTP 로깅 활성화 여부
     */
    private boolean enabled = true;

    /**
     * 요청 본문 로깅 여부 (민감정보 주의)
     */
    private boolean logRequestBody = false;

    /**
     * 응답 본문 로깅 여부 (민감정보 주의)
     */
    private boolean logResponseBody = false;

    /**
     * 본문 로깅 시 최대 길이
     */
    private int maxBodyLength = 1000;

    /**
     * 로깅 제외 경로 패턴 (Ant 패턴)
     */
    private List<String> excludePaths = new ArrayList<>(List.of(
            "/actuator/**",
            "/health",
            "/health/**",
            "/favicon.ico",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    ));

    /**
     * 로깅 제외 헤더 (대소문자 무관)
     */
    private List<String> excludeHeaders = new ArrayList<>(List.of(
            "Authorization",
            "Cookie",
            "Set-Cookie",
            "X-Api-Key",
            "Api-Key"
    ));

    /**
     * 경로 정규화 패턴 목록
     */
    private List<PathPattern> pathPatterns = new ArrayList<>();

    /**
     * 느린 요청으로 판단할 임계값 (ms)
     */
    private long slowRequestThresholdMs = 3000;


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


    /**
     * 경로 정규화 패턴 정의.
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
