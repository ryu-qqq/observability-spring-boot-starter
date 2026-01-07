package com.ryuqq.observability.webflux.context;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.webflux.trace.ReactiveTraceIdFilter;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.Scannable;
import reactor.util.annotation.Nullable;
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
 * <p><b>중요:</b> Scannable 인터페이스를 구현하여 Reactor의 연산자 체인 스캔과
 * 리소스 정리가 올바르게 동작하도록 합니다. 이는 Netty ByteBuf 메모리 누수를 방지합니다.</p>
 *
 * @param <T> 구독 데이터 타입
 * @deprecated Micrometer Context Propagation으로 대체되었습니다.
 *             이 클래스는 Netty ByteBuf 메모리 누수와 Prometheus/Actuator 엔드포인트 문제를 야기할 수 있습니다.
 * @see com.ryuqq.observability.webflux.context.propagation.ContextPropagationConfiguration
 */
@Deprecated(since = "1.3.0", forRemoval = true)
public class MdcContextLifter<T> implements CoreSubscriber<T>, Scannable {

    private static final Logger log = LoggerFactory.getLogger(MdcContextLifter.class);

    private final CoreSubscriber<T> delegate;
    private Subscription subscription;

    public MdcContextLifter(CoreSubscriber<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Context currentContext() {
        return delegate.currentContext();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
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
     * Reactor Scannable 인터페이스 구현.
     * Reactor가 연산자 체인을 올바르게 스캔하고 리소스를 정리할 수 있게 합니다.
     *
     * @param key 스캔할 속성 키
     * @return 해당 키에 대한 값
     */
    @Override
    @Nullable
    public Object scanUnsafe(Attr key) {
        if (key == Attr.PARENT) {
            return subscription;
        }
        if (key == Attr.ACTUAL) {
            return delegate;
        }
        if (key == Attr.RUN_STYLE) {
            return Attr.RunStyle.SYNC;
        }
        if (key == Attr.PREFETCH) {
            return Integer.MAX_VALUE;
        }
        // delegate가 Scannable인 경우 위임
        if (delegate instanceof Scannable) {
            return ((Scannable) delegate).scanUnsafe(key);
        }
        return null;
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

    /**
     * Fuseable.ConditionalSubscriber를 지원하는 MdcContextLifter.
     * Reactor의 Fusion 최적화를 유지하여 성능 저하와 메모리 누수를 방지합니다.
     *
     * @param <T> 구독 데이터 타입
     */
    public static class Conditional<T> extends MdcContextLifter<T>
            implements Fuseable.ConditionalSubscriber<T> {

        private final Fuseable.ConditionalSubscriber<T> conditionalDelegate;

        public Conditional(Fuseable.ConditionalSubscriber<T> delegate) {
            super(delegate);
            this.conditionalDelegate = delegate;
        }

        @Override
        public boolean tryOnNext(T t) {
            copyContextToMdcInternal();
            return conditionalDelegate.tryOnNext(t);
        }

        private void copyContextToMdcInternal() {
            Context ctx = currentContext();

            try {
                Optional<String> traceId = ctx.getOrEmpty(ReactiveTraceIdFilter.TRACE_ID_CONTEXT_KEY);
                traceId.ifPresent(TraceIdHolder::set);

                Optional<String> serviceName = ctx.getOrEmpty(ReactiveTraceIdFilter.SERVICE_NAME_CONTEXT_KEY);
                serviceName.ifPresent(TraceIdHolder::setServiceName);

                Optional<String> userId = ctx.getOrEmpty(ReactiveTraceIdFilter.USER_ID_CONTEXT_KEY);
                userId.ifPresent(TraceIdHolder::setUserId);

                Optional<String> tenantId = ctx.getOrEmpty(ReactiveTraceIdFilter.TENANT_ID_CONTEXT_KEY);
                tenantId.ifPresent(TraceIdHolder::setTenantId);

                Optional<String> organizationId = ctx.getOrEmpty(ReactiveTraceIdFilter.ORGANIZATION_ID_CONTEXT_KEY);
                organizationId.ifPresent(TraceIdHolder::setOrganizationId);

                Optional<String> userRoles = ctx.getOrEmpty(ReactiveTraceIdFilter.USER_ROLES_CONTEXT_KEY);
                userRoles.ifPresent(TraceIdHolder::setUserRoles);

            } catch (Exception e) {
                // Silently ignore
            }
        }
    }
}
