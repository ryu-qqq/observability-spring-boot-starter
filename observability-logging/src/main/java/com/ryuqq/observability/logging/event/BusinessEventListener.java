package com.ryuqq.observability.logging.event;

import com.ryuqq.observability.logging.config.BusinessLoggingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import java.util.Map;

/**
 * 비즈니스 이벤트 리스너.
 *
 * <p>발행된 BusinessEvent를 자동으로 로깅합니다.</p>
 *
 * <p>이 리스너는 Spring의 ApplicationEventPublisher를 통해 발행된
 * BusinessEvent 하위 클래스를 자동으로 감지하여 로깅합니다.</p>
 */
public class BusinessEventListener {

    private static final Logger businessLogger = LoggerFactory.getLogger("observability.business");

    private final BusinessLoggingProperties properties;

    public BusinessEventListener(BusinessLoggingProperties properties) {
        this.properties = properties;
    }

    @EventListener
    public void handleBusinessEvent(BusinessEvent event) {
        if (!properties.isEnabled()) {
            return;
        }

        String logMessage = formatEventLog(event);
        businessLogger.info("[BUSINESS-EVENT] {}", logMessage);
    }

    private String formatEventLog(BusinessEvent event) {
        StringBuilder sb = new StringBuilder();

        sb.append("eventId=").append(event.getEventId());
        sb.append(" action=").append(event.getAction());

        if (event.getEntity() != null && !event.getEntity().isEmpty()) {
            sb.append(" entity=").append(event.getEntity());
        }

        sb.append(" timestamp=").append(event.getTimestamp());

        // 컨텍스트 정보 추가
        Map<String, Object> context = event.getContext();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            sb.append(" ").append(entry.getKey()).append("=");

            Object value = entry.getValue();
            if (value instanceof String str && str.contains(" ")) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
        }

        return sb.toString();
    }
}
