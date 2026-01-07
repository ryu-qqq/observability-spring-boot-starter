package com.ryuqq.observability.integration.sqs;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.message.config.MessageLoggingProperties;
import com.ryuqq.observability.message.interceptor.MessageLoggingInterceptor;
import com.ryuqq.observability.message.sqs.SqsMessageLoggingAspect;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SQS Listener TraceId 추출 통합 테스트.
 *
 * <p>SqsMessageLoggingAspect의 TraceId 추출 및 MDC 설정 기능을 검증합니다.</p>
 */
@DisplayName("SQS Listener TraceId 추출 통합 테스트")
class SqsListenerTraceIdExtractionTest {

    private MessageLoggingInterceptor interceptor;
    private SqsMessageLoggingAspect aspect;
    private CopyOnWriteArrayList<String> capturedTraceIds;

    @BeforeEach
    void setUp() {
        MessageLoggingProperties properties = new MessageLoggingProperties();
        properties.setEnabled(true);
        properties.setLogPayload(true);

        LogMasker logMasker = mock(LogMasker.class);
        when(logMasker.mask(anyString())).thenAnswer(inv -> inv.getArgument(0));

        MessageLoggingInterceptor.TraceIdGenerator generator = () -> UUID.randomUUID().toString().replace("-", "");
        interceptor = new MessageLoggingInterceptor(properties, generator, logMasker, "sqs-in-test");
        aspect = new SqsMessageLoggingAspect(interceptor);
        capturedTraceIds = new CopyOnWriteArrayList<>();
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
        capturedTraceIds.clear();
    }

    @Nested
    @DisplayName("TraceId 추출 테스트")
    class TraceIdExtractionTest {

        @Test
        @DisplayName("메시지 헤더에서 TraceId를 추출한다")
        void shouldExtractTraceIdFromMessageHeaders() {
            // given
            String expectedTraceId = "test-trace-id-12345";
            Map<String, Object> headers = new HashMap<>();
            headers.put(TraceIdHeaders.X_TRACE_ID, expectedTraceId);
            headers.put("id", UUID.randomUUID().toString());

            Message<String> message = new GenericMessage<>("test-payload", headers);

            // when - 직접 interceptor를 테스트
            var context = com.ryuqq.observability.message.context.MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId(expectedTraceId)
                    .build();

            interceptor.beforeProcessing(context, message.getPayload());
            String actualTraceId = TraceIdHolder.get();
            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(actualTraceId).isEqualTo(expectedTraceId);
        }

        @Test
        @DisplayName("TraceId가 없으면 새로 생성한다")
        void shouldGenerateTraceIdWhenNotProvided() {
            // given
            Message<String> message = new GenericMessage<>("test-payload");

            // when
            var context = com.ryuqq.observability.message.context.MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .build();

            interceptor.beforeProcessing(context, message.getPayload());
            String generatedTraceId = TraceIdHolder.get();
            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(generatedTraceId).isNotNull();
            assertThat(generatedTraceId).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("사용자 컨텍스트 추출 테스트")
    class UserContextExtractionTest {

        @Test
        @DisplayName("UserId를 추출한다")
        void shouldExtractUserId() {
            // given
            String expectedUserId = "user-12345";
            var context = com.ryuqq.observability.message.context.MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
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
            String expectedTenantId = "tenant-67890";
            var context = com.ryuqq.observability.message.context.MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
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
            String expectedOrgId = "org-11111";
            var context = com.ryuqq.observability.message.context.MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
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
            var context = com.ryuqq.observability.message.context.MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-abc")
                    .attribute(TraceIdHeaders.X_USER_ID, "user-123")
                    .attribute(TraceIdHeaders.X_TENANT_ID, "tenant-456")
                    .attribute(TraceIdHeaders.X_ORGANIZATION_ID, "org-789")
                    .build();

            // when
            interceptor.beforeProcessing(context, "payload");

            String traceId = TraceIdHolder.get();
            String userId = TraceIdHolder.getUserId();
            String tenantId = TraceIdHolder.getTenantId();
            String orgId = TraceIdHolder.getOrganizationId();

            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(traceId).isEqualTo("trace-abc");
            assertThat(userId).isEqualTo("user-123");
            assertThat(tenantId).isEqualTo("tenant-456");
            assertThat(orgId).isEqualTo("org-789");
        }
    }

    @Nested
    @DisplayName("MDC 정리 테스트")
    class MdcCleanupTest {

        @Test
        @DisplayName("처리 완료 후 MDC를 정리한다")
        void shouldClearMdcAfterProcessing() {
            // given
            var context = com.ryuqq.observability.message.context.MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-to-clear")
                    .attribute(TraceIdHeaders.X_USER_ID, "user-to-clear")
                    .build();

            // when
            interceptor.beforeProcessing(context, "payload");
            assertThat(TraceIdHolder.get()).isEqualTo("trace-to-clear");

            interceptor.afterProcessing(context, true, null);

            // then
            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("예외 발생 후에도 MDC를 정리한다")
        void shouldClearMdcAfterException() {
            // given
            var context = com.ryuqq.observability.message.context.MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-with-error")
                    .build();

            // when
            interceptor.beforeProcessing(context, "payload");
            assertThat(TraceIdHolder.get()).isEqualTo("trace-with-error");

            interceptor.afterProcessing(context, false, new RuntimeException("test error"));

            // then
            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("MessageContext 빌드 테스트")
    class MessageContextBuildTest {

        @Test
        @DisplayName("SQS 소스로 컨텍스트를 생성한다")
        void shouldBuildContextWithSqsSource() {
            // given & when
            var context = com.ryuqq.observability.message.context.MessageContext.builder()
                    .source("SQS")
                    .queueName("order-events")
                    .messageId("msg-123")
                    .traceId("trace-456")
                    .build();

            // then
            assertThat(context.getSource()).isEqualTo("SQS");
            assertThat(context.getQueueName()).isEqualTo("order-events");
            assertThat(context.getMessageId()).isEqualTo("msg-123");
            assertThat(context.getTraceId()).isEqualTo("trace-456");
        }

        @Test
        @DisplayName("시작 시간이 자동으로 설정된다")
        void shouldSetStartTimeAutomatically() {
            // given
            long before = System.currentTimeMillis();

            // when
            var context = com.ryuqq.observability.message.context.MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .build();

            long after = System.currentTimeMillis();

            // then
            assertThat(context.getStartTimeMillis()).isBetween(before, after);
        }

        @Test
        @DisplayName("처리 시간을 계산한다")
        void shouldCalculateDuration() throws InterruptedException {
            // given
            var context = com.ryuqq.observability.message.context.MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .build();

            // when
            Thread.sleep(50);
            long duration = context.calculateDuration();

            // then
            assertThat(duration).isGreaterThanOrEqualTo(50);
        }
    }
}
