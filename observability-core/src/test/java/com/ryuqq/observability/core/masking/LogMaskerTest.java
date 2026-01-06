package com.ryuqq.observability.core.masking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogMasker 테스트")
class LogMaskerTest {

    private LogMasker masker;

    @BeforeEach
    void setUp() {
        masker = new LogMasker();
    }

    @Nested
    @DisplayName("기본 생성자 테스트")
    class DefaultConstructorTest {

        @Test
        @DisplayName("기본 생성자로 생성 시 마스킹이 활성화된다")
        void shouldBeEnabledByDefault() {
            assertThat(masker.isEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("마스킹 활성화/비활성화 테스트")
    class EnabledDisabledTest {

        @Test
        @DisplayName("마스킹이 비활성화되면 원본 문자열이 반환된다")
        void shouldReturnOriginalWhenDisabled() {
            MaskingProperties props = new MaskingProperties();
            props.setEnabled(false);
            LogMasker disabledMasker = new LogMasker(props);

            String input = "card=1234-5678-9012-3456";
            assertThat(disabledMasker.mask(input)).isEqualTo(input);
            assertThat(disabledMasker.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("null 입력 시 null이 반환된다")
        void shouldReturnNullForNullInput() {
            assertThat(masker.mask(null)).isNull();
        }

        @Test
        @DisplayName("빈 문자열 입력 시 빈 문자열이 반환된다")
        void shouldReturnEmptyForEmptyInput() {
            assertThat(masker.mask("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("이메일 마스킹 테스트")
    class EmailMaskingTest {

        @Test
        @DisplayName("이메일 주소가 마스킹된다")
        void shouldMaskEmail() {
            String input = "user email is example@domain.com";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("user email is ex***@domain.com");
        }

        @Test
        @DisplayName("여러 이메일 주소가 모두 마스킹된다")
        void shouldMaskMultipleEmails() {
            String input = "from: sender@test.com to: receiver@example.org";
            String result = masker.mask(input);
            assertThat(result).contains("se***@test.com");
            assertThat(result).contains("re***@example.org");
        }
    }

    @Nested
    @DisplayName("신용카드 마스킹 테스트")
    class CreditCardMaskingTest {

        @Test
        @DisplayName("하이픈 구분 카드번호가 마스킹된다")
        void shouldMaskCreditCardWithDash() {
            String input = "card=1234-5678-9012-3456";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("card=****-****-****-3456");
        }

        @Test
        @DisplayName("연속 카드번호가 마스킹된다")
        void shouldMaskCreditCardPlain() {
            String input = "card=1234567890123456";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("card=************3456");
        }
    }

    @Nested
    @DisplayName("전화번호 마스킹 테스트")
    class PhoneMaskingTest {

        @Test
        @DisplayName("한국 휴대폰 번호가 마스킹된다")
        void shouldMaskKoreanPhone() {
            String input = "phone: 010-1234-5678";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("phone: 010-****-5678");
        }

        @Test
        @DisplayName("하이픈 없는 전화번호도 마스킹된다")
        void shouldMaskPhoneWithoutDash() {
            String input = "phone: 01012345678";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("phone: 010-****-5678");
        }
    }

    @Nested
    @DisplayName("주민등록번호 마스킹 테스트")
    class SsnMaskingTest {

        @Test
        @DisplayName("주민등록번호가 마스킹된다")
        void shouldMaskKoreanSsn() {
            String input = "ssn: 900101-1234567";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("ssn: 900101-*******");
        }

        @Test
        @DisplayName("하이픈 없는 주민등록번호도 마스킹된다")
        void shouldMaskSsnWithoutDash() {
            // 전화번호 패턴(010)과 충돌하지 않는 주민번호 사용
            String input = "ssn: 9006121234567";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("ssn: 900612-*******");
        }
    }

    @Nested
    @DisplayName("Bearer 토큰 마스킹 테스트")
    class BearerTokenMaskingTest {

        @Test
        @DisplayName("Bearer 토큰이 마스킹된다")
        void shouldMaskBearerToken() {
            String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("Authorization: Bearer [MASKED]");
        }
    }

    @Nested
    @DisplayName("JSON 필드 마스킹 테스트")
    class JsonFieldMaskingTest {

        @Test
        @DisplayName("JSON password 필드가 마스킹된다")
        void shouldMaskPasswordInJson() {
            String input = "{\"password\":\"secret123\"}";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("{\"password\":\"[MASKED]\"}");
        }

        @Test
        @DisplayName("JSON apiKey 필드가 마스킹된다")
        void shouldMaskApiKeyInJson() {
            String input = "{\"apiKey\":\"my-secret-key\"}";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("{\"apiKey\":\"[MASKED]\"}");
        }

        @Test
        @DisplayName("JSON token 필드의 JWT가 마스킹된다")
        void shouldMaskJwtInJson() {
            String input = "{\"accessToken\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U\"}";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("{\"accessToken\":\"[MASKED]\"}");
        }
    }

    @Nested
    @DisplayName("커스텀 패턴 테스트")
    class CustomPatternTest {

        @Test
        @DisplayName("커스텀 패턴이 적용된다")
        void shouldApplyCustomPattern() {
            MaskingProperties props = new MaskingProperties();
            MaskingProperties.MaskingPattern customPattern = new MaskingProperties.MaskingPattern(
                    "custom", "SECRET-\\d+", "[CUSTOM-MASKED]"
            );
            props.setPatterns(List.of(customPattern));

            LogMasker customMasker = new LogMasker(props);
            String input = "code: SECRET-12345";
            String result = customMasker.mask(input);
            assertThat(result).isEqualTo("code: [CUSTOM-MASKED]");
        }

        @Test
        @DisplayName("addPattern으로 패턴을 추가할 수 있다")
        void shouldAddPatternDynamically() {
            masker.addPattern("CUSTOM-\\d+", "[ADDED]");
            String result = masker.mask("code: CUSTOM-999");
            assertThat(result).isEqualTo("code: [ADDED]");
        }
    }

    @Nested
    @DisplayName("필드명 기반 마스킹 테스트")
    class FieldBasedMaskingTest {

        @Test
        @DisplayName("설정된 필드명이 마스킹된다")
        void shouldMaskConfiguredFields() {
            MaskingProperties props = new MaskingProperties();
            props.setMaskFields(List.of("secretField"));

            LogMasker fieldMasker = new LogMasker(props);
            String input = "{\"secretField\":\"my-secret-value\"}";
            String result = fieldMasker.mask(input);
            assertThat(result).isEqualTo("{\"secretField\":\"[MASKED]\"}");
        }

        @Test
        @DisplayName("기본 maskFields가 적용된다")
        void shouldApplyDefaultMaskFields() {
            String input = "{\"creditCard\":\"1234567890123456\"}";
            String result = masker.mask(input);
            assertThat(result).isEqualTo("{\"creditCard\":\"[MASKED]\"}");
        }
    }

    @Nested
    @DisplayName("복합 마스킹 테스트")
    class CompositeMaskingTest {

        @Test
        @DisplayName("여러 민감정보가 한 번에 마스킹된다")
        void shouldMaskMultipleSensitiveData() {
            String input = "User email@test.com paid with card 1234-5678-9012-3456 from phone 010-1111-2222";
            String result = masker.mask(input);

            assertThat(result)
                    .contains("em***@test.com")
                    .contains("****-****-****-3456")
                    .contains("010-****-2222");
        }
    }
}
