package com.ryuqq.observability.message.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageContext 테스트")
class MessageContextTest {

    @Nested
    @DisplayName("Builder 테스트")
    class BuilderTest {

        @Test
        @DisplayName("모든 필드를 설정하여 생성한다")
        void shouldBuildWithAllFields() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("order-events")
                    .messageId("msg-123")
                    .traceId("trace-456")
                    .startTimeMillis(1000L)
                    .build();

            assertThat(context.getSource()).isEqualTo("SQS");
            assertThat(context.getQueueName()).isEqualTo("order-events");
            assertThat(context.getMessageId()).isEqualTo("msg-123");
            assertThat(context.getTraceId()).isEqualTo("trace-456");
            assertThat(context.getStartTimeMillis()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("기본 시작 시간이 설정된다")
        void shouldSetDefaultStartTime() {
            long before = System.currentTimeMillis();
            MessageContext context = MessageContext.builder()
                    .source("REDIS")
                    .build();
            long after = System.currentTimeMillis();

            assertThat(context.getStartTimeMillis()).isBetween(before, after);
        }

        @Test
        @DisplayName("단일 속성을 추가한다")
        void shouldAddSingleAttribute() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .attribute("key1", "value1")
                    .attribute("key2", "value2")
                    .build();

            assertThat(context.getAttribute("key1")).isEqualTo("value1");
            assertThat(context.getAttribute("key2")).isEqualTo("value2");
        }

        @Test
        @DisplayName("null 키나 값은 속성에 추가하지 않는다")
        void shouldNotAddNullKeyOrValue() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .attribute(null, "value")
                    .attribute("key", null)
                    .build();

            assertThat(context.getAttributes()).isEmpty();
        }

        @Test
        @DisplayName("Map으로 속성을 일괄 추가한다")
        void shouldAddAttributesFromMap() {
            Map<String, String> attrs = new HashMap<>();
            attrs.put("key1", "value1");
            attrs.put("key2", "value2");

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .attributes(attrs)
                    .build();

            assertThat(context.getAttributes()).hasSize(2);
            assertThat(context.getAttribute("key1")).isEqualTo("value1");
        }

        @Test
        @DisplayName("null Map은 무시한다")
        void shouldIgnoreNullAttributeMap() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .attributes(null)
                    .build();

            assertThat(context.getAttributes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAttributes 테스트")
    class GetAttributesTest {

        @Test
        @DisplayName("불변 Map을 반환한다")
        void shouldReturnImmutableMap() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .attribute("key", "value")
                    .build();

            Map<String, String> attributes = context.getAttributes();

            assertThat(attributes).isUnmodifiable();
        }

        @Test
        @DisplayName("내부 Map의 복사본을 반환한다")
        void shouldReturnCopyOfInternalMap() {
            Map<String, String> original = new HashMap<>();
            original.put("key", "value");

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .attributes(original)
                    .build();

            // 원본 수정
            original.put("key2", "value2");

            // 컨텍스트에는 영향 없음
            assertThat(context.getAttributes()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("calculateDuration 테스트")
    class CalculateDurationTest {

        @Test
        @DisplayName("처리 시간을 계산한다")
        void shouldCalculateDuration() throws InterruptedException {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .build();

            Thread.sleep(50);

            long duration = context.calculateDuration();

            assertThat(duration).isGreaterThanOrEqualTo(50);
        }

        @Test
        @DisplayName("지정된 시작 시간 기준으로 계산한다")
        void shouldCalculateFromSpecifiedStartTime() {
            long startTime = System.currentTimeMillis() - 100;

            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .startTimeMillis(startTime)
                    .build();

            long duration = context.calculateDuration();

            assertThat(duration).isGreaterThanOrEqualTo(100);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("필드 정보를 포함한 문자열을 반환한다")
        void shouldReturnStringWithFields() {
            MessageContext context = MessageContext.builder()
                    .source("SQS")
                    .queueName("order-events")
                    .messageId("msg-123")
                    .traceId("trace-456")
                    .build();

            String result = context.toString();

            assertThat(result)
                    .contains("source='SQS'")
                    .contains("queueName='order-events'")
                    .contains("messageId='msg-123'")
                    .contains("traceId='trace-456'");
        }
    }
}
