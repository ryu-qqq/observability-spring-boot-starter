package com.ryuqq.observability.webflux.context.propagation;

import io.micrometer.context.ContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Hooks;

/**
 * Micrometer Context Propagation 설정.
 *
 * <p>Reactor Context와 ThreadLocal(MDC) 간 자동 동기화를 설정합니다.
 * 기존 MdcContextLifterHook을 대체하여 다음 문제들을 해결합니다:</p>
 *
 * <ul>
 *   <li>Netty ByteBuf Fusion 최적화와의 충돌 방지</li>
 *   <li>메모리 누수 방지</li>
 *   <li>Prometheus/Actuator 스트리밍 엔드포인트 호환성</li>
 * </ul>
 *
 * <p>사용 방법:</p>
 * <pre>
 * {@code
 * // 애플리케이션 시작 시 (AutoConfiguration에서 자동 호출)
 * ContextPropagationConfiguration.install();
 *
 * // 애플리케이션 종료 시
 * ContextPropagationConfiguration.uninstall();
 * }
 * </pre>
 */
public final class ContextPropagationConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ContextPropagationConfiguration.class);

    private static volatile boolean installed = false;

    private ContextPropagationConfiguration() {
    }

    /**
     * Context Propagation을 설치합니다.
     *
     * <p>모든 ThreadLocalAccessor를 ContextRegistry에 등록하고
     * Reactor의 자동 컨텍스트 전파를 활성화합니다.</p>
     */
    public static synchronized void install() {
        if (installed) {
            log.debug("Context Propagation already installed, skipping");
            return;
        }

        log.info("Installing Micrometer Context Propagation for Reactor MDC propagation");

        ContextRegistry registry = ContextRegistry.getInstance();

        // ThreadLocalAccessor 등록
        registry.registerThreadLocalAccessor(new TraceContextThreadLocalAccessor());
        registry.registerThreadLocalAccessor(new UserIdThreadLocalAccessor());
        registry.registerThreadLocalAccessor(new TenantIdThreadLocalAccessor());
        registry.registerThreadLocalAccessor(new OrganizationIdThreadLocalAccessor());
        registry.registerThreadLocalAccessor(new UserRolesThreadLocalAccessor());
        registry.registerThreadLocalAccessor(new ServiceNameThreadLocalAccessor());

        // Reactor 자동 컨텍스트 전파 활성화
        Hooks.enableAutomaticContextPropagation();

        installed = true;
        log.info("Context Propagation installed successfully with {} ThreadLocalAccessors", 6);
    }

    /**
     * Context Propagation을 해제합니다.
     *
     * <p>테스트나 애플리케이션 종료 시 사용합니다.</p>
     */
    public static synchronized void uninstall() {
        if (!installed) {
            log.debug("Context Propagation not installed, skipping uninstall");
            return;
        }

        log.debug("Uninstalling Context Propagation");

        // ContextRegistry는 clear 메서드가 없으므로 installed 상태만 변경
        // Hooks.resetOnEachOperator()를 호출하면 다른 훅도 제거될 수 있으므로 주의

        installed = false;
    }

    /**
     * Context Propagation 설치 상태를 반환합니다.
     *
     * @return 설치 여부
     */
    public static boolean isInstalled() {
        return installed;
    }
}
