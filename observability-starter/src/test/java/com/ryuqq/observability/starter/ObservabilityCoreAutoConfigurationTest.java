package com.ryuqq.observability.starter;

import com.ryuqq.observability.core.masking.LogMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservabilityCoreAutoConfiguration 테스트")
class ObservabilityCoreAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObservabilityCoreAutoConfiguration.class));

    @Nested
    @DisplayName("LogMasker 빈 생성 테스트")
    class LogMaskerBeanTest {

        @Test
        @DisplayName("LogMasker 빈이 생성된다")
        void shouldCreateLogMaskerBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(LogMasker.class);
            });
        }

        @Test
        @DisplayName("ObservabilityProperties 빈이 생성된다")
        void shouldCreateObservabilityPropertiesBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(ObservabilityProperties.class);
            });
        }

        @Test
        @DisplayName("커스텀 LogMasker가 있으면 자동 생성하지 않는다")
        void shouldNotCreateLogMaskerWhenCustomBeanExists() {
            ObservabilityProperties props = new ObservabilityProperties();
            LogMasker customMasker = new LogMasker(props.getMasking());

            contextRunner
                    .withBean(LogMasker.class, () -> customMasker)
                    .run(context -> {
                        assertThat(context).hasSingleBean(LogMasker.class);
                        assertThat(context.getBean(LogMasker.class)).isSameAs(customMasker);
                    });
        }

        @Test
        @DisplayName("마스킹 설정이 적용된다")
        void shouldApplyMaskingSettings() {
            contextRunner
                    .withPropertyValues(
                            "observability.masking.enabled=true"
                    )
                    .run(context -> {
                        assertThat(context).hasSingleBean(LogMasker.class);
                        ObservabilityProperties props = context.getBean(ObservabilityProperties.class);
                        assertThat(props.getMasking().isEnabled()).isTrue();
                    });
        }
    }

    @Nested
    @DisplayName("프로퍼티 바인딩 테스트")
    class PropertyBindingTest {

        @Test
        @DisplayName("서비스 이름이 바인딩된다")
        void shouldBindServiceName() {
            contextRunner
                    .withPropertyValues("observability.service-name=my-service")
                    .run(context -> {
                        ObservabilityProperties props = context.getBean(ObservabilityProperties.class);
                        assertThat(props.getServiceName()).isEqualTo("my-service");
                    });
        }

        @Test
        @DisplayName("서비스 이름이 없으면 기본값 사용")
        void shouldUseDefaultServiceName() {
            contextRunner.run(context -> {
                ObservabilityProperties props = context.getBean(ObservabilityProperties.class);
                assertThat(props.getServiceName()).isEqualTo("unknown");
            });
        }

        @Test
        @DisplayName("여러 프로퍼티가 바인딩된다")
        void shouldBindMultipleProperties() {
            contextRunner
                    .withPropertyValues(
                            "observability.service-name=test-service",
                            "observability.masking.enabled=false"
                    )
                    .run(context -> {
                        ObservabilityProperties props = context.getBean(ObservabilityProperties.class);
                        assertThat(props.getServiceName()).isEqualTo("test-service");
                        assertThat(props.getMasking().isEnabled()).isFalse();
                    });
        }
    }
}
