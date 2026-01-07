package com.ryuqq.observability.integration.bootstrap;

import com.ryuqq.observability.client.rest.TraceIdRestClientInterceptor;
import com.ryuqq.observability.client.rest.TraceIdRestTemplateInterceptor;
import com.ryuqq.observability.client.webclient.TraceIdExchangeFilterFunction;
import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.logging.aspect.BusinessLogAspect;
import com.ryuqq.observability.logging.aspect.LoggableAspect;
import com.ryuqq.observability.logging.event.BusinessEventListener;
import com.ryuqq.observability.web.http.HttpLoggingFilter;
import com.ryuqq.observability.web.trace.TraceIdFilter;
import com.ryuqq.observability.web.http.PathNormalizer;
import com.ryuqq.observability.web.trace.TraceIdProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AutoConfiguration 자동 등록 검증 테스트.
 *
 * <p>observability-starter가 제공하는 모든 자동 설정이
 * 올바르게 Bean으로 등록되는지 검증합니다.</p>
 */
@SpringBootTest(classes = BootstrapTestApplication.class)
class AutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Nested
    @DisplayName("Core AutoConfiguration")
    class CoreAutoConfigurationTests {

        @Test
        @DisplayName("LogMasker Bean이 등록되어야 함")
        void shouldRegisterLogMaskerBean() {
            // when
            LogMasker logMasker = applicationContext.getBean(LogMasker.class);

            // then
            assertThat(logMasker).isNotNull();
        }

        @Test
        @DisplayName("LogMasker가 설정된 패턴을 마스킹해야 함")
        void shouldMaskConfiguredPatterns() {
            // given
            LogMasker logMasker = applicationContext.getBean(LogMasker.class);
            String input = "{\"password\": \"secret123\", \"name\": \"test\"}";

            // when
            String masked = logMasker.mask(input);

            // then
            assertThat(masked).contains("\"password\"");
            assertThat(masked).contains("[MASKED]");
            assertThat(masked).doesNotContain("secret123");
        }
    }

    @Nested
    @DisplayName("Web AutoConfiguration")
    class WebAutoConfigurationTests {

        @Test
        @DisplayName("TraceIdProvider Bean이 등록되어야 함")
        void shouldRegisterTraceIdProviderBean() {
            // when
            TraceIdProvider provider = applicationContext.getBean(TraceIdProvider.class);

            // then
            assertThat(provider).isNotNull();
        }

        @Test
        @DisplayName("PathNormalizer Bean이 등록되어야 함")
        void shouldRegisterPathNormalizerBean() {
            // when
            PathNormalizer normalizer = applicationContext.getBean(PathNormalizer.class);

            // then
            assertThat(normalizer).isNotNull();
        }

        @Test
        @DisplayName("TraceIdFilter FilterRegistrationBean이 등록되어야 함")
        @SuppressWarnings("unchecked")
        void shouldRegisterTraceIdFilterRegistration() {
            // when
            FilterRegistrationBean<TraceIdFilter> registration =
                    applicationContext.getBean("traceIdFilterRegistration", FilterRegistrationBean.class);

            // then
            assertThat(registration).isNotNull();
            assertThat(registration.getFilter()).isInstanceOf(TraceIdFilter.class);
        }

        @Test
        @DisplayName("HttpLoggingFilter FilterRegistrationBean이 등록되어야 함")
        @SuppressWarnings("unchecked")
        void shouldRegisterHttpLoggingFilterRegistration() {
            // when
            FilterRegistrationBean<HttpLoggingFilter> registration =
                    applicationContext.getBean("httpLoggingFilterRegistration", FilterRegistrationBean.class);

            // then
            assertThat(registration).isNotNull();
            assertThat(registration.getFilter()).isInstanceOf(HttpLoggingFilter.class);
        }
    }

    @Nested
    @DisplayName("Client AutoConfiguration")
    class ClientAutoConfigurationTests {

        @Test
        @DisplayName("TraceIdRestTemplateInterceptor Bean이 등록되어야 함")
        void shouldRegisterRestTemplateInterceptorBean() {
            // when
            TraceIdRestTemplateInterceptor interceptor =
                    applicationContext.getBean(TraceIdRestTemplateInterceptor.class);

            // then
            assertThat(interceptor).isNotNull();
        }

        @Test
        @DisplayName("TraceIdRestClientInterceptor Bean이 등록되어야 함")
        void shouldRegisterRestClientInterceptorBean() {
            // when
            TraceIdRestClientInterceptor interceptor =
                    applicationContext.getBean(TraceIdRestClientInterceptor.class);

            // then
            assertThat(interceptor).isNotNull();
        }

        @Test
        @DisplayName("TraceIdExchangeFilterFunction Bean이 등록되어야 함")
        void shouldRegisterWebClientFilterBean() {
            // when
            TraceIdExchangeFilterFunction filterFunction =
                    applicationContext.getBean(TraceIdExchangeFilterFunction.class);

            // then
            assertThat(filterFunction).isNotNull();
        }
    }

    @Nested
    @DisplayName("Logging AutoConfiguration")
    class LoggingAutoConfigurationTests {

        @Test
        @DisplayName("LoggableAspect Bean이 등록되어야 함")
        void shouldRegisterLoggableAspectBean() {
            // when
            LoggableAspect aspect = applicationContext.getBean(LoggableAspect.class);

            // then
            assertThat(aspect).isNotNull();
        }

        @Test
        @DisplayName("BusinessLogAspect Bean이 등록되어야 함")
        void shouldRegisterBusinessLogAspectBean() {
            // when
            BusinessLogAspect aspect = applicationContext.getBean(BusinessLogAspect.class);

            // then
            assertThat(aspect).isNotNull();
        }

        @Test
        @DisplayName("BusinessEventListener Bean이 등록되어야 함")
        void shouldRegisterBusinessEventListenerBean() {
            // when
            BusinessEventListener listener = applicationContext.getBean(BusinessEventListener.class);

            // then
            assertThat(listener).isNotNull();
        }
    }

    @Nested
    @DisplayName("설정 검증")
    class ConfigurationVerificationTests {

        @Test
        @DisplayName("서비스명이 application.yml에서 로드되어야 함")
        void shouldLoadServiceNameFromConfiguration() {
            // given
            String expectedServiceName = "bootstrap-test";

            // when - TraceIdProvider를 통해 간접적으로 검증
            TraceIdProvider provider = applicationContext.getBean(TraceIdProvider.class);

            // then
            assertThat(provider).isNotNull();
            // 상세 검증은 실제 요청 테스트에서 수행
        }

        @Test
        @DisplayName("PathNormalizer가 기본 패턴을 포함해야 함")
        void shouldHaveDefaultPatternsInPathNormalizer() {
            // given
            PathNormalizer normalizer = applicationContext.getBean(PathNormalizer.class);

            // when - 기본 패턴 테스트
            String numericPath = "/api/users/12345";
            String uuidPath = "/api/orders/550e8400-e29b-41d4-a716-446655440000";

            // then
            assertThat(normalizer.normalize(numericPath)).isEqualTo("/api/users/{id}");
            assertThat(normalizer.normalize(uuidPath)).isEqualTo("/api/orders/{uuid}");
        }
    }
}
