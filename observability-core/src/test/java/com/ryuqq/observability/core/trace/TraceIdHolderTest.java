package com.ryuqq.observability.core.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TraceIdHolder 테스트")
class TraceIdHolderTest {

    @BeforeEach
    void setUp() {
        TraceIdHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("TraceId 관리 테스트")
    class TraceIdManagementTest {

        @Test
        @DisplayName("TraceId를 설정하고 조회할 수 있다")
        void shouldSetAndGetTraceId() {
            TraceIdHolder.set("test-trace-id");
            assertThat(TraceIdHolder.get()).isEqualTo("test-trace-id");
        }

        @Test
        @DisplayName("TraceId가 없으면 'unknown'을 반환한다")
        void shouldReturnUnknownWhenNotSet() {
            assertThat(TraceIdHolder.get()).isEqualTo("unknown");
        }

        @Test
        @DisplayName("TraceId를 Optional로 조회할 수 있다")
        void shouldGetOptionalTraceId() {
            assertThat(TraceIdHolder.getOptional()).isEmpty();

            TraceIdHolder.set("test-trace-id");
            Optional<String> result = TraceIdHolder.getOptional();
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo("test-trace-id");
        }

        @Test
        @DisplayName("null TraceId는 설정되지 않는다")
        void shouldNotSetNullTraceId() {
            TraceIdHolder.set(null);
            assertThat(TraceIdHolder.get()).isEqualTo("unknown");
        }

        @Test
        @DisplayName("빈 문자열 TraceId는 설정되지 않는다")
        void shouldNotSetEmptyTraceId() {
            TraceIdHolder.set("");
            assertThat(TraceIdHolder.get()).isEqualTo("unknown");
        }

        @Test
        @DisplayName("TraceId 존재 여부를 확인할 수 있다")
        void shouldCheckIfPresent() {
            assertThat(TraceIdHolder.isPresent()).isFalse();

            TraceIdHolder.set("trace-id");
            assertThat(TraceIdHolder.isPresent()).isTrue();
        }
    }

    @Nested
    @DisplayName("SpanId 관리 테스트")
    class SpanIdManagementTest {

        @Test
        @DisplayName("SpanId를 설정할 수 있다")
        void shouldSetSpanId() {
            TraceIdHolder.setSpanId("span-123");
            assertThat(MDC.get(TraceIdHeaders.MDC_SPAN_ID)).isEqualTo("span-123");
        }

        @Test
        @DisplayName("null SpanId는 설정되지 않는다")
        void shouldNotSetNullSpanId() {
            TraceIdHolder.setSpanId(null);
            assertThat(MDC.get(TraceIdHeaders.MDC_SPAN_ID)).isNull();
        }

        @Test
        @DisplayName("빈 문자열 SpanId는 설정되지 않는다")
        void shouldNotSetEmptySpanId() {
            TraceIdHolder.setSpanId("");
            assertThat(MDC.get(TraceIdHeaders.MDC_SPAN_ID)).isNull();
        }
    }

    @Nested
    @DisplayName("ServiceName 관리 테스트")
    class ServiceNameManagementTest {

        @Test
        @DisplayName("ServiceName을 설정할 수 있다")
        void shouldSetServiceName() {
            TraceIdHolder.setServiceName("my-service");
            assertThat(MDC.get(TraceIdHeaders.MDC_SERVICE_NAME)).isEqualTo("my-service");
        }

        @Test
        @DisplayName("null ServiceName은 설정되지 않는다")
        void shouldNotSetNullServiceName() {
            TraceIdHolder.setServiceName(null);
            assertThat(MDC.get(TraceIdHeaders.MDC_SERVICE_NAME)).isNull();
        }
    }

    @Nested
    @DisplayName("User Context 관리 테스트")
    class UserContextManagementTest {

        @Test
        @DisplayName("UserId를 설정하고 조회할 수 있다")
        void shouldSetAndGetUserId() {
            TraceIdHolder.setUserId("user-123");
            assertThat(TraceIdHolder.getUserId()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("TenantId를 설정하고 조회할 수 있다")
        void shouldSetAndGetTenantId() {
            TraceIdHolder.setTenantId("tenant-456");
            assertThat(TraceIdHolder.getTenantId()).isEqualTo("tenant-456");
        }

        @Test
        @DisplayName("OrganizationId를 설정하고 조회할 수 있다")
        void shouldSetAndGetOrganizationId() {
            TraceIdHolder.setOrganizationId("org-789");
            assertThat(TraceIdHolder.getOrganizationId()).isEqualTo("org-789");
        }

        @Test
        @DisplayName("UserRoles를 설정하고 조회할 수 있다")
        void shouldSetAndGetUserRoles() {
            TraceIdHolder.setUserRoles("ADMIN,USER");
            assertThat(TraceIdHolder.getUserRoles()).isEqualTo("ADMIN,USER");
        }

        @Test
        @DisplayName("null 값은 설정되지 않는다")
        void shouldNotSetNullValues() {
            TraceIdHolder.setUserId(null);
            TraceIdHolder.setTenantId(null);
            TraceIdHolder.setOrganizationId(null);
            TraceIdHolder.setUserRoles(null);

            assertThat(TraceIdHolder.getUserId()).isNull();
            assertThat(TraceIdHolder.getTenantId()).isNull();
            assertThat(TraceIdHolder.getOrganizationId()).isNull();
            assertThat(TraceIdHolder.getUserRoles()).isNull();
        }

        @Test
        @DisplayName("빈 문자열은 설정되지 않는다")
        void shouldNotSetEmptyValues() {
            TraceIdHolder.setUserId("");
            TraceIdHolder.setTenantId("");
            TraceIdHolder.setOrganizationId("");
            TraceIdHolder.setUserRoles("");

            assertThat(TraceIdHolder.getUserId()).isNull();
            assertThat(TraceIdHolder.getTenantId()).isNull();
            assertThat(TraceIdHolder.getOrganizationId()).isNull();
            assertThat(TraceIdHolder.getUserRoles()).isNull();
        }
    }

    @Nested
    @DisplayName("Message Context 관리 테스트")
    class MessageContextManagementTest {

        @Test
        @DisplayName("MessageSource를 설정할 수 있다")
        void shouldSetMessageSource() {
            TraceIdHolder.setMessageSource("SQS");
            assertThat(MDC.get(TraceIdHeaders.MDC_MESSAGE_SOURCE)).isEqualTo("SQS");
        }

        @Test
        @DisplayName("MessageId를 설정할 수 있다")
        void shouldSetMessageId() {
            TraceIdHolder.setMessageId("msg-12345");
            assertThat(MDC.get(TraceIdHeaders.MDC_MESSAGE_ID)).isEqualTo("msg-12345");
        }

        @Test
        @DisplayName("null 값은 설정되지 않는다")
        void shouldNotSetNullMessageValues() {
            TraceIdHolder.setMessageSource(null);
            TraceIdHolder.setMessageId(null);

            assertThat(MDC.get(TraceIdHeaders.MDC_MESSAGE_SOURCE)).isNull();
            assertThat(MDC.get(TraceIdHeaders.MDC_MESSAGE_ID)).isNull();
        }
    }

    @Nested
    @DisplayName("추가 컨텍스트 관리 테스트")
    class AdditionalContextManagementTest {

        @Test
        @DisplayName("추가 컨텍스트를 설정하고 조회할 수 있다")
        void shouldAddAndGetContext() {
            TraceIdHolder.addContext("orderId", "ORD-123");
            assertThat(TraceIdHolder.getContext("orderId")).isEqualTo("ORD-123");
        }

        @Test
        @DisplayName("추가 컨텍스트가 MDC에도 저장된다")
        void shouldStoreContextInMdc() {
            TraceIdHolder.addContext("orderId", "ORD-123");
            assertThat(MDC.get(TraceIdHeaders.CONTEXT_PREFIX + "orderId")).isEqualTo("ORD-123");
        }

        @Test
        @DisplayName("모든 추가 컨텍스트를 조회할 수 있다")
        void shouldGetAllContext() {
            TraceIdHolder.addContext("key1", "value1");
            TraceIdHolder.addContext("key2", "value2");

            Map<String, String> context = TraceIdHolder.getAllContext();
            assertThat(context).hasSize(2);
            assertThat(context).containsEntry("key1", "value1");
            assertThat(context).containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("null 키 또는 값은 저장되지 않는다")
        void shouldNotAddNullKeyOrValue() {
            TraceIdHolder.addContext(null, "value");
            TraceIdHolder.addContext("key", null);

            assertThat(TraceIdHolder.getAllContext()).isEmpty();
        }
    }

    @Nested
    @DisplayName("clear 테스트")
    class ClearTest {

        @Test
        @DisplayName("clear 호출 시 모든 값이 제거된다")
        void shouldClearAllValues() {
            // 모든 값 설정
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setSpanId("span-id");
            TraceIdHolder.setServiceName("service");
            TraceIdHolder.setUserId("user");
            TraceIdHolder.setTenantId("tenant");
            TraceIdHolder.setOrganizationId("org");
            TraceIdHolder.setUserRoles("roles");
            TraceIdHolder.setMessageSource("SQS");
            TraceIdHolder.setMessageId("msg");
            TraceIdHolder.addContext("custom", "value");

            // clear 호출
            TraceIdHolder.clear();

            // 모든 값 확인
            assertThat(TraceIdHolder.get()).isEqualTo("unknown");
            assertThat(MDC.get(TraceIdHeaders.MDC_SPAN_ID)).isNull();
            assertThat(MDC.get(TraceIdHeaders.MDC_SERVICE_NAME)).isNull();
            assertThat(TraceIdHolder.getUserId()).isNull();
            assertThat(TraceIdHolder.getTenantId()).isNull();
            assertThat(TraceIdHolder.getOrganizationId()).isNull();
            assertThat(TraceIdHolder.getUserRoles()).isNull();
            assertThat(MDC.get(TraceIdHeaders.MDC_MESSAGE_SOURCE)).isNull();
            assertThat(MDC.get(TraceIdHeaders.MDC_MESSAGE_ID)).isNull();
            assertThat(TraceIdHolder.getAllContext()).isEmpty();
        }

        @Test
        @DisplayName("추가 컨텍스트의 MDC 키도 제거된다")
        void shouldRemoveContextFromMdc() {
            TraceIdHolder.addContext("custom", "value");
            assertThat(MDC.get(TraceIdHeaders.CONTEXT_PREFIX + "custom")).isEqualTo("value");

            TraceIdHolder.clear();
            assertThat(MDC.get(TraceIdHeaders.CONTEXT_PREFIX + "custom")).isNull();
        }
    }
}
