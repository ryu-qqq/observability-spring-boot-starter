package com.ryuqq.observability.webflux.context.propagation;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.webflux.trace.ReactiveTraceIdFilter;
import io.micrometer.context.ThreadLocalAccessor;

/**
 * TraceId용 ThreadLocalAccessor.
 *
 * <p>Micrometer Context Propagation을 사용하여 Reactor Context와
 * ThreadLocal(MDC) 간 TraceId를 자동으로 동기화합니다.</p>
 *
 * <p>이 클래스는 {@code Hooks.enableAutomaticContextPropagation()}과 함께 사용되어
 * 기존 {@code MdcContextLifterHook}을 대체합니다.</p>
 *
 * <p>장점:</p>
 * <ul>
 *   <li>Reactor의 Fusion 최적화와 충돌하지 않음</li>
 *   <li>Netty ByteBuf 메모리 누수 방지</li>
 *   <li>Prometheus/Actuator 엔드포인트와 호환</li>
 * </ul>
 */
public class TraceContextThreadLocalAccessor implements ThreadLocalAccessor<String> {

    /**
     * Reactor Context에서 사용하는 키.
     * {@link ReactiveTraceIdFilter#TRACE_ID_CONTEXT_KEY}와 동일해야 함.
     */
    public static final String KEY = ReactiveTraceIdFilter.TRACE_ID_CONTEXT_KEY;

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public String getValue() {
        return TraceIdHolder.getOptional().orElse(null);
    }

    @Override
    public void setValue(String value) {
        if (value != null && !value.isEmpty()) {
            TraceIdHolder.set(value);
        }
    }

    @Override
    public void setValue() {
        // 기본값 없음 - MDC 상태 유지
    }

    @Override
    public void restore(String previousValue) {
        if (previousValue != null) {
            TraceIdHolder.set(previousValue);
        } else {
            // 이전 값이 없으면 현재 MDC 값 유지 (clear하지 않음)
        }
    }
}
