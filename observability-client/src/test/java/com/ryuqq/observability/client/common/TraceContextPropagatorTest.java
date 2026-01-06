package com.ryuqq.observability.client.common;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TraceContextPropagator 테스트")
class TraceContextPropagatorTest {

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("propagate 테스트")
    class PropagateTest {

        @Test
        @DisplayName("TraceId를 전파한다")
        void shouldPropagateTraceId() {
            TraceIdHolder.set("test-trace-id");

            Map<String, String> headers = new HashMap<>();
            TraceContextPropagator.propagate(headers::put);

            assertThat(headers).containsEntry(TraceIdHeaders.X_TRACE_ID, "test-trace-id");
        }

        @Test
        @DisplayName("User Context를 전파한다")
        void shouldPropagateUserContext() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("user-123");
            TraceIdHolder.setTenantId("tenant-456");
            TraceIdHolder.setOrganizationId("org-789");

            Map<String, String> headers = new HashMap<>();
            TraceContextPropagator.propagate(headers::put);

            assertThat(headers)
                    .containsEntry(TraceIdHeaders.X_USER_ID, "user-123")
                    .containsEntry(TraceIdHeaders.X_TENANT_ID, "tenant-456")
                    .containsEntry(TraceIdHeaders.X_ORGANIZATION_ID, "org-789");
        }

        @Test
        @DisplayName("TraceId가 없으면 전파하지 않는다")
        void shouldNotPropagateWhenNoTraceId() {
            Map<String, String> headers = new HashMap<>();
            TraceContextPropagator.propagate(headers::put);

            assertThat(headers).doesNotContainKey(TraceIdHeaders.X_TRACE_ID);
        }

        @Test
        @DisplayName("null 값은 전파하지 않는다")
        void shouldNotPropagateNullValues() {
            TraceIdHolder.set("trace-id");
            // userId, tenantId, organizationId를 설정하지 않음

            Map<String, String> headers = new HashMap<>();
            TraceContextPropagator.propagate(headers::put);

            assertThat(headers)
                    .containsEntry(TraceIdHeaders.X_TRACE_ID, "trace-id")
                    .doesNotContainKey(TraceIdHeaders.X_USER_ID)
                    .doesNotContainKey(TraceIdHeaders.X_TENANT_ID)
                    .doesNotContainKey(TraceIdHeaders.X_ORGANIZATION_ID);
        }

        @Test
        @DisplayName("빈 값은 전파하지 않는다")
        void shouldNotPropagateEmptyValues() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("");

            Map<String, String> headers = new HashMap<>();
            TraceContextPropagator.propagate(headers::put);

            assertThat(headers).doesNotContainKey(TraceIdHeaders.X_USER_ID);
        }
    }

    @Nested
    @DisplayName("getHeaders 테스트")
    class GetHeadersTest {

        @Test
        @DisplayName("모든 컨텍스트를 Map으로 반환한다")
        void shouldReturnAllContextAsMap() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("user-id");

            Map<String, String> headers = TraceContextPropagator.getHeaders();

            assertThat(headers)
                    .containsEntry(TraceIdHeaders.X_TRACE_ID, "trace-id")
                    .containsEntry(TraceIdHeaders.X_USER_ID, "user-id");
        }

        @Test
        @DisplayName("컨텍스트가 없으면 빈 Map을 반환한다")
        void shouldReturnEmptyMapWhenNoContext() {
            Map<String, String> headers = TraceContextPropagator.getHeaders();

            assertThat(headers).isEmpty();
        }

        @Test
        @DisplayName("불변 Map을 반환한다")
        void shouldReturnImmutableMap() {
            TraceIdHolder.set("trace-id");

            Map<String, String> headers = TraceContextPropagator.getHeaders();

            assertThat(headers).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("propagateTraceIdOnly 테스트")
    class PropagateTraceIdOnlyTest {

        @Test
        @DisplayName("TraceId만 전파한다")
        void shouldPropagateOnlyTraceId() {
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("user-id");

            Map<String, String> headers = new HashMap<>();
            TraceContextPropagator.propagateTraceIdOnly(headers::put);

            assertThat(headers)
                    .containsEntry(TraceIdHeaders.X_TRACE_ID, "trace-id")
                    .doesNotContainKey(TraceIdHeaders.X_USER_ID);
        }

        @Test
        @DisplayName("TraceId가 없으면 전파하지 않는다")
        void shouldNotPropagateWhenNoTraceId() {
            Map<String, String> headers = new HashMap<>();
            TraceContextPropagator.propagateTraceIdOnly(headers::put);

            assertThat(headers).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTraceId 테스트")
    class GetTraceIdTest {

        @Test
        @DisplayName("TraceId를 반환한다")
        void shouldReturnTraceId() {
            TraceIdHolder.set("test-trace-id");

            String traceId = TraceContextPropagator.getTraceId();

            assertThat(traceId).isEqualTo("test-trace-id");
        }

        @Test
        @DisplayName("TraceId가 없으면 null을 반환한다")
        void shouldReturnNullWhenNoTraceId() {
            String traceId = TraceContextPropagator.getTraceId();

            assertThat(traceId).isNull();
        }
    }

    @Nested
    @DisplayName("hasTraceId 테스트")
    class HasTraceIdTest {

        @Test
        @DisplayName("TraceId가 있으면 true를 반환한다")
        void shouldReturnTrueWhenTraceIdExists() {
            TraceIdHolder.set("trace-id");

            assertThat(TraceContextPropagator.hasTraceId()).isTrue();
        }

        @Test
        @DisplayName("TraceId가 없으면 false를 반환한다")
        void shouldReturnFalseWhenNoTraceId() {
            assertThat(TraceContextPropagator.hasTraceId()).isFalse();
        }
    }
}
