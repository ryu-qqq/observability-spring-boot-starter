package com.ryuqq.observability.core.masking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaskingPatterns 테스트")
class MaskingPatternsTest {

    @Nested
    @DisplayName("이메일 패턴 테스트")
    class EmailPatternTest {

        @Test
        @DisplayName("이메일 패턴이 이메일을 매칭한다")
        void shouldMatchEmail() {
            String email = "example@domain.com";
            Matcher matcher = MaskingPatterns.EMAIL.matcher(email);
            assertThat(matcher.find()).isTrue();
            String result = matcher.replaceAll(MaskingPatterns.EMAIL_REPLACEMENT);
            assertThat(result).isEqualTo("ex***@domain.com");
        }

        @Test
        @DisplayName("짧은 이메일도 매칭한다")
        void shouldMatchShortEmail() {
            String email = "ab@test.org";
            Matcher matcher = MaskingPatterns.EMAIL.matcher(email);
            assertThat(matcher.find()).isTrue();
        }

        @Test
        @DisplayName("잘못된 형식은 매칭하지 않는다")
        void shouldNotMatchInvalidEmail() {
            String invalid = "not-an-email";
            Matcher matcher = MaskingPatterns.EMAIL.matcher(invalid);
            assertThat(matcher.find()).isFalse();
        }
    }

    @Nested
    @DisplayName("신용카드 패턴 테스트")
    class CreditCardPatternTest {

        @Test
        @DisplayName("하이픈 구분 카드번호를 매칭한다")
        void shouldMatchCardWithDash() {
            String card = "1234-5678-9012-3456";
            Matcher matcher = MaskingPatterns.CREDIT_CARD_DASH.matcher(card);
            assertThat(matcher.find()).isTrue();
            String result = matcher.replaceAll(MaskingPatterns.CREDIT_CARD_DASH_REPLACEMENT);
            assertThat(result).isEqualTo("****-****-****-3456");
        }

        @Test
        @DisplayName("연속 카드번호를 매칭한다")
        void shouldMatchCardPlain() {
            String card = "1234567890123456";
            Matcher matcher = MaskingPatterns.CREDIT_CARD_PLAIN.matcher(card);
            assertThat(matcher.find()).isTrue();
            String result = matcher.replaceAll(MaskingPatterns.CREDIT_CARD_PLAIN_REPLACEMENT);
            assertThat(result).isEqualTo("************3456");
        }

        @Test
        @DisplayName("잘못된 카드번호는 매칭하지 않는다")
        void shouldNotMatchInvalidCard() {
            String invalid = "1234-5678";
            Matcher matcher = MaskingPatterns.CREDIT_CARD_DASH.matcher(invalid);
            assertThat(matcher.find()).isFalse();
        }
    }

    @Nested
    @DisplayName("전화번호 패턴 테스트")
    class PhonePatternTest {

        @Test
        @DisplayName("한국 휴대폰 번호를 매칭한다")
        void shouldMatchKoreanPhone() {
            String phone = "010-1234-5678";
            Matcher matcher = MaskingPatterns.PHONE_KR.matcher(phone);
            assertThat(matcher.find()).isTrue();
            String result = matcher.replaceAll(MaskingPatterns.PHONE_KR_REPLACEMENT);
            assertThat(result).isEqualTo("010-****-5678");
        }

        @Test
        @DisplayName("공백으로 구분된 전화번호도 매칭한다")
        void shouldMatchPhoneWithSpace() {
            String phone = "010 1234 5678";
            Matcher matcher = MaskingPatterns.PHONE_KR.matcher(phone);
            assertThat(matcher.find()).isTrue();
        }

        @Test
        @DisplayName("011 번호도 매칭한다")
        void shouldMatch011Phone() {
            String phone = "011-1234-5678";
            Matcher matcher = MaskingPatterns.PHONE_KR.matcher(phone);
            assertThat(matcher.find()).isTrue();
        }
    }

    @Nested
    @DisplayName("주민등록번호 패턴 테스트")
    class SsnPatternTest {

        @Test
        @DisplayName("주민등록번호를 매칭한다")
        void shouldMatchSsn() {
            String ssn = "900101-1234567";
            Matcher matcher = MaskingPatterns.SSN_KR.matcher(ssn);
            assertThat(matcher.find()).isTrue();
            String result = matcher.replaceAll(MaskingPatterns.SSN_KR_REPLACEMENT);
            assertThat(result).isEqualTo("900101-*******");
        }

        @Test
        @DisplayName("하이픈 없는 주민등록번호도 매칭한다")
        void shouldMatchSsnWithoutDash() {
            String ssn = "9001011234567";
            Matcher matcher = MaskingPatterns.SSN_KR.matcher(ssn);
            assertThat(matcher.find()).isTrue();
        }

        @Test
        @DisplayName("여성 주민등록번호도 매칭한다")
        void shouldMatchFemaleSsn() {
            String ssn = "900101-2234567";
            Matcher matcher = MaskingPatterns.SSN_KR.matcher(ssn);
            assertThat(matcher.find()).isTrue();
        }
    }

    @Nested
    @DisplayName("Bearer 토큰 패턴 테스트")
    class BearerTokenPatternTest {

        @Test
        @DisplayName("Bearer 토큰을 매칭한다")
        void shouldMatchBearerToken() {
            String bearer = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.signature";
            Matcher matcher = MaskingPatterns.BEARER_TOKEN.matcher(bearer);
            assertThat(matcher.find()).isTrue();
            String result = matcher.replaceAll(MaskingPatterns.BEARER_TOKEN_REPLACEMENT);
            assertThat(result).isEqualTo("Bearer [MASKED]");
        }

        @Test
        @DisplayName("Bearer 없는 토큰은 매칭하지 않는다")
        void shouldNotMatchTokenWithoutBearer() {
            String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.signature";
            Matcher matcher = MaskingPatterns.BEARER_TOKEN.matcher(token);
            assertThat(matcher.find()).isFalse();
        }
    }

    @Nested
    @DisplayName("JSON 내 JWT 토큰 패턴 테스트")
    class JwtInJsonPatternTest {

        @Test
        @DisplayName("JSON accessToken을 매칭한다")
        void shouldMatchAccessToken() {
            String json = "\"accessToken\":\"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.sig\"";
            Matcher matcher = MaskingPatterns.JWT_IN_JSON.matcher(json);
            assertThat(matcher.find()).isTrue();
            String result = matcher.replaceAll(MaskingPatterns.JWT_IN_JSON_REPLACEMENT);
            assertThat(result).isEqualTo("\"accessToken\":\"[MASKED]\"");
        }

        @Test
        @DisplayName("JSON refresh_token을 매칭한다")
        void shouldMatchRefreshToken() {
            String json = "\"refresh_token\":\"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.sig\"";
            Matcher matcher = MaskingPatterns.JWT_IN_JSON.matcher(json);
            assertThat(matcher.find()).isTrue();
        }
    }

    @Nested
    @DisplayName("JSON 내 Password 패턴 테스트")
    class PasswordInJsonPatternTest {

        @Test
        @DisplayName("password 필드를 매칭한다")
        void shouldMatchPassword() {
            String json = "\"password\":\"mysecret\"";
            Matcher matcher = MaskingPatterns.PASSWORD_IN_JSON.matcher(json);
            assertThat(matcher.find()).isTrue();
            String result = matcher.replaceAll(MaskingPatterns.PASSWORD_IN_JSON_REPLACEMENT);
            assertThat(result).isEqualTo("\"password\":\"[MASKED]\"");
        }

        @Test
        @DisplayName("secret 필드를 매칭한다")
        void shouldMatchSecret() {
            String json = "\"secret\":\"supersecret\"";
            Matcher matcher = MaskingPatterns.PASSWORD_IN_JSON.matcher(json);
            assertThat(matcher.find()).isTrue();
        }

        @Test
        @DisplayName("api_key 필드를 매칭한다")
        void shouldMatchApiKey() {
            String json = "\"api_key\":\"key123\"";
            Matcher matcher = MaskingPatterns.PASSWORD_IN_JSON.matcher(json);
            assertThat(matcher.find()).isTrue();
        }
    }

    @Nested
    @DisplayName("IPv4 패턴 테스트")
    class Ipv4PatternTest {

        @Test
        @DisplayName("IPv4 주소를 매칭한다")
        void shouldMatchIpv4() {
            String ip = "192.168.1.100";
            Matcher matcher = MaskingPatterns.IPV4.matcher(ip);
            assertThat(matcher.find()).isTrue();
            String result = matcher.replaceAll(MaskingPatterns.IPV4_REPLACEMENT);
            assertThat(result).isEqualTo("192.168.*.*");
        }

        @Test
        @DisplayName("잘못된 IP는 매칭하지 않는다")
        void shouldNotMatchInvalidIp() {
            String invalid = "192.168";
            Matcher matcher = MaskingPatterns.IPV4.matcher(invalid);
            assertThat(matcher.find()).isFalse();
        }
    }
}
