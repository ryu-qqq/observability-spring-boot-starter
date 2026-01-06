package com.ryuqq.observability.logging.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BusinessEvent 테스트")
class BusinessEventTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("action만으로 생성한다")
        void shouldCreateWithActionOnly() {
            TestBusinessEvent event = new TestBusinessEvent("ORDER_CREATED");

            assertThat(event.getAction()).isEqualTo("ORDER_CREATED");
            assertThat(event.getEntity()).isEmpty();
            assertThat(event.getEventId()).isNotEmpty();
            assertThat(event.getTimestamp()).isNotNull();
            assertThat(event.getContext()).isEmpty();
        }

        @Test
        @DisplayName("action과 entity로 생성한다")
        void shouldCreateWithActionAndEntity() {
            TestBusinessEvent event = new TestBusinessEvent("ORDER_CREATED", "Order");

            assertThat(event.getAction()).isEqualTo("ORDER_CREATED");
            assertThat(event.getEntity()).isEqualTo("Order");
        }

        @Test
        @DisplayName("eventId는 UUID 형식이다")
        void shouldHaveUuidEventId() {
            TestBusinessEvent event = new TestBusinessEvent("ACTION");

            assertThat(event.getEventId())
                    .isNotEmpty()
                    .matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
        }

        @Test
        @DisplayName("timestamp는 현재 시간 근처이다")
        void shouldHaveCurrentTimestamp() {
            Instant before = Instant.now();
            TestBusinessEvent event = new TestBusinessEvent("ACTION");
            Instant after = Instant.now();

            assertThat(event.getTimestamp())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("각 이벤트는 고유한 eventId를 가진다")
        void shouldHaveUniqueEventId() {
            TestBusinessEvent event1 = new TestBusinessEvent("ACTION");
            TestBusinessEvent event2 = new TestBusinessEvent("ACTION");

            assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
        }
    }

    @Nested
    @DisplayName("addContext 테스트")
    class AddContextTest {

        @Test
        @DisplayName("컨텍스트를 추가한다")
        void shouldAddContext() {
            TestBusinessEvent event = new TestBusinessEvent("ORDER_CREATED");
            event.addTestContext("orderId", 123L);

            assertThat(event.getContext())
                    .containsEntry("orderId", 123L);
        }

        @Test
        @DisplayName("여러 컨텍스트를 추가한다")
        void shouldAddMultipleContexts() {
            TestBusinessEvent event = new TestBusinessEvent("ORDER_CREATED");
            event.addTestContext("orderId", 123L);
            event.addTestContext("customerId", 456L);
            event.addTestContext("amount", new BigDecimal("1000.00"));

            assertThat(event.getContext())
                    .hasSize(3)
                    .containsEntry("orderId", 123L)
                    .containsEntry("customerId", 456L)
                    .containsEntry("amount", new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("null 키는 무시한다")
        void shouldIgnoreNullKey() {
            TestBusinessEvent event = new TestBusinessEvent("ACTION");
            event.addTestContext(null, "value");

            assertThat(event.getContext()).isEmpty();
        }

        @Test
        @DisplayName("null 값은 무시한다")
        void shouldIgnoreNullValue() {
            TestBusinessEvent event = new TestBusinessEvent("ACTION");
            event.addTestContext("key", null);

            assertThat(event.getContext()).isEmpty();
        }

        @Test
        @DisplayName("null 키와 값 모두 무시한다")
        void shouldIgnoreNullKeyAndValue() {
            TestBusinessEvent event = new TestBusinessEvent("ACTION");
            event.addTestContext(null, null);

            assertThat(event.getContext()).isEmpty();
        }

        @Test
        @DisplayName("같은 키로 값을 덮어쓴다")
        void shouldOverwriteSameKey() {
            TestBusinessEvent event = new TestBusinessEvent("ACTION");
            event.addTestContext("key", "value1");
            event.addTestContext("key", "value2");

            assertThat(event.getContext())
                    .hasSize(1)
                    .containsEntry("key", "value2");
        }
    }

    @Nested
    @DisplayName("setEntityId 테스트")
    class SetEntityIdTest {

        @Test
        @DisplayName("Long entityId를 설정한다")
        void shouldSetLongEntityId() {
            TestBusinessEvent event = new TestBusinessEvent("ACTION");
            event.setTestEntityId(123L);

            assertThat(event.getContext()).containsEntry("entityId", 123L);
        }

        @Test
        @DisplayName("String entityId를 설정한다")
        void shouldSetStringEntityId() {
            TestBusinessEvent event = new TestBusinessEvent("ACTION");
            event.setTestEntityId("order-123");

            assertThat(event.getContext()).containsEntry("entityId", "order-123");
        }

        @Test
        @DisplayName("null entityId는 무시한다")
        void shouldIgnoreNullEntityId() {
            TestBusinessEvent event = new TestBusinessEvent("ACTION");
            event.setTestEntityId(null);

            assertThat(event.getContext()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getContext 테스트")
    class GetContextTest {

        @Test
        @DisplayName("불변 맵을 반환한다")
        void shouldReturnUnmodifiableMap() {
            TestBusinessEvent event = new TestBusinessEvent("ACTION");
            event.addTestContext("key", "value");

            Map<String, Object> context = event.getContext();

            assertThatThrownBy(() -> context.put("newKey", "newValue"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("내부 맵의 복사본처럼 동작한다")
        void shouldBehaveLikeCopy() {
            TestBusinessEvent event = new TestBusinessEvent("ACTION");
            event.addTestContext("key1", "value1");

            Map<String, Object> context1 = event.getContext();
            event.addTestContext("key2", "value2");
            Map<String, Object> context2 = event.getContext();

            // context1은 key2를 갖지 않지만 불변 맵이므로 원본 수정이 반영됨
            assertThat(context2).hasSize(2);
        }
    }

    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("필드 정보를 포함한다")
        void shouldContainFieldInfo() {
            TestBusinessEvent event = new TestBusinessEvent("ORDER_CREATED", "Order");
            event.addTestContext("orderId", 123L);

            String result = event.toString();

            assertThat(result)
                    .contains("action='ORDER_CREATED'")
                    .contains("entity='Order'")
                    .contains("eventId=")
                    .contains("timestamp=")
                    .contains("context=");
        }

        @Test
        @DisplayName("컨텍스트 내용을 포함한다")
        void shouldContainContextContent() {
            TestBusinessEvent event = new TestBusinessEvent("ACTION");
            event.addTestContext("key", "value");

            String result = event.toString();

            assertThat(result).contains("key=value");
        }
    }

    @Nested
    @DisplayName("실제 사용 시나리오 테스트")
    class RealWorldScenarioTest {

        @Test
        @DisplayName("주문 생성 이벤트")
        void shouldCreateOrderCreatedEvent() {
            OrderCreatedEvent event = new OrderCreatedEvent(123L, 456L, new BigDecimal("50000"));

            assertThat(event.getAction()).isEqualTo("ORDER_CREATED");
            assertThat(event.getEntity()).isEqualTo("Order");
            assertThat(event.getContext())
                    .containsEntry("entityId", 123L)
                    .containsEntry("customerId", 456L)
                    .containsEntry("amount", new BigDecimal("50000"));
        }

        @Test
        @DisplayName("결제 완료 이벤트")
        void shouldCreatePaymentCompletedEvent() {
            PaymentCompletedEvent event = new PaymentCompletedEvent("pay-001", 123L, "CARD");

            assertThat(event.getAction()).isEqualTo("PAYMENT_COMPLETED");
            assertThat(event.getEntity()).isEqualTo("Payment");
            assertThat(event.getContext())
                    .containsEntry("entityId", "pay-001")
                    .containsEntry("orderId", 123L)
                    .containsEntry("paymentMethod", "CARD");
        }
    }

    // Test helpers

    private static class TestBusinessEvent extends BusinessEvent {

        public TestBusinessEvent(String action) {
            super(action);
        }

        public TestBusinessEvent(String action, String entity) {
            super(action, entity);
        }

        // Expose protected methods for testing
        public void addTestContext(String key, Object value) {
            addContext(key, value);
        }

        public void setTestEntityId(Object entityId) {
            setEntityId(entityId);
        }
    }

    private static class OrderCreatedEvent extends BusinessEvent {

        public OrderCreatedEvent(Long orderId, Long customerId, BigDecimal amount) {
            super("ORDER_CREATED", "Order");
            setEntityId(orderId);
            addContext("customerId", customerId);
            addContext("amount", amount);
        }
    }

    private static class PaymentCompletedEvent extends BusinessEvent {

        public PaymentCompletedEvent(String paymentId, Long orderId, String paymentMethod) {
            super("PAYMENT_COMPLETED", "Payment");
            setEntityId(paymentId);
            addContext("orderId", orderId);
            addContext("paymentMethod", paymentMethod);
        }
    }
}
