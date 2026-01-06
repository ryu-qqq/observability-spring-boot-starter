package com.ryuqq.observability.message.sqs;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.message.context.MessageContext;
import com.ryuqq.observability.message.interceptor.MessageLoggingInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * SQS 메시지 리스너 자동 로깅 AOP Aspect.
 *
 * <p>Spring Cloud AWS의 {@code @SqsListener} 어노테이션이 붙은 메서드에
 * 자동으로 로깅을 적용합니다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>메시지 수신 시 TraceId 자동 설정</li>
 *   <li>메시지 속성에서 TraceId 추출 (X-Trace-Id)</li>
 *   <li>처리 시간 자동 측정</li>
 *   <li>성공/실패 로그 자동 기록</li>
 * </ul>
 *
 * <pre>
 * {@code
 * // 이 어노테이션이 있는 메서드는 자동으로 로깅됩니다
 * @SqsListener("order-events")
 * public void handleOrderEvent(OrderEvent event) {
 *     // 비즈니스 로직
 * }
 * }
 * </pre>
 */
@Aspect
public class SqsMessageLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(SqsMessageLoggingAspect.class);
    private static final String SQS_LISTENER_ANNOTATION = "io.awspring.cloud.sqs.annotation.SqsListener";

    private final MessageLoggingInterceptor interceptor;

    public SqsMessageLoggingAspect(MessageLoggingInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    /**
     * @SqsListener 어노테이션이 붙은 메서드에 대한 Pointcut
     */
    @Pointcut("@annotation(io.awspring.cloud.sqs.annotation.SqsListener)")
    public void sqsListenerMethod() {
    }

    /**
     * SQS 메시지 처리를 감싸서 로깅합니다.
     */
    @Around("sqsListenerMethod()")
    public Object aroundSqsListener(ProceedingJoinPoint joinPoint) throws Throwable {
        MessageContext context = buildMessageContext(joinPoint);
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
    private MessageContext buildMessageContext(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // @SqsListener에서 큐 이름 추출
        String queueName = extractQueueName(method);

        // 메시지에서 TraceId 및 MessageId 추출
        String traceId = extractTraceIdFromArgs(joinPoint);
        String messageId = extractMessageIdFromArgs(joinPoint);
        Map<String, String> attributes = extractAttributesFromArgs(joinPoint);

        return MessageContext.builder()
                .source("SQS")
                .queueName(queueName)
                .messageId(messageId)
                .traceId(traceId)
                .attributes(attributes)
                .build();
    }

    /**
     * @SqsListener 어노테이션에서 큐 이름을 추출합니다.
     */
    @SuppressWarnings("unchecked")
    private String extractQueueName(Method method) {
        try {
            Class<? extends Annotation> annotationClass =
                    (Class<? extends Annotation>) Class.forName(SQS_LISTENER_ANNOTATION);
            Annotation annotation = method.getAnnotation(annotationClass);

            if (annotation != null) {
                Method valueMethod = annotationClass.getMethod("value");
                String[] values = (String[]) valueMethod.invoke(annotation);
                if (values != null && values.length > 0) {
                    return values[0];
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract queue name from @SqsListener annotation", e);
        }

        return "unknown";
    }

    /**
     * 메서드 인자에서 TraceId를 추출합니다.
     * Message 객체나 MessageHeaders에서 X-Trace-Id 헤더를 찾습니다.
     */
    private String extractTraceIdFromArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg == null) continue;

            // io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement 는 스킵
            String className = arg.getClass().getName();

            // Spring Message 객체에서 헤더 추출
            if (className.contains("Message") || className.contains("GenericMessage")) {
                String traceId = extractHeaderFromMessage(arg, TraceIdHeaders.X_TRACE_ID);
                if (traceId != null) {
                    return traceId;
                }
            }
        }

        return null;
    }

    /**
     * 메서드 인자에서 MessageId를 추출합니다.
     */
    private String extractMessageIdFromArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg == null) continue;

            String className = arg.getClass().getName();

            // Spring Message 객체에서 MessageId 추출
            if (className.contains("Message")) {
                String messageId = extractHeaderFromMessage(arg, "id");
                if (messageId == null) {
                    messageId = extractHeaderFromMessage(arg, "Sqs_MessageId");
                }
                if (messageId != null) {
                    return messageId;
                }
            }
        }

        return null;
    }

    /**
     * 메서드 인자에서 속성들을 추출합니다.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> extractAttributesFromArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        java.util.Map<String, String> attributes = new java.util.HashMap<>();

        for (Object arg : args) {
            if (arg == null) continue;

            String className = arg.getClass().getName();

            // Spring Message 객체에서 헤더들 추출
            if (className.contains("Message")) {
                // 사용자 컨텍스트 헤더 추출
                String userId = extractHeaderFromMessage(arg, TraceIdHeaders.X_USER_ID);
                if (userId != null) attributes.put(TraceIdHeaders.X_USER_ID, userId);

                String tenantId = extractHeaderFromMessage(arg, TraceIdHeaders.X_TENANT_ID);
                if (tenantId != null) attributes.put(TraceIdHeaders.X_TENANT_ID, tenantId);

                String orgId = extractHeaderFromMessage(arg, TraceIdHeaders.X_ORGANIZATION_ID);
                if (orgId != null) attributes.put(TraceIdHeaders.X_ORGANIZATION_ID, orgId);
            }
        }

        return attributes;
    }

    /**
     * Message 객체에서 특정 헤더를 추출합니다.
     */
    @SuppressWarnings("unchecked")
    private String extractHeaderFromMessage(Object message, String headerName) {
        try {
            // getHeaders() 메서드 호출
            Method getHeaders = message.getClass().getMethod("getHeaders");
            Object headers = getHeaders.invoke(message);

            if (headers != null) {
                // Map 형태의 헤더에서 값 추출
                if (headers instanceof Map) {
                    Object value = ((Map<?, ?>) headers).get(headerName);
                    return value != null ? value.toString() : null;
                }

                // get(String) 메서드 시도
                try {
                    Method get = headers.getClass().getMethod("get", Object.class);
                    Object value = get.invoke(headers, headerName);
                    return value != null ? value.toString() : null;
                } catch (NoSuchMethodException e) {
                    // 무시
                }
            }
        } catch (Exception e) {
            log.trace("Failed to extract header '{}' from message", headerName, e);
        }

        return null;
    }

    /**
     * 메서드 인자에서 페이로드를 추출합니다.
     */
    private Object extractPayload(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg == null) continue;

            String className = arg.getClass().getName();

            // Acknowledgement 등 메타데이터 객체 스킵
            if (className.contains("Acknowledgement") ||
                    className.contains("Headers") ||
                    className.contains("Visibility")) {
                continue;
            }

            // Message 객체인 경우 payload 추출
            if (className.contains("Message")) {
                try {
                    Method getPayload = arg.getClass().getMethod("getPayload");
                    return getPayload.invoke(arg);
                } catch (Exception e) {
                    // getPayload 실패 시 Message 객체 자체 반환
                    return arg;
                }
            }

            // 일반 객체는 페이로드로 간주
            return arg;
        }

        return null;
    }
}
