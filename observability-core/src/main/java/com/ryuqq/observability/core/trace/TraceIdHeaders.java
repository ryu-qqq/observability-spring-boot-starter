package com.ryuqq.observability.core.trace;

/**
 * TraceId 및 사용자 컨텍스트 관련 헤더/MDC 키 상수 정의.
 *
 * <p>Gateway에서 전달하는 헤더와 MDC 키 매핑:</p>
 * <ul>
 *   <li>X-Trace-Id → traceId (MDC)</li>
 *   <li>X-User-Id → userId (MDC)</li>
 *   <li>X-Tenant-Id → tenantId (MDC)</li>
 *   <li>X-Organization-Id → organizationId (MDC)</li>
 * </ul>
 *
 * <p>이 클래스는 순수 Java로 구현되어 Domain Layer에서 사용할 수 있습니다.</p>
 */
public final class TraceIdHeaders {

    private TraceIdHeaders() {
    }

    // ==================== Trace 헤더 ====================
    public static final String X_TRACE_ID = "X-Trace-Id";
    public static final String X_REQUEST_ID = "X-Request-Id";

    // W3C Trace Context
    public static final String TRACEPARENT = "traceparent";

    // AWS X-Ray
    public static final String X_AMZN_TRACE_ID = "X-Amzn-Trace-Id";

    // ==================== User Context 헤더 (Gateway에서 전달) ====================
    public static final String X_USER_ID = "X-User-Id";
    public static final String X_TENANT_ID = "X-Tenant-Id";
    public static final String X_ORGANIZATION_ID = "X-Organization-Id";
    public static final String X_USER_ROLES = "X-User-Roles";
    public static final String X_USER_PERMISSIONS = "X-User-Permissions";

    // ==================== MDC 키 ====================
    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_SPAN_ID = "spanId";
    public static final String MDC_SERVICE_NAME = "service";

    // User Context MDC 키
    public static final String MDC_USER_ID = "userId";
    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_ORGANIZATION_ID = "organizationId";
    public static final String MDC_USER_ROLES = "userRoles";

    // 메시지 소스 MDC 키 (SQS, Redis 등)
    public static final String MDC_MESSAGE_SOURCE = "messageSource";
    public static final String MDC_MESSAGE_ID = "messageId";

    // 컨텍스트 키 접두사
    public static final String CONTEXT_PREFIX = "ctx.";
}
