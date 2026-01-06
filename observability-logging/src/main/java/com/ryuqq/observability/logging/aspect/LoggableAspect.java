package com.ryuqq.observability.logging.aspect;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.logging.annotation.Loggable;
import com.ryuqq.observability.logging.config.BusinessLoggingProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @Loggable 어노테이션 처리 AOP Aspect.
 *
 * <p>메서드 실행의 시작/종료 및 실행 시간을 자동으로 로깅합니다.</p>
 */
@Aspect
public class LoggableAspect {

    private final BusinessLoggingProperties properties;
    private final LogMasker logMasker;

    public LoggableAspect(BusinessLoggingProperties properties, LogMasker logMasker) {
        this.properties = properties;
        this.logMasker = logMasker;
    }

    @Around("@annotation(loggable)")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        String methodName = getMethodName(loggable, method);
        long startTime = System.currentTimeMillis();

        // 시작 로그
        logStart(logger, loggable, methodName, joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();

            // 종료 로그
            long duration = System.currentTimeMillis() - startTime;
            logSuccess(logger, loggable, methodName, duration, result);

            // 느린 실행 경고
            logSlowExecution(logger, loggable, methodName, duration);

            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            logError(logger, loggable, methodName, duration, e);
            throw e;
        }
    }

    private String getMethodName(Loggable loggable, Method method) {
        if (!loggable.value().isEmpty()) {
            return loggable.value();
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    private void logStart(Logger logger, Loggable loggable, String methodName, Object[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append(" started");

        if (loggable.includeArgs() && args != null && args.length > 0) {
            String argsStr = formatArgs(args);
            sb.append(" with args: ").append(argsStr);
        }

        log(logger, loggable.level(), sb.toString());
    }

    private void logSuccess(Logger logger, Loggable loggable, String methodName, long duration, Object result) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append(" completed");

        if (loggable.includeExecutionTime()) {
            sb.append(" in ").append(duration).append("ms");
        }

        if (loggable.includeResult() && result != null) {
            String resultStr = logMasker.mask(result.toString());
            sb.append(" with result: ").append(truncate(resultStr));
        }

        log(logger, loggable.level(), sb.toString());
    }

    private void logSlowExecution(Logger logger, Loggable loggable, String methodName, long duration) {
        long threshold = loggable.slowThreshold() > 0
                ? loggable.slowThreshold()
                : properties.getSlowExecutionThreshold();

        if (duration > threshold) {
            logger.warn("{} slow execution detected: {}ms (threshold: {}ms)",
                    methodName, duration, threshold);
        }
    }

    private void logError(Logger logger, Loggable loggable, String methodName, long duration, Throwable e) {
        String message = String.format("%s failed after %dms: %s - %s",
                methodName, duration, e.getClass().getSimpleName(), e.getMessage());

        log(logger, loggable.errorLevel(), message, e);
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        String argsStr = Arrays.toString(args);
        String masked = logMasker.mask(argsStr);
        return truncate(masked);
    }

    private String truncate(String str) {
        if (str == null) {
            return null;
        }
        int maxLength = 500;
        if (str.length() > maxLength) {
            return str.substring(0, maxLength) + "...[TRUNCATED]";
        }
        return str;
    }

    private void log(Logger logger, Loggable.LogLevel level, String message) {
        log(logger, level, message, null);
    }

    private void log(Logger logger, Loggable.LogLevel level, String message, Throwable e) {
        switch (level) {
            case TRACE -> {
                if (e != null) logger.trace(message, e);
                else logger.trace(message);
            }
            case DEBUG -> {
                if (e != null) logger.debug(message, e);
                else logger.debug(message);
            }
            case INFO -> {
                if (e != null) logger.info(message, e);
                else logger.info(message);
            }
            case WARN -> {
                if (e != null) logger.warn(message, e);
                else logger.warn(message);
            }
            case ERROR -> {
                if (e != null) logger.error(message, e);
                else logger.error(message);
            }
        }
    }
}
