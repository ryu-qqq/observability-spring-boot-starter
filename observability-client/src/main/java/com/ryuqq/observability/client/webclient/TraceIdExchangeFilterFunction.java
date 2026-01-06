package com.ryuqq.observability.client.webclient;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient (Reactive)용 TraceId 전파 필터.
 *
 * <p>MDC의 TraceId와 사용자 컨텍스트를 HTTP 헤더로 전파합니다.</p>
 *
 * <p>Reactive 환경에서도 Trace 컨텍스트가 올바르게 전파되도록 합니다.</p>
 *
 * <pre>
 * {@code
 * @Bean
 * public WebClient webClient(TraceIdExchangeFilterFunction filter) {
 *     return WebClient.builder()
 *             .filter(filter)
 *             .build();
 * }
 * }
 * </pre>
 *
 * <p>주의: Reactive 환경에서 MDC는 ThreadLocal 기반이므로,
 * 구독 시점에서 컨텍스트를 캡처합니다. Reactor Context를 함께 사용하는 것을 권장합니다.</p>
 */
public class TraceIdExchangeFilterFunction implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        // 현재 스레드의 MDC 컨텍스트 캡처
        String traceId = TraceIdHolder.getOptional().orElse(null);
        String userId = TraceIdHolder.getUserId();
        String tenantId = TraceIdHolder.getTenantId();
        String organizationId = TraceIdHolder.getOrganizationId();

        // 요청에 헤더 추가
        ClientRequest.Builder requestBuilder = ClientRequest.from(request);

        if (traceId != null) {
            requestBuilder.header(TraceIdHeaders.X_TRACE_ID, traceId);
        }

        if (userId != null) {
            requestBuilder.header(TraceIdHeaders.X_USER_ID, userId);
        }

        if (tenantId != null) {
            requestBuilder.header(TraceIdHeaders.X_TENANT_ID, tenantId);
        }

        if (organizationId != null) {
            requestBuilder.header(TraceIdHeaders.X_ORGANIZATION_ID, organizationId);
        }

        return next.exchange(requestBuilder.build());
    }

    /**
     * WebClient 빌더에 필터를 적용하는 편의 메서드.
     *
     * @return 새로운 TraceIdExchangeFilterFunction 인스턴스
     */
    public static TraceIdExchangeFilterFunction create() {
        return new TraceIdExchangeFilterFunction();
    }
}
