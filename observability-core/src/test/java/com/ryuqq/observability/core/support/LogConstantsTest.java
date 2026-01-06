package com.ryuqq.observability.core.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogConstants 상수 테스트")
class LogConstantsTest {

    @Test
    @DisplayName("로거 이름 상수가 올바르게 정의되어 있다")
    void shouldHaveCorrectLoggerNames() {
        assertThat(LogConstants.LOGGER_HTTP).isEqualTo("observability.http");
        assertThat(LogConstants.LOGGER_MESSAGE).isEqualTo("observability.message");
        assertThat(LogConstants.LOGGER_CLIENT).isEqualTo("observability.client");
        assertThat(LogConstants.LOGGER_SCHEDULER).isEqualTo("observability.scheduler");
        assertThat(LogConstants.LOGGER_ERROR).isEqualTo("observability.error");
        assertThat(LogConstants.LOGGER_BUSINESS).isEqualTo("observability.business");
    }

    @Test
    @DisplayName("JSON 필드명 상수가 올바르게 정의되어 있다")
    void shouldHaveCorrectJsonFieldNames() {
        assertThat(LogConstants.FIELD_TIMESTAMP).isEqualTo("@timestamp");
        assertThat(LogConstants.FIELD_LEVEL).isEqualTo("level");
        assertThat(LogConstants.FIELD_LOGGER).isEqualTo("logger");
        assertThat(LogConstants.FIELD_MESSAGE).isEqualTo("message");
        assertThat(LogConstants.FIELD_TRACE_ID).isEqualTo("traceId");
        assertThat(LogConstants.FIELD_SPAN_ID).isEqualTo("spanId");
        assertThat(LogConstants.FIELD_SERVICE).isEqualTo("service");
        assertThat(LogConstants.FIELD_ENVIRONMENT).isEqualTo("environment");
        assertThat(LogConstants.FIELD_THREAD).isEqualTo("thread");
        assertThat(LogConstants.FIELD_CONTEXT).isEqualTo("context");
        assertThat(LogConstants.FIELD_ERROR).isEqualTo("error");
    }

    @Test
    @DisplayName("HTTP 관련 필드 상수가 올바르게 정의되어 있다")
    void shouldHaveCorrectHttpFieldNames() {
        assertThat(LogConstants.FIELD_HTTP).isEqualTo("http");
        assertThat(LogConstants.FIELD_HTTP_METHOD).isEqualTo("method");
        assertThat(LogConstants.FIELD_HTTP_URI).isEqualTo("uri");
        assertThat(LogConstants.FIELD_HTTP_STATUS).isEqualTo("status");
        assertThat(LogConstants.FIELD_HTTP_DURATION).isEqualTo("durationMs");
        assertThat(LogConstants.FIELD_HTTP_CLIENT_IP).isEqualTo("clientIp");
        assertThat(LogConstants.FIELD_HTTP_DIRECTION).isEqualTo("direction");
    }

    @Test
    @DisplayName("메시지 관련 필드 상수가 올바르게 정의되어 있다")
    void shouldHaveCorrectMessagingFieldNames() {
        assertThat(LogConstants.FIELD_MESSAGING).isEqualTo("messaging");
        assertThat(LogConstants.FIELD_MSG_SYSTEM).isEqualTo("system");
        assertThat(LogConstants.FIELD_MSG_QUEUE).isEqualTo("queue");
        assertThat(LogConstants.FIELD_MSG_MESSAGE_ID).isEqualTo("messageId");
        assertThat(LogConstants.FIELD_MSG_MESSAGE_TYPE).isEqualTo("messageType");
        assertThat(LogConstants.FIELD_MSG_DIRECTION).isEqualTo("direction");
    }

    @Test
    @DisplayName("방향 값 상수가 올바르게 정의되어 있다")
    void shouldHaveCorrectDirectionValues() {
        assertThat(LogConstants.DIRECTION_REQUEST).isEqualTo("REQUEST");
        assertThat(LogConstants.DIRECTION_RESPONSE).isEqualTo("RESPONSE");
        assertThat(LogConstants.DIRECTION_RECEIVE).isEqualTo("RECEIVE");
        assertThat(LogConstants.DIRECTION_SEND).isEqualTo("SEND");
    }

    @Test
    @DisplayName("에러 관련 필드 상수가 올바르게 정의되어 있다")
    void shouldHaveCorrectErrorFieldNames() {
        assertThat(LogConstants.FIELD_ERROR_TYPE).isEqualTo("type");
        assertThat(LogConstants.FIELD_ERROR_MESSAGE).isEqualTo("message");
        assertThat(LogConstants.FIELD_ERROR_STACK_TRACE).isEqualTo("stackTrace");
    }
}
