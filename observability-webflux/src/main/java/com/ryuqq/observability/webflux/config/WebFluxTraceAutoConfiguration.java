package com.ryuqq.observability.webflux.config;

import com.ryuqq.observability.webflux.context.propagation.ContextPropagationConfiguration;
import com.ryuqq.observability.webflux.trace.DefaultReactiveTraceIdProvider;
import com.ryuqq.observability.webflux.trace.ReactiveTraceIdFilter;
import com.ryuqq.observability.webflux.trace.ReactiveTraceIdProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.WebFilter;

/**
 * WebFlux TraceId 자동 설정.
 *
 * <p>Spring WebFlux 환경에서만 활성화되며, 다음 기능을 제공합니다:</p>
 * <ul>
 *   <li>ReactiveTraceIdFilter - TraceId 추출/생성 WebFilter</li>
 *   <li>Micrometer Context Propagation - Reactor Context ↔ MDC 전파</li>
 *   <li>ReactiveTraceIdProvider - TraceId 생성/추출 전략</li>
 * </ul>
 *
 * <p>Micrometer Context Propagation을 사용하여 다음 문제를 해결합니다:</p>
 * <ul>
 *   <li>Netty ByteBuf Fusion 최적화와의 충돌 방지</li>
 *   <li>메모리 누수 방지</li>
 *   <li>Prometheus/Actuator 스트리밍 엔드포인트 호환성</li>
 * </ul>
 *
 * <p>설정 예시:</p>
 * <pre>
 * observability:
 *   reactive-trace:
 *     enabled: true
 *     generate-if-missing: true
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass({WebFilter.class, WebFluxConfigurer.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "observability.reactive-trace", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ReactiveTraceProperties.class)
public class WebFluxTraceAutoConfiguration implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(WebFluxTraceAutoConfiguration.class);

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    /**
     * ReactiveTraceIdProvider 기본 구현체를 등록합니다.
     * 커스텀 구현이 있으면 대체됩니다.
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveTraceIdProvider reactiveTraceIdProvider() {
        log.debug("Creating default ReactiveTraceIdProvider");
        return new DefaultReactiveTraceIdProvider();
    }

    /**
     * ReactiveTraceIdFilter를 등록합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveTraceIdFilter reactiveTraceIdFilter(ReactiveTraceIdProvider traceIdProvider,
                                                        ReactiveTraceProperties properties) {
        log.info("Registering ReactiveTraceIdFilter for WebFlux application: {}", applicationName);
        return new ReactiveTraceIdFilter(traceIdProvider, properties, applicationName);
    }

    /**
     * Micrometer Context Propagation을 설치합니다.
     * Reactor Context와 ThreadLocal(MDC) 간 자동 동기화를 활성화합니다.
     */
    @Override
    public void afterPropertiesSet() {
        ContextPropagationConfiguration.install();
    }

    /**
     * 애플리케이션 종료 시 Context Propagation을 해제합니다.
     */
    @Override
    public void destroy() {
        ContextPropagationConfiguration.uninstall();
    }
}
