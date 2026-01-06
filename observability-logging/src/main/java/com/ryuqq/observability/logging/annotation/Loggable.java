package com.ryuqq.observability.logging.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행 로깅 어노테이션.
 *
 * <p>메서드 진입/종료 및 실행 시간을 자동으로 로깅합니다.</p>
 *
 * <pre>
 * {@code
 * @Service
 * public class OrderService {
 *
 *     @Loggable
 *     public Order createOrder(CreateOrderCommand command) {
 *         // ...
 *     }
 *
 *     @Loggable(includeArgs = true, includeResult = true)
 *     public Order findById(Long orderId) {
 *         // ...
 *     }
 * }
 * }
 * </pre>
 *
 * <p>로그 출력 예시:</p>
 * <pre>
 * 2024-01-01 12:00:00 INFO  [traceId=abc123] OrderService.createOrder started
 * 2024-01-01 12:00:01 INFO  [traceId=abc123] OrderService.createOrder completed in 1000ms
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {

    /**
     * 로그 메시지 접두사. 비어있으면 메서드 이름 사용.
     */
    String value() default "";

    /**
     * 메서드 인자 로깅 포함 여부.
     */
    boolean includeArgs() default false;

    /**
     * 메서드 결과 로깅 포함 여부.
     */
    boolean includeResult() default false;

    /**
     * 실행 시간 로깅 포함 여부.
     */
    boolean includeExecutionTime() default true;

    /**
     * 느린 실행 경고 임계값 (밀리초). -1이면 경고 없음.
     */
    long slowThreshold() default -1;

    /**
     * 로그 레벨.
     */
    LogLevel level() default LogLevel.INFO;

    /**
     * 예외 발생 시 로그 레벨.
     */
    LogLevel errorLevel() default LogLevel.ERROR;

    /**
     * 로그 레벨 열거형.
     */
    enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}
