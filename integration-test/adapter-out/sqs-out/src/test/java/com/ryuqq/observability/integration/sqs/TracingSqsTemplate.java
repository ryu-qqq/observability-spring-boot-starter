package com.ryuqq.observability.integration.sqs;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import io.awspring.cloud.sqs.operations.SendResult;
import io.awspring.cloud.sqs.operations.SqsOperations;
import io.awspring.cloud.sqs.operations.SqsReceiveOptions;
import io.awspring.cloud.sqs.operations.SqsSendOptions;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * TraceId 전파를 지원하는 SqsTemplate 래퍼.
 *
 * <p>메시지 발송 시 TraceIdHolder의 컨텍스트를 자동으로
 * 메시지 헤더(속성)에 추가합니다.</p>
 *
 * <p>이 클래스는 테스트 목적으로 사용되며, 실제 구현에서는
 * observability-message 모듈에서 동일한 기능을 제공해야 합니다.</p>
 */
public class TracingSqsTemplate implements SqsOperations {

    private final SqsTemplate delegate;

    public TracingSqsTemplate(SqsAsyncClient sqsAsyncClient) {
        this.delegate = SqsTemplate.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }

    @Override
    public <T> SendResult<T> send(T payload) {
        return send(to -> to.payload(payload));
    }

    @Override
    public <T> SendResult<T> send(String queue, T payload) {
        return send(to -> to.queue(queue).payload(payload));
    }

    @Override
    public <T> SendResult<T> send(String queue, Message<T> message) {
        // Message 객체에서 페이로드와 헤더를 추출하여 새로운 send 호출
        T payload = message.getPayload();
        return send(to -> {
            to.queue(queue).payload(payload);
            // 기존 헤더 복사
            message.getHeaders().forEach((key, value) -> {
                if (value instanceof String) {
                    to.header(key, value);
                }
            });
        });
    }

    @Override
    public <T> SendResult<T> send(Consumer<SqsSendOptions<T>> to) {
        return delegate.send(options -> {
            // 사용자 옵션 적용
            to.accept(options);

            // TraceId 전파
            TraceIdHolder.getOptional().ifPresent(traceId ->
                    options.header(TraceIdHeaders.X_TRACE_ID, traceId));

            // UserId 전파
            String userId = TraceIdHolder.getUserId();
            if (userId != null) {
                options.header(TraceIdHeaders.X_USER_ID, userId);
            }

            // TenantId 전파
            String tenantId = TraceIdHolder.getTenantId();
            if (tenantId != null) {
                options.header(TraceIdHeaders.X_TENANT_ID, tenantId);
            }

            // OrganizationId 전파
            String organizationId = TraceIdHolder.getOrganizationId();
            if (organizationId != null) {
                options.header(TraceIdHeaders.X_ORGANIZATION_ID, organizationId);
            }
        });
    }

    @Override
    public <T> SendResult.Batch<T> sendMany(String queue, Collection<Message<T>> messages) {
        // 각 메시지에 TraceId 헤더 추가
        Collection<Message<T>> messagesWithTrace = messages.stream()
                .map(this::addTraceHeaders)
                .collect(Collectors.toList());
        return delegate.sendMany(queue, messagesWithTrace);
    }

    private <T> Message<T> addTraceHeaders(Message<T> message) {
        MessageBuilder<T> builder = MessageBuilder.fromMessage(message);

        TraceIdHolder.getOptional().ifPresent(traceId ->
                builder.setHeader(TraceIdHeaders.X_TRACE_ID, traceId));

        String userId = TraceIdHolder.getUserId();
        if (userId != null) {
            builder.setHeader(TraceIdHeaders.X_USER_ID, userId);
        }

        String tenantId = TraceIdHolder.getTenantId();
        if (tenantId != null) {
            builder.setHeader(TraceIdHeaders.X_TENANT_ID, tenantId);
        }

        String organizationId = TraceIdHolder.getOrganizationId();
        if (organizationId != null) {
            builder.setHeader(TraceIdHeaders.X_ORGANIZATION_ID, organizationId);
        }

        return builder.build();
    }

    @Override
    public Optional<Message<?>> receive() {
        return delegate.receive();
    }

    @Override
    public <T> Optional<Message<T>> receive(String queue, Class<T> payloadClass) {
        return delegate.receive(queue, payloadClass);
    }

    @Override
    public Optional<Message<?>> receive(Consumer<SqsReceiveOptions> from) {
        return delegate.receive(from);
    }

    @Override
    public <T> Optional<Message<T>> receive(Consumer<SqsReceiveOptions> from, Class<T> payloadClass) {
        return delegate.receive(from, payloadClass);
    }

    @Override
    public Collection<Message<?>> receiveMany() {
        return delegate.receiveMany();
    }

    @Override
    public <T> Collection<Message<T>> receiveMany(String queue, Class<T> payloadClass) {
        return delegate.receiveMany(queue, payloadClass);
    }

    @Override
    public Collection<Message<?>> receiveMany(Consumer<SqsReceiveOptions> from) {
        return delegate.receiveMany(from);
    }

    @Override
    public <T> Collection<Message<T>> receiveMany(Consumer<SqsReceiveOptions> from, Class<T> payloadClass) {
        return delegate.receiveMany(from, payloadClass);
    }
}
