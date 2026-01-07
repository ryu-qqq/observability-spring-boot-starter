package com.ryuqq.observability.integration.gateway;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.webflux.context.propagation.ContextPropagationConfiguration;
import com.ryuqq.observability.webflux.context.propagation.TraceContextThreadLocalAccessor;
import com.ryuqq.observability.webflux.context.propagation.UserIdThreadLocalAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Micrometer Context Propagation 테스트.
 *
 * <p>검증 항목:</p>
 * <ul>
 *   <li>Reactor Context → ThreadLocal(MDC) 자동 전파</li>
 *   <li>ThreadLocal → Reactor Context 자동 전파</li>
 *   <li>다중 컨텍스트 키 동시 전파</li>
 * </ul>
 */
class ContextPropagationTest {

    @BeforeAll
    static void setUp() {
        ContextPropagationConfiguration.install();
    }

    @AfterAll
    static void tearDown() {
        TraceIdHolder.clear();
        ContextPropagationConfiguration.uninstall();
    }

    @Test
    @DisplayName("Reactor Context의 값이 ThreadLocal로 전파되어야 한다")
    void shouldPropagateFromReactorContextToThreadLocal() {
        String testTraceId = "test-trace-id-12345";

        Mono<String> mono = Mono.deferContextual(ctx -> {
                    // 이 시점에서 ThreadLocal에 값이 있어야 함
                    String mdcValue = TraceIdHolder.get();
                    return Mono.just(mdcValue);
                })
                .contextWrite(ctx -> ctx.put(TraceContextThreadLocalAccessor.KEY, testTraceId));

        StepVerifier.create(mono)
                .expectNext(testTraceId)
                .verifyComplete();
    }

    @Test
    @DisplayName("다중 컨텍스트 키가 동시에 전파되어야 한다")
    void shouldPropagateMultipleContextKeys() {
        String testTraceId = "multi-trace-id";
        String testUserId = "user-123";

        Mono<String[]> mono = Mono.deferContextual(ctx -> {
                    String traceId = TraceIdHolder.get();
                    String userId = TraceIdHolder.getUserId();
                    return Mono.just(new String[]{traceId, userId});
                })
                .contextWrite(ctx -> ctx
                        .put(TraceContextThreadLocalAccessor.KEY, testTraceId)
                        .put(UserIdThreadLocalAccessor.KEY, testUserId));

        StepVerifier.create(mono)
                .assertNext(values -> {
                    assertThat(values[0]).isEqualTo(testTraceId);
                    assertThat(values[1]).isEqualTo(testUserId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("flatMap 체인에서도 컨텍스트가 전파되어야 한다")
    void shouldPropagateContextThroughFlatMap() {
        String testTraceId = "flatmap-trace-id";

        Mono<String> mono = Mono.just("initial")
                .flatMap(val -> Mono.deferContextual(ctx -> {
                    String mdcValue = TraceIdHolder.get();
                    return Mono.just(mdcValue);
                }))
                .contextWrite(ctx -> ctx.put(TraceContextThreadLocalAccessor.KEY, testTraceId));

        StepVerifier.create(mono)
                .expectNext(testTraceId)
                .verifyComplete();
    }

    @Test
    @DisplayName("ContextPropagationConfiguration이 설치되어야 한다")
    void shouldBeInstalled() {
        assertThat(ContextPropagationConfiguration.isInstalled()).isTrue();
    }
}
