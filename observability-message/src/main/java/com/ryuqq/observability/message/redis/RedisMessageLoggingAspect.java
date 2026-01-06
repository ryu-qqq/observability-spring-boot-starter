package com.ryuqq.observability.message.redis;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.message.context.MessageContext;
import com.ryuqq.observability.message.interceptor.MessageLoggingInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Pub/Sub 메시지 리스너 자동 로깅 AOP Aspect.
 *
 * <p>Spring Data Redis의 메시지 리스너에 자동으로 로깅을 적용합니다.</p>
 *
 * <p>지원하는 리스너 패턴:</p>
 * <ul>
 *   <li>MessageListener 인터페이스 구현체</li>
 *   <li>@RedisListener 커스텀 어노테이션 (선택적)</li>
 *   <li>Redis Stream 리스너</li>
 * </ul>
 *
 * <pre>
 * {@code
 * // MessageListener 구현체는 자동으로 로깅됩니다
 * @Component
 * public class OrderEventListener implements MessageListener {
 *     @Override
 *     public void onMessage(Message message, byte[] pattern) {
 *         // 비즈니스 로직
 *     }
 * }
 * }
 * </pre>
 */
@Aspect
public class RedisMessageLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageLoggingAspect.class);

    private final MessageLoggingInterceptor interceptor;

    public RedisMessageLoggingAspect(MessageLoggingInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * MessageListener.onMessage() 메서드에 대한 Pointcut
     */
    @Pointcut("execution(* org.springframework.data.redis.connection.MessageListener.onMessage(..))")
    public void redisMessageListenerMethod() {
    }

    /**
     * StreamListener.onMessage() 메서드에 대한 Pointcut
     */
    @Pointcut("execution(* org.springframework.data.redis.stream.StreamListener.onMessage(..))")
    public void redisStreamListenerMethod() {
    }

    /**
     * Redis Pub/Sub 메시지 처리를 감싸서 로깅합니다.
     */
    @Around("redisMessageListenerMethod()")
    public Object aroundRedisMessageListener(ProceedingJoinPoint joinPoint) throws Throwable {
        return processWithLogging(joinPoint, "REDIS_PUBSUB");
    }

    /**
     * Redis Stream 메시지 처리를 감싸서 로깅합니다.
     */
    @Around("redisStreamListenerMethod()")
    public Object aroundRedisStreamListener(ProceedingJoinPoint joinPoint) throws Throwable {
        return processWithLogging(joinPoint, "REDIS_STREAM");
    }

    /**
     * 로깅과 함께 메시지를 처리합니다.
     */
    private Object processWithLogging(ProceedingJoinPoint joinPoint, String source) throws Throwable {
        MessageContext context = buildMessageContext(joinPoint, source);
        Object payload = extractPayload(joinPoint);

        // 처리 시작 로깅
        interceptor.beforeProcessing(context, payload);

        boolean success = false;
        Throwable error = null;

        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            // 처리 완료 로깅
            interceptor.afterProcessing(context, success, error);
        }
    }

    /**
     * 메시지 컨텍스트를 생성합니다.
     */
    private MessageContext buildMessageContext(ProceedingJoinPoint joinPoint, String source) {
        Object[] args = joinPoint.getArgs();

        String channel = extractChannel(args);
        String traceId = extractTraceId(args);
        String messageId = extractMessageId(args);
        Map<String, String> attributes = extractAttributes(args);

        return MessageContext.builder()
                .source(source)
                .queueName(channel)
                .messageId(messageId)
                .traceId(traceId)
                .attributes(attributes)
                .build();
    }

    /**
     * 메시지에서 채널/패턴 이름을 추출합니다.
     */
    private String extractChannel(Object[] args) {
        for (Object arg : args) {
            if (arg == null) continue;

            // byte[] pattern 파라미터 (Pub/Sub)
            if (arg instanceof byte[]) {
                return new String((byte[]) arg);
            }

            // Redis Message 객체에서 채널 추출
            if (arg.getClass().getName().contains("Message")) {
                try {
                    Method getChannel = arg.getClass().getMethod("getChannel");
                    Object channel = getChannel.invoke(arg);
                    if (channel instanceof byte[]) {
                        return new String((byte[]) channel);
                    }
                    return channel != null ? channel.toString() : null;
                } catch (Exception e) {
                    log.trace("Failed to extract channel from message", e);
                }
            }

            // MapRecord (Redis Stream)
            if (arg.getClass().getName().contains("Record")) {
                try {
                    Method getStream = arg.getClass().getMethod("getStream");
                    Object stream = getStream.invoke(arg);
                    return stream != null ? stream.toString() : null;
                } catch (Exception e) {
                    log.trace("Failed to extract stream name from record", e);
                }
            }
        }

        return "unknown";
    }

    /**
     * 메시지에서 TraceId를 추출합니다.
     */
    private String extractTraceId(Object[] args) {
        for (Object arg : args) {
            if (arg == null) continue;

            // MapRecord (Redis Stream) - 값에서 TraceId 추출
            if (arg.getClass().getName().contains("Record")) {
                try {
                    Method getValue = arg.getClass().getMethod("getValue");
                    Object value = getValue.invoke(arg);
                    if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<Object, Object> map = (Map<Object, Object>) value;
                        Object traceId = map.get(TraceIdHeaders.X_TRACE_ID);
                        if (traceId == null) {
                            traceId = map.get(TraceIdHeaders.MDC_TRACE_ID);
                        }
                        return traceId != null ? traceId.toString() : null;
                    }
                } catch (Exception e) {
                    log.trace("Failed to extract traceId from record", e);
                }
            }

            // Redis Message body에서 JSON으로 TraceId 추출 시도
            if (arg.getClass().getName().contains("Message")) {
                String body = extractBodyAsString(arg);
                if (body != null && body.contains(TraceIdHeaders.X_TRACE_ID)) {
                    // 간단한 JSON 파싱 (Jackson 없이)
                    return extractJsonField(body, TraceIdHeaders.X_TRACE_ID);
                }
            }
        }

        return null;
    }

    /**
     * 메시지에서 MessageId를 추출합니다 (Redis Stream).
     */
    private String extractMessageId(Object[] args) {
        for (Object arg : args) {
            if (arg == null) continue;

            // MapRecord (Redis Stream)
            if (arg.getClass().getName().contains("Record")) {
                try {
                    Method getId = arg.getClass().getMethod("getId");
                    Object id = getId.invoke(arg);
                    return id != null ? id.toString() : null;
                } catch (Exception e) {
                    log.trace("Failed to extract id from record", e);
                }
            }
        }

        return null;
    }

    /**
     * 메시지에서 속성들을 추출합니다.
     */
    private Map<String, String> extractAttributes(Object[] args) {
        Map<String, String> attributes = new HashMap<>();

        for (Object arg : args) {
            if (arg == null) continue;

            // MapRecord (Redis Stream)
            if (arg.getClass().getName().contains("Record")) {
                try {
                    Method getValue = arg.getClass().getMethod("getValue");
                    Object value = getValue.invoke(arg);
                    if (value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<Object, Object> map = (Map<Object, Object>) value;

                        // 사용자 컨텍스트 헤더 추출
                        extractIfPresent(map, TraceIdHeaders.X_USER_ID, attributes);
                        extractIfPresent(map, TraceIdHeaders.X_TENANT_ID, attributes);
                        extractIfPresent(map, TraceIdHeaders.X_ORGANIZATION_ID, attributes);
                    }
                } catch (Exception e) {
                    log.trace("Failed to extract attributes from record", e);
                }
            }
        }

        return attributes;
    }

    /**
     * Map에서 키가 존재하면 속성에 추가합니다.
     */
    private void extractIfPresent(Map<Object, Object> source, String key, Map<String, String> target) {
        Object value = source.get(key);
        if (value != null) {
            target.put(key, value.toString());
        }
    }

    /**
     * 메서드 인자에서 페이로드를 추출합니다.
     */
    private Object extractPayload(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg == null) continue;

            String className = arg.getClass().getName();

            // Redis Message 객체
            if (className.contains("Message") && !className.contains("Headers")) {
                return extractBodyAsString(arg);
            }

            // MapRecord (Redis Stream)
            if (className.contains("Record")) {
                try {
                    Method getValue = arg.getClass().getMethod("getValue");
                    return getValue.invoke(arg);
                } catch (Exception e) {
                    return arg;
                }
            }

            // byte[] pattern은 스킵
            if (arg instanceof byte[]) {
                continue;
            }

            return arg;
        }

        return null;
    }

    /**
     * Redis Message에서 body를 String으로 추출합니다.
     */
    private String extractBodyAsString(Object message) {
        try {
            Method getBody = message.getClass().getMethod("getBody");
            Object body = getBody.invoke(message);
            if (body instanceof byte[]) {
                return new String((byte[]) body);
            }
            return body != null ? body.toString() : null;
        } catch (Exception e) {
            log.trace("Failed to extract body from message", e);
            return null;
        }
    }

    /**
     * JSON 문자열에서 필드 값을 추출합니다 (간단한 파싱).
     */
    private String extractJsonField(String json, String fieldName) {
        try {
            // "fieldName":"value" 패턴 찾기
            String pattern = "\"" + fieldName + "\":\"";
            int startIdx = json.indexOf(pattern);
            if (startIdx == -1) {
                return null;
            }

            startIdx += pattern.length();
            int endIdx = json.indexOf("\"", startIdx);
            if (endIdx == -1) {
                return null;
            }

            return json.substring(startIdx, endIdx);
        } catch (Exception e) {
            return null;
        }
    }
}
