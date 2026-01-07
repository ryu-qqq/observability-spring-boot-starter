package com.ryuqq.observability.webflux.context.propagation;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.webflux.trace.ReactiveTraceIdFilter;
import io.micrometer.context.ThreadLocalAccessor;

/**
 * OrganizationId용 ThreadLocalAccessor.
 *
 * <p>Gateway에서 전달된 X-Organization-Id 헤더 값을 Reactor Context와
 * ThreadLocal(MDC) 간에 자동으로 동기화합니다.</p>
 */
public class OrganizationIdThreadLocalAccessor implements ThreadLocalAccessor<String> {

    public static final String KEY = ReactiveTraceIdFilter.ORGANIZATION_ID_CONTEXT_KEY;

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public String getValue() {
        return TraceIdHolder.getOrganizationId();
    }

    @Override
    public void setValue(String value) {
        if (value != null && !value.isEmpty()) {
            TraceIdHolder.setOrganizationId(value);
        }
    }

    @Override
    public void setValue() {
        // 기본값 없음
    }

    @Override
    public void restore(String previousValue) {
        if (previousValue != null) {
            TraceIdHolder.setOrganizationId(previousValue);
        }
    }
}
