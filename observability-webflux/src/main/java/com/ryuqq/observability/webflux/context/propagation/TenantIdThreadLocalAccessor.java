package com.ryuqq.observability.webflux.context.propagation;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.webflux.trace.ReactiveTraceIdFilter;
import io.micrometer.context.ThreadLocalAccessor;

/**
 * TenantId용 ThreadLocalAccessor.
 *
 * <p>Gateway에서 전달된 X-Tenant-Id 헤더 값을 Reactor Context와
 * ThreadLocal(MDC) 간에 자동으로 동기화합니다.</p>
 */
public class TenantIdThreadLocalAccessor implements ThreadLocalAccessor<String> {

    public static final String KEY = ReactiveTraceIdFilter.TENANT_ID_CONTEXT_KEY;

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public String getValue() {
        return TraceIdHolder.getTenantId();
    }

    @Override
    public void setValue(String value) {
        if (value != null && !value.isEmpty()) {
            TraceIdHolder.setTenantId(value);
        }
    }

    @Override
    public void setValue() {
        // 기본값 없음
    }

    @Override
    public void restore(String previousValue) {
        if (previousValue != null) {
            TraceIdHolder.setTenantId(previousValue);
        }
    }
}
