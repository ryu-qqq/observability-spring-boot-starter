package com.ryuqq.observability.logging.event;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 비즈니스 이벤트 베이스 클래스.
 *
 * <p>도메인 이벤트를 발행하면 자동으로 비즈니스 로그가 기록됩니다.</p>
 *
 * <pre>
 * {@code
 * // 도메인 이벤트 정의
 * public class OrderCreatedEvent extends BusinessEvent {
 *     public OrderCreatedEvent(Long orderId, Long customerId, BigDecimal amount) {
 *         super("ORDER_CREATED", "Order");
 *         addContext("orderId", orderId);
 *         addContext("customerId", customerId);
 *         addContext("amount", amount);
 *     }
 * }
 *
 * // 이벤트 발행 (Spring ApplicationEventPublisher 사용)
 * eventPublisher.publishEvent(new OrderCreatedEvent(orderId, customerId, amount));
 * }
 * </pre>
 */
public abstract class BusinessEvent {

    private final String eventId;
    private final String action;
    private final String entity;
    private final Instant timestamp;
    private final Map<String, Object> context;

    protected BusinessEvent(String action) {
        this(action, "");
    }

    protected BusinessEvent(String action, String entity) {
        this.eventId = UUID.randomUUID().toString();
        this.action = action;
        this.entity = entity;
        this.timestamp = Instant.now();
        this.context = new LinkedHashMap<>();
    }

    /**
     * 이벤트 ID를 반환합니다.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * 비즈니스 액션을 반환합니다.
     */
    public String getAction() {
        return action;
    }

    /**
     * 관련 엔티티 타입을 반환합니다.
     */
    public String getEntity() {
        return entity;
    }

    /**
     * 이벤트 발생 시간을 반환합니다.
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * 이벤트 컨텍스트를 반환합니다 (불변).
     */
    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(context);
    }

    /**
     * 컨텍스트 정보를 추가합니다.
     *
     * @param key   컨텍스트 키
     * @param value 컨텍스트 값
     */
    protected void addContext(String key, Object value) {
        if (key != null && value != null) {
            context.put(key, value);
        }
    }

    /**
     * 엔티티 ID를 추가합니다 (편의 메서드).
     *
     * @param entityId 엔티티 ID
     */
    protected void setEntityId(Object entityId) {
        addContext("entityId", entityId);
    }

    @Override
    public String toString() {
        return "BusinessEvent{" +
                "eventId='" + eventId + '\'' +
                ", action='" + action + '\'' +
                ", entity='" + entity + '\'' +
                ", timestamp=" + timestamp +
                ", context=" + context +
                '}';
    }
}
