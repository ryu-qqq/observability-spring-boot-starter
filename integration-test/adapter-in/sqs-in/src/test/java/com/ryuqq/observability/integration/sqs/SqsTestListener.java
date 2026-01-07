package com.ryuqq.observability.integration.sqs;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;

/**
 * 테스트용 SQS 리스너.
 *
 * <p>SQS 메시지를 수신하고 TraceIdHolder에서 컨텍스트를 캡처합니다.</p>
 *
 * <p>Message&lt;String&gt; 객체를 받아야 SqsMessageLoggingAspect가
 * 헤더에서 TraceId를 추출할 수 있습니다.</p>
 */
public class SqsTestListener {

    private static final Logger log = LoggerFactory.getLogger(SqsTestListener.class);

    private final TestMessageCaptureHolder captureHolder;

    public SqsTestListener(TestMessageCaptureHolder captureHolder) {
        this.captureHolder = captureHolder;
    }

    @SqsListener("${test.sqs.queue-name:test-queue}")
    public void handleMessage(Message<String> message) {
        String payload = message.getPayload();
        Object traceIdHeader = message.getHeaders().get(TraceIdHeaders.X_TRACE_ID);

        log.info("Received SQS message: payload={}, traceIdHeader={}", payload, traceIdHeader);
        log.info("TraceIdHolder.get()={}", TraceIdHolder.getOptional().orElse("null"));

        String traceId = TraceIdHolder.getOptional().orElse(null);
        String userId = TraceIdHolder.getUserId();
        String tenantId = TraceIdHolder.getTenantId();
        String organizationId = TraceIdHolder.getOrganizationId();

        captureHolder.capture(traceId, userId, tenantId, organizationId, payload);

        log.info("Captured context - traceId: {}, userId: {}, tenantId: {}, orgId: {}",
                traceId, userId, tenantId, organizationId);
    }
}
