package com.ryuqq.observability.message.interceptor;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import com.ryuqq.observability.message.config.MessageLoggingProperties;
import com.ryuqq.observability.message.context.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 메시지 로깅을 처리하는 인터셉터.
 *
 * <p>SQS, Redis, Kafka 등 다양한 메시지 소스에서 공통으로 사용합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>TraceId 추출 및 MDC 설정</li>
 *   <li>메시지 수신/처리 완료 로깅</li>
 *   <li>페이로드 마스킹</li>
 *   <li>처리 시간 측정</li>
 * </ul>
 */
public class MessageLoggingInterceptor {

    private static final Logger log = LoggerFactory.getLogger("observability.message");

    private final MessageLoggingProperties properties;
    private final TraceIdGenerator traceIdGenerator;
    private final LogMasker logMasker;
    private final String serviceName;

    public MessageLoggingInterceptor(MessageLoggingProperties properties,
                                     TraceIdGenerator traceIdGenerator,
                                     LogMasker logMasker,
                                     String serviceName) {
        this.properties = properties;
        this.traceIdGenerator = traceIdGenerator;
        this.logMasker = logMasker;
        this.serviceName = serviceName;
    }

    /**
     * 메시지 처리 시작 시 호출.
     * TraceId를 설정하고 수신 로그를 기록합니다.
     *
     * @param context 메시지 컨텍스트
     * @param payload 메시지 페이로드 (로깅용)
     */
    public void beforeProcessing(MessageContext context, Object payload) {
        // 1. TraceId 설정 (메시지 속성에서 추출 또는 새로 생성)
        String traceId = context.getTraceId();
        if (traceId == null || traceId.isEmpty()) {
            traceId = traceIdGenerator.generate();
        }
        TraceIdHolder.set(traceId);

        // 2. 메시지 소스 및 ID 설정
        TraceIdHolder.setMessageSource(context.getSource());
        if (context.getMessageId() != null) {
            TraceIdHolder.setMessageId(context.getMessageId());
        }

        // 3. 서비스 이름 설정
        if (serviceName != null) {
            TraceIdHolder.setServiceName(serviceName);
        }

        // 4. 메시지 속성에서 사용자 컨텍스트 추출
        extractUserContextFromAttributes(context);

        // 5. 수신 로그 기록
        logMessageReceived(context, payload);
    }

    /**
     * 메시지 처리 완료 후 호출.
     *
     * @param context 메시지 컨텍스트
     * @param success 처리 성공 여부
     * @param error   에러 (실패 시)
     */
    public void afterProcessing(MessageContext context, boolean success, Throwable error) {
        try {
            long duration = context.calculateDuration();

            if (success) {
                logMessageProcessed(context, duration);
            } else {
                logMessageFailed(context, duration, error);
            }
        } finally {
            // MDC 정리
            TraceIdHolder.clear();
        }
    }

    /**
     * 메시지 속성에서 사용자 컨텍스트를 추출합니다.
     */
    private void extractUserContextFromAttributes(MessageContext context) {
        // X-User-Id
        String userId = context.getAttribute(TraceIdHeaders.X_USER_ID);
        if (userId != null) {
            TraceIdHolder.setUserId(userId);
        }

        // X-Tenant-Id
        String tenantId = context.getAttribute(TraceIdHeaders.X_TENANT_ID);
        if (tenantId != null) {
            TraceIdHolder.setTenantId(tenantId);
        }

        // X-Organization-Id
        String organizationId = context.getAttribute(TraceIdHeaders.X_ORGANIZATION_ID);
        if (organizationId != null) {
            TraceIdHolder.setOrganizationId(organizationId);
        }
    }

    /**
     * 메시지 수신 로그를 기록합니다.
     */
    private void logMessageReceived(MessageContext context, Object payload) {
        if (!properties.isEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Message Received: ")
                .append(context.getSource())
                .append(" | queue=").append(context.getQueueName());

        if (context.getMessageId() != null) {
            sb.append(" | messageId=").append(context.getMessageId());
        }

        if (properties.isLogPayload() && payload != null) {
            String payloadStr = truncateAndMask(payload.toString());
            sb.append(" | payload=").append(payloadStr);
        }

        log.info(sb.toString());
    }

    /**
     * 메시지 처리 완료 로그를 기록합니다.
     */
    private void logMessageProcessed(MessageContext context, long duration) {
        if (!properties.isEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Message Processed: ")
                .append(context.getSource())
                .append(" | queue=").append(context.getQueueName())
                .append(" | duration=").append(duration).append("ms");

        if (context.getMessageId() != null) {
            sb.append(" | messageId=").append(context.getMessageId());
        }

        log.info(sb.toString());
    }

    /**
     * 메시지 처리 실패 로그를 기록합니다.
     */
    private void logMessageFailed(MessageContext context, long duration, Throwable error) {
        if (!properties.isEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Message Failed: ")
                .append(context.getSource())
                .append(" | queue=").append(context.getQueueName())
                .append(" | duration=").append(duration).append("ms");

        if (context.getMessageId() != null) {
            sb.append(" | messageId=").append(context.getMessageId());
        }

        if (error != null) {
            sb.append(" | error=").append(error.getClass().getSimpleName())
                    .append(": ").append(error.getMessage());
        }

        log.error(sb.toString(), error);
    }

    /**
     * 페이로드를 최대 길이로 자르고 마스킹합니다.
     */
    private String truncateAndMask(String payload) {
        if (payload == null) {
            return null;
        }

        // 마스킹 적용
        String masked = logMasker.mask(payload);

        // 최대 길이 제한
        int maxLength = properties.getMaxPayloadLength();
        if (masked.length() > maxLength) {
            return masked.substring(0, maxLength) + "...[TRUNCATED]";
        }

        return masked;
    }

    /**
     * TraceId 생성을 위한 함수형 인터페이스.
     */
    @FunctionalInterface
    public interface TraceIdGenerator {
        String generate();
    }
}
