package com.ryuqq.observability.webflux.context.propagation;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.webflux.trace.ReactiveTraceIdFilter;
import io.micrometer.context.ThreadLocalAccessor;

/**
 * ServiceName용 ThreadLocalAccessor.
 *
 * <p>애플리케이션의 서비스 이름을 Reactor Context와
 * ThreadLocal(MDC) 간에 자동으로 동기화합니다.</p>
 */
public class ServiceNameThreadLocalAccessor implements ThreadLocalAccessor<String> {

    public static final String KEY = ReactiveTraceIdFilter.SERVICE_NAME_CONTEXT_KEY;

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public String getValue() {
        return TraceIdHolder.getServiceName();
    }

    @Override
    public void setValue(String value) {
        if (value != null && !value.isEmpty()) {
            TraceIdHolder.setServiceName(value);
        }
    }

    @Override
    public void setValue() {
        // 기본값 없음
    }

    @Override
    public void restore(String previousValue) {
        if (previousValue != null) {
            TraceIdHolder.setServiceName(previousValue);
        }
    }
}
