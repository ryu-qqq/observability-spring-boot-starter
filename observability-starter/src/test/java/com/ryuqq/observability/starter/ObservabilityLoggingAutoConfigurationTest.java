package com.ryuqq.observability.starter;

import com.ryuqq.observability.logging.aspect.BusinessLogAspect;
import com.ryuqq.observability.logging.aspect.LoggableAspect;
import com.ryuqq.observability.logging.event.BusinessEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservabilityLoggingAutoConfiguration 테스트")
class ObservabilityLoggingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObservabilityCoreAutoConfiguration.class,
                    ObservabilityLoggingAutoConfiguration.class
            ));

    @Nested
    @DisplayName("LoggableAspect 빈 생성 테스트")
    class LoggableAspectBeanTest {

        @Test
        @DisplayName("LoggableAspect 빈이 생성된다")
        void shouldCreateLoggableAspectBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(LoggableAspect.class);
            });
        }

        @Test
        @DisplayName("커스텀 LoggableAspect가 있으면 자동 생성하지 않는다")
        void shouldNotCreateLoggableAspectWhenCustomBeanExists() {
            contextRunner
                    .withBean(LoggableAspect.class, () -> {
                        ObservabilityProperties props = new ObservabilityProperties();
                        return new LoggableAspect(props.getLogging(), null);
                    })
                    .run(context -> {
                        assertThat(context).hasSingleBean(LoggableAspect.class);
                    });
        }
    }

    @Nested
    @DisplayName("BusinessLogAspect 빈 생성 테스트")
    class BusinessLogAspectBeanTest {

        @Test
        @DisplayName("BusinessLogAspect 빈이 생성된다")
        void shouldCreateBusinessLogAspectBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(BusinessLogAspect.class);
            });
        }

        @Test
        @DisplayName("커스텀 BusinessLogAspect가 있으면 자동 생성하지 않는다")
        void shouldNotCreateBusinessLogAspectWhenCustomBeanExists() {
            contextRunner
                    .withBean(BusinessLogAspect.class, () -> {
                        ObservabilityProperties props = new ObservabilityProperties();
                        return new BusinessLogAspect(props.getLogging());
                    })
                    .run(context -> {
                        assertThat(context).hasSingleBean(BusinessLogAspect.class);
                    });
        }
    }

    @Nested
    @DisplayName("BusinessEventListener 빈 생성 테스트")
    class BusinessEventListenerBeanTest {

        @Test
        @DisplayName("BusinessEventListener 빈이 생성된다")
        void shouldCreateBusinessEventListenerBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(BusinessEventListener.class);
            });
        }

        @Test
        @DisplayName("커스텀 BusinessEventListener가 있으면 자동 생성하지 않는다")
        void shouldNotCreateBusinessEventListenerWhenCustomBeanExists() {
            contextRunner
                    .withBean(BusinessEventListener.class, () -> {
                        ObservabilityProperties props = new ObservabilityProperties();
                        return new BusinessEventListener(props.getLogging());
                    })
                    .run(context -> {
                        assertThat(context).hasSingleBean(BusinessEventListener.class);
                    });
        }
    }

    @Nested
    @DisplayName("활성화/비활성화 테스트")
    class EnableDisableTest {

        @Test
        @DisplayName("logging.enabled=false면 빈이 생성되지 않는다")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("observability.logging.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(LoggableAspect.class);
                        assertThat(context).doesNotHaveBean(BusinessLogAspect.class);
                        assertThat(context).doesNotHaveBean(BusinessEventListener.class);
                    });
        }

        @Test
        @DisplayName("logging.enabled=true면 빈이 생성된다")
        void shouldCreateBeansWhenEnabled() {
            contextRunner
                    .withPropertyValues("observability.logging.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(LoggableAspect.class);
                        assertThat(context).hasSingleBean(BusinessLogAspect.class);
                        assertThat(context).hasSingleBean(BusinessEventListener.class);
                    });
        }

        @Test
        @DisplayName("기본적으로 활성화된다 (matchIfMissing=true)")
        void shouldBeEnabledByDefault() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(LoggableAspect.class);
                assertThat(context).hasSingleBean(BusinessLogAspect.class);
                assertThat(context).hasSingleBean(BusinessEventListener.class);
            });
        }
    }

    @Nested
    @DisplayName("프로퍼티 바인딩 테스트")
    class PropertyBindingTest {

        @Test
        @DisplayName("로깅 설정이 적용된다")
        void shouldApplyLoggingSettings() {
            contextRunner
                    .withPropertyValues(
                            "observability.logging.log-arguments=true",
                            "observability.logging.log-result=true"
                    )
                    .run(context -> {
                        ObservabilityProperties props = context.getBean(ObservabilityProperties.class);
                        assertThat(props.getLogging().isLogArguments()).isTrue();
                        assertThat(props.getLogging().isLogResult()).isTrue();
                    });
        }
    }

    @Nested
    @DisplayName("통합 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("모든 로깅 빈이 함께 생성된다")
        void shouldCreateAllLoggingBeans() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(LoggableAspect.class);
                assertThat(context).hasSingleBean(BusinessLogAspect.class);
                assertThat(context).hasSingleBean(BusinessEventListener.class);
            });
        }

        @Test
        @DisplayName("Core 설정과 함께 동작한다")
        void shouldWorkWithCoreConfiguration() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(ObservabilityProperties.class);
                assertThat(context).hasSingleBean(LoggableAspect.class);
            });
        }
    }
}
