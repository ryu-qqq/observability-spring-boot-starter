package com.ryuqq.observability.web.trace;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.web.config.TraceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TraceId와 사용자 컨텍스트를 자동으로 생성/추출하고 MDC에 설정하는 필터.
 *
 * <p>가장 높은 우선순위(-100)로 실행되어 모든 로그에 TraceId와
 * 사용자 정보가 포함되도록 합니다.</p>
 *
 * <p>처리 흐름:</p>
 * <ol>
 *   <li>요청 헤더에서 TraceId 추출 시도</li>
 *   <li>없으면 새로운 TraceId 생성</li>
 *   <li>Gateway 사용자 컨텍스트 헤더 추출 (X-User-Id, X-Tenant-Id 등)</li>
 *   <li>MDC에 TraceId 및 사용자 컨텍스트 설정</li>
 *   <li>요청 처리 (chain.doFilter)</li>
 *   <li>응답 헤더에 TraceId 추가 (설정된 경우)</li>
 *   <li>MDC 정리</li>
 * </ol>
 */
public class TraceIdFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    /**
     * 가장 높은 우선순위 (다른 필터보다 먼저 실행)
     */
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    private final TraceIdProvider traceIdProvider;
    private final TraceProperties properties;
    private final String serviceName;

    public TraceIdFilter(TraceIdProvider traceIdProvider,
                         TraceProperties properties,
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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = null;

        try {
            // 1. 요청에서 TraceId 추출 시도
            traceId = traceIdProvider.extractFromRequest(request);

            // 2. 없으면 새로 생성
            if (traceId == null && properties.isGenerateIfMissing()) {
                traceId = traceIdProvider.generate();
                log.debug("Generated new TraceId: {}", traceId);
            }

            // 3. MDC에 TraceId 설정
            if (traceId != null) {
                TraceIdHolder.set(traceId);
            }

            // 4. 서비스 이름 설정
            if (serviceName != null) {
                TraceIdHolder.setServiceName(serviceName);
            }

            // 5. Gateway 사용자 컨텍스트 헤더 추출 및 MDC 설정
            extractUserContext(request);

            // 6. 응답 헤더에 TraceId 추가
            if (properties.isIncludeInResponse() && traceId != null) {
                response.setHeader(properties.getResponseHeaderName(), traceId);
            }

            // 7. 요청 처리
            filterChain.doFilter(request, response);

        } finally {
            // 8. MDC 정리
            TraceIdHolder.clear();
        }
    }

    /**
     * Gateway에서 전달된 사용자 컨텍스트 헤더를 추출하여 MDC에 설정합니다.
     *
     * <p>추출되는 헤더:</p>
     * <ul>
     *   <li>X-User-Id → userId</li>
     *   <li>X-Tenant-Id → tenantId</li>
     *   <li>X-Organization-Id → organizationId</li>
     *   <li>X-User-Roles → userRoles</li>
     * </ul>
     *
     * @param request HTTP 요청
     */
    private void extractUserContext(HttpServletRequest request) {
        // X-User-Id
        String userId = request.getHeader(TraceIdHeaders.X_USER_ID);
        if (userId != null && !userId.isEmpty()) {
            TraceIdHolder.setUserId(userId);
        }

        // X-Tenant-Id
        String tenantId = request.getHeader(TraceIdHeaders.X_TENANT_ID);
        if (tenantId != null && !tenantId.isEmpty()) {
            TraceIdHolder.setTenantId(tenantId);
        }

        // X-Organization-Id
        String organizationId = request.getHeader(TraceIdHeaders.X_ORGANIZATION_ID);
        if (organizationId != null && !organizationId.isEmpty()) {
            TraceIdHolder.setOrganizationId(organizationId);
        }

        // X-User-Roles
        String userRoles = request.getHeader(TraceIdHeaders.X_USER_ROLES);
        if (userRoles != null && !userRoles.isEmpty()) {
            TraceIdHolder.setUserRoles(userRoles);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 항상 필터링 (TraceId는 모든 요청에 필요)
        return false;
    }
}
