package com.ryuqq.observability.core.trace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TraceIdHeaders 상수 테스트")
class TraceIdHeadersTest {

    @Test
    @DisplayName("Trace 헤더 상수가 올바르게 정의되어 있다")
    void shouldHaveCorrectTraceHeaders() {
        assertThat(TraceIdHeaders.X_TRACE_ID).isEqualTo("X-Trace-Id");
        assertThat(TraceIdHeaders.X_REQUEST_ID).isEqualTo("X-Request-Id");
        assertThat(TraceIdHeaders.TRACEPARENT).isEqualTo("traceparent");
        assertThat(TraceIdHeaders.X_AMZN_TRACE_ID).isEqualTo("X-Amzn-Trace-Id");
    }

    @Test
    @DisplayName("User Context 헤더 상수가 올바르게 정의되어 있다")
    void shouldHaveCorrectUserContextHeaders() {
        assertThat(TraceIdHeaders.X_USER_ID).isEqualTo("X-User-Id");
        assertThat(TraceIdHeaders.X_TENANT_ID).isEqualTo("X-Tenant-Id");
        assertThat(TraceIdHeaders.X_ORGANIZATION_ID).isEqualTo("X-Organization-Id");
        assertThat(TraceIdHeaders.X_USER_ROLES).isEqualTo("X-User-Roles");
        assertThat(TraceIdHeaders.X_USER_PERMISSIONS).isEqualTo("X-User-Permissions");
    }

    @Test
    @DisplayName("MDC 키 상수가 올바르게 정의되어 있다")
    void shouldHaveCorrectMdcKeys() {
        assertThat(TraceIdHeaders.MDC_TRACE_ID).isEqualTo("traceId");
        assertThat(TraceIdHeaders.MDC_SPAN_ID).isEqualTo("spanId");
        assertThat(TraceIdHeaders.MDC_SERVICE_NAME).isEqualTo("service");
    }

    @Test
    @DisplayName("User Context MDC 키가 올바르게 정의되어 있다")
    void shouldHaveCorrectUserContextMdcKeys() {
        assertThat(TraceIdHeaders.MDC_USER_ID).isEqualTo("userId");
        assertThat(TraceIdHeaders.MDC_TENANT_ID).isEqualTo("tenantId");
        assertThat(TraceIdHeaders.MDC_ORGANIZATION_ID).isEqualTo("organizationId");
        assertThat(TraceIdHeaders.MDC_USER_ROLES).isEqualTo("userRoles");
    }

    @Test
    @DisplayName("메시지 관련 MDC 키가 올바르게 정의되어 있다")
    void shouldHaveCorrectMessageMdcKeys() {
        assertThat(TraceIdHeaders.MDC_MESSAGE_SOURCE).isEqualTo("messageSource");
        assertThat(TraceIdHeaders.MDC_MESSAGE_ID).isEqualTo("messageId");
    }

    @Test
    @DisplayName("컨텍스트 접두사가 올바르게 정의되어 있다")
    void shouldHaveCorrectContextPrefix() {
        assertThat(TraceIdHeaders.CONTEXT_PREFIX).isEqualTo("ctx.");
    }
}
