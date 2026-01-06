package com.ryuqq.observability.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HttpLoggingProperties 테스트")
class HttpLoggingPropertiesTest {

    @Nested
    @DisplayName("기본값 테스트")
    class DefaultValuesTest {

        @Test
        @DisplayName("enabled 기본값은 true이다")
        void shouldHaveEnabledDefaultTrue() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("logRequestBody 기본값은 false이다")
        void shouldHaveLogRequestBodyDefaultFalse() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            assertThat(properties.isLogRequestBody()).isFalse();
        }

        @Test
        @DisplayName("logResponseBody 기본값은 false이다")
        void shouldHaveLogResponseBodyDefaultFalse() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            assertThat(properties.isLogResponseBody()).isFalse();
        }

        @Test
        @DisplayName("maxBodyLength 기본값은 1000이다")
        void shouldHaveDefaultMaxBodyLength() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            assertThat(properties.getMaxBodyLength()).isEqualTo(1000);
        }

        @Test
        @DisplayName("excludePaths에 기본 제외 경로가 포함된다")
        void shouldHaveDefaultExcludePaths() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            List<String> paths = properties.getExcludePaths();

            assertThat(paths).contains(
                    "/actuator/**",
                    "/health",
                    "/health/**",
                    "/favicon.ico"
            );
        }

        @Test
        @DisplayName("excludeHeaders에 기본 제외 헤더가 포함된다")
        void shouldHaveDefaultExcludeHeaders() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            List<String> headers = properties.getExcludeHeaders();

            assertThat(headers).contains(
                    "Authorization",
                    "Cookie",
                    "Set-Cookie"
            );
        }

        @Test
        @DisplayName("slowRequestThresholdMs 기본값은 3000이다")
        void shouldHaveDefaultSlowRequestThreshold() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            assertThat(properties.getSlowRequestThresholdMs()).isEqualTo(3000);
        }

        @Test
        @DisplayName("pathPatterns 기본값은 빈 리스트이다")
        void shouldHaveEmptyPathPatterns() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            assertThat(properties.getPathPatterns()).isEmpty();
        }
    }

    @Nested
    @DisplayName("setter 테스트")
    class SetterTest {

        @Test
        @DisplayName("enabled를 설정할 수 있다")
        void shouldSetEnabled() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            properties.setEnabled(false);
            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("logRequestBody를 설정할 수 있다")
        void shouldSetLogRequestBody() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            properties.setLogRequestBody(true);
            assertThat(properties.isLogRequestBody()).isTrue();
        }

        @Test
        @DisplayName("logResponseBody를 설정할 수 있다")
        void shouldSetLogResponseBody() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            properties.setLogResponseBody(true);
            assertThat(properties.isLogResponseBody()).isTrue();
        }

        @Test
        @DisplayName("maxBodyLength를 설정할 수 있다")
        void shouldSetMaxBodyLength() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            properties.setMaxBodyLength(5000);
            assertThat(properties.getMaxBodyLength()).isEqualTo(5000);
        }

        @Test
        @DisplayName("excludePaths를 설정할 수 있다")
        void shouldSetExcludePaths() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            properties.setExcludePaths(List.of("/custom/**"));
            assertThat(properties.getExcludePaths()).containsExactly("/custom/**");
        }

        @Test
        @DisplayName("excludeHeaders를 설정할 수 있다")
        void shouldSetExcludeHeaders() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            properties.setExcludeHeaders(List.of("Custom-Header"));
            assertThat(properties.getExcludeHeaders()).containsExactly("Custom-Header");
        }

        @Test
        @DisplayName("slowRequestThresholdMs를 설정할 수 있다")
        void shouldSetSlowRequestThreshold() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            properties.setSlowRequestThresholdMs(5000);
            assertThat(properties.getSlowRequestThresholdMs()).isEqualTo(5000);
        }

        @Test
        @DisplayName("pathPatterns를 설정할 수 있다")
        void shouldSetPathPatterns() {
            HttpLoggingProperties properties = new HttpLoggingProperties();
            HttpLoggingProperties.PathPattern pattern = new HttpLoggingProperties.PathPattern("/api/users/\\d+", "/api/users/{id}");
            properties.setPathPatterns(List.of(pattern));
            assertThat(properties.getPathPatterns()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("PathPattern 내부 클래스 테스트")
    class PathPatternTest {

        @Test
        @DisplayName("기본 생성자로 생성할 수 있다")
        void shouldCreateWithDefaultConstructor() {
            HttpLoggingProperties.PathPattern pattern = new HttpLoggingProperties.PathPattern();
            assertThat(pattern.getPattern()).isNull();
            assertThat(pattern.getReplacement()).isNull();
        }

        @Test
        @DisplayName("파라미터 생성자로 생성할 수 있다")
        void shouldCreateWithParameters() {
            HttpLoggingProperties.PathPattern pattern = new HttpLoggingProperties.PathPattern("/api/\\d+", "/api/{id}");
            assertThat(pattern.getPattern()).isEqualTo("/api/\\d+");
            assertThat(pattern.getReplacement()).isEqualTo("/api/{id}");
        }

        @Test
        @DisplayName("pattern을 설정할 수 있다")
        void shouldSetPattern() {
            HttpLoggingProperties.PathPattern pattern = new HttpLoggingProperties.PathPattern();
            pattern.setPattern("/custom/\\d+");
            assertThat(pattern.getPattern()).isEqualTo("/custom/\\d+");
        }

        @Test
        @DisplayName("replacement를 설정할 수 있다")
        void shouldSetReplacement() {
            HttpLoggingProperties.PathPattern pattern = new HttpLoggingProperties.PathPattern();
            pattern.setReplacement("/custom/{id}");
            assertThat(pattern.getReplacement()).isEqualTo("/custom/{id}");
        }
    }
}
