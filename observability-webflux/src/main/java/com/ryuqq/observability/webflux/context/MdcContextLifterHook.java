package com.ryuqq.observability.webflux.context;

import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;

import java.util.function.Function;

/**
 * Reactor Hooks를 통해 MdcContextLifter를 전역으로 등록하는 유틸리티.
 *
 * <p>Spring Boot 애플리케이션 시작 시 이 클래스의 {@link #install()} 메서드를
 * 호출하면 모든 Reactor 연산에서 자동으로 MDC 컨텍스트가 전파됩니다.</p>
 *
 * <p><b>중요:</b> Fuseable.ConditionalSubscriber를 감지하여 적절한 lifter를 사용합니다.
 * 이는 Reactor의 Fusion 최적화를 유지하고 Netty ByteBuf 메모리 누수를 방지합니다.</p>
 *
 * <pre>
 * {@code
 * @PostConstruct
 * public void setupReactorHooks() {
 *     MdcContextLifterHook.install();
 * }
 * }
 * </pre>
 *
 * @deprecated Micrometer Context Propagation으로 대체되었습니다.
 *             {@link com.ryuqq.observability.webflux.context.propagation.ContextPropagationConfiguration}을 사용하세요.
 *             이 클래스는 Netty ByteBuf 메모리 누수와 Prometheus/Actuator 엔드포인트 문제를 야기할 수 있습니다.
 * @see com.ryuqq.observability.webflux.context.propagation.ContextPropagationConfiguration
 */
@Deprecated(since = "1.3.0", forRemoval = true)
public final class MdcContextLifterHook {

    private static final String HOOK_KEY = "mdcContextLifter";

    private MdcContextLifterHook() {
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MdcContextLifterHook.class);

    /**
     * MdcContextLifter Hook을 전역으로 설치합니다.
     * 이미 설치된 경우 무시됩니다.
     *
     * @deprecated since 1.3.0. Use {@link com.ryuqq.observability.webflux.context.propagation.ContextPropagationConfiguration#install()} instead.
     */
    @Deprecated(since = "1.3.0", forRemoval = true)
    public static void install() {
        log.warn("MdcContextLifterHook is deprecated since 1.3.0 and will be removed in a future version. " +
                "This can cause Netty ByteBuf memory leaks and Prometheus/Actuator endpoint issues. " +
                "Use ContextPropagationConfiguration.install() instead, which is now automatically configured.");
        Hooks.onEachOperator(HOOK_KEY, liftFunction());
    }

    /**
     * MdcContextLifter Hook을 제거합니다.
     *
     * @deprecated since 1.3.0. Use {@link com.ryuqq.observability.webflux.context.propagation.ContextPropagationConfiguration#uninstall()} instead.
     */
    @Deprecated(since = "1.3.0", forRemoval = true)
    public static void uninstall() {
        Hooks.resetOnEachOperator(HOOK_KEY);
    }

    /**
     * Operators.lift()를 통해 MdcContextLifter를 적용하는 Function을 반환합니다.
     *
     * <p>Fuseable.ConditionalSubscriber인 경우 MdcContextLifter.Conditional을 사용하여
     * Reactor의 Fusion 최적화를 유지합니다.</p>
     */
    @SuppressWarnings("unchecked")
    private static <T> Function<? super Publisher<T>, ? extends Publisher<T>> liftFunction() {
        return Operators.lift((scannable, subscriber) -> {
            // ConditionalSubscriber인 경우 Conditional lifter 사용
            if (subscriber instanceof Fuseable.ConditionalSubscriber) {
                return new MdcContextLifter.Conditional<>(
                        (Fuseable.ConditionalSubscriber<T>) subscriber
                );
            }
            // 일반 CoreSubscriber인 경우
            return new MdcContextLifter<>((CoreSubscriber<T>) subscriber);
        });
    }
}
