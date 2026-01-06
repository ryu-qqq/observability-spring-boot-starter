package com.ryuqq.observability.logging.aspect;

import com.ryuqq.observability.logging.annotation.BusinessLog;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("BusinessLogAspect 테스트")
class BusinessLogAspectTest {

    private BusinessLoggingProperties properties;
    private BusinessLogAspect aspect;

    @BeforeEach
    void setUp() {
        properties = new BusinessLoggingProperties();
        aspect = new BusinessLogAspect(properties);
    }

    @Nested
    @DisplayName("logBusinessEvent 테스트")
    class LogBusinessEventTest {

        @Test
        @DisplayName("로깅이 비활성화되면 메서드만 실행한다")
        void shouldJustProceedWhenLoggingDisabled() throws Throwable {
            properties.setEnabled(false);

            ProceedingJoinPoint joinPoint = createMockJoinPoint("expectedResult");
            BusinessLog businessLog = createBusinessLog("ORDER_CREATED", "Order", "주문 생성", "", new String[0], false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("expectedResult");
        }

        @Test
        @DisplayName("기본 비즈니스 로그를 기록한다")
        void shouldLogBasicBusinessEvent() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("result");
            BusinessLog businessLog = createBusinessLog("ORDER_CREATED", "Order", "주문 생성", "", new String[0], false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("SpEL entityId를 평가한다")
        void shouldEvaluateSpelEntityId() throws Throwable {
            TestCommand command = new TestCommand(123L, "test");
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs("result", new Object[]{command});
            BusinessLog businessLog = createBusinessLog("ORDER_CREATED", "Order", "", "#command.id", new String[0], false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("SpEL context를 평가한다")
        void shouldEvaluateSpelContext() throws Throwable {
            TestCommand command = new TestCommand(123L, "testName");
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs("result", new Object[]{command});
            BusinessLog businessLog = createBusinessLog("ORDER_CREATED", "Order", "", "",
                    new String[]{"orderId=#command.id", "name=#command.name"}, false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("잘못된 SpEL 표현식은 무시한다")
        void shouldIgnoreInvalidSpelExpression() throws Throwable {
            TestCommand command = new TestCommand(123L, "test");
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs("result", new Object[]{command});
            BusinessLog businessLog = createBusinessLog("ORDER_CREATED", "Order", "", "#nonExistent.field", new String[0], false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("잘못된 context 표현식은 무시한다")
        void shouldIgnoreInvalidContextExpression() throws Throwable {
            TestCommand command = new TestCommand(123L, "test");
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs("result", new Object[]{command});
            BusinessLog businessLog = createBusinessLog("ORDER_CREATED", "Order", "", "",
                    new String[]{"invalid=#nonExistent", "alsoinvalid"}, false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("예외 발생 시 에러 로그를 기록한다")
        void shouldLogErrorOnException() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.proceed()).thenThrow(new RuntimeException("Test error"));

            MethodSignature signature = mock(MethodSignature.class);
            Method method = TestService.class.getMethod("process", TestCommand.class);
            when(signature.getMethod()).thenReturn(method);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[]{new TestCommand(1L, "test")});

            BusinessLog businessLog = createBusinessLog("ORDER_CREATED", "Order", "", "", new String[0], false);

            assertThatThrownBy(() -> aspect.logBusinessEvent(joinPoint, businessLog))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Test error");
        }

        @Test
        @DisplayName("onSuccessOnly가 true면 실패 시 로깅하지 않는다")
        void shouldNotLogOnFailureWhenOnSuccessOnly() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.proceed()).thenThrow(new RuntimeException("Test error"));

            MethodSignature signature = mock(MethodSignature.class);
            Method method = TestService.class.getMethod("process", TestCommand.class);
            when(signature.getMethod()).thenReturn(method);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[]{new TestCommand(1L, "test")});

            BusinessLog businessLog = createBusinessLog("ORDER_CREATED", "Order", "", "", new String[0], true);

            assertThatThrownBy(() -> aspect.logBusinessEvent(joinPoint, businessLog))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("onSuccessOnly가 false면 항상 로깅한다")
        void shouldAlwaysLogWhenOnSuccessOnlyFalse() throws Throwable {
            ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
            when(joinPoint.proceed()).thenThrow(new RuntimeException("Test error"));

            MethodSignature signature = mock(MethodSignature.class);
            Method method = TestService.class.getMethod("process", TestCommand.class);
            when(signature.getMethod()).thenReturn(method);
            when(joinPoint.getSignature()).thenReturn(signature);
            when(joinPoint.getTarget()).thenReturn(new TestService());
            when(joinPoint.getArgs()).thenReturn(new Object[]{new TestCommand(1L, "test")});

            BusinessLog businessLog = createBusinessLog("ORDER_CREATED", "Order", "", "", new String[0], false);

            assertThatThrownBy(() -> aspect.logBusinessEvent(joinPoint, businessLog))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("엔티티 및 설명 테스트")
    class EntityAndDescriptionTest {

        @Test
        @DisplayName("엔티티가 비어있으면 로그에 포함하지 않는다")
        void shouldNotIncludeEmptyEntity() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("result");
            BusinessLog businessLog = createBusinessLog("ACTION", "", "", "", new String[0], false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("설명이 비어있으면 로그에 포함하지 않는다")
        void shouldNotIncludeEmptyDescription() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("result");
            BusinessLog businessLog = createBusinessLog("ACTION", "Entity", "", "", new String[0], false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("공백이 포함된 설명은 따옴표로 감싼다")
        void shouldQuoteDescriptionWithSpaces() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPoint("result");
            BusinessLog businessLog = createBusinessLog("ACTION", "Entity", "설명 with 공백", "", new String[0], false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }
    }

    @Nested
    @DisplayName("SpEL 컨텍스트 평가 테스트")
    class SpelContextEvaluationTest {

        @Test
        @DisplayName("메서드 파라미터에 접근할 수 있다")
        void shouldAccessMethodParameters() throws Throwable {
            TestCommand command = new TestCommand(999L, "testValue");
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs("result", new Object[]{command});
            BusinessLog businessLog = createBusinessLog("ACTION", "", "", "#command.id",
                    new String[]{"value=#command.name"}, false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("여러 파라미터가 있는 경우 처리")
        void shouldHandleMultipleParameters() throws Throwable {
            ProceedingJoinPoint joinPoint = createMockJoinPointWithMultipleArgs("result",
                    new Object[]{123L, "name"});
            BusinessLog businessLog = createBusinessLog("ACTION", "", "", "#id",
                    new String[]{"paramName=#name"}, false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("null entityId 평가 결과는 무시한다")
        void shouldIgnoreNullEntityIdResult() throws Throwable {
            TestCommand command = new TestCommand(null, "test");
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs("result", new Object[]{command});
            BusinessLog businessLog = createBusinessLog("ACTION", "", "", "#command.id", new String[0], false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }

        @Test
        @DisplayName("null context 값은 무시한다")
        void shouldIgnoreNullContextValue() throws Throwable {
            TestCommand command = new TestCommand(123L, null);
            ProceedingJoinPoint joinPoint = createMockJoinPointWithArgs("result", new Object[]{command});
            BusinessLog businessLog = createBusinessLog("ACTION", "", "", "",
                    new String[]{"name=#command.name"}, false);

            Object result = aspect.logBusinessEvent(joinPoint, businessLog);

            assertThat(result).isEqualTo("result");
        }
    }

    // Helper methods

    private ProceedingJoinPoint createMockJoinPoint(Object returnValue) throws Throwable {
        return createMockJoinPointWithArgs(returnValue, new Object[]{new TestCommand(1L, "test")});
    }

    private ProceedingJoinPoint createMockJoinPointWithArgs(Object returnValue, Object[] args) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        Method method = TestService.class.getMethod("process", TestCommand.class);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(returnValue);

        return joinPoint;
    }

    private ProceedingJoinPoint createMockJoinPointWithMultipleArgs(Object returnValue, Object[] args) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        Method method = TestService.class.getMethod("processMultiple", Long.class, String.class);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(returnValue);

        return joinPoint;
    }

    private BusinessLog createBusinessLog(String action, String entity, String description,
                                           String entityId, String[] context, boolean onSuccessOnly) {
        return new BusinessLog() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return BusinessLog.class;
            }

            @Override
            public String action() {
                return action;
            }

            @Override
            public String entity() {
                return entity;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public String entityId() {
                return entityId;
            }

            @Override
            public String[] context() {
                return context;
            }

            @Override
            public boolean onSuccessOnly() {
                return onSuccessOnly;
            }
        };
    }

    // Test helpers

    public static class TestCommand {
        private final Long id;
        private final String name;

        public TestCommand(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class TestService {
        public String process(TestCommand command) {
            return "processed";
        }

        public String processMultiple(Long id, String name) {
            return "processed";
        }
    }
}
