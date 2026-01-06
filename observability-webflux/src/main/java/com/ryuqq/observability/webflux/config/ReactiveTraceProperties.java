package com.ryuqq.observability.webflux.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reactive TraceId 필터 설정 프로퍼티.
 *
 * <p>application.yml 예시:</p>
 * <pre>
 * observability:
 *   reactive-trace:
 *     enabled: true
 *     generate-if-missing: true
 *     include-in-response: true
 *     response-header-name: X-Trace-Id
 * </pre>
 */
@ConfigurationProperties(prefix = "observability.reactive-trace")
public class ReactiveTraceProperties {

    /**
     * TraceId 필터 활성화 여부 (기본값: true)
     */
    private boolean enabled = true;

    /**
     * TraceId가 없을 경우 새로 생성 (기본값: true)
     */
    private boolean generateIfMissing = true;

    /**
     * 응답 헤더에 TraceId 포함 여부 (기본값: true)
     */
    private boolean includeInResponse = true;

    /**
     * 응답 헤더 이름 (기본값: X-Trace-Id)
     */
    private String responseHeaderName = "X-Trace-Id";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isGenerateIfMissing() {
        return generateIfMissing;
    }

    public void setGenerateIfMissing(boolean generateIfMissing) {
        this.generateIfMissing = generateIfMissing;
    }

    public boolean isIncludeInResponse() {
        return includeInResponse;
    }

    public void setIncludeInResponse(boolean includeInResponse) {
        this.includeInResponse = includeInResponse;
    }

    public String getResponseHeaderName() {
        return responseHeaderName;
    }

    public void setResponseHeaderName(String responseHeaderName) {
        this.responseHeaderName = responseHeaderName;
    }
}
