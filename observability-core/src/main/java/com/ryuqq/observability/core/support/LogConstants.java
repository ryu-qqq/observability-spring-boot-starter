package com.ryuqq.observability.core.support;

/**
 * 로깅 관련 상수 정의.
 *
 * <p>이 클래스는 순수 Java로 구현되어 Domain Layer에서도 사용할 수 있습니다.</p>
 */
public final class LogConstants {

    private LogConstants() {
    }

    // 로거 이름
    public static final String LOGGER_HTTP = "observability.http";
    public static final String LOGGER_MESSAGE = "observability.message";
    public static final String LOGGER_CLIENT = "observability.client";
    public static final String LOGGER_SCHEDULER = "observability.scheduler";
    public static final String LOGGER_ERROR = "observability.error";
    public static final String LOGGER_BUSINESS = "observability.business";

    // JSON 필드명
    public static final String FIELD_TIMESTAMP = "@timestamp";
    public static final String FIELD_LEVEL = "level";
    public static final String FIELD_LOGGER = "logger";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_TRACE_ID = "traceId";
    public static final String FIELD_SPAN_ID = "spanId";
    public static final String FIELD_SERVICE = "service";
    public static final String FIELD_ENVIRONMENT = "environment";
    public static final String FIELD_THREAD = "thread";
    public static final String FIELD_CONTEXT = "context";
    public static final String FIELD_ERROR = "error";

    // HTTP 관련 필드
    public static final String FIELD_HTTP = "http";
    public static final String FIELD_HTTP_METHOD = "method";
    public static final String FIELD_HTTP_URI = "uri";
    public static final String FIELD_HTTP_STATUS = "status";
    public static final String FIELD_HTTP_DURATION = "durationMs";
    public static final String FIELD_HTTP_CLIENT_IP = "clientIp";
    public static final String FIELD_HTTP_DIRECTION = "direction";

    // 메시지 관련 필드
    public static final String FIELD_MESSAGING = "messaging";
    public static final String FIELD_MSG_SYSTEM = "system";
    public static final String FIELD_MSG_QUEUE = "queue";
    public static final String FIELD_MSG_MESSAGE_ID = "messageId";
    public static final String FIELD_MSG_MESSAGE_TYPE = "messageType";
    public static final String FIELD_MSG_DIRECTION = "direction";

    // 방향 값
    public static final String DIRECTION_REQUEST = "REQUEST";
    public static final String DIRECTION_RESPONSE = "RESPONSE";
    public static final String DIRECTION_RECEIVE = "RECEIVE";
    public static final String DIRECTION_SEND = "SEND";

    // 에러 관련 필드
    public static final String FIELD_ERROR_TYPE = "type";
    public static final String FIELD_ERROR_MESSAGE = "message";
    public static final String FIELD_ERROR_STACK_TRACE = "stackTrace";
}
