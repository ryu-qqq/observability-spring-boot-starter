package com.ryuqq.observability.integration.redis.unit;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.message.config.MessageLoggingProperties;
import com.ryuqq.observability.message.context.MessageContext;
import com.ryuqq.observability.message.interceptor.MessageLoggingInterceptor;
import com.ryuqq.observability.message.redis.RedisMessageLoggingAspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Redis Listener TraceId 추출 통합 테스트.
 *
 * <p>RedisMessageLoggingAspect의 TraceId 추출 및 MDC 설정 기능을 검증합니다.</p>
 */
@DisplayName("Redis Listener TraceId 추출 통합 테스트")
class RedisListenerTraceIdExtractionTest {

    private MessageLoggingInterceptor interceptor;
    private RedisMessageLoggingAspect aspect;

    @BeforeEach
    void setUp() {
        MessageLoggingProperties properties = new MessageLoggingProperties();
        properties.setEnabled(true);
        properties.setLogPayload(true);

        LogMasker logMasker = mock(LogMasker.class);
        when(logMasker.mask(anyString())).thenAnswer(inv -> inv.getArgument(0));

        MessageLoggingInterceptor.TraceIdGenerator generator = () -> UUID.randomUUID().toString().replace("-", "");
        interceptor = new MessageLoggingInterceptor(properties, generator, logMasker, "redis-in-test");
        aspect = new RedisMessageLoggingAspect(interceptor);
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("Redis Pub/Sub TraceId 추출 테스트")
    class RedisPubSubTraceIdExtractionTest {

        @Test
        @DisplayName("Pub/Sub 메시지에서 TraceId를 추출한다")
        void shouldExtractTraceIdFromPubSubMessage() {
            // given
            String expectedTraceId = "redis-trace-id-12345";
            var context = MessageContext.builder()
                    .source("REDIS_PUBSUB")
                    .queueName("order-events")
                    .traceId(expectedTraceId)
                    .build();

            // when
            interceptor.beforeProcessing(context, "{\"orderId\": 123}");
            String actualTraceId = TraceIdHolder.get();
            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(actualTraceId).isEqualTo(expectedTraceId);
        }

        @Test
        @DisplayName("TraceId가 없으면 새로 생성한다")
        void shouldGenerateTraceIdWhenNotProvided() {
            // given
            var context = MessageContext.builder()
                    .source("REDIS_PUBSUB")
                    .queueName("user-events")
                    .build();

            // when
            interceptor.beforeProcessing(context, "{\"userId\": 456}");
            String generatedTraceId = TraceIdHolder.get();
            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(generatedTraceId).isNotNull();
            assertThat(generatedTraceId).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Redis Stream TraceId 추출 테스트")
    class RedisStreamTraceIdExtractionTest {

        @Test
        @DisplayName("Stream 레코드에서 TraceId를 추출한다")
        void shouldExtractTraceIdFromStreamRecord() {
            // given
            String expectedTraceId = "stream-trace-id-67890";
            var context = MessageContext.builder()
                    .source("REDIS_STREAM")
                    .queueName("payment-stream")
                    .messageId("1234567890-0")
                    .traceId(expectedTraceId)
                    .build();

            // when
            interceptor.beforeProcessing(context, "{\"paymentId\": 789}");
            String actualTraceId = TraceIdHolder.get();
            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(actualTraceId).isEqualTo(expectedTraceId);
        }

        @Test
        @DisplayName("Stream MessageId가 컨텍스트에 포함된다")
        void shouldIncludeStreamMessageIdInContext() {
            // given
            String messageId = "1234567890-1";
            var context = MessageContext.builder()
                    .source("REDIS_STREAM")
                    .queueName("inventory-stream")
                    .messageId(messageId)
                    .traceId("trace-id")
                    .build();

            // then
            assertThat(context.getMessageId()).isEqualTo(messageId);
        }
    }

    @Nested
    @DisplayName("사용자 컨텍스트 추출 테스트")
    class UserContextExtractionTest {

        @Test
        @DisplayName("UserId를 추출한다")
        void shouldExtractUserId() {
            // given
            String expectedUserId = "user-redis-123";
            var context = MessageContext.builder()
                    .source("REDIS_PUBSUB")
                    .queueName("user-channel")
                    .traceId("trace-id")
                    .attribute(TraceIdHeaders.X_USER_ID, expectedUserId)
                    .build();

            // when
            interceptor.beforeProcessing(context, "payload");
            String actualUserId = TraceIdHolder.getUserId();
            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(actualUserId).isEqualTo(expectedUserId);
        }

        @Test
        @DisplayName("TenantId를 추출한다")
        void shouldExtractTenantId() {
            // given
            String expectedTenantId = "tenant-redis-456";
            var context = MessageContext.builder()
                    .source("REDIS_STREAM")
                    .queueName("tenant-stream")
                    .traceId("trace-id")
                    .attribute(TraceIdHeaders.X_TENANT_ID, expectedTenantId)
                    .build();

            // when
            interceptor.beforeProcessing(context, "payload");
            String actualTenantId = TraceIdHolder.getTenantId();
            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(actualTenantId).isEqualTo(expectedTenantId);
        }

        @Test
        @DisplayName("OrganizationId를 추출한다")
        void shouldExtractOrganizationId() {
            // given
            String expectedOrgId = "org-redis-789";
            var context = MessageContext.builder()
                    .source("REDIS_PUBSUB")
                    .queueName("org-channel")
                    .traceId("trace-id")
                    .attribute(TraceIdHeaders.X_ORGANIZATION_ID, expectedOrgId)
                    .build();

            // when
            interceptor.beforeProcessing(context, "payload");
            String actualOrgId = TraceIdHolder.getOrganizationId();
            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(actualOrgId).isEqualTo(expectedOrgId);
        }

        @Test
        @DisplayName("모든 사용자 컨텍스트를 추출한다")
        void shouldExtractAllUserContext() {
            // given
            var context = MessageContext.builder()
                    .source("REDIS_STREAM")
                    .queueName("full-context-stream")
                    .traceId("trace-redis-full")
                    .attribute(TraceIdHeaders.X_USER_ID, "user-full-123")
                    .attribute(TraceIdHeaders.X_TENANT_ID, "tenant-full-456")
                    .attribute(TraceIdHeaders.X_ORGANIZATION_ID, "org-full-789")
                    .build();

            // when
            interceptor.beforeProcessing(context, "payload");

            String traceId = TraceIdHolder.get();
            String userId = TraceIdHolder.getUserId();
            String tenantId = TraceIdHolder.getTenantId();
            String orgId = TraceIdHolder.getOrganizationId();

            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(traceId).isEqualTo("trace-redis-full");
            assertThat(userId).isEqualTo("user-full-123");
            assertThat(tenantId).isEqualTo("tenant-full-456");
            assertThat(orgId).isEqualTo("org-full-789");
        }
    }

    @Nested
    @DisplayName("MDC 정리 테스트")
    class MdcCleanupTest {

        @Test
        @DisplayName("Pub/Sub 처리 완료 후 MDC를 정리한다")
        void shouldClearMdcAfterPubSubProcessing() {
            // given
            var context = MessageContext.builder()
                    .source("REDIS_PUBSUB")
                    .queueName("cleanup-channel")
                    .traceId("trace-pubsub-cleanup")
                    .attribute(TraceIdHeaders.X_USER_ID, "user-cleanup")
                    .build();

            // when
            interceptor.beforeProcessing(context, "payload");
            assertThat(TraceIdHolder.get()).isEqualTo("trace-pubsub-cleanup");

            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Stream 처리 완료 후 MDC를 정리한다")
        void shouldClearMdcAfterStreamProcessing() {
            // given
            var context = MessageContext.builder()
                    .source("REDIS_STREAM")
                    .queueName("cleanup-stream")
                    .messageId("cleanup-msg-id")
                    .traceId("trace-stream-cleanup")
                    .build();

            // when
            interceptor.beforeProcessing(context, "payload");
            assertThat(TraceIdHolder.get()).isEqualTo("trace-stream-cleanup");

            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("예외 발생 후에도 MDC를 정리한다")
        void shouldClearMdcAfterException() {
            // given
            var context = MessageContext.builder()
                    .source("REDIS_PUBSUB")
                    .queueName("error-channel")
                    .traceId("trace-with-error")
                    .build();

            // when
            interceptor.beforeProcessing(context, "payload");
            assertThat(TraceIdHolder.get()).isEqualTo("trace-with-error");

            interceptor.afterProcessing(context, false, new RuntimeException("redis error"));

            // then
            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("소스별 MessageContext 빌드 테스트")
    class SourceSpecificContextBuildTest {

        @Test
        @DisplayName("REDIS_PUBSUB 소스로 컨텍스트를 생성한다")
        void shouldBuildContextWithPubSubSource() {
            // given & when
            var context = MessageContext.builder()
                    .source("REDIS_PUBSUB")
                    .queueName("pubsub-channel")
                    .traceId("pubsub-trace")
                    .build();

            // then
            assertThat(context.getSource()).isEqualTo("REDIS_PUBSUB");
            assertThat(context.getQueueName()).isEqualTo("pubsub-channel");
            assertThat(context.getTraceId()).isEqualTo("pubsub-trace");
        }

        @Test
        @DisplayName("REDIS_STREAM 소스로 컨텍스트를 생성한다")
        void shouldBuildContextWithStreamSource() {
            // given & when
            var context = MessageContext.builder()
                    .source("REDIS_STREAM")
                    .queueName("stream-name")
                    .messageId("stream-msg-id")
                    .traceId("stream-trace")
                    .build();

            // then
            assertThat(context.getSource()).isEqualTo("REDIS_STREAM");
            assertThat(context.getQueueName()).isEqualTo("stream-name");
            assertThat(context.getMessageId()).isEqualTo("stream-msg-id");
            assertThat(context.getTraceId()).isEqualTo("stream-trace");
        }

        @Test
        @DisplayName("처리 시간을 계산한다")
        void shouldCalculateDuration() throws InterruptedException {
            // given
            var context = MessageContext.builder()
                    .source("REDIS_PUBSUB")
                    .queueName("duration-channel")
                    .build();

            // when
            Thread.sleep(50);
            long duration = context.calculateDuration();

            // then
            assertThat(duration).isGreaterThanOrEqualTo(50);
        }
    }
}
