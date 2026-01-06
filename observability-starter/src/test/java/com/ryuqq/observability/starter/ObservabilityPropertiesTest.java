package com.ryuqq.observability.starter;

import com.ryuqq.observability.core.masking.MaskingProperties;
import com.ryuqq.observability.logging.config.BusinessLoggingProperties;
import com.ryuqq.observability.message.config.MessageLoggingProperties;
import com.ryuqq.observability.web.config.HttpLoggingProperties;
import com.ryuqq.observability.web.config.TraceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservabilityProperties 테스트")
class ObservabilityPropertiesTest {

    private ObservabilityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ObservabilityProperties();
    }

    @Nested
    @DisplayName("기본값 테스트")
    class DefaultValueTest {

        @Test
        @DisplayName("serviceName 기본값은 'unknown'이다")
        void shouldHaveServiceNameDefaultUnknown() {
            assertThat(properties.getServiceName()).isEqualTo("unknown");
        }

        @Test
        @DisplayName("trace는 기본 인스턴스가 생성된다")
        void shouldHaveDefaultTraceProperties() {
            assertThat(properties.getTrace()).isNotNull();
            assertThat(properties.getTrace()).isInstanceOf(TraceProperties.class);
        }

        @Test
        @DisplayName("http는 기본 인스턴스가 생성된다")
        void shouldHaveDefaultHttpProperties() {
            assertThat(properties.getHttp()).isNotNull();
            assertThat(properties.getHttp()).isInstanceOf(HttpLoggingProperties.class);
        }

        @Test
        @DisplayName("message는 기본 인스턴스가 생성된다")
        void shouldHaveDefaultMessageProperties() {
            assertThat(properties.getMessage()).isNotNull();
            assertThat(properties.getMessage()).isInstanceOf(MessageLoggingProperties.class);
        }

        @Test
        @DisplayName("logging은 기본 인스턴스가 생성된다")
        void shouldHaveDefaultLoggingProperties() {
            assertThat(properties.getLogging()).isNotNull();
            assertThat(properties.getLogging()).isInstanceOf(BusinessLoggingProperties.class);
        }

        @Test
        @DisplayName("masking은 기본 인스턴스가 생성된다")
        void shouldHaveDefaultMaskingProperties() {
            assertThat(properties.getMasking()).isNotNull();
            assertThat(properties.getMasking()).isInstanceOf(MaskingProperties.class);
        }
    }

    @Nested
    @DisplayName("setter 테스트")
    class SetterTest {

        @Test
        @DisplayName("serviceName을 설정한다")
        void shouldSetServiceName() {
            properties.setServiceName("my-service");
            assertThat(properties.getServiceName()).isEqualTo("my-service");
        }

        @Test
        @DisplayName("trace를 설정한다")
        void shouldSetTrace() {
            TraceProperties trace = new TraceProperties();
            trace.setEnabled(false);

            properties.setTrace(trace);

            assertThat(properties.getTrace()).isSameAs(trace);
            assertThat(properties.getTrace().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("http를 설정한다")
        void shouldSetHttp() {
            HttpLoggingProperties http = new HttpLoggingProperties();
            http.setEnabled(false);

            properties.setHttp(http);

            assertThat(properties.getHttp()).isSameAs(http);
            assertThat(properties.getHttp().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("message를 설정한다")
        void shouldSetMessage() {
            MessageLoggingProperties message = new MessageLoggingProperties();
            message.setEnabled(false);

            properties.setMessage(message);

            assertThat(properties.getMessage()).isSameAs(message);
            assertThat(properties.getMessage().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("logging을 설정한다")
        void shouldSetLogging() {
            BusinessLoggingProperties logging = new BusinessLoggingProperties();
            logging.setEnabled(false);

            properties.setLogging(logging);

            assertThat(properties.getLogging()).isSameAs(logging);
            assertThat(properties.getLogging().isEnabled()).isFalse();
        }

        @Test
        @DisplayName("masking을 설정한다")
        void shouldSetMasking() {
            MaskingProperties masking = new MaskingProperties();
            masking.setEnabled(false);

            properties.setMasking(masking);

            assertThat(properties.getMasking()).isSameAs(masking);
            assertThat(properties.getMasking().isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("통합 설정 테스트")
    class IntegratedConfigurationTest {

        @Test
        @DisplayName("모든 중첩 속성을 독립적으로 설정할 수 있다")
        void shouldSetAllNestedPropertiesIndependently() {
            // Given
            TraceProperties trace = new TraceProperties();
            HttpLoggingProperties http = new HttpLoggingProperties();
            MessageLoggingProperties message = new MessageLoggingProperties();
            BusinessLoggingProperties logging = new BusinessLoggingProperties();
            MaskingProperties masking = new MaskingProperties();

            // When
            properties.setServiceName("test-service");
            properties.setTrace(trace);
            properties.setHttp(http);
            properties.setMessage(message);
            properties.setLogging(logging);
            properties.setMasking(masking);

            // Then
            assertThat(properties.getServiceName()).isEqualTo("test-service");
            assertThat(properties.getTrace()).isSameAs(trace);
            assertThat(properties.getHttp()).isSameAs(http);
            assertThat(properties.getMessage()).isSameAs(message);
            assertThat(properties.getLogging()).isSameAs(logging);
            assertThat(properties.getMasking()).isSameAs(masking);
        }

        @Test
        @DisplayName("중첩 속성 변경이 원래 객체에 반영된다")
        void shouldReflectNestedPropertyChanges() {
            // When
            properties.getTrace().setEnabled(false);
            properties.getHttp().setLogRequestBody(true);
            properties.getMessage().setLogPayload(true);
            properties.getLogging().setLogArguments(true);
            properties.getMasking().setEnabled(false);

            // Then
            assertThat(properties.getTrace().isEnabled()).isFalse();
            assertThat(properties.getHttp().isLogRequestBody()).isTrue();
            assertThat(properties.getMessage().isLogPayload()).isTrue();
            assertThat(properties.getLogging().isLogArguments()).isTrue();
            assertThat(properties.getMasking().isEnabled()).isFalse();
        }
    }
}
