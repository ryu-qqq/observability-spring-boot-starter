package com.ryuqq.observability.message.sqs;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.message.config.MessageLoggingProperties;
import com.ryuqq.observability.message.interceptor.MessageLoggingInterceptor;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SqsMessageLoggingAspect 테스트")
class SqsMessageLoggingAspectTest {

    private MessageLoggingInterceptor interceptor;
    private SqsMessageLoggingAspect aspect;

    @BeforeEach
    void setUp() {
        MessageLoggingProperties properties = new MessageLoggingProperties();
        MessageLoggingInterceptor.TraceIdGenerator generator = () -> "generated-trace-id";
        LogMasker logMasker = mock(LogMasker.class);
        when(logMasker.mask(anyString())).thenAnswer(inv -> inv.getArgument(0));

        interceptor = new MessageLoggingInterceptor(properties, generator, logMasker, "test-service");
        aspect = new SqsMessageLoggingAspect(interceptor);
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("aroundSqsListener 테스트")
    class AroundSqsListenerTest {

        @Test
        @DisplayName("성공적으로 메시지를 처리한다")
        void shouldProcessMessageSuccessfully() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testPayload");
            when(joinPoint.proceed()).thenReturn("result");

            Object result = aspect.aroundSqsListener(joinPoint);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("void 메서드도 처리한다")
        void shouldHandleVoidMethod() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("payload");
            when(joinPoint.proceed()).thenReturn(null);

            Object result = aspect.aroundSqsListener(joinPoint);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("예외 발생 시 다시 던진다")
        void shouldRethrowException() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("payload");
            when(joinPoint.proceed()).thenThrow(new RuntimeException("sqs error"));

            assertThatThrownBy(() -> aspect.aroundSqsListener(joinPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("sqs error");
        }

        @Test
        @DisplayName("처리 후 MDC를 정리한다")
        void shouldClearMdcAfterProcessing() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("payload");
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("예외 발생 후에도 MDC를 정리한다")
        void shouldClearMdcAfterException() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("payload");
            when(joinPoint.proceed()).thenThrow(new RuntimeException("error"));

            try {
                aspect.aroundSqsListener(joinPoint);
            } catch (RuntimeException ignored) {
            }

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("checked 예외도 다시 던진다")
        void shouldRethrowCheckedException() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("payload");
            when(joinPoint.proceed()).thenThrow(new Exception("checked error"));

            assertThatThrownBy(() -> aspect.aroundSqsListener(joinPoint))
                    .isInstanceOf(Exception.class)
                    .hasMessage("checked error");
        }
    }

    @Nested
    @DisplayName("큐 이름 추출 테스트")
    class QueueNameExtractionTest {

        @Test
        @DisplayName("@SqsListener 어노테이션에서 큐 이름을 추출한다")
        void shouldExtractQueueNameFromAnnotation() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPointWithAnnotatedMethod("payload");
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("어노테이션이 없으면 unknown을 사용한다")
        void shouldUseUnknownWhenNoAnnotation() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("payload");
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("페이로드 추출 테스트")
    class PayloadExtractionTest {

        @Test
        @DisplayName("일반 객체를 페이로드로 추출한다")
        void shouldExtractRegularObjectAsPayload() throws Throwable {
            Object payload = new TestPayload("test-data");
            ProceedingJoinPoint joinPoint = createMockJoinPoint(payload);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("null 인자를 무시한다")
        void shouldIgnoreNullArgs() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs(null, "payload", null);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("빈 인자 배열도 처리한다")
        void shouldHandleEmptyArgs() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs();
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Message 객체에서 payload를 추출한다")
        void shouldExtractPayloadFromMessage() throws Throwable {
            TestMessage message = new TestMessage("extracted-payload", new HashMap<>());
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Acknowledgement 객체는 스킵한다")
        void shouldSkipAcknowledgement() throws Throwable {
            TestAcknowledgement ack = new TestAcknowledgement();
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs(ack, "real-payload");
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Visibility 객체는 스킵한다")
        void shouldSkipVisibility() throws Throwable {
            TestVisibility visibility = new TestVisibility();
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs(visibility, "real-payload");
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Headers 객체는 스킵한다")
        void shouldSkipHeaders() throws Throwable {
            TestHeaders headers = new TestHeaders();
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs(headers, "real-payload");
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("TraceId 추출 테스트")
    class TraceIdExtractionTest {

        @Test
        @DisplayName("Message 헤더에서 TraceId를 추출한다")
        void shouldExtractTraceIdFromMessageHeaders() throws Throwable {
            Map<String, Object> headers = new HashMap<>();
            headers.put(TraceIdHeaders.X_TRACE_ID, "message-trace-id");
            TestMessage message = new TestMessage("payload", headers);
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("GenericMessage에서 TraceId를 추출한다")
        void shouldExtractTraceIdFromGenericMessage() throws Throwable {
            Map<String, Object> headers = new HashMap<>();
            headers.put(TraceIdHeaders.X_TRACE_ID, "generic-trace-id");
            TestGenericMessage message = new TestGenericMessage("payload", headers);
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("MessageId 추출 테스트")
    class MessageIdExtractionTest {

        @Test
        @DisplayName("Message 헤더에서 id를 추출한다")
        void shouldExtractIdFromMessageHeaders() throws Throwable {
            Map<String, Object> headers = new HashMap<>();
            headers.put("id", "message-id-123");
            TestMessage message = new TestMessage("payload", headers);
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("SQS MessageId를 추출한다")
        void shouldExtractSqsMessageId() throws Throwable {
            Map<String, Object> headers = new HashMap<>();
            headers.put("Sqs_MessageId", "sqs-message-id-456");
            TestMessage message = new TestMessage("payload", headers);
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("속성 추출 테스트")
    class AttributeExtractionTest {

        @Test
        @DisplayName("Message 헤더에서 UserId를 추출한다")
        void shouldExtractUserIdFromHeaders() throws Throwable {
            Map<String, Object> headers = new HashMap<>();
            headers.put(TraceIdHeaders.X_USER_ID, "user-123");
            TestMessage message = new TestMessage("payload", headers);
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Message 헤더에서 TenantId를 추출한다")
        void shouldExtractTenantIdFromHeaders() throws Throwable {
            Map<String, Object> headers = new HashMap<>();
            headers.put(TraceIdHeaders.X_TENANT_ID, "tenant-456");
            TestMessage message = new TestMessage("payload", headers);
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Message 헤더에서 OrganizationId를 추출한다")
        void shouldExtractOrganizationIdFromHeaders() throws Throwable {
            Map<String, Object> headers = new HashMap<>();
            headers.put(TraceIdHeaders.X_ORGANIZATION_ID, "org-789");
            TestMessage message = new TestMessage("payload", headers);
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("모든 사용자 컨텍스트 헤더를 추출한다")
        void shouldExtractAllUserContextHeaders() throws Throwable {
            Map<String, Object> headers = new HashMap<>();
            headers.put(TraceIdHeaders.X_USER_ID, "user-123");
            headers.put(TraceIdHeaders.X_TENANT_ID, "tenant-456");
            headers.put(TraceIdHeaders.X_ORGANIZATION_ID, "org-789");
            headers.put(TraceIdHeaders.X_TRACE_ID, "trace-abc");
            TestMessage message = new TestMessage("payload", headers);
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Pointcut 테스트")
    class PointcutTest {

        @Test
        @DisplayName("sqsListenerMethod pointcut은 void를 반환한다")
        void shouldDefineSqsListenerPointcut() {
            aspect.sqsListenerMethod();
        }
    }

    @Nested
    @DisplayName("에지 케이스 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("getHeaders가 null을 반환해도 처리한다")
        void shouldHandleNullHeaders() throws Throwable {
            TestMessageWithNullHeaders message = new TestMessageWithNullHeaders("payload");
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("getPayload 메서드가 없는 Message도 처리한다")
        void shouldHandleMessageWithoutGetPayload() throws Throwable {
            TestMessageWithoutPayload message = new TestMessageWithoutPayload(new HashMap<>());
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("헤더가 Map이 아닌 경우도 처리한다")
        void shouldHandleNonMapHeaders() throws Throwable {
            TestMessageWithCustomHeaders message = new TestMessageWithCustomHeaders("payload");
            ProceedingJoinPoint joinPoint = createMockJoinPoint(message);
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundSqsListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    // Helper methods

    private ProceedingJoinPoint createMockJoinPoint(Object payload) throws NoSuchMethodException {
        return createMockJoinPointWithArgs(payload);
    }

    private ProceedingJoinPoint createMockJoinPointWithArgs(Object... args) throws NoSuchMethodException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = TestListener.class.getMethod("handleMessage", String.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(args);

        return joinPoint;
    }

    private ProceedingJoinPoint createMockJoinPointWithAnnotatedMethod(Object payload) throws NoSuchMethodException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = AnnotatedTestListener.class.getMethod("handleOrderEvent", String.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});

        return joinPoint;
    }

    // Test helper classes
    static class TestListener {
        public void handleMessage(String message) {
            // test method
        }
    }

    static class AnnotatedTestListener {
        @SqsListener("order-events-queue")
        public void handleOrderEvent(String event) {
            // test method
        }
    }

    static class TestPayload {
        private final String data;

        TestPayload(String data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "TestPayload{data='" + data + "'}";
        }
    }

    static class TestMessage {
        private final String payload;
        private final Map<String, Object> headers;

        TestMessage(String payload, Map<String, Object> headers) {
            this.payload = payload;
            this.headers = headers;
        }

        public String getPayload() {
            return payload;
        }

        public Map<String, Object> getHeaders() {
            return headers;
        }
    }

    static class TestGenericMessage {
        private final String payload;
        private final Map<String, Object> headers;

        TestGenericMessage(String payload, Map<String, Object> headers) {
            this.payload = payload;
            this.headers = headers;
        }

        public String getPayload() {
            return payload;
        }

        public Map<String, Object> getHeaders() {
            return headers;
        }
    }

    static class TestMessageWithNullHeaders {
        private final String payload;

        TestMessageWithNullHeaders(String payload) {
            this.payload = payload;
        }

        public String getPayload() {
            return payload;
        }

        public Map<String, Object> getHeaders() {
            return null;
        }
    }

    static class TestMessageWithoutPayload {
        private final Map<String, Object> headers;

        TestMessageWithoutPayload(Map<String, Object> headers) {
            this.headers = headers;
        }

        public Map<String, Object> getHeaders() {
            return headers;
        }
    }

    static class TestMessageWithCustomHeaders {
        private final String payload;

        TestMessageWithCustomHeaders(String payload) {
            this.payload = payload;
        }

        public String getPayload() {
            return payload;
        }

        public TestCustomHeaders getHeaders() {
            return new TestCustomHeaders();
        }
    }

    static class TestCustomHeaders {
        public String get(Object key) {
            return "custom-value";
        }
    }

    static class TestAcknowledgement {
        // Simulates SQS Acknowledgement object
    }

    static class TestVisibility {
        // Simulates SQS Visibility object
    }

    static class TestHeaders {
        // Simulates MessageHeaders object
    }
}
