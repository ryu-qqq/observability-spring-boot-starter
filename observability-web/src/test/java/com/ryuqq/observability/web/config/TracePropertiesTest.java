package com.ryuqq.observability.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TraceProperties 테스트")
class TracePropertiesTest {

    @Nested
    @DisplayName("기본값 테스트")
    class DefaultValuesTest {

        @Test
        @DisplayName("enabled 기본값은 true이다")
        void shouldHaveEnabledDefaultTrue() {
            TraceProperties properties = new TraceProperties();
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("headerNames 기본값에 표준 헤더가 포함된다")
        void shouldHaveDefaultHeaderNames() {
            TraceProperties properties = new TraceProperties();
            List<String> headers = properties.getHeaderNames();

            assertThat(headers).containsExactly(
                    "X-Trace-Id",
                    "X-Request-Id",
                    "traceparent",
                    "X-Amzn-Trace-Id"
            );
        }

        @Test
        @DisplayName("includeInResponse 기본값은 true이다")
        void shouldHaveIncludeInResponseDefaultTrue() {
            TraceProperties properties = new TraceProperties();
            assertThat(properties.isIncludeInResponse()).isTrue();
        }

        @Test
        @DisplayName("generateIfMissing 기본값은 true이다")
        void shouldHaveGenerateIfMissingDefaultTrue() {
            TraceProperties properties = new TraceProperties();
            assertThat(properties.isGenerateIfMissing()).isTrue();
        }

        @Test
        @DisplayName("responseHeaderName 기본값은 X-Trace-Id이다")
        void shouldHaveDefaultResponseHeaderName() {
            TraceProperties properties = new TraceProperties();
            assertThat(properties.getResponseHeaderName()).isEqualTo("X-Trace-Id");
        }
    }

    @Nested
    @DisplayName("setter 테스트")
    class SetterTest {

        @Test
        @DisplayName("enabled를 설정할 수 있다")
        void shouldSetEnabled() {
            TraceProperties properties = new TraceProperties();
            properties.setEnabled(false);
            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("headerNames를 설정할 수 있다")
        void shouldSetHeaderNames() {
            TraceProperties properties = new TraceProperties();
            List<String> customHeaders = List.of("Custom-Trace-Id");
            properties.setHeaderNames(customHeaders);
            assertThat(properties.getHeaderNames()).containsExactly("Custom-Trace-Id");
        }

        @Test
        @DisplayName("includeInResponse를 설정할 수 있다")
        void shouldSetIncludeInResponse() {
            TraceProperties properties = new TraceProperties();
            properties.setIncludeInResponse(false);
            assertThat(properties.isIncludeInResponse()).isFalse();
        }

        @Test
        @DisplayName("generateIfMissing을 설정할 수 있다")
        void shouldSetGenerateIfMissing() {
            TraceProperties properties = new TraceProperties();
            properties.setGenerateIfMissing(false);
            assertThat(properties.isGenerateIfMissing()).isFalse();
        }

        @Test
        @DisplayName("responseHeaderName을 설정할 수 있다")
        void shouldSetResponseHeaderName() {
            TraceProperties properties = new TraceProperties();
            properties.setResponseHeaderName("X-Custom-Trace-Id");
            assertThat(properties.getResponseHeaderName()).isEqualTo("X-Custom-Trace-Id");
        }
    }
}
