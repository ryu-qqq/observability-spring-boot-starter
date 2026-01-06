package com.ryuqq.observability.core.masking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaskingProperties 테스트")
class MaskingPropertiesTest {

    private MaskingProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MaskingProperties();
    }

    @Nested
    @DisplayName("기본값 테스트")
    class DefaultValuesTest {

        @Test
        @DisplayName("기본적으로 활성화되어 있다")
        void shouldBeEnabledByDefault() {
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("기본 패턴 목록은 비어있다")
        void shouldHaveEmptyPatternsByDefault() {
            assertThat(properties.getPatterns()).isEmpty();
        }

        @Test
        @DisplayName("기본 maskFields가 설정되어 있다")
        void shouldHaveDefaultMaskFields() {
            List<String> fields = properties.getMaskFields();
            assertThat(fields).isNotEmpty();
            assertThat(fields).contains("password", "secret", "apiKey", "token");
        }
    }

    @Nested
    @DisplayName("enabled 속성 테스트")
    class EnabledPropertyTest {

        @Test
        @DisplayName("enabled를 설정할 수 있다")
        void shouldSetEnabled() {
            properties.setEnabled(false);
            assertThat(properties.isEnabled()).isFalse();

            properties.setEnabled(true);
            assertThat(properties.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("patterns 속성 테스트")
    class PatternsPropertyTest {

        @Test
        @DisplayName("패턴 목록을 설정할 수 있다")
        void shouldSetPatterns() {
            MaskingProperties.MaskingPattern pattern1 = new MaskingProperties.MaskingPattern(
                    "test1", "pattern1", "replacement1"
            );
            MaskingProperties.MaskingPattern pattern2 = new MaskingProperties.MaskingPattern(
                    "test2", "pattern2", "replacement2"
            );

            properties.setPatterns(List.of(pattern1, pattern2));

            assertThat(properties.getPatterns()).hasSize(2);
            assertThat(properties.getPatterns().get(0).getName()).isEqualTo("test1");
            assertThat(properties.getPatterns().get(1).getName()).isEqualTo("test2");
        }
    }

    @Nested
    @DisplayName("maskFields 속성 테스트")
    class MaskFieldsPropertyTest {

        @Test
        @DisplayName("maskFields를 설정할 수 있다")
        void shouldSetMaskFields() {
            properties.setMaskFields(List.of("customField1", "customField2"));

            assertThat(properties.getMaskFields()).hasSize(2);
            assertThat(properties.getMaskFields()).contains("customField1", "customField2");
        }
    }

    @Nested
    @DisplayName("MaskingPattern 내부 클래스 테스트")
    class MaskingPatternTest {

        @Test
        @DisplayName("기본 생성자로 생성할 수 있다")
        void shouldCreateWithDefaultConstructor() {
            MaskingProperties.MaskingPattern pattern = new MaskingProperties.MaskingPattern();
            assertThat(pattern.getName()).isNull();
            assertThat(pattern.getPattern()).isNull();
            assertThat(pattern.getReplacement()).isNull();
        }

        @Test
        @DisplayName("모든 인자를 받는 생성자로 생성할 수 있다")
        void shouldCreateWithAllArgsConstructor() {
            MaskingProperties.MaskingPattern pattern = new MaskingProperties.MaskingPattern(
                    "testName", "testPattern", "testReplacement"
            );

            assertThat(pattern.getName()).isEqualTo("testName");
            assertThat(pattern.getPattern()).isEqualTo("testPattern");
            assertThat(pattern.getReplacement()).isEqualTo("testReplacement");
        }

        @Test
        @DisplayName("setter로 값을 설정할 수 있다")
        void shouldSetValuesWithSetters() {
            MaskingProperties.MaskingPattern pattern = new MaskingProperties.MaskingPattern();

            pattern.setName("name");
            pattern.setPattern("pattern");
            pattern.setReplacement("replacement");

            assertThat(pattern.getName()).isEqualTo("name");
            assertThat(pattern.getPattern()).isEqualTo("pattern");
            assertThat(pattern.getReplacement()).isEqualTo("replacement");
        }
    }
}
