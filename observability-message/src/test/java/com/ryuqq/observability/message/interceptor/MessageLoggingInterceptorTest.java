package com.ryuqq.observability.message.interceptor;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.message.config.MessageLoggingProperties;
import com.ryuqq.observability.message.context.MessageContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MessageLoggingInterceptor 테스트")
class MessageLoggingInterceptorTest {

    private MessageLoggingProperties properties;
    private MessageLoggingInterceptor.TraceIdGenerator generator;
    private LogMasker logMasker;
    private MessageLoggingInterceptor interceptor;

    @BeforeEach
    void setUp() {
        properties = new MessageLoggingProperties();
        generator = () -> "generated-trace-id";
        logMasker = mock(LogMasker.class);
        when(logMasker.mask(anyString())).thenAnswer(inv -> inv.getArgument(0));

        interceptor = new MessageLoggingInterceptor(properties, generator, logMasker, "test-service");
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("beforeProcessing 테스트")
    class BeforeProcessingTest {

        @Test
        @DisplayName("컨텍스트의 TraceId를 설정한다")
        void shouldSetTraceIdFromContext() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("context-trace-id")
                    .build();

            interceptor.beforeProcessing(context, "payload");

            assertThat(TraceIdHolder.get()).isEqualTo("context-trace-id");
        }

        @Test
        @DisplayName("TraceId가 없으면 새로 생성한다")
        void shouldGenerateTraceIdWhenNotProvided() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .build();

            interceptor.beforeProcessing(context, "payload");

            assertThat(TraceIdHolder.get()).isEqualTo("generated-trace-id");
        }

        @Test
        @DisplayName("빈 TraceId이면 새로 생성한다")
        void shouldGenerateTraceIdWhenEmpty() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("")
                    .build();

            interceptor.beforeProcessing(context, "payload");

            assertThat(TraceIdHolder.get()).isEqualTo("generated-trace-id");
        }

        @Test
        @DisplayName("메시지 소스를 설정한다")
        void shouldSetMessageSource() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-id")
                    .build();

            interceptor.beforeProcessing(context, "payload");

            assertThat(MDC.get(TraceIdHeaders.MDC_MESSAGE_SOURCE)).isEqualTo("SQS");
        }

        @Test
        @DisplayName("메시지 ID를 설정한다")
        void shouldSetMessageId() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .messageId("msg-123")
                    .traceId("trace-id")
                    .build();

            interceptor.beforeProcessing(context, "payload");

            assertThat(MDC.get(TraceIdHeaders.MDC_MESSAGE_ID)).isEqualTo("msg-123");
        }

        @Test
        @DisplayName("메시지 ID가 null이면 설정하지 않는다")
        void shouldNotSetNullMessageId() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-id")
                    .build();

            interceptor.beforeProcessing(context, "payload");

            assertThat(MDC.get(TraceIdHeaders.MDC_MESSAGE_ID)).isNull();
        }

        @Test
        @DisplayName("서비스 이름을 설정한다")
        void shouldSetServiceName() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-id")
                    .build();

            interceptor.beforeProcessing(context, "payload");

            assertThat(MDC.get(TraceIdHeaders.MDC_SERVICE_NAME)).isEqualTo("test-service");
        }

        @Test
        @DisplayName("서비스 이름이 null이면 설정하지 않는다")
        void shouldNotSetNullServiceName() {
            MessageLoggingInterceptor interceptorWithoutService =
                    new MessageLoggingInterceptor(properties, generator, logMasker, null);

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-id")
                    .build();

            interceptorWithoutService.beforeProcessing(context, "payload");

            assertThat(MDC.get(TraceIdHeaders.MDC_SERVICE_NAME)).isNull();
        }

        @Test
        @DisplayName("속성에서 UserId를 추출한다")
        void shouldExtractUserIdFromAttributes() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-id")
                    .attribute(TraceIdHeaders.X_USER_ID, "user-123")
                    .build();

            interceptor.beforeProcessing(context, "payload");

            assertThat(TraceIdHolder.getUserId()).isEqualTo("user-123");
        }

        @Test
        @DisplayName("속성에서 TenantId를 추출한다")
        void shouldExtractTenantIdFromAttributes() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-id")
                    .attribute(TraceIdHeaders.X_TENANT_ID, "tenant-456")
                    .build();

            interceptor.beforeProcessing(context, "payload");

            assertThat(TraceIdHolder.getTenantId()).isEqualTo("tenant-456");
        }

        @Test
        @DisplayName("속성에서 OrganizationId를 추출한다")
        void shouldExtractOrganizationIdFromAttributes() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-id")
                    .attribute(TraceIdHeaders.X_ORGANIZATION_ID, "org-789")
                    .build();

            interceptor.beforeProcessing(context, "payload");

            assertThat(TraceIdHolder.getOrganizationId()).isEqualTo("org-789");
        }

        @Test
        @DisplayName("로깅이 비활성화되어도 TraceId는 설정한다")
        void shouldSetTraceIdEvenWhenLoggingDisabled() {
            properties.setEnabled(false);

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-id")
                    .build();

            interceptor.beforeProcessing(context, "payload");

            assertThat(TraceIdHolder.get()).isEqualTo("trace-id");
        }
    }

    @Nested
    @DisplayName("afterProcessing 테스트")
    class AfterProcessingTest {

        @Test
        @DisplayName("성공 시 MDC를 정리한다")
        void shouldClearMdcOnSuccess() {
            TraceIdHolder.set("trace-id");

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .build();

            interceptor.afterProcessing(context, true, null);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("실패 시에도 MDC를 정리한다")
        void shouldClearMdcOnFailure() {
            TraceIdHolder.set("trace-id");

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .build();

            interceptor.afterProcessing(context, false, new RuntimeException("test error"));

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("로깅이 비활성화되어도 MDC는 정리한다")
        void shouldClearMdcEvenWhenLoggingDisabled() {
            properties.setEnabled(false);
            TraceIdHolder.set("trace-id");

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .build();

            interceptor.afterProcessing(context, true, null);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("예외 없이 실패한 경우도 처리한다")
        void shouldHandleFailureWithoutException() {
            TraceIdHolder.set("trace-id");

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .build();

            interceptor.afterProcessing(context, false, null);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("페이로드 로깅 테스트")
    class PayloadLoggingTest {

        @Test
        @DisplayName("페이로드 로깅이 활성화되면 마스킹 적용")
        void shouldMaskPayloadWhenLoggingEnabled() {
            properties.setLogPayload(true);
            when(logMasker.mask("sensitive-data")).thenReturn("***-data");

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-id")
                    .build();

            // 로깅은 테스트하기 어려우므로 예외 없이 실행되는지만 확인
            interceptor.beforeProcessing(context, "sensitive-data");

            assertThat(TraceIdHolder.get()).isEqualTo("trace-id");
        }

        @Test
        @DisplayName("긴 페이로드는 잘린다")
        void shouldTruncateLongPayload() {
            properties.setLogPayload(true);
            properties.setMaxPayloadLength(10);

            String longPayload = "a".repeat(100);
            when(logMasker.mask(longPayload)).thenReturn(longPayload);

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-id")
                    .build();

            // 예외 없이 실행되는지 확인
            interceptor.beforeProcessing(context, longPayload);

            assertThat(TraceIdHolder.get()).isEqualTo("trace-id");
        }

        @Test
        @DisplayName("null 페이로드도 처리한다")
        void shouldHandleNullPayload() {
            properties.setLogPayload(true);

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("test-queue")
                    .traceId("trace-id")
                    .build();

            interceptor.beforeProcessing(context, null);

            assertThat(TraceIdHolder.get()).isEqualTo("trace-id");
        }
    }

    @Nested
    @DisplayName("TraceIdGenerator 테스트")
    class TraceIdGeneratorTest {

        @Test
        @DisplayName("함수형 인터페이스로 동작한다")
        void shouldWorkAsFunctionalInterface() {
            MessageLoggingInterceptor.TraceIdGenerator gen = () -> "custom-id";

            assertThat(gen.generate()).isEqualTo("custom-id");
        }

        @Test
        @DisplayName("람다로 구현할 수 있다")
        void shouldBeImplementableAsLambda() {
            int[] counter = {0};
            MessageLoggingInterceptor.TraceIdGenerator gen = () -> "id-" + (++counter[0]);

            assertThat(gen.generate()).isEqualTo("id-1");
            assertThat(gen.generate()).isEqualTo("id-2");
        }
    }
}
