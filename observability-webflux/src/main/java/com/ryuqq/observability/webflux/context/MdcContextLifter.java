package com.ryuqq.observability.webflux.context;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.webflux.trace.ReactiveTraceIdFilter;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

import java.util.Optional;

/**
 * Reactor Context에서 MDC로 컨텍스트를 복사하는 CoreSubscriber 데코레이터.
 *
 * <p>WebFlux/Reactor 환경에서는 스레드가 계속 변경되기 때문에
 * ThreadLocal 기반의 MDC가 자동으로 전파되지 않습니다.</p>
 *
 * <p>이 클래스는 Reactor의 각 연산 단계에서 Reactor Context에 저장된
 * TraceId와 사용자 컨텍스트를 MDC에 복사하여 로깅이 올바르게 동작하도록 합니다.</p>
 *
 * @param <T> 구독 데이터 타입
 */
public class MdcContextLifter<T> implements CoreSubscriber<T> {

    private static final Logger log = LoggerFactory.getLogger(MdcContextLifter.class);

    private final CoreSubscriber<T> delegate;

    public MdcContextLifter(CoreSubscriber<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Context currentContext() {
        return delegate.currentContext();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        copyContextToMdc();
        delegate.onSubscribe(subscription);
    }

    @Override
    public void onNext(T t) {
        copyContextToMdc();
        delegate.onNext(t);
    }

    @Override
    public void onError(Throwable throwable) {
        copyContextToMdc();
        delegate.onError(throwable);
    }

    @Override
    public void onComplete() {
        copyContextToMdc();
        delegate.onComplete();
    }

    /**
     * Reactor Context의 값들을 MDC로 복사합니다.
     */
    private void copyContextToMdc() {
        Context ctx = currentContext();

        try {
            // TraceId
            Optional<String> traceId = ctx.getOrEmpty(ReactiveTraceIdFilter.TRACE_ID_CONTEXT_KEY);
            traceId.ifPresent(TraceIdHolder::set);

            // Service Name
            Optional<String> serviceName = ctx.getOrEmpty(ReactiveTraceIdFilter.SERVICE_NAME_CONTEXT_KEY);
            serviceName.ifPresent(TraceIdHolder::setServiceName);

            // User Context
            Optional<String> userId = ctx.getOrEmpty(ReactiveTraceIdFilter.USER_ID_CONTEXT_KEY);
            userId.ifPresent(TraceIdHolder::setUserId);

            Optional<String> tenantId = ctx.getOrEmpty(ReactiveTraceIdFilter.TENANT_ID_CONTEXT_KEY);
            tenantId.ifPresent(TraceIdHolder::setTenantId);

            Optional<String> organizationId = ctx.getOrEmpty(ReactiveTraceIdFilter.ORGANIZATION_ID_CONTEXT_KEY);
            organizationId.ifPresent(TraceIdHolder::setOrganizationId);

            Optional<String> userRoles = ctx.getOrEmpty(ReactiveTraceIdFilter.USER_ROLES_CONTEXT_KEY);
            userRoles.ifPresent(TraceIdHolder::setUserRoles);

        } catch (Exception e) {
            log.trace("Failed to copy context to MDC", e);
        }
    }
}
