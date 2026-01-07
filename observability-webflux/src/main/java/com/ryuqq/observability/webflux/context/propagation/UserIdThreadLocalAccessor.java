package com.ryuqq.observability.webflux.context.propagation;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.webflux.trace.ReactiveTraceIdFilter;
import io.micrometer.context.ThreadLocalAccessor;

/**
 * UserId용 ThreadLocalAccessor.
 *
 * <p>Gateway에서 전달된 X-User-Id 헤더 값을 Reactor Context와
 * ThreadLocal(MDC) 간에 자동으로 동기화합니다.</p>
 */
public class UserIdThreadLocalAccessor implements ThreadLocalAccessor<String> {

    public static final String KEY = ReactiveTraceIdFilter.USER_ID_CONTEXT_KEY;

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public String getValue() {
        return TraceIdHolder.getUserId();
    }

    @Override
    public void setValue(String value) {
        if (value != null && !value.isEmpty()) {
            TraceIdHolder.setUserId(value);
        }
    }

    @Override
    public void setValue() {
        // 기본값 없음
    }

    @Override
    public void restore(String previousValue) {
        if (previousValue != null) {
            TraceIdHolder.setUserId(previousValue);
        }
    }
}
