package com.ryuqq.observability.logging.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessLoggingProperties 테스트")
class BusinessLoggingPropertiesTest {

    private BusinessLoggingProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BusinessLoggingProperties();
    }

    @Nested
    @DisplayName("기본값 테스트")
    class DefaultValueTest {

        @Test
        @DisplayName("enabled 기본값은 true이다")
        void shouldHaveEnabledDefaultTrue() {
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("logArguments 기본값은 false이다")
        void shouldHaveLogArgumentsDefaultFalse() {
            assertThat(properties.isLogArguments()).isFalse();
        }

        @Test
        @DisplayName("logResult 기본값은 false이다")
        void shouldHaveLogResultDefaultFalse() {
            assertThat(properties.isLogResult()).isFalse();
        }

        @Test
        @DisplayName("logExecutionTime 기본값은 true이다")
        void shouldHaveLogExecutionTimeDefaultTrue() {
            assertThat(properties.isLogExecutionTime()).isTrue();
        }

        @Test
        @DisplayName("slowExecutionThreshold 기본값은 1000이다")
        void shouldHaveSlowExecutionThresholdDefault1000() {
            assertThat(properties.getSlowExecutionThreshold()).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("setter 테스트")
    class SetterTest {

        @Test
        @DisplayName("enabled를 설정한다")
        void shouldSetEnabled() {
            properties.setEnabled(false);
            assertThat(properties.isEnabled()).isFalse();

            properties.setEnabled(true);
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("logArguments를 설정한다")
        void shouldSetLogArguments() {
            properties.setLogArguments(true);
            assertThat(properties.isLogArguments()).isTrue();

            properties.setLogArguments(false);
            assertThat(properties.isLogArguments()).isFalse();
        }

        @Test
        @DisplayName("logResult를 설정한다")
        void shouldSetLogResult() {
            properties.setLogResult(true);
            assertThat(properties.isLogResult()).isTrue();

            properties.setLogResult(false);
            assertThat(properties.isLogResult()).isFalse();
        }

        @Test
        @DisplayName("logExecutionTime을 설정한다")
        void shouldSetLogExecutionTime() {
            properties.setLogExecutionTime(false);
            assertThat(properties.isLogExecutionTime()).isFalse();

            properties.setLogExecutionTime(true);
            assertThat(properties.isLogExecutionTime()).isTrue();
        }

        @Test
        @DisplayName("slowExecutionThreshold를 설정한다")
        void shouldSetSlowExecutionThreshold() {
            properties.setSlowExecutionThreshold(5000);
            assertThat(properties.getSlowExecutionThreshold()).isEqualTo(5000);

            properties.setSlowExecutionThreshold(500);
            assertThat(properties.getSlowExecutionThreshold()).isEqualTo(500);
        }
    }
}
