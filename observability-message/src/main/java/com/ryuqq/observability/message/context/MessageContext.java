package com.ryuqq.observability.message.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 메시지 처리 컨텍스트 정보를 담는 객체.
 *
 * <p>SQS, Redis 등 다양한 메시지 소스에서 공통으로 사용됩니다.</p>
 *
 * <pre>
 * {@code
 * MessageContext context = MessageContext.builder()
 *     .source("SQS")
 *     .queueName("order-events")
 *     .messageId("msg-12345")
 *     .traceId("trace-abc")
 *     .build();
 * }
 * </pre>
 */
public class MessageContext {

    private final String source;
    private final String queueName;
    private final String messageId;
    private final String traceId;
    private final long startTimeMillis;
    private final Map<String, String> attributes;

    private MessageContext(Builder builder) {
        this.source = builder.source;
        this.queueName = builder.queueName;
        this.messageId = builder.messageId;
        this.traceId = builder.traceId;
        this.startTimeMillis = builder.startTimeMillis;
        this.attributes = new HashMap<>(builder.attributes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSource() {
        return source;
    }

    public String getQueueName() {
        return queueName;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getTraceId() {
        return traceId;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public Map<String, String> getAttributes() {
        return Map.copyOf(attributes);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * 처리 시간을 계산합니다.
     *
     * @return 처리 시간 (밀리초)
     */
    public long calculateDuration() {
        return System.currentTimeMillis() - startTimeMillis;
    }

    public static class Builder {
        private String source;
        private String queueName;
        private String messageId;
        private String traceId;
        private long startTimeMillis = System.currentTimeMillis();
        private final Map<String, String> attributes = new HashMap<>();

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder queueName(String queueName) {
            this.queueName = queueName;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder startTimeMillis(long startTimeMillis) {
            this.startTimeMillis = startTimeMillis;
            return this;
        }

        public Builder attribute(String key, String value) {
            if (key != null && value != null) {
                this.attributes.put(key, value);
            }
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            if (attributes != null) {
                this.attributes.putAll(attributes);
            }
            return this;
        }

        public MessageContext build() {
            return new MessageContext(this);
        }
    }

    @Override
    public String toString() {
        return "MessageContext{" +
                "source='" + source + '\'' +
                ", queueName='" + queueName + '\'' +
                ", messageId='" + messageId + '\'' +
                ", traceId='" + traceId + '\'' +
                '}';
    }
}
