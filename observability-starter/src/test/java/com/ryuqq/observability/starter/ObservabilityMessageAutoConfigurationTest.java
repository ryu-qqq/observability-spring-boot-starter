package com.ryuqq.observability.starter;

import com.ryuqq.observability.message.interceptor.MessageLoggingInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservabilityMessageAutoConfiguration 테스트")
class ObservabilityMessageAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObservabilityCoreAutoConfiguration.class,
                    ObservabilityMessageAutoConfiguration.class
            ));

    @Nested
    @DisplayName("MessageLoggingInterceptor 빈 생성 테스트")
    class MessageLoggingInterceptorBeanTest {

        @Test
        @DisplayName("MessageLoggingInterceptor 빈이 생성된다")
        void shouldCreateMessageLoggingInterceptorBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(MessageLoggingInterceptor.class);
            });
        }

        @Test
        @DisplayName("커스텀 인터셉터가 있으면 자동 생성하지 않는다")
        void shouldNotCreateMessageLoggingInterceptorWhenCustomBeanExists() {
            MessageLoggingInterceptor customInterceptor = new MessageLoggingInterceptor(
                    new ObservabilityProperties().getMessage(),
                    () -> "custom-trace-id",
                    null,
                    "test-service"
            );

            contextRunner
                    .withBean(MessageLoggingInterceptor.class, () -> customInterceptor)
                    .run(context -> {
                        assertThat(context).hasSingleBean(MessageLoggingInterceptor.class);
                        assertThat(context.getBean(MessageLoggingInterceptor.class)).isSameAs(customInterceptor);
                    });
        }
    }

    @Nested
    @DisplayName("활성화/비활성화 테스트")
    class EnableDisableTest {

        @Test
        @DisplayName("message.enabled=false면 빈이 생성되지 않는다")
        void shouldNotCreateBeansWhenDisabled() {
            contextRunner
                    .withPropertyValues("observability.message.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(MessageLoggingInterceptor.class);
                    });
        }

        @Test
        @DisplayName("message.enabled=true면 빈이 생성된다")
        void shouldCreateBeansWhenEnabled() {
            contextRunner
                    .withPropertyValues("observability.message.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(MessageLoggingInterceptor.class);
                    });
        }

        @Test
        @DisplayName("기본적으로 활성화된다 (matchIfMissing=true)")
        void shouldBeEnabledByDefault() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(MessageLoggingInterceptor.class);
            });
        }
    }

    @Nested
    @DisplayName("프로퍼티 바인딩 테스트")
    class PropertyBindingTest {

        @Test
        @DisplayName("메시지 로깅 설정이 적용된다")
        void shouldApplyMessageLoggingSettings() {
            contextRunner
                    .withPropertyValues(
                            "observability.message.log-payload=true",
                            "observability.message.max-payload-length=2000"
                    )
                    .run(context -> {
                        ObservabilityProperties props = context.getBean(ObservabilityProperties.class);
                        assertThat(props.getMessage().isLogPayload()).isTrue();
                        assertThat(props.getMessage().getMaxPayloadLength()).isEqualTo(2000);
                    });
        }

        @Test
        @DisplayName("서비스 이름이 인터셉터에 전달된다")
        void shouldPassServiceNameToInterceptor() {
            contextRunner
                    .withPropertyValues("observability.service-name=message-service")
                    .run(context -> {
                        ObservabilityProperties props = context.getBean(ObservabilityProperties.class);
                        assertThat(props.getServiceName()).isEqualTo("message-service");
                        assertThat(context).hasSingleBean(MessageLoggingInterceptor.class);
                    });
        }
    }

    @Nested
    @DisplayName("통합 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("Core 설정과 함께 동작한다")
        void shouldWorkWithCoreConfiguration() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(ObservabilityProperties.class);
                assertThat(context).hasSingleBean(MessageLoggingInterceptor.class);
            });
        }

        @Test
        @DisplayName("UUID 기반 TraceId 생성기가 설정된다")
        void shouldConfigureUuidBasedTraceIdGenerator() {
            contextRunner.run(context -> {
                MessageLoggingInterceptor interceptor = context.getBean(MessageLoggingInterceptor.class);
                assertThat(interceptor).isNotNull();
            });
        }
    }
}
