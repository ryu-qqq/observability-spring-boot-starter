package com.ryuqq.observability.web.trace;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;

/**
 * 기본 TraceIdProvider 구현체.
 *
 * <p>UUID 기반으로 TraceId를 생성하고, 설정된 헤더 목록에서
 * 순서대로 TraceId를 추출합니다.</p>
 */
public class DefaultTraceIdProvider implements TraceIdProvider {

    private final List<String> headerNames;

    public DefaultTraceIdProvider(List<String> headerNames) {
        this.headerNames = headerNames;
    }

    @Override
    public String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String extractFromRequest(HttpServletRequest request) {
        for (String headerName : headerNames) {
            String value = request.getHeader(headerName);
            if (value != null && !value.isEmpty()) {
                return parseHeaderValue(headerName, value);
            }
        }
        return null;
    }

    /**
     * 헤더 이름에 따라 적절한 파싱 로직을 적용합니다.
     */
    private String parseHeaderValue(String headerName, String value) {
        // W3C Trace Context
        if (TraceIdHeaders.TRACEPARENT.equalsIgnoreCase(headerName)) {
            String parsed = parseW3CTraceId(value);
            return parsed != null ? parsed : value;
        }

        // AWS X-Ray
        if (TraceIdHeaders.X_AMZN_TRACE_ID.equalsIgnoreCase(headerName)) {
            String parsed = parseXRayTraceId(value);
            return parsed != null ? parsed : value;
        }

        // 그 외는 값 그대로 사용
        return value;
    }
}
