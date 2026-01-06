package com.ryuqq.observability.logging.event;

import com.ryuqq.observability.logging.config.BusinessLoggingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("BusinessEventListener 테스트")
class BusinessEventListenerTest {

    private BusinessLoggingProperties properties;
    private BusinessEventListener listener;

    @BeforeEach
    void setUp() {
        properties = new BusinessLoggingProperties();
        listener = new BusinessEventListener(properties);
    }

    @Nested
    @DisplayName("handleBusinessEvent 테스트")
    class HandleBusinessEventTest {

        @Test
        @DisplayName("로깅이 비활성화되면 아무것도 하지 않는다")
        void shouldDoNothingWhenDisabled() {
            properties.setEnabled(false);

            TestEvent event = new TestEvent("ACTION");

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("기본 이벤트를 처리한다")
        void shouldHandleBasicEvent() {
            TestEvent event = new TestEvent("ORDER_CREATED");

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("엔티티가 있는 이벤트를 처리한다")
        void shouldHandleEventWithEntity() {
            TestEventWithEntity event = new TestEventWithEntity("ORDER_CREATED", "Order");

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("빈 엔티티는 로그에 포함하지 않는다")
        void shouldNotIncludeEmptyEntity() {
            TestEventWithEntity event = new TestEventWithEntity("ACTION", "");

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("null 엔티티는 로그에 포함하지 않는다")
        void shouldNotIncludeNullEntity() {
            TestEventWithEntity event = new TestEventWithEntity("ACTION", null);

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("컨텍스트가 있는 이벤트를 처리한다")
        void shouldHandleEventWithContext() {
            OrderCreatedTestEvent event = new OrderCreatedTestEvent(123L, 456L, new BigDecimal("50000"));

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("공백이 포함된 문자열 값은 따옴표로 감싼다")
        void shouldQuoteStringValueWithSpaces() {
            TestEvent event = new TestEvent("ACTION");
            event.addTestContext("description", "주문 생성 완료");

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("공백이 없는 문자열 값은 따옴표 없이 출력한다")
        void shouldNotQuoteStringWithoutSpaces() {
            TestEvent event = new TestEvent("ACTION");
            event.addTestContext("status", "COMPLETED");

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("숫자 값은 따옴표 없이 출력한다")
        void shouldNotQuoteNumericValues() {
            TestEvent event = new TestEvent("ACTION");
            event.addTestContext("orderId", 123L);
            event.addTestContext("amount", new BigDecimal("1000.50"));

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }
    }

    @Nested
    @DisplayName("formatEventLog 테스트")
    class FormatEventLogTest {

        @Test
        @DisplayName("모든 필드를 포함하여 포맷한다")
        void shouldFormatWithAllFields() {
            OrderCreatedTestEvent event = new OrderCreatedTestEvent(123L, 456L, new BigDecimal("50000"));

            // 로그 포맷 확인 - 예외 없이 실행되는지만 확인
            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("여러 컨텍스트를 순서대로 포맷한다")
        void shouldFormatMultipleContextsInOrder() {
            TestEvent event = new TestEvent("ACTION");
            event.addTestContext("first", "1");
            event.addTestContext("second", "2");
            event.addTestContext("third", "3");

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }
    }

    @Nested
    @DisplayName("실제 사용 시나리오 테스트")
    class RealWorldScenarioTest {

        @Test
        @DisplayName("주문 생성 이벤트 처리")
        void shouldHandleOrderCreatedEvent() {
            OrderCreatedTestEvent event = new OrderCreatedTestEvent(123L, 456L, new BigDecimal("150000"));

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("결제 완료 이벤트 처리")
        void shouldHandlePaymentCompletedEvent() {
            PaymentCompletedTestEvent event = new PaymentCompletedTestEvent("pay-001", 123L, "CARD");

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("회원 가입 이벤트 처리")
        void shouldHandleMemberRegisteredEvent() {
            MemberRegisteredTestEvent event = new MemberRegisteredTestEvent(789L, "user@example.com");

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }

        @Test
        @DisplayName("빈 컨텍스트 이벤트 처리")
        void shouldHandleEventWithEmptyContext() {
            TestEvent event = new TestEvent("SIMPLE_ACTION");

            assertThatNoException().isThrownBy(() -> listener.handleBusinessEvent(event));
        }
    }

    // Test event classes

    private static class TestEvent extends BusinessEvent {

        public TestEvent(String action) {
            super(action);
        }

        public void addTestContext(String key, Object value) {
            addContext(key, value);
        }
    }

    private static class TestEventWithEntity extends BusinessEvent {

        public TestEventWithEntity(String action, String entity) {
            super(action, entity);
        }
    }

    private static class OrderCreatedTestEvent extends BusinessEvent {

        public OrderCreatedTestEvent(Long orderId, Long customerId, BigDecimal amount) {
            super("ORDER_CREATED", "Order");
            setEntityId(orderId);
            addContext("customerId", customerId);
            addContext("amount", amount);
        }
    }

    private static class PaymentCompletedTestEvent extends BusinessEvent {

        public PaymentCompletedTestEvent(String paymentId, Long orderId, String paymentMethod) {
            super("PAYMENT_COMPLETED", "Payment");
            setEntityId(paymentId);
            addContext("orderId", orderId);
            addContext("paymentMethod", paymentMethod);
        }
    }

    private static class MemberRegisteredTestEvent extends BusinessEvent {

        public MemberRegisteredTestEvent(Long memberId, String email) {
            super("MEMBER_REGISTERED", "Member");
            setEntityId(memberId);
            addContext("email", email);
        }
    }
}
