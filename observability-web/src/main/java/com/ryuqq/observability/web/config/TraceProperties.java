package com.ryuqq.observability.web.config;

import java.util.ArrayList;
import java.util.List;

/**
 * TraceId 관련 설정.
 *
 * <pre>
 * observability:
 *   trace:
 *     enabled: true
 *     header-names:
 *       - X-Trace-Id
 *       - X-Request-Id
 *       - traceparent
 *     include-in-response: true
 *     generate-if-missing: true
 *     response-header-name: X-Trace-Id
 * </pre>
 */
public class TraceProperties {

    /**
     * TraceId 기능 활성화 여부
     */
    private boolean enabled = true;

    /**
     * TraceId를 추출할 요청 헤더 이름들 (우선순위 순)
     */
    private List<String> headerNames = new ArrayList<>(List.of(
            "X-Trace-Id",
            "X-Request-Id",
            "traceparent",           // W3C Trace Context
            "X-Amzn-Trace-Id"        // AWS X-Ray
    ));

    /**
     * 응답 헤더에 TraceId 포함 여부
     */
    private boolean includeInResponse = true;

    /**
     * 요청에 TraceId가 없을 때 자동 생성 여부
     */
    private boolean generateIfMissing = true;

    /**
     * 응답 헤더에 사용할 이름
     */
    private String responseHeaderName = "X-Trace-Id";


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getHeaderNames() {
        return headerNames;
    }

    public void setHeaderNames(List<String> headerNames) {
        this.headerNames = headerNames;
    }

    public boolean isIncludeInResponse() {
        return includeInResponse;
    }

    public void setIncludeInResponse(boolean includeInResponse) {
        this.includeInResponse = includeInResponse;
    }

    public boolean isGenerateIfMissing() {
        return generateIfMissing;
    }

    public void setGenerateIfMissing(boolean generateIfMissing) {
        this.generateIfMissing = generateIfMissing;
    }

    public String getResponseHeaderName() {
        return responseHeaderName;
    }

    public void setResponseHeaderName(String responseHeaderName) {
        this.responseHeaderName = responseHeaderName;
    }
}
