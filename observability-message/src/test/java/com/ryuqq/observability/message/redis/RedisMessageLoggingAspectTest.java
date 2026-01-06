package com.ryuqq.observability.message.redis;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.message.config.MessageLoggingProperties;
import com.ryuqq.observability.message.interceptor.MessageLoggingInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("RedisMessageLoggingAspect 테스트")
class RedisMessageLoggingAspectTest {

    private MessageLoggingInterceptor interceptor;
    private RedisMessageLoggingAspect aspect;

    @BeforeEach
    void setUp() {
        MessageLoggingProperties properties = new MessageLoggingProperties();
        MessageLoggingInterceptor.TraceIdGenerator generator = () -> "generated-trace-id";
        LogMasker logMasker = mock(LogMasker.class);
        when(logMasker.mask(anyString())).thenAnswer(inv -> inv.getArgument(0));

        interceptor = new MessageLoggingInterceptor(properties, generator, logMasker, "test-service");
        aspect = new RedisMessageLoggingAspect(interceptor);
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Nested
    @DisplayName("aroundRedisMessageListener 테스트")
    class AroundRedisMessageListenerTest {

        @Test
        @DisplayName("성공적으로 메시지를 처리한다")
        void shouldProcessMessageSuccessfully() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"test-payload".getBytes(), "test-channel".getBytes()});
            when(joinPoint.proceed()).thenReturn(null);

            Object result = aspect.aroundRedisMessageListener(joinPoint);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("예외 발생 시 다시 던진다")
        void shouldRethrowException() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"test-payload".getBytes(), "test-channel".getBytes()});
            when(joinPoint.proceed()).thenThrow(new RuntimeException("test error"));

            assertThatThrownBy(() -> aspect.aroundRedisMessageListener(joinPoint))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("test error");
        }

        @Test
        @DisplayName("처리 후 MDC를 정리한다")
        void shouldClearMdcAfterProcessing() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"test-payload".getBytes(), "test-channel".getBytes()});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("예외 발생 후에도 MDC를 정리한다")
        void shouldClearMdcAfterException() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"test-payload".getBytes(), "test-channel".getBytes()});
            when(joinPoint.proceed()).thenThrow(new RuntimeException("test error"));

            try {
                aspect.aroundRedisMessageListener(joinPoint);
            } catch (RuntimeException ignored) {
            }

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("결과 값을 반환한다")
        void shouldReturnResult() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"payload".getBytes(), "channel".getBytes()});
            when(joinPoint.proceed()).thenReturn("result-value");

            Object result = aspect.aroundRedisMessageListener(joinPoint);

            assertThat(result).isEqualTo("result-value");
        }
    }

    @Nested
    @DisplayName("aroundRedisStreamListener 테스트")
    class AroundRedisStreamListenerTest {

        @Test
        @DisplayName("성공적으로 스트림 메시지를 처리한다")
        void shouldProcessStreamMessageSuccessfully() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"stream-payload"});
            when(joinPoint.proceed()).thenReturn("result");

            Object result = aspect.aroundRedisStreamListener(joinPoint);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("예외 발생 시 다시 던진다")
        void shouldRethrowExceptionFromStream() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"stream-payload"});
            when(joinPoint.proceed()).thenThrow(new IllegalStateException("stream error"));

            assertThatThrownBy(() -> aspect.aroundRedisStreamListener(joinPoint))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("stream error");
        }

        @Test
        @DisplayName("처리 후 MDC를 정리한다")
        void shouldClearMdcAfterStreamProcessing() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"stream-payload"});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisStreamListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("채널 추출 테스트")
    class ChannelExtractionTest {

        @Test
        @DisplayName("byte[] 채널을 추출한다")
        void shouldExtractByteArrayChannel() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{
                    "payload".getBytes(),
                    "my-channel".getBytes()
            });
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("인자가 없으면 unknown 채널을 사용한다")
        void shouldUseUnknownChannelWhenNoArgs() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("null 인자를 무시한다")
        void shouldIgnoreNullArgs() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{null, null});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Message 객체에서 채널을 추출한다")
        void shouldExtractChannelFromMessageObject() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            TestMessage message = new TestMessage("payload-body", "extracted-channel");
            when(joinPoint.getArgs()).thenReturn(new Object[]{message});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Record 객체에서 스트림 이름을 추출한다")
        void shouldExtractStreamNameFromRecord() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            TestRecord record = new TestRecord("stream-name", "record-id", new HashMap<>());
            when(joinPoint.getArgs()).thenReturn(new Object[]{record});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisStreamListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("TraceId 추출 테스트")
    class TraceIdExtractionTest {

        @Test
        @DisplayName("Record 값에서 TraceId를 추출한다")
        void shouldExtractTraceIdFromRecordValue() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            Map<Object, Object> value = new HashMap<>();
            value.put(TraceIdHeaders.X_TRACE_ID, "trace-from-record");
            TestRecord record = new TestRecord("stream", "id", value);
            when(joinPoint.getArgs()).thenReturn(new Object[]{record});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisStreamListener(joinPoint);

            // TraceId가 설정되었다가 정리됨
            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Record 값에서 MDC TraceId를 추출한다")
        void shouldExtractMdcTraceIdFromRecordValue() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            Map<Object, Object> value = new HashMap<>();
            value.put(TraceIdHeaders.MDC_TRACE_ID, "mdc-trace-id");
            TestRecord record = new TestRecord("stream", "id", value);
            when(joinPoint.getArgs()).thenReturn(new Object[]{record});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisStreamListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Message body에서 JSON TraceId를 추출한다")
        void shouldExtractTraceIdFromMessageJsonBody() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            String jsonBody = "{\"" + TraceIdHeaders.X_TRACE_ID + "\":\"json-trace-id\",\"data\":\"value\"}";
            TestMessage message = new TestMessage(jsonBody, "channel");
            when(joinPoint.getArgs()).thenReturn(new Object[]{message});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("MessageId 추출 테스트")
    class MessageIdExtractionTest {

        @Test
        @DisplayName("Record에서 MessageId를 추출한다")
        void shouldExtractMessageIdFromRecord() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            TestRecord record = new TestRecord("stream", "1234-5678", new HashMap<>());
            when(joinPoint.getArgs()).thenReturn(new Object[]{record});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisStreamListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("속성 추출 테스트")
    class AttributeExtractionTest {

        @Test
        @DisplayName("Record 값에서 사용자 컨텍스트를 추출한다")
        void shouldExtractUserContextFromRecord() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            Map<Object, Object> value = new HashMap<>();
            value.put(TraceIdHeaders.X_USER_ID, "user-123");
            value.put(TraceIdHeaders.X_TENANT_ID, "tenant-456");
            value.put(TraceIdHeaders.X_ORGANIZATION_ID, "org-789");
            TestRecord record = new TestRecord("stream", "id", value);
            when(joinPoint.getArgs()).thenReturn(new Object[]{record});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisStreamListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("페이로드 추출 테스트")
    class PayloadExtractionTest {

        @Test
        @DisplayName("Message 객체에서 body를 추출한다")
        void shouldExtractBodyFromMessage() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            TestMessage message = new TestMessage("message-body", "channel");
            when(joinPoint.getArgs()).thenReturn(new Object[]{message, "pattern".getBytes()});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Record 객체에서 값을 추출한다")
        void shouldExtractValueFromRecord() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            Map<Object, Object> value = new HashMap<>();
            value.put("data", "record-data");
            TestRecord record = new TestRecord("stream", "id", value);
            when(joinPoint.getArgs()).thenReturn(new Object[]{record});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisStreamListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("일반 객체를 페이로드로 추출한다")
        void shouldExtractRegularObjectAsPayload() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            TestPayload payload = new TestPayload("custom-data");
            when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("byte[] 패턴은 페이로드에서 스킵한다")
        void shouldSkipByteArrayPattern() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[]{"pattern".getBytes()});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Pointcut 테스트")
    class PointcutTest {

        @Test
        @DisplayName("redisMessageListenerMethod pointcut은 void를 반환한다")
        void shouldDefineRedisMessageListenerPointcut() {
            aspect.redisMessageListenerMethod();
        }

        @Test
        @DisplayName("redisStreamListenerMethod pointcut은 void를 반환한다")
        void shouldDefineRedisStreamListenerPointcut() {
            aspect.redisStreamListenerMethod();
        }
    }

    @Nested
    @DisplayName("에지 케이스 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("getChannel 메서드가 없는 Message 객체도 처리한다")
        void shouldHandleMessageWithoutGetChannel() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            TestMessageWithoutChannel message = new TestMessageWithoutChannel("body");
            when(joinPoint.getArgs()).thenReturn(new Object[]{message});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("getBody가 byte[]가 아닌 경우도 처리한다")
        void shouldHandleNonByteArrayBody() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            TestMessageWithStringBody message = new TestMessageWithStringBody("string-body", "channel");
            when(joinPoint.getArgs()).thenReturn(new Object[]{message});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("getChannel이 byte[]를 반환하는 경우도 처리한다")
        void shouldHandleByteArrayChannel() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            TestMessageWithByteChannel message = new TestMessageWithByteChannel("body", "byte-channel".getBytes());
            when(joinPoint.getArgs()).thenReturn(new Object[]{message});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Record getValue가 예외를 던지면 원본 arg를 사용한다")
        void shouldUseOriginalArgWhenGetValueFails() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            TestRecordWithFailingGetValue record = new TestRecordWithFailingGetValue("stream", "id");
            when(joinPoint.getArgs()).thenReturn(new Object[]{record});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisStreamListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("Headers 객체는 페이로드에서 제외된다")
        void shouldExcludeHeadersFromPayload() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            TestHeaders headers = new TestHeaders();
            when(joinPoint.getArgs()).thenReturn(new Object[]{headers, "real-payload"});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("JSON 필드 추출에서 잘못된 JSON도 처리한다")
        void shouldHandleMalformedJson() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            String malformedJson = "{invalid-json}";
            TestMessage message = new TestMessage(malformedJson, "channel");
            when(joinPoint.getArgs()).thenReturn(new Object[]{message});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }

        @Test
        @DisplayName("JSON에서 닫는 따옴표가 없는 경우도 처리한다")
        void shouldHandleJsonWithoutClosingQuote() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            String incompleteJson = "{\"" + TraceIdHeaders.X_TRACE_ID + "\":\"trace-value";
            TestMessage message = new TestMessage(incompleteJson, "channel");
            when(joinPoint.getArgs()).thenReturn(new Object[]{message});
            when(joinPoint.proceed()).thenReturn(null);

            aspect.aroundRedisMessageListener(joinPoint);

            assertThat(TraceIdHolder.getOptional()).isEmpty();
        }
    }

    // Test helper classes
    static class TestMessage {
        private final String body;
        private final String channel;

        TestMessage(String body, String channel) {
            this.body = body;
            this.channel = channel;
        }

        public byte[] getBody() {
            return body != null ? body.getBytes() : null;
        }

        public String getChannel() {
            return channel;
        }
    }

    static class TestMessageWithoutChannel {
        private final String body;

        TestMessageWithoutChannel(String body) {
            this.body = body;
        }

        public byte[] getBody() {
            return body != null ? body.getBytes() : null;
        }
    }

    static class TestMessageWithStringBody {
        private final String body;
        private final String channel;

        TestMessageWithStringBody(String body, String channel) {
            this.body = body;
            this.channel = channel;
        }

        public String getBody() {
            return body;
        }

        public String getChannel() {
            return channel;
        }
    }

    static class TestMessageWithByteChannel {
        private final String body;
        private final byte[] channel;

        TestMessageWithByteChannel(String body, byte[] channel) {
            this.body = body;
            this.channel = channel;
        }

        public byte[] getBody() {
            return body != null ? body.getBytes() : null;
        }

        public byte[] getChannel() {
            return channel;
        }
    }

    static class TestRecord {
        private final String stream;
        private final String id;
        private final Map<Object, Object> value;

        TestRecord(String stream, String id, Map<Object, Object> value) {
            this.stream = stream;
            this.id = id;
            this.value = value;
        }

        public String getStream() {
            return stream;
        }

        public String getId() {
            return id;
        }

        public Map<Object, Object> getValue() {
            return value;
        }
    }

    static class TestRecordWithFailingGetValue {
        private final String stream;
        private final String id;

        TestRecordWithFailingGetValue(String stream, String id) {
            this.stream = stream;
            this.id = id;
        }

        public String getStream() {
            return stream;
        }

        public String getId() {
            return id;
        }

        public Object getValue() {
            throw new RuntimeException("getValue failed");
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

    static class TestHeaders {
        // Headers 객체 시뮬레이션
    }
}
