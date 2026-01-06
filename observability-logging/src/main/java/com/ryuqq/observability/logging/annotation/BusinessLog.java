package com.ryuqq.observability.logging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비즈니스 로그 어노테이션.
 *
 * <p>비즈니스 이벤트를 구조화된 형식으로 로깅합니다.</p>
 *
 * <pre>
 * {@code
 * @Service
 * public class OrderService {
 *
 *     @BusinessLog(
 *         action = "ORDER_CREATED",
 *         entity = "Order",
 *         description = "주문 생성"
 *     )
 *     public Order createOrder(CreateOrderCommand command) {
 *         // ...
 *     }
 * }
 * }
 * </pre>
 *
 * <p>로그 출력 예시:</p>
 * <pre>
 * 2024-01-01 12:00:00 INFO  [traceId=abc123] [BUSINESS] action=ORDER_CREATED entity=Order description="주문 생성"
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BusinessLog {

    /**
     * 비즈니스 액션 이름 (예: ORDER_CREATED, PAYMENT_COMPLETED).
     */
    String action();

    /**
     * 관련 엔티티 타입 (예: Order, Payment).
     */
    String entity() default "";

    /**
     * 비즈니스 로그 설명.
     */
    String description() default "";

    /**
     * 로그에 포함할 SpEL 표현식 (메서드 인자에서 추출).
     *
     * <p>예시:</p>
     * <pre>
     * {@code
     * @BusinessLog(action = "ORDER_CREATED", entityId = "#command.orderId")
     * public Order createOrder(CreateOrderCommand command) { ... }
     * }
     * </pre>
     */
    String entityId() default "";

    /**
     * 추가 컨텍스트 정보 (SpEL 표현식).
     *
     * <p>예시:</p>
     * <pre>
     * {@code
     * @BusinessLog(
     *     action = "ORDER_CREATED",
     *     context = {"amount=#command.totalAmount", "items=#command.items.size()"}
     * )
     * }
     * </pre>
     */
    String[] context() default {};

    /**
     * 성공 시에만 로깅할지 여부.
     */
    boolean onSuccessOnly() default false;
}
