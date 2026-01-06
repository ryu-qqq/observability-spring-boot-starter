package com.ryuqq.observability.client.rest;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * RestTemplate용 TraceId 전파 인터셉터.
 *
 * <p>MDC의 TraceId와 사용자 컨텍스트를 HTTP 헤더로 전파합니다.</p>
 *
 * <p>전파되는 헤더:</p>
 * <ul>
 *   <li>X-Trace-Id: 분산 추적 ID</li>
 *   <li>X-User-Id: 사용자 ID</li>
 *   <li>X-Tenant-Id: 테넌트 ID</li>
 *   <li>X-Organization-Id: 조직 ID</li>
 * </ul>
 *
 * <pre>
 * {@code
 * @Bean
 * public RestTemplate restTemplate(TraceIdRestTemplateInterceptor interceptor) {
 *     RestTemplate restTemplate = new RestTemplate();
 *     restTemplate.getInterceptors().add(interceptor);
 *     return restTemplate;
 * }
 * }
 * </pre>
 */
public class TraceIdRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                         byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        propagateTraceContext(request);
        return execution.execute(request, body);
    }

    /**
     * MDC의 Trace 컨텍스트를 HTTP 헤더로 전파합니다.
     */
    private void propagateTraceContext(HttpRequest request) {
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
