package com.ryuqq.observability.webflux.trace;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.webflux.config.ReactiveTraceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Reactive TraceId WebFilter.
 *
 * <p>TraceId와 사용자 컨텍스트를 자동으로 생성/추출하고
 * Reactor Context와 MDC에 설정하는 WebFilter입니다.</p>
 *
 * <p>가장 높은 우선순위(-100)로 실행되어 모든 로그에 TraceId와
 * 사용자 정보가 포함되도록 합니다.</p>
 *
 * <p>처리 흐름:</p>
 * <ol>
 *   <li>요청 헤더에서 TraceId 추출 시도</li>
 *   <li>없으면 새로운 TraceId 생성</li>
 *   <li>Gateway 사용자 컨텍스트 헤더 추출</li>
 *   <li>Reactor Context에 TraceId 및 사용자 컨텍스트 저장</li>
 *   <li>요청 처리</li>
 *   <li>응답 헤더에 TraceId 추가 (설정된 경우)</li>
 * </ol>
 */
public class ReactiveTraceIdFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ReactiveTraceIdFilter.class);

    /**
     * 가장 높은 우선순위 (다른 필터보다 먼저 실행)
     */
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    /**
     * Reactor Context 키
     */
    public static final String TRACE_ID_CONTEXT_KEY = "traceId";
    public static final String USER_ID_CONTEXT_KEY = "userId";
    public static final String TENANT_ID_CONTEXT_KEY = "tenantId";
    public static final String ORGANIZATION_ID_CONTEXT_KEY = "organizationId";
    public static final String USER_ROLES_CONTEXT_KEY = "userRoles";
    public static final String SERVICE_NAME_CONTEXT_KEY = "serviceName";

    private final ReactiveTraceIdProvider traceIdProvider;
    private final ReactiveTraceProperties properties;
    private final String serviceName;

    public ReactiveTraceIdFilter(ReactiveTraceIdProvider traceIdProvider,
                                 ReactiveTraceProperties properties,
                                 String serviceName) {
        this.traceIdProvider = traceIdProvider;
        this.properties = properties;
        this.serviceName = serviceName;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return resolveTraceId(exchange)
                .flatMap(traceId -> processRequest(exchange, chain, traceId));
    }

    /**
     * TraceId를 추출하거나 생성합니다.
     */
    private Mono<String> resolveTraceId(ServerWebExchange exchange) {
        String traceId = traceIdProvider.extractFromExchange(exchange);

        if (traceId == null && properties.isGenerateIfMissing()) {
            traceId = traceIdProvider.generate();
            log.debug("Generated new TraceId: {}", traceId);
        }

        return Mono.justOrEmpty(traceId);
    }

    /**
     * 요청을 처리하고 Reactor Context에 TraceId와 사용자 컨텍스트를 전파합니다.
     */
    private Mono<Void> processRequest(ServerWebExchange exchange, WebFilterChain chain, String traceId) {
        // 응답 헤더에 TraceId 추가
        if (properties.isIncludeInResponse() && traceId != null) {
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().add(properties.getResponseHeaderName(), traceId);
        }

        // 요청 헤더에 TraceId 추가 (downstream 전파용)
        ServerWebExchange mutatedExchange = exchange;
        if (traceId != null) {
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(TraceIdHeaders.X_TRACE_ID, traceId)
                    .build();
            mutatedExchange = exchange.mutate().request(mutatedRequest).build();
        }

        // 사용자 컨텍스트 추출
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String userId = headers.getFirst(TraceIdHeaders.X_USER_ID);
        String tenantId = headers.getFirst(TraceIdHeaders.X_TENANT_ID);
        String organizationId = headers.getFirst(TraceIdHeaders.X_ORGANIZATION_ID);
        String userRoles = headers.getFirst(TraceIdHeaders.X_USER_ROLES);

        final String finalTraceId = traceId;
        final ServerWebExchange finalExchange = mutatedExchange;

        return chain.filter(finalExchange)
                .contextWrite(ctx -> buildContext(ctx, finalTraceId, userId, tenantId, organizationId, userRoles))
                .doOnEach(signal -> {
                    if (signal.isOnComplete() || signal.isOnError()) {
                        // 요청 완료 시 MDC 정리 (WebFlux에서는 onEach로 처리)
                        TraceIdHolder.clear();
                    }
                });
    }

    /**
     * Reactor Context를 구성합니다.
     */
    private Context buildContext(Context ctx, String traceId, String userId,
                                  String tenantId, String organizationId, String userRoles) {
        Context newCtx = ctx;

        if (traceId != null && !traceId.isEmpty()) {
            newCtx = newCtx.put(TRACE_ID_CONTEXT_KEY, traceId);
        }

        if (serviceName != null && !serviceName.isEmpty()) {
            newCtx = newCtx.put(SERVICE_NAME_CONTEXT_KEY, serviceName);
        }

        if (userId != null && !userId.isEmpty()) {
            newCtx = newCtx.put(USER_ID_CONTEXT_KEY, userId);
        }

        if (tenantId != null && !tenantId.isEmpty()) {
            newCtx = newCtx.put(TENANT_ID_CONTEXT_KEY, tenantId);
        }

        if (organizationId != null && !organizationId.isEmpty()) {
            newCtx = newCtx.put(ORGANIZATION_ID_CONTEXT_KEY, organizationId);
        }

        if (userRoles != null && !userRoles.isEmpty()) {
            newCtx = newCtx.put(USER_ROLES_CONTEXT_KEY, userRoles);
        }

        return newCtx;
    }
}
