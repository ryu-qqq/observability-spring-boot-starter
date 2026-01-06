package com.ryuqq.observability.webflux.context;

import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;

import java.util.function.Function;

/**
 * Reactor Hooks를 통해 MdcContextLifter를 전역으로 등록하는 유틸리티.
 *
 * <p>Spring Boot 애플리케이션 시작 시 이 클래스의 {@link #install()} 메서드를
 * 호출하면 모든 Reactor 연산에서 자동으로 MDC 컨텍스트가 전파됩니다.</p>
 *
 * <pre>
 * {@code
 * @PostConstruct
 * public void setupReactorHooks() {
 *     MdcContextLifterHook.install();
 * }
 * }
 * </pre>
 */
public final class MdcContextLifterHook {

    private static final String HOOK_KEY = "mdcContextLifter";

    private MdcContextLifterHook() {
    }

    /**
     * MdcContextLifter Hook을 전역으로 설치합니다.
     * 이미 설치된 경우 무시됩니다.
     */
    public static void install() {
        Hooks.onEachOperator(HOOK_KEY, liftFunction());
    }

    /**
     * MdcContextLifter Hook을 제거합니다.
     */
    public static void uninstall() {
        Hooks.resetOnEachOperator(HOOK_KEY);
    }

    /**
     * Operators.lift()를 통해 MdcContextLifter를 적용하는 Function을 반환합니다.
     */
    @SuppressWarnings("unchecked")
    private static <T> Function<? super Publisher<T>, ? extends Publisher<T>> liftFunction() {
        return Operators.lift((scannable, subscriber) -> new MdcContextLifter<>((CoreSubscriber<T>) subscriber));
    }
}
