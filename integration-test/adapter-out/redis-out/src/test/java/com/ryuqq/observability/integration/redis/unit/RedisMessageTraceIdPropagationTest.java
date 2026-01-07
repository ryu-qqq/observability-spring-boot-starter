package com.ryuqq.observability.integration.redis.unit;

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
 * Redis 메시지 발행 시 TraceId 전파 통합 테스트.
 *
 * <p>Redis Pub/Sub 또는 Stream 발행 시 TraceIdHolder에서 TraceId를 가져와
 * 메시지 본문 또는 Stream 필드에 주입하는 기능을 검증합니다.</p>
 *
 * <p>실제 Redis 발행 인터셉터가 구현되면 이 테스트와 통합됩니다.</p>
 */
@DisplayName("Redis 메시지 발행 TraceId 전파 통합 테스트")
class RedisMessageTraceIdPropagationTest {

    @BeforeEach
    void setUp() {
        TraceIdHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("Redis Pub/Sub TraceId 전파 테스트")
    class RedisPubSubPropagationTest {

        @Test
        @DisplayName("현재 컨텍스트의 TraceId가 메시지에 포함된다")
        void shouldPropagateTraceIdToPubSubMessage() {
            // given
            String currentTraceId = "pubsub-trace-id-12345";
            TraceIdHolder.set(currentTraceId);

            // when - Pub/Sub 메시지 발행 시 컨텍스트 추가 (인터셉터가 수행할 동작 시뮬레이션)
            Map<String, Object> messagePayload = buildPubSubMessageWithContext("order.created", "{\"orderId\": 123}");

            // then
            assertThat(messagePayload).containsEntry(TraceIdHeaders.X_TRACE_ID, currentTraceId);
        }

        @Test
        @DisplayName("사용자 컨텍스트가 메시지에 포함된다")
        void shouldPropagateUserContextToPubSubMessage() {
            // given
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("pubsub-user-123");
            TraceIdHolder.setTenantId("pubsub-tenant-456");
            TraceIdHolder.setOrganizationId("pubsub-org-789");

            // when
            Map<String, Object> messagePayload = buildPubSubMessageWithContext("user.updated", "{\"userId\": 456}");

            // then
            assertThat(messagePayload)
                    .containsEntry(TraceIdHeaders.X_USER_ID, "pubsub-user-123")
                    .containsEntry(TraceIdHeaders.X_TENANT_ID, "pubsub-tenant-456")
                    .containsEntry(TraceIdHeaders.X_ORGANIZATION_ID, "pubsub-org-789");
        }
    }

    @Nested
    @DisplayName("Redis Stream TraceId 전파 테스트")
    class RedisStreamPropagationTest {

        @Test
        @DisplayName("현재 컨텍스트의 TraceId가 Stream 레코드 필드에 포함된다")
        void shouldPropagateTraceIdToStreamRecord() {
            // given
            String currentTraceId = "stream-trace-id-67890";
            TraceIdHolder.set(currentTraceId);

            // when - Stream 레코드 발행 시 컨텍스트 추가
            Map<String, String> streamFields = buildStreamRecordFieldsWithContext();

            // then
            assertThat(streamFields).containsEntry(TraceIdHeaders.X_TRACE_ID, currentTraceId);
        }

        @Test
        @DisplayName("사용자 컨텍스트가 Stream 레코드 필드에 포함된다")
        void shouldPropagateUserContextToStreamRecord() {
            // given
            TraceIdHolder.set("trace-id");
            TraceIdHolder.setUserId("stream-user-123");
            TraceIdHolder.setTenantId("stream-tenant-456");

            // when
            Map<String, String> streamFields = buildStreamRecordFieldsWithContext();

            // then
            assertThat(streamFields)
                    .containsEntry(TraceIdHeaders.X_USER_ID, "stream-user-123")
                    .containsEntry(TraceIdHeaders.X_TENANT_ID, "stream-tenant-456");
        }
    }

    @Nested
    @DisplayName("전체 컨텍스트 전파 테스트")
    class FullContextPropagationTest {

        @Test
        @DisplayName("모든 컨텍스트 정보가 메시지에 포함된다")
        void shouldPropagateAllContextToMessage() {
            // given
            TraceIdHolder.set("full-redis-trace-id");
            TraceIdHolder.setUserId("full-redis-user-id");
            TraceIdHolder.setTenantId("full-redis-tenant-id");
            TraceIdHolder.setOrganizationId("full-redis-org-id");

            // when
            Map<String, String> streamFields = buildStreamRecordFieldsWithContext();

            // then
            assertThat(streamFields)
                    .containsEntry(TraceIdHeaders.X_TRACE_ID, "full-redis-trace-id")
                    .containsEntry(TraceIdHeaders.X_USER_ID, "full-redis-user-id")
                    .containsEntry(TraceIdHeaders.X_TENANT_ID, "full-redis-tenant-id")
                    .containsEntry(TraceIdHeaders.X_ORGANIZATION_ID, "full-redis-org-id");
        }

        @Test
        @DisplayName("부분적인 컨텍스트만 있어도 정상 처리된다")
        void shouldHandlePartialContext() {
            // given - TraceId만 설정
            TraceIdHolder.set("partial-redis-trace-id");

            // when
            Map<String, String> streamFields = buildStreamRecordFieldsWithContext();

            // then
            assertThat(streamFields)
                    .containsKey(TraceIdHeaders.X_TRACE_ID)
                    .doesNotContainKey(TraceIdHeaders.X_USER_ID)
                    .doesNotContainKey(TraceIdHeaders.X_TENANT_ID)
                    .doesNotContainKey(TraceIdHeaders.X_ORGANIZATION_ID);
        }

        @Test
        @DisplayName("빈 컨텍스트에서도 에러 없이 빈 맵을 반환한다")
        void shouldReturnEmptyMapForEmptyContext() {
            // given - 아무것도 설정하지 않음

            // when
            Map<String, String> streamFields = buildStreamRecordFieldsWithContext();

            // then
            assertThat(streamFields).isEmpty();
        }
    }

    @Nested
    @DisplayName("채널별 발행 테스트")
    class ChannelSpecificPublishTest {

        @Test
        @DisplayName("다른 채널에 발행해도 동일한 컨텍스트가 전파된다")
        void shouldPropagateContextToMultipleChannels() {
            // given
            TraceIdHolder.set("multi-channel-trace");
            TraceIdHolder.setUserId("multi-channel-user");

            // when
            Map<String, Object> orderMessage = buildPubSubMessageWithContext("order-events", "{\"orderId\": 1}");
            Map<String, Object> paymentMessage = buildPubSubMessageWithContext("payment-events", "{\"paymentId\": 2}");

            // then
            assertThat(orderMessage).containsEntry(TraceIdHeaders.X_TRACE_ID, "multi-channel-trace");
            assertThat(paymentMessage).containsEntry(TraceIdHeaders.X_TRACE_ID, "multi-channel-trace");
            assertThat(orderMessage).containsEntry(TraceIdHeaders.X_USER_ID, "multi-channel-user");
            assertThat(paymentMessage).containsEntry(TraceIdHeaders.X_USER_ID, "multi-channel-user");
        }
    }

    /**
     * Pub/Sub 메시지에 현재 컨텍스트를 추가합니다.
     *
     * <p>실제 Redis 발행 인터셉터가 수행할 동작을 시뮬레이션합니다.</p>
     */
    private Map<String, Object> buildPubSubMessageWithContext(String channel, String payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("channel", channel);
        message.put("payload", payload);

        TraceIdHolder.getOptional().ifPresent(traceId ->
                message.put(TraceIdHeaders.X_TRACE_ID, traceId));

        String userId = TraceIdHolder.getUserId();
        if (userId != null) {
            message.put(TraceIdHeaders.X_USER_ID, userId);
        }

        String tenantId = TraceIdHolder.getTenantId();
        if (tenantId != null) {
            message.put(TraceIdHeaders.X_TENANT_ID, tenantId);
        }

        String orgId = TraceIdHolder.getOrganizationId();
        if (orgId != null) {
            message.put(TraceIdHeaders.X_ORGANIZATION_ID, orgId);
        }

        return message;
    }

    /**
     * Stream 레코드 필드에 현재 컨텍스트를 추가합니다.
     *
     * <p>실제 Redis Stream 발행 인터셉터가 수행할 동작을 시뮬레이션합니다.</p>
     */
    private Map<String, String> buildStreamRecordFieldsWithContext() {
        Map<String, String> fields = new HashMap<>();

        TraceIdHolder.getOptional().ifPresent(traceId ->
                fields.put(TraceIdHeaders.X_TRACE_ID, traceId));

        String userId = TraceIdHolder.getUserId();
        if (userId != null) {
            fields.put(TraceIdHeaders.X_USER_ID, userId);
        }

        String tenantId = TraceIdHolder.getTenantId();
        if (tenantId != null) {
            fields.put(TraceIdHeaders.X_TENANT_ID, tenantId);
        }

        String orgId = TraceIdHolder.getOrganizationId();
        if (orgId != null) {
            fields.put(TraceIdHeaders.X_ORGANIZATION_ID, orgId);
        }

        return fields;
    }
}
