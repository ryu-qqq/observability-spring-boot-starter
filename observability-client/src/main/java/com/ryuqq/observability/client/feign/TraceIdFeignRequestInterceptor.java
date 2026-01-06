package com.ryuqq.observability.client.feign;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * OpenFeign 클라이언트용 TraceId 전파 인터셉터.
 *
 * <p>MDC의 TraceId와 사용자 컨텍스트를 HTTP 헤더로 전파합니다.</p>
 *
 * <p>Spring Cloud OpenFeign 환경에서 자동으로 적용됩니다.</p>
 *
 * <pre>
 * {@code
 * @Bean
 * public TraceIdFeignRequestInterceptor traceIdFeignInterceptor() {
 *     return new TraceIdFeignRequestInterceptor();
 * }
 * }
 * </pre>
 *
 * <p>또는 @FeignClient의 configuration 속성을 사용:</p>
 * <pre>
 * {@code
 * @FeignClient(name = "order-service", configuration = FeignConfig.class)
 * public interface OrderClient {
 *     // ...
 * }
 *
 * @Configuration
 * public class FeignConfig {
 *     @Bean
 *     public TraceIdFeignRequestInterceptor traceIdInterceptor() {
 *         return new TraceIdFeignRequestInterceptor();
 *     }
 * }
 * }
 * </pre>
 */
public class TraceIdFeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        propagateTraceContext(template);
    }

    /**
     * MDC의 Trace 컨텍스트를 HTTP 헤더로 전파합니다.
     */
    private void propagateTraceContext(RequestTemplate template) {
        // TraceId 전파
        TraceIdHolder.getOptional().ifPresent(traceId ->
                template.header(TraceIdHeaders.X_TRACE_ID, traceId)
        );

        // User Context 전파
        String userId = TraceIdHolder.getUserId();
        if (userId != null) {
            template.header(TraceIdHeaders.X_USER_ID, userId);
        }

        String tenantId = TraceIdHolder.getTenantId();
        if (tenantId != null) {
            template.header(TraceIdHeaders.X_TENANT_ID, tenantId);
        }

        String organizationId = TraceIdHolder.getOrganizationId();
        if (organizationId != null) {
            template.header(TraceIdHeaders.X_ORGANIZATION_ID, organizationId);
        }
    }
}
