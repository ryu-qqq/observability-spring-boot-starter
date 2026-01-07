package com.ryuqq.observability.integration.redis;

import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * Redis Pub/Sub 메시지 리스너 테스트 구현체.
 *
 * <p>메시지 수신 시 TraceIdHolder에서 컨텍스트를 캡처하여
 * 테스트에서 검증할 수 있도록 합니다.</p>
 */
public class RedisTestListener implements MessageListener {

    private final RedisMessageCaptureHolder captureHolder;

    public RedisTestListener(RedisMessageCaptureHolder captureHolder) {
        this.captureHolder = captureHolder;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 메시지 처리 시점에 TraceIdHolder에서 컨텍스트 캡처
        String traceId = TraceIdHolder.getOptional().orElse(null);
        String userId = TraceIdHolder.getUserId();
        String tenantId = TraceIdHolder.getTenantId();
        String organizationId = TraceIdHolder.getOrganizationId();
        String payload = new String(message.getBody());
        String channel = pattern != null ? new String(pattern) : new String(message.getChannel());

        captureHolder.capture(traceId, userId, tenantId, organizationId, payload, channel);
    }
}
