package com.ryuqq.observability.logging.aspect;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.logging.annotation.Loggable;
import com.ryuqq.observability.logging.config.BusinessLoggingProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LoggableAspect 테스트")
class LoggableAspectTest {

    private BusinessLoggingProperties properties;
    private LogMasker logMasker;
    private LoggableAspect aspect;

    @BeforeEach
    void setUp() {
        properties = new BusinessLoggingProperties();
        logMasker = mock(LogMasker.class);
        when(logMasker.mask(anyString())).thenAnswer(inv -> inv.getArgument(0));
        aspect = new LoggableAspect(properties, logMasker);
    }

    @Nested
    @DisplayName("logMethodExecution 테스트")
    class LogMethodExecutionTest {

        @Test
        @DisplayName("로깅이 비활성화되면 메서드만 실행한다")
        void shouldJustProceedWhenLoggingDisabled() throws Throwable {
            properties.setEnabled(false);

            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "expectedResult");
            Loggable loggable = createLoggable();

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("expectedResult");
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("기본 설정으로 메서드를 로깅한다")
        void shouldLogMethodWithDefaultSettings() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            Loggable loggable = createLoggable();

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
            verify(joinPoint).proceed();
        }

        @Test
        @DisplayName("인자 로깅이 활성화되면 인자를 로깅한다")
        void shouldLogArgsWhenEnabled() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", 123});
            Loggable loggable = createLoggable(true, false, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
            verify(logMasker).mask("[arg1, 123]");
        }

        @Test
        @DisplayName("결과 로깅이 활성화되면 결과를 로깅한다")
        void shouldLogResultWhenEnabled() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "myResult");
            Loggable loggable = createLoggable(false, true, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("myResult");
            verify(logMasker).mask("myResult");
        }

        @Test
        @DisplayName("null 결과도 처리한다")
        void shouldHandleNullResult() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", null);
            Loggable loggable = createLoggable(false, true, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("예외 발생 시 에러 로깅 후 다시 던진다")
        void shouldLogErrorAndRethrowException() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            RuntimeException exception = new RuntimeException("Test error");
            when(joinPoint.proceed()).thenThrow(exception);

            MethodSignature signature = mock(MethodSignature.class);
            Method method = TestTarget.class.getMethod("testMethod");
            when(signature.getMethod()).thenReturn(method);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getTarget()).thenReturn(new TestTarget());
            when(joinPoint.getArgs()).thenReturn(new Object[0]);

            Loggable loggable = createLoggable();

            assertThatThrownBy(() -> aspect.logMethodExecution(joinPoint, loggable))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test error");
        }

        @Test
        @DisplayName("사용자 정의 메서드 이름을 사용한다")
        void shouldUseCustomMethodName() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            Loggable loggable = createLoggable(false, false, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "CustomOperation");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }
    }

    @Nested
    @DisplayName("느린 실행 감지 테스트")
    class SlowExecutionTest {

        @Test
        @DisplayName("어노테이션 임계값 사용")
        void shouldUseAnnotationThreshold() throws Throwable {
            ProceedingJoinPoint joinPoint = createSlowJoinPoint("testMethod", "result", 100);
            Loggable loggable = createLoggable(false, false, true, 50, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("프로퍼티 임계값 사용 (어노테이션 값이 음수일 때)")
        void shouldUsePropertyThresholdWhenAnnotationNegative() throws Throwable {
            properties.setSlowExecutionThreshold(50);
            ProceedingJoinPoint joinPoint = createSlowJoinPoint("testMethod", "result", 100);
            Loggable loggable = createLoggable(false, false, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("임계값 미만이면 경고 없음")
        void shouldNotWarnBelowThreshold() throws Throwable {
            properties.setSlowExecutionThreshold(1000);
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            Loggable loggable = createLoggable();

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }
    }

    @Nested
    @DisplayName("로그 레벨 테스트")
    class LogLevelTest {

        @Test
        @DisplayName("TRACE 레벨로 로깅")
        void shouldLogAtTraceLevel() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            Loggable loggable = createLoggable(false, false, true, -1, Loggable.LogLevel.TRACE, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("DEBUG 레벨로 로깅")
        void shouldLogAtDebugLevel() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            Loggable loggable = createLoggable(false, false, true, -1, Loggable.LogLevel.DEBUG, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("INFO 레벨로 로깅")
        void shouldLogAtInfoLevel() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            Loggable loggable = createLoggable(false, false, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("WARN 레벨로 로깅")
        void shouldLogAtWarnLevel() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            Loggable loggable = createLoggable(false, false, true, -1, Loggable.LogLevel.WARN, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("ERROR 레벨로 로깅")
        void shouldLogAtErrorLevel() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            Loggable loggable = createLoggable(false, false, true, -1, Loggable.LogLevel.ERROR, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("예외 시 에러 레벨 사용")
        void shouldUseErrorLevelOnException() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.proceed()).thenThrow(new RuntimeException("error"));

            MethodSignature signature = mock(MethodSignature.class);
            Method method = TestTarget.class.getMethod("testMethod");
            when(signature.getMethod()).thenReturn(method);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getTarget()).thenReturn(new TestTarget());
            when(joinPoint.getArgs()).thenReturn(new Object[0]);

            Loggable loggable = createLoggable(false, false, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.WARN, "");

            assertThatThrownBy(() -> aspect.logMethodExecution(joinPoint, loggable))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("인자 포맷팅 테스트")
    class ArgsFormattingTest {

        @Test
        @DisplayName("null 인자 배열 처리")
        void shouldHandleNullArgs() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            when(joinPoint.getArgs()).thenReturn(null);
            Loggable loggable = createLoggable(true, false, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("빈 인자 배열 처리")
        void shouldHandleEmptyArgs() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            when(joinPoint.getArgs()).thenReturn(new Object[0]);
            Loggable loggable = createLoggable(true, false, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("긴 인자는 잘린다")
        void shouldTruncateLongArgs() throws Throwable {
            String longArg = "a".repeat(600);
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            when(joinPoint.getArgs()).thenReturn(new Object[]{longArg});
            Loggable loggable = createLoggable(true, false, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
            verify(logMasker).mask("[" + longArg + "]");
        }

        @Test
        @DisplayName("긴 결과는 잘린다")
        void shouldTruncateLongResult() throws Throwable {
            String longResult = "b".repeat(600);
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", longResult);
            Loggable loggable = createLoggable(false, true, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo(longResult);
            verify(logMasker).mask(longResult);
        }
    }

    @Nested
    @DisplayName("실행 시간 로깅 테스트")
    class ExecutionTimeLoggingTest {

        @Test
        @DisplayName("실행 시간 로깅 비활성화")
        void shouldNotLogExecutionTimeWhenDisabled() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("testMethod", "result");
            Loggable loggable = createLoggable(false, false, false, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");

            Object result = aspect.logMethodExecution(joinPoint, loggable);

            assertThat(result).isEqualTo("result");
        }
    }

    // Helper methods

    private ProceedingJoinPoint createMockJoinPoint(String methodName, Object returnValue) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        Method method = TestTarget.class.getMethod("testMethod");
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new TestTarget());
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenReturn(returnValue);

        return joinPoint;
    }

    private ProceedingJoinPoint createSlowJoinPoint(String methodName, Object returnValue, long delayMs) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        Method method = TestTarget.class.getMethod("testMethod");
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new TestTarget());
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenAnswer(inv -> {
            Thread.sleep(delayMs);
            return returnValue;
        });

        return joinPoint;
    }

    private Loggable createLoggable() {
        return createLoggable(false, false, true, -1, Loggable.LogLevel.INFO, Loggable.LogLevel.ERROR, "");
    }

    private Loggable createLoggable(boolean includeArgs, boolean includeResult, boolean includeExecutionTime,
                                     long slowThreshold, Loggable.LogLevel level, Loggable.LogLevel errorLevel, String value) {
        return new Loggable() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Loggable.class;
            }

            @Override
            public String value() {
                return value;
            }

            @Override
            public boolean includeArgs() {
                return includeArgs;
            }

            @Override
            public boolean includeResult() {
                return includeResult;
            }

            @Override
            public boolean includeExecutionTime() {
                return includeExecutionTime;
            }

            @Override
            public long slowThreshold() {
                return slowThreshold;
            }

            @Override
            public LogLevel level() {
                return level;
            }

            @Override
            public LogLevel errorLevel() {
                return errorLevel;
            }
        };
    }

    // Test target class
    public static class TestTarget {
        public String testMethod() {
            return "test";
        }
    }
}
