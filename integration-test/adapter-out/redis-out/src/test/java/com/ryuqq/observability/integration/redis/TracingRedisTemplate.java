package com.ryuqq.observability.integration.redis;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * TraceId 전파를 지원하는 RedisTemplate 래퍼.
 *
 * <p>Redis Pub/Sub 및 Stream 발행 시 TraceIdHolder의 컨텍스트를 자동으로
 * 메시지에 추가합니다.</p>
 *
 * <p>Pub/Sub의 경우 JSON 형태로 TraceId를 포함하며,
 * Stream의 경우 필드로 TraceId를 추가합니다.</p>
 *
 * <p>이 클래스는 테스트 목적으로 사용되며, 실제 구현에서는
 * observability-message 모듈에서 동일한 기능을 제공해야 합니다.</p>
 */
public class TracingRedisTemplate extends RedisTemplate<String, String> {

    public TracingRedisTemplate(RedisConnectionFactory connectionFactory) {
        setConnectionFactory(connectionFactory);
        setKeySerializer(new StringRedisSerializer());
        setValueSerializer(new StringRedisSerializer());
        setHashKeySerializer(new StringRedisSerializer());
        setHashValueSerializer(new StringRedisSerializer());
        afterPropertiesSet();
    }

    /**
     * TraceId가 포함된 JSON 메시지를 Pub/Sub 채널로 발행합니다.
     *
     * @param channel 발행할 채널
     * @param payload 원본 페이로드
     * @return 발행된 메시지 (TraceId 포함)
     */
    public String convertAndSendWithTrace(String channel, String payload) {
        String messageWithTrace = buildMessageWithTrace(payload);
        convertAndSend(channel, messageWithTrace);
        return messageWithTrace;
    }

    /**
     * TraceId가 포함된 Stream 레코드를 발행합니다.
     *
     * @param streamKey Stream 키
     * @param payload 원본 페이로드
     * @return 발행된 레코드 ID
     */
    public RecordId addToStreamWithTrace(String streamKey, Map<String, String> payload) {
        Map<String, String> fieldsWithTrace = buildStreamFieldsWithTrace(payload);
        ObjectRecord<String, Map<String, String>> record = ObjectRecord.create(streamKey, fieldsWithTrace);
        return opsForStream().add(record);
    }

    /**
     * TraceId를 JSON 형태로 메시지에 포함합니다.
     */
    private String buildMessageWithTrace(String payload) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"payload\":").append(payload.startsWith("{") ? payload : "\"" + payload + "\"");

        TraceIdHolder.getOptional().ifPresent(traceId ->
                sb.append(",\"").append(TraceIdHeaders.X_TRACE_ID).append("\":\"").append(traceId).append("\""));

        String userId = TraceIdHolder.getUserId();
        if (userId != null) {
            sb.append(",\"").append(TraceIdHeaders.X_USER_ID).append("\":\"").append(userId).append("\"");
        }

        String tenantId = TraceIdHolder.getTenantId();
        if (tenantId != null) {
            sb.append(",\"").append(TraceIdHeaders.X_TENANT_ID).append("\":\"").append(tenantId).append("\"");
        }

        String orgId = TraceIdHolder.getOrganizationId();
        if (orgId != null) {
            sb.append(",\"").append(TraceIdHeaders.X_ORGANIZATION_ID).append("\":\"").append(orgId).append("\"");
        }

        sb.append("}");
        return sb.toString();
    }

    /**
     * TraceId를 Stream 필드로 추가합니다.
     */
    private Map<String, String> buildStreamFieldsWithTrace(Map<String, String> originalFields) {
        Map<String, String> fields = new HashMap<>(originalFields);

        TraceIdHolder.getOptional().ifPresent(traceId ->
                fields.put(TraceIdHeaders.X_TRACE_ID, traceId));

        String userId = TraceIdHolder.getUserId();
        if (userId != null) {
            fields.put(TraceIdHeaders.X_USER_ID, userId);
        }

        String tenantId = TraceIdHolder.getTenantId();
        if (tenantId != null) {
            fields.put(TraceIdHeaders.X_TENANT_ID, tenantId);
        }

        String orgId = TraceIdHolder.getOrganizationId();
        if (orgId != null) {
            fields.put(TraceIdHeaders.X_ORGANIZATION_ID, orgId);
        }

        return fields;
    }
}
