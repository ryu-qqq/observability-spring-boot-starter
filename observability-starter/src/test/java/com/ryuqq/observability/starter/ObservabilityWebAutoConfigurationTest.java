package com.ryuqq.observability.starter;

import com.ryuqq.observability.web.http.HttpLoggingFilter;
import com.ryuqq.observability.web.http.PathNormalizer;
import com.ryuqq.observability.web.trace.TraceIdFilter;
import com.ryuqq.observability.web.trace.TraceIdProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservabilityWebAutoConfiguration 테스트")
class ObservabilityWebAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObservabilityCoreAutoConfiguration.class,
                    ObservabilityWebAutoConfiguration.class
            ));

    @Nested
    @DisplayName("TraceIdProvider 빈 생성 테스트")
    class TraceIdProviderBeanTest {

        @Test
        @DisplayName("TraceIdProvider 빈이 생성된다")
        void shouldCreateTraceIdProviderBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(TraceIdProvider.class);
            });
        }

        @Test
        @DisplayName("커스텀 TraceIdProvider가 있으면 자동 생성하지 않는다")
        void shouldNotCreateTraceIdProviderWhenCustomBeanExists() {
            TraceIdProvider customProvider = new TraceIdProvider() {
                @Override
                public String generate() {
                    return "custom-trace-id";
                }

                @Override
                public String extractFromRequest(jakarta.servlet.http.HttpServletRequest request) {
                    return "custom-trace-id";
                }
            };

            contextRunner
                    .withBean(TraceIdProvider.class, () -> customProvider)
                    .run(context -> {
                        assertThat(context).hasSingleBean(TraceIdProvider.class);
                        assertThat(context.getBean(TraceIdProvider.class)).isSameAs(customProvider);
                    });
        }
    }

    @Nested
    @DisplayName("PathNormalizer 빈 생성 테스트")
    class PathNormalizerBeanTest {

        @Test
        @DisplayName("PathNormalizer 빈이 생성된다")
        void shouldCreatePathNormalizerBean() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(PathNormalizer.class);
            });
        }

        @Test
        @DisplayName("커스텀 PathNormalizer가 있으면 자동 생성하지 않는다")
        void shouldNotCreatePathNormalizerWhenCustomBeanExists() {
            PathNormalizer customNormalizer = new PathNormalizer();

            contextRunner
                    .withBean(PathNormalizer.class, () -> customNormalizer)
                    .run(context -> {
                        assertThat(context).hasSingleBean(PathNormalizer.class);
                        assertThat(context.getBean(PathNormalizer.class)).isSameAs(customNormalizer);
                    });
        }
    }

    @Nested
    @DisplayName("TraceIdFilter 필터 등록 테스트")
    class TraceIdFilterRegistrationTest {

        @Test
        @DisplayName("TraceIdFilter가 등록된다")
        void shouldRegisterTraceIdFilter() {
            contextRunner.run(context -> {
                assertThat(context).hasBean("traceIdFilterRegistration");
                FilterRegistrationBean<?> registration =
                        context.getBean("traceIdFilterRegistration", FilterRegistrationBean.class);
                assertThat(registration.getFilter()).isInstanceOf(TraceIdFilter.class);
            });
        }

        @Test
        @DisplayName("trace.enabled=false면 TraceIdFilter가 등록되지 않는다")
        void shouldNotRegisterTraceIdFilterWhenDisabled() {
            contextRunner
                    .withPropertyValues("observability.trace.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean("traceIdFilterRegistration");
                    });
        }

        @Test
        @DisplayName("trace.enabled=true면 TraceIdFilter가 등록된다")
        void shouldRegisterTraceIdFilterWhenEnabled() {
            contextRunner
                    .withPropertyValues("observability.trace.enabled=true")
                    .run(context -> {
                        assertThat(context).hasBean("traceIdFilterRegistration");
                    });
        }
    }

    @Nested
    @DisplayName("HttpLoggingFilter 필터 등록 테스트")
    class HttpLoggingFilterRegistrationTest {

        @Test
        @DisplayName("HttpLoggingFilter가 등록된다")
        void shouldRegisterHttpLoggingFilter() {
            contextRunner.run(context -> {
                assertThat(context).hasBean("httpLoggingFilterRegistration");
                FilterRegistrationBean<?> registration =
                        context.getBean("httpLoggingFilterRegistration", FilterRegistrationBean.class);
                assertThat(registration.getFilter()).isInstanceOf(HttpLoggingFilter.class);
            });
        }

        @Test
        @DisplayName("http.enabled=false면 HttpLoggingFilter가 등록되지 않는다")
        void shouldNotRegisterHttpLoggingFilterWhenDisabled() {
            contextRunner
                    .withPropertyValues("observability.http.enabled=false")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean("httpLoggingFilterRegistration");
                    });
        }

        @Test
        @DisplayName("http.enabled=true면 HttpLoggingFilter가 등록된다")
        void shouldRegisterHttpLoggingFilterWhenEnabled() {
            contextRunner
                    .withPropertyValues("observability.http.enabled=true")
                    .run(context -> {
                        assertThat(context).hasBean("httpLoggingFilterRegistration");
                    });
        }
    }

    @Nested
    @DisplayName("프로퍼티 바인딩 테스트")
    class PropertyBindingTest {

        @Test
        @DisplayName("서비스 이름이 TraceIdFilter에 전달된다")
        void shouldPassServiceNameToTraceIdFilter() {
            contextRunner
                    .withPropertyValues("observability.service-name=web-service")
                    .run(context -> {
                        ObservabilityProperties props = context.getBean(ObservabilityProperties.class);
                        assertThat(props.getServiceName()).isEqualTo("web-service");
                    });
        }

        @Test
        @DisplayName("HTTP 로깅 설정이 적용된다")
        void shouldApplyHttpLoggingSettings() {
            contextRunner
                    .withPropertyValues(
                            "observability.http.log-request-body=true",
                            "observability.http.max-body-length=2000"
                    )
                    .run(context -> {
                        ObservabilityProperties props = context.getBean(ObservabilityProperties.class);
                        assertThat(props.getHttp().isLogRequestBody()).isTrue();
                        assertThat(props.getHttp().getMaxBodyLength()).isEqualTo(2000);
                    });
        }
    }
}
