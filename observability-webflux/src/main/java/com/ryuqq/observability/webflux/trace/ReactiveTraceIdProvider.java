package com.ryuqq.observability.webflux.trace;

import org.springframework.web.server.ServerWebExchange;

/**
 * Reactive TraceId 생성 및 추출 전략 인터페이스.
 *
 * <p>커스텀 TraceId 생성/추출 전략이 필요한 경우 이 인터페이스를 구현하여
 * Spring Bean으로 등록하면 됩니다.</p>
 *
 * <pre>
 * {@code
 * @Bean
 * public ReactiveTraceIdProvider customTraceIdProvider() {
 *     return new ReactiveTraceIdProvider() {
 *         @Override
 *         public String generate() {
 *             return "CUSTOM-" + UUID.randomUUID().toString();
 *         }
 *     };
 * }
 * }
 * </pre>
 */
public interface ReactiveTraceIdProvider {

    /**
     * 새로운 TraceId를 생성합니다.
     *
     * @return 생성된 TraceId
     */
    String generate();

    /**
     * ServerWebExchange에서 TraceId를 추출합니다.
     *
     * @param exchange WebFlux ServerWebExchange
     * @return 추출된 TraceId, 없으면 null
     */
    String extractFromExchange(ServerWebExchange exchange);

    /**
     * W3C traceparent 헤더에서 TraceId를 파싱합니다.
     * 형식: {version}-{trace-id}-{parent-id}-{trace-flags}
     * 예: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
     *
     * @param traceparent traceparent 헤더 값
     * @return 추출된 TraceId, 파싱 실패 시 null
     */
    default String parseW3CTraceId(String traceparent) {
        if (traceparent == null || traceparent.isEmpty()) {
            return null;
        }
        String[] parts = traceparent.split("-");
        if (parts.length >= 2) {
            return parts[1];
        }
        return null;
    }

    /**
     * AWS X-Ray 헤더에서 TraceId를 파싱합니다.
     * 형식: Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1
     *
     * @param xrayHeader X-Amzn-Trace-Id 헤더 값
     * @return 추출된 TraceId, 파싱 실패 시 null
     */
    default String parseXRayTraceId(String xrayHeader) {
        if (xrayHeader == null || xrayHeader.isEmpty()) {
            return null;
        }
        for (String part : xrayHeader.split(";")) {
            if (part.trim().startsWith("Root=")) {
                return part.trim().substring(5);
            }
        }
        return null;
    }
}
