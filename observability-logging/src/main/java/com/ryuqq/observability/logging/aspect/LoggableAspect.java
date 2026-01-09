package com.ryuqq.observability.logging.aspect;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.logging.annotation.Loggable;
import com.ryuqq.observability.logging.config.BusinessLoggingProperties;
import net.logstash.logback.marker.Markers;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

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
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("method", methodName);
        logData.put("phase", "started");

        if (loggable.includeArgs() && args != null && args.length > 0) {
            logData.put("args", formatArgs(args));
        }

        Marker marker = Markers.appendEntries(logData);
        log(logger, loggable.level(), marker, "{} started", methodName);
    }

    private void logSuccess(Logger logger, Loggable loggable, String methodName, long duration, Object result) {
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("method", methodName);
        logData.put("phase", "completed");

        if (loggable.includeExecutionTime()) {
            logData.put("duration", duration);
        }

        if (loggable.includeResult() && result != null) {
            String resultStr = logMasker.mask(result.toString());
            logData.put("result", truncate(resultStr));
        }

        Marker marker = Markers.appendEntries(logData);
        if (loggable.includeExecutionTime()) {
            log(logger, loggable.level(), marker, "{} completed in {}ms", methodName, duration);
        } else {
            log(logger, loggable.level(), marker, "{} completed", methodName);
        }
    }

    private void logSlowExecution(Logger logger, Loggable loggable, String methodName, long duration) {
        long threshold = loggable.slowThreshold() > 0
                ? loggable.slowThreshold()
                : properties.getSlowExecutionThreshold();

        if (duration > threshold) {
            Map<String, Object> logData = new LinkedHashMap<>();
            logData.put("method", methodName);
            logData.put("phase", "slow_execution");
            logData.put("duration", duration);
            logData.put("threshold", threshold);

            Marker marker = Markers.appendEntries(logData);
            logger.warn(marker, "{} slow execution detected: {}ms (threshold: {}ms)",
                    methodName, duration, threshold);
        }
    }

    private void logError(Logger logger, Loggable loggable, String methodName, long duration, Throwable e) {
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("method", methodName);
        logData.put("phase", "failed");
        logData.put("duration", duration);
        logData.put("error", e.getClass().getSimpleName());

        String maskedErrorMessage = truncate(logMasker.mask(String.valueOf(e.getMessage())));
        logData.put("errorMessage", maskedErrorMessage);

        Marker marker = Markers.appendEntries(logData);
        log(logger, loggable.errorLevel(), marker, "{} failed after {}ms: {} - {}",
                methodName, duration, e.getClass().getSimpleName(), maskedErrorMessage, e);
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

    private void log(Logger logger, Loggable.LogLevel level, Marker marker, String format, Object... args) {
        switch (level) {
            case TRACE -> logger.trace(marker, format, args);
            case DEBUG -> logger.debug(marker, format, args);
            case INFO -> logger.info(marker, format, args);
            case WARN -> logger.warn(marker, format, args);
            case ERROR -> logger.error(marker, format, args);
        }
    }
}
