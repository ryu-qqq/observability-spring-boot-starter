package com.ryuqq.observability.core.trace;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * TraceId 및 사용자 컨텍스트를 관리하는 유틸리티 클래스.
 *
 * <p>SLF4J MDC를 기반으로 동작하며, 어디서든 현재 요청의 TraceId와
 * 사용자 정보에 접근할 수 있습니다.</p>
 *
 * <p>이 클래스는 순수 Java + SLF4J API만 사용하여 Domain Layer에서도
 * 사용할 수 있습니다.</p>
 *
 * <pre>
 * {@code
 * // TraceId 조회
 * String traceId = TraceIdHolder.get();
 *
 * // 사용자 정보 조회 (Gateway에서 전달된 헤더)
 * String userId = TraceIdHolder.getUserId();
 * String tenantId = TraceIdHolder.getTenantId();
 *
 * // 추가 컨텍스트 설정 (로그에 포함됨)
 * TraceIdHolder.addContext("orderId", "ORD-67890");
 * }
 * </pre>
 */
public final class TraceIdHolder {

    private TraceIdHolder() {
    }

    private static final ThreadLocal<Map<String, String>> additionalContext =
            ThreadLocal.withInitial(HashMap::new);

    // ==================== TraceId 관리 ====================

    /**
     * 현재 TraceId를 반환합니다.
     *
     * @return TraceId, 없으면 "unknown"
     */
    public static String get() {
        return Optional.ofNullable(MDC.get(TraceIdHeaders.MDC_TRACE_ID))
                .orElse("unknown");
    }

    /**
     * 현재 TraceId를 Optional로 반환합니다.
     *
     * @return TraceId Optional
     */
    public static Optional<String> getOptional() {
        return Optional.ofNullable(MDC.get(TraceIdHeaders.MDC_TRACE_ID));
    }

    /**
     * TraceId를 설정합니다.
     *
     * @param traceId 설정할 TraceId
     */
    public static void set(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TraceIdHeaders.MDC_TRACE_ID, traceId);
        }
    }

    /**
     * SpanId를 설정합니다.
     *
     * @param spanId 설정할 SpanId
     */
    public static void setSpanId(String spanId) {
        if (spanId != null && !spanId.isEmpty()) {
            MDC.put(TraceIdHeaders.MDC_SPAN_ID, spanId);
        }
    }

    /**
     * 서비스 이름을 설정합니다.
     *
     * @param serviceName 서비스 이름
     */
    public static void setServiceName(String serviceName) {
        if (serviceName != null && !serviceName.isEmpty()) {
            MDC.put(TraceIdHeaders.MDC_SERVICE_NAME, serviceName);
        }
    }

    // ==================== User Context 관리 ====================

    /**
     * 사용자 ID를 설정합니다 (Gateway X-User-Id 헤더에서 추출).
     *
     * @param userId 사용자 ID
     */
    public static void setUserId(String userId) {
        if (userId != null && !userId.isEmpty()) {
            MDC.put(TraceIdHeaders.MDC_USER_ID, userId);
        }
    }

    /**
     * 현재 사용자 ID를 반환합니다.
     *
     * @return 사용자 ID, 없으면 null
     */
    public static String getUserId() {
        return MDC.get(TraceIdHeaders.MDC_USER_ID);
    }

    /**
     * 테넌트 ID를 설정합니다 (Gateway X-Tenant-Id 헤더에서 추출).
     *
     * @param tenantId 테넌트 ID
     */
    public static void setTenantId(String tenantId) {
        if (tenantId != null && !tenantId.isEmpty()) {
            MDC.put(TraceIdHeaders.MDC_TENANT_ID, tenantId);
        }
    }

    /**
     * 현재 테넌트 ID를 반환합니다.
     *
     * @return 테넌트 ID, 없으면 null
     */
    public static String getTenantId() {
        return MDC.get(TraceIdHeaders.MDC_TENANT_ID);
    }

    /**
     * 조직 ID를 설정합니다 (Gateway X-Organization-Id 헤더에서 추출).
     *
     * @param organizationId 조직 ID
     */
    public static void setOrganizationId(String organizationId) {
        if (organizationId != null && !organizationId.isEmpty()) {
            MDC.put(TraceIdHeaders.MDC_ORGANIZATION_ID, organizationId);
        }
    }

    /**
     * 현재 조직 ID를 반환합니다.
     *
     * @return 조직 ID, 없으면 null
     */
    public static String getOrganizationId() {
        return MDC.get(TraceIdHeaders.MDC_ORGANIZATION_ID);
    }

    /**
     * 사용자 역할을 설정합니다 (Gateway X-User-Roles 헤더에서 추출).
     *
     * @param roles 사용자 역할 (쉼표로 구분된 문자열)
     */
    public static void setUserRoles(String roles) {
        if (roles != null && !roles.isEmpty()) {
            MDC.put(TraceIdHeaders.MDC_USER_ROLES, roles);
        }
    }

    /**
     * 현재 사용자 역할을 반환합니다.
     *
     * @return 사용자 역할, 없으면 null
     */
    public static String getUserRoles() {
        return MDC.get(TraceIdHeaders.MDC_USER_ROLES);
    }

    // ==================== Message Context 관리 ====================

    /**
     * 메시지 소스를 설정합니다 (SQS, Redis 등).
     *
     * @param source 메시지 소스 식별자
     */
    public static void setMessageSource(String source) {
        if (source != null && !source.isEmpty()) {
            MDC.put(TraceIdHeaders.MDC_MESSAGE_SOURCE, source);
        }
    }

    /**
     * 메시지 ID를 설정합니다.
     *
     * @param messageId 메시지 ID
     */
    public static void setMessageId(String messageId) {
        if (messageId != null && !messageId.isEmpty()) {
            MDC.put(TraceIdHeaders.MDC_MESSAGE_ID, messageId);
        }
    }

    // ==================== 추가 컨텍스트 ====================

    /**
     * 추가 컨텍스트를 설정합니다. MDC에도 함께 저장됩니다.
     *
     * @param key   컨텍스트 키
     * @param value 컨텍스트 값
     */
    public static void addContext(String key, String value) {
        if (key != null && value != null) {
            String mdcKey = TraceIdHeaders.CONTEXT_PREFIX + key;
            MDC.put(mdcKey, value);
            additionalContext.get().put(key, value);
        }
    }

    /**
     * 추가 컨텍스트를 조회합니다.
     *
     * @param key 컨텍스트 키
     * @return 컨텍스트 값, 없으면 null
     */
    public static String getContext(String key) {
        return additionalContext.get().get(key);
    }

    /**
     * 모든 추가 컨텍스트를 반환합니다.
     *
     * @return 컨텍스트 맵 (불변 복사본)
     */
    public static Map<String, String> getAllContext() {
        return Map.copyOf(additionalContext.get());
    }

    /**
     * TraceId 및 모든 컨텍스트를 정리합니다.
     * 요청 처리 완료 시 반드시 호출해야 합니다.
     */
    public static void clear() {
        // 추가 컨텍스트 MDC에서 제거
        for (String key : additionalContext.get().keySet()) {
            MDC.remove(TraceIdHeaders.CONTEXT_PREFIX + key);
        }
        additionalContext.get().clear();
        additionalContext.remove();

        // 기본 MDC 키 제거
        MDC.remove(TraceIdHeaders.MDC_TRACE_ID);
        MDC.remove(TraceIdHeaders.MDC_SPAN_ID);
        MDC.remove(TraceIdHeaders.MDC_SERVICE_NAME);

        // User Context MDC 키 제거
        MDC.remove(TraceIdHeaders.MDC_USER_ID);
        MDC.remove(TraceIdHeaders.MDC_TENANT_ID);
        MDC.remove(TraceIdHeaders.MDC_ORGANIZATION_ID);
        MDC.remove(TraceIdHeaders.MDC_USER_ROLES);

        // Message Context MDC 키 제거
        MDC.remove(TraceIdHeaders.MDC_MESSAGE_SOURCE);
        MDC.remove(TraceIdHeaders.MDC_MESSAGE_ID);
    }

    /**
     * TraceId가 설정되어 있는지 확인합니다.
     *
     * @return TraceId 존재 여부
     */
    public static boolean isPresent() {
        return MDC.get(TraceIdHeaders.MDC_TRACE_ID) != null;
    }
}
