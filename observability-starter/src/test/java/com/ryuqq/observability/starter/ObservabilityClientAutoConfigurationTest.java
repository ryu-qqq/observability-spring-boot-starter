package com.ryuqq.observability.starter;

import com.ryuqq.observability.client.rest.TraceIdRestClientInterceptor;
import com.ryuqq.observability.client.rest.TraceIdRestTemplateInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservabilityClientAutoConfiguration 테스트")
class ObservabilityClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObservabilityCoreAutoConfiguration.class,
                    ObservabilityClientAutoConfiguration.class
            ));

    @Nested
    @DisplayName("RestTemplate 인터셉터 테스트")
    class RestTemplateInterceptorTest {

        @Test
        @DisplayName("TraceIdRestTemplateInterceptor 빈이 생성된다")
        void shouldCreateRestTemplateInterceptorBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(TraceIdRestTemplateInterceptor.class);
            });
        }

        @Test
        @DisplayName("커스텀 인터셉터가 있으면 자동 생성하지 않는다")
        void shouldNotCreateRestTemplateInterceptorWhenCustomBeanExists() {
            TraceIdRestTemplateInterceptor customInterceptor = new TraceIdRestTemplateInterceptor();

            contextRunner
                    .withBean(TraceIdRestTemplateInterceptor.class, () -> customInterceptor)
                    .run(context -> {
                        assertThat(context).hasSingleBean(TraceIdRestTemplateInterceptor.class);
                        assertThat(context.getBean(TraceIdRestTemplateInterceptor.class)).isSameAs(customInterceptor);
                    });
        }
    }

    @Nested
    @DisplayName("RestClient 인터셉터 테스트")
    class RestClientInterceptorTest {

        @Test
        @DisplayName("TraceIdRestClientInterceptor 빈이 생성된다")
        void shouldCreateRestClientInterceptorBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(TraceIdRestClientInterceptor.class);
            });
        }

        @Test
        @DisplayName("커스텀 인터셉터가 있으면 자동 생성하지 않는다")
        void shouldNotCreateRestClientInterceptorWhenCustomBeanExists() {
            TraceIdRestClientInterceptor customInterceptor = new TraceIdRestClientInterceptor();

            contextRunner
                    .withBean(TraceIdRestClientInterceptor.class, () -> customInterceptor)
                    .run(context -> {
                        assertThat(context).hasSingleBean(TraceIdRestClientInterceptor.class);
                        assertThat(context.getBean(TraceIdRestClientInterceptor.class)).isSameAs(customInterceptor);
                    });
        }
    }

    @Nested
    @DisplayName("통합 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("모든 클라이언트 인터셉터가 함께 생성된다")
        void shouldCreateAllClientInterceptors() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(TraceIdRestTemplateInterceptor.class);
                assertThat(context).hasSingleBean(TraceIdRestClientInterceptor.class);
            });
        }

        @Test
        @DisplayName("Core 설정과 함께 동작한다")
        void shouldWorkWithCoreConfiguration() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(ObservabilityProperties.class);
                assertThat(context).hasSingleBean(TraceIdRestTemplateInterceptor.class);
            });
        }
    }
}
