package com.ryuqq.observability.integration.sqs;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQS 메시지 발행 시 TraceId 전파 통합 테스트.
 *
 * <p>SQS 메시지 발행 시 TraceIdHolder에서 TraceId를 가져와
 * 메시지 속성(MessageAttributes)에 주입하는 기능을 검증합니다.</p>
 *
 * <p>실제 SQS 발행 인터셉터가 구현되면 이 테스트와 통합됩니다.</p>
 */
@DisplayName("SQS 메시지 발행 TraceId 전파 통합 테스트")
class SqsMessageTraceIdPropagationTest {

    @BeforeEach
    void setUp() {
        TraceIdHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("TraceId 메시지 속성 주입 테스트")
    class TraceIdMessageAttributeInjectionTest {

        @Test
        @DisplayName("현재 컨텍스트의 TraceId가 메시지 속성으로 전달된다")
        void shouldPropagateTraceIdToMessageAttributes() {
            // given
            String currentTraceId = "current-trace-id-12345";
            TraceIdHolder.set(currentTraceId);

            // when - 메시지 발행 전 현재 TraceId를 속성에 추가 (인터셉터가 수행할 동작 시뮬레이션)
            Map<String, String> messageAttributes = buildMessageAttributesFromContext();

            // then
            assertThat(messageAttributes).containsEntry(TraceIdHeaders.X_TRACE_ID, currentTraceId);
        }

        @Test
        @DisplayName("TraceId가 없으면 메시지 속성에 포함되지 않는다")
        void shouldNotIncludeTraceIdWhenNotPresent() {
            // given - TraceIdHolder가 비어있음

            // when
            Map<String, String> messageAttributes = buildMessageAttributesFromContext();

            // then
            assertThat(messageAttributes).doesNotContainKey(TraceIdHeaders.X_TRACE_ID);
        }

        @Test
        @DisplayName("UserId가 있으면 메시지 속성으로 전달된다")
        void shouldPropagateUserIdToMessageAttributes() {
            // given
            String userId = "user-sqs-out-123";
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId(userId);

            // when
            Map<String, String> messageAttributes = buildMessageAttributesFromContext();

            // then
            assertThat(messageAttributes).containsEntry(TraceIdHeaders.X_USER_ID, userId);
        }

        @Test
        @DisplayName("TenantId가 있으면 메시지 속성으로 전달된다")
        void shouldPropagateTenantIdToMessageAttributes() {
            // given
            String tenantId = "tenant-sqs-out-456";
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setTenantId(tenantId);

            // when
            Map<String, String> messageAttributes = buildMessageAttributesFromContext();

            // then
            assertThat(messageAttributes).containsEntry(TraceIdHeaders.X_TENANT_ID, tenantId);
        }

        @Test
        @DisplayName("OrganizationId가 있으면 메시지 속성으로 전달된다")
        void shouldPropagateOrganizationIdToMessageAttributes() {
            // given
            String orgId = "org-sqs-out-789";
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setOrganizationId(orgId);

            // when
            Map<String, String> messageAttributes = buildMessageAttributesFromContext();

            // then
            assertThat(messageAttributes).containsEntry(TraceIdHeaders.X_ORGANIZATION_ID, orgId);
        }
    }

    @Nested
    @DisplayName("전체 컨텍스트 전파 테스트")
    class FullContextPropagationTest {

        @Test
        @DisplayName("모든 컨텍스트 정보가 메시지 속성으로 전달된다")
        void shouldPropagateAllContextToMessageAttributes() {
            // given
            TraceIdHolder.set("full-trace-id");
            TraceIdHolder.setUserId("full-user-id");
            TraceIdHolder.setTenantId("full-tenant-id");
            TraceIdHolder.setOrganizationId("full-org-id");

            // when
            Map<String, String> messageAttributes = buildMessageAttributesFromContext();

            // then
            assertThat(messageAttributes)
                    .containsEntry(TraceIdHeaders.X_TRACE_ID, "full-trace-id")
                    .containsEntry(TraceIdHeaders.X_USER_ID, "full-user-id")
                    .containsEntry(TraceIdHeaders.X_TENANT_ID, "full-tenant-id")
                    .containsEntry(TraceIdHeaders.X_ORGANIZATION_ID, "full-org-id");
        }

        @Test
        @DisplayName("부분적인 컨텍스트만 있어도 메시지 속성에 정상 포함된다")
        void shouldHandlePartialContext() {
            // given - TraceId와 UserId만 설정
            TraceIdHolder.set("partial-trace-id");
            TraceIdHolder.setUserId("partial-user-id");

            // when
            Map<String, String> messageAttributes = buildMessageAttributesFromContext();

            // then
            assertThat(messageAttributes)
                    .containsEntry(TraceIdHeaders.X_TRACE_ID, "partial-trace-id")
                    .containsEntry(TraceIdHeaders.X_USER_ID, "partial-user-id")
                    .doesNotContainKey(TraceIdHeaders.X_TENANT_ID)
                    .doesNotContainKey(TraceIdHeaders.X_ORGANIZATION_ID);
        }
    }

    @Nested
    @DisplayName("메시지 속성 빌드 유틸리티 테스트")
    class MessageAttributeBuilderTest {

        @Test
        @DisplayName("빈 컨텍스트에서도 에러 없이 빈 맵을 반환한다")
        void shouldReturnEmptyMapForEmptyContext() {
            // given - 아무것도 설정하지 않음

            // when
            Map<String, String> messageAttributes = buildMessageAttributesFromContext();

            // then
            assertThat(messageAttributes).isEmpty();
        }

        @Test
        @DisplayName("null 값은 메시지 속성에 포함되지 않는다")
        void shouldNotIncludeNullValues() {
            // given
            TraceIdHolder.set("trace-id");
            // UserId, TenantId, OrganizationId는 설정하지 않음

            // when
            Map<String, String> messageAttributes = buildMessageAttributesFromContext();

            // then
            assertThat(messageAttributes)
                    .containsKey(TraceIdHeaders.X_TRACE_ID)
                    .doesNotContainKey(TraceIdHeaders.X_USER_ID)
                    .doesNotContainKey(TraceIdHeaders.X_TENANT_ID)
                    .doesNotContainKey(TraceIdHeaders.X_ORGANIZATION_ID);
        }
    }

    /**
     * 현재 TraceIdHolder 컨텍스트에서 메시지 속성 맵을 생성합니다.
     *
     * <p>실제 SQS 발행 인터셉터가 수행할 동작을 시뮬레이션합니다.</p>
     *
     * @return 메시지 속성 맵 (SQS MessageAttributes에 해당)
     */
    private Map<String, String> buildMessageAttributesFromContext() {
        Map<String, String> attributes = new HashMap<>();

        TraceIdHolder.getOptional().ifPresent(traceId ->
                attributes.put(TraceIdHeaders.X_TRACE_ID, traceId));

        String userId = TraceIdHolder.getUserId();
        if (userId != null) {
            attributes.put(TraceIdHeaders.X_USER_ID, userId);
        }

        String tenantId = TraceIdHolder.getTenantId();
        if (tenantId != null) {
            attributes.put(TraceIdHeaders.X_TENANT_ID, tenantId);
        }

        String orgId = TraceIdHolder.getOrganizationId();
        if (orgId != null) {
            attributes.put(TraceIdHeaders.X_ORGANIZATION_ID, orgId);
        }

        return attributes;
    }
}
