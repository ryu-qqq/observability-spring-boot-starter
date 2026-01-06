package com.ryuqq.observability.webflux.trace;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import java.util.UUID;

/**
 * 기본 Reactive TraceId 제공자.
 *
 * <p>다음 우선순위로 TraceId를 추출합니다:</p>
 * <ol>
 *   <li>X-Trace-Id 헤더</li>
 *   <li>X-Request-Id 헤더</li>
 *   <li>W3C traceparent 헤더</li>
 *   <li>AWS X-Amzn-Trace-Id 헤더</li>
 * </ol>
 *
 * <p>새 TraceId 생성 시 UUID 형식을 사용합니다.</p>
 */
public class DefaultReactiveTraceIdProvider implements ReactiveTraceIdProvider {

    @Override
    public String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String extractFromExchange(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        // 1. X-Trace-Id 헤더 확인
        String traceId = headers.getFirst(TraceIdHeaders.X_TRACE_ID);
        if (isValidTraceId(traceId)) {
            return traceId;
        }

        // 2. X-Request-Id 헤더 확인
        traceId = headers.getFirst(TraceIdHeaders.X_REQUEST_ID);
        if (isValidTraceId(traceId)) {
            return traceId;
        }

        // 3. W3C traceparent 헤더 확인
        String traceparent = headers.getFirst(TraceIdHeaders.TRACEPARENT);
        traceId = parseW3CTraceId(traceparent);
        if (isValidTraceId(traceId)) {
            return traceId;
        }

        // 4. AWS X-Ray 헤더 확인
        String xrayHeader = headers.getFirst(TraceIdHeaders.X_AMZN_TRACE_ID);
        traceId = parseXRayTraceId(xrayHeader);
        if (isValidTraceId(traceId)) {
            return traceId;
        }

        return null;
    }

    private boolean isValidTraceId(String traceId) {
        return traceId != null && !traceId.isEmpty();
    }
}
