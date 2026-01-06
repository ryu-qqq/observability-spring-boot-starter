package com.ryuqq.observability.message.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageLoggingProperties 테스트")
class MessageLoggingPropertiesTest {

    private MessageLoggingProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MessageLoggingProperties();
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
        @DisplayName("logPayload 기본값은 false이다")
        void shouldHaveLogPayloadDefaultFalse() {
            assertThat(properties.isLogPayload()).isFalse();
        }

        @Test
        @DisplayName("maxPayloadLength 기본값은 500이다")
        void shouldHaveMaxPayloadLengthDefault500() {
            assertThat(properties.getMaxPayloadLength()).isEqualTo(500);
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
        @DisplayName("logPayload를 설정한다")
        void shouldSetLogPayload() {
            properties.setLogPayload(true);
            assertThat(properties.isLogPayload()).isTrue();

            properties.setLogPayload(false);
            assertThat(properties.isLogPayload()).isFalse();
        }

        @Test
        @DisplayName("maxPayloadLength를 설정한다")
        void shouldSetMaxPayloadLength() {
            properties.setMaxPayloadLength(1000);
            assertThat(properties.getMaxPayloadLength()).isEqualTo(1000);

            properties.setMaxPayloadLength(100);
            assertThat(properties.getMaxPayloadLength()).isEqualTo(100);
        }
    }
}
