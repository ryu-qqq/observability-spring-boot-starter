package com.ryuqq.observability.client.common;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Trace 컨텍스트 전파를 위한 공통 유틸리티 클래스.
 *
 * <p>MDC에 저장된 TraceId와 사용자 컨텍스트를 HTTP 헤더로 전파하는
 * 공통 로직을 제공합니다.</p>
 *
 * <p>사용 예시:</p>
 * <pre>
 * {@code
 * // BiConsumer를 통한 헤더 설정
 * TraceContextPropagator.propagate((name, value) -> {
 *     request.getHeaders().add(name, value);
 * });
 *
 * // Map으로 헤더 수집
 * Map<String, String> headers = TraceContextPropagator.getHeaders();
 * headers.forEach((name, value) -> request.addHeader(name, value));
 * }
 * </pre>
 */
public final class TraceContextPropagator {

    private TraceContextPropagator() {
    }

    /**
     * 현재 스레드의 Trace 컨텍스트를 전파합니다.
     *
     * @param headerSetter 헤더를 설정하는 BiConsumer (헤더명, 값)
     */
    public static void propagate(BiConsumer<String, String> headerSetter) {
        // TraceId 전파
        TraceIdHolder.getOptional().ifPresent(traceId ->
                headerSetter.accept(TraceIdHeaders.X_TRACE_ID, traceId)
        );

        // User Context 전파
        propagateIfPresent(TraceIdHeaders.X_USER_ID, TraceIdHolder.getUserId(), headerSetter);
        propagateIfPresent(TraceIdHeaders.X_TENANT_ID, TraceIdHolder.getTenantId(), headerSetter);
        propagateIfPresent(TraceIdHeaders.X_ORGANIZATION_ID, TraceIdHolder.getOrganizationId(), headerSetter);
    }

    /**
     * 현재 스레드의 Trace 컨텍스트를 Map으로 반환합니다.
     *
     * @return 전파할 헤더 맵 (불변)
     */
    public static Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        propagate(headers::put);
        return Map.copyOf(headers);
    }

    /**
     * TraceId만 전파합니다.
     *
     * @param headerSetter 헤더를 설정하는 BiConsumer
     */
    public static void propagateTraceIdOnly(BiConsumer<String, String> headerSetter) {
        TraceIdHolder.getOptional().ifPresent(traceId ->
                headerSetter.accept(TraceIdHeaders.X_TRACE_ID, traceId)
        );
    }

    /**
     * 현재 TraceId를 반환합니다.
     *
     * @return TraceId 또는 null
     */
    public static String getTraceId() {
        return TraceIdHolder.getOptional().orElse(null);
    }

    /**
     * TraceId가 존재하는지 확인합니다.
     *
     * @return TraceId 존재 여부
     */
    public static boolean hasTraceId() {
        return TraceIdHolder.isPresent();
    }

    private static void propagateIfPresent(String headerName, String value,
                                           BiConsumer<String, String> headerSetter) {
        if (value != null && !value.isEmpty()) {
            headerSetter.accept(headerName, value);
        }
    }
}
