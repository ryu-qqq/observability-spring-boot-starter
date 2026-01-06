package com.ryuqq.observability.logging.aspect;

import com.ryuqq.observability.logging.annotation.BusinessLog;
import com.ryuqq.observability.logging.config.BusinessLoggingProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @BusinessLog 어노테이션 처리 AOP Aspect.
 *
 * <p>비즈니스 이벤트를 구조화된 형식으로 로깅합니다.</p>
 */
@Aspect
public class BusinessLogAspect {

    private static final Logger businessLogger = LoggerFactory.getLogger("observability.business");

    private final BusinessLoggingProperties properties;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public BusinessLogAspect(BusinessLoggingProperties properties) {
        this.properties = properties;
    }

    @Around("@annotation(businessLog)")
    public Object logBusinessEvent(ProceedingJoinPoint joinPoint, BusinessLog businessLog) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // SpEL 평가 컨텍스트 생성
        EvaluationContext evalContext = createEvaluationContext(method, args, joinPoint.getTarget());

        boolean success = false;
        Object result = null;
        Throwable error = null;

        try {
            result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Throwable e) {
            error = e;
            throw e;
        } finally {
            // 성공 시에만 로깅하거나 항상 로깅
            if (!businessLog.onSuccessOnly() || success) {
                logBusinessEvent(businessLog, evalContext, success, error);
            }
        }
    }

    private void logBusinessEvent(BusinessLog businessLog, EvaluationContext evalContext,
                                  boolean success, Throwable error) {
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("action", businessLog.action());

        if (!businessLog.entity().isEmpty()) {
            logData.put("entity", businessLog.entity());
        }

        if (!businessLog.description().isEmpty()) {
            logData.put("description", businessLog.description());
        }

        // entityId SpEL 평가
        if (!businessLog.entityId().isEmpty()) {
            try {
                Object entityId = evaluateExpression(businessLog.entityId(), evalContext);
                if (entityId != null) {
                    logData.put("entityId", entityId.toString());
                }
            } catch (Exception e) {
                // SpEL 평가 실패 시 무시
            }
        }

        // 추가 컨텍스트 SpEL 평가
        for (String contextExpr : businessLog.context()) {
            try {
                String[] parts = contextExpr.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String expression = parts[1].trim();
                    Object value = evaluateExpression(expression, evalContext);
                    if (value != null) {
                        logData.put(key, value);
                    }
                }
            } catch (Exception e) {
                // SpEL 평가 실패 시 무시
            }
        }

        logData.put("success", success);

        if (error != null) {
            logData.put("error", error.getClass().getSimpleName());
            logData.put("errorMessage", error.getMessage());
        }

        // 구조화된 로그 출력
        String logMessage = formatLogMessage(logData);

        if (success) {
            businessLogger.info("[BUSINESS] {}", logMessage);
        } else {
            businessLogger.error("[BUSINESS] {}", logMessage);
        }
    }

    private EvaluationContext createEvaluationContext(Method method, Object[] args, Object target) {
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                target, method, args, parameterNameDiscoverer);

        // 파라미터 이름으로 직접 접근 가능하도록 설정
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        return context;
    }

    private Object evaluateExpression(String expression, EvaluationContext context) {
        try {
            return parser.parseExpression(expression).getValue(context);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatLogMessage(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!first) {
                sb.append(" ");
            }
            first = false;

            sb.append(entry.getKey()).append("=");

            Object value = entry.getValue();
            if (value instanceof String str && str.contains(" ")) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
        }

        return sb.toString();
    }
}
