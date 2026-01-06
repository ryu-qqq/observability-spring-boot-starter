package com.ryuqq.observability.client.rest;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInitializer;

/**
 * RestClient (Spring 6.1+)용 TraceId 전파 인터셉터.
 *
 * <p>MDC의 TraceId와 사용자 컨텍스트를 HTTP 헤더로 전파합니다.</p>
 *
 * <p>Spring Framework 6.1부터 도입된 RestClient의 새로운 초기화 방식을 지원합니다.</p>
 *
 * <pre>
 * {@code
 * @Bean
 * public RestClient restClient(TraceIdRestClientInterceptor interceptor) {
 *     return RestClient.builder()
 *             .requestInitializer(interceptor)
 *             .build();
 * }
 * }
 * </pre>
 */
public class TraceIdRestClientInterceptor implements ClientHttpRequestInitializer {

    @Override
    public void initialize(ClientHttpRequest request) {
        propagateTraceContext(request);
    }

    /**
     * MDC의 Trace 컨텍스트를 HTTP 헤더로 전파합니다.
     */
    private void propagateTraceContext(ClientHttpRequest request) {
        // TraceId 전파
        TraceIdHolder.getOptional().ifPresent(traceId ->
                request.getHeaders().add(TraceIdHeaders.X_TRACE_ID, traceId)
        );

        // User Context 전파
        String userId = TraceIdHolder.getUserId();
        if (userId != null) {
            request.getHeaders().add(TraceIdHeaders.X_USER_ID, userId);
        }

        String tenantId = TraceIdHolder.getTenantId();
        if (tenantId != null) {
            request.getHeaders().add(TraceIdHeaders.X_TENANT_ID, tenantId);
        }

        String organizationId = TraceIdHolder.getOrganizationId();
        if (organizationId != null) {
            request.getHeaders().add(TraceIdHeaders.X_ORGANIZATION_ID, organizationId);
        }
    }
}
