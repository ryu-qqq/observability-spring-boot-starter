package com.ryuqq.observability.web.http;

import com.ryuqq.observability.web.config.HttpLoggingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PathNormalizer 테스트")
class PathNormalizerTest {

    private PathNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new PathNormalizer();
    }

    @Nested
    @DisplayName("기본 생성자 테스트")
    class DefaultConstructorTest {

        @Test
        @DisplayName("기본 생성자로 인스턴스를 생성할 수 있다")
        void shouldCreateWithDefaultConstructor() {
            PathNormalizer instance = new PathNormalizer();
            assertThat(instance).isNotNull();
        }
    }

    @Nested
    @DisplayName("PathPattern 리스트 생성자 테스트")
    class PathPatternConstructorTest {

        @Test
        @DisplayName("null 패턴 리스트로 생성할 수 있다")
        void shouldCreateWithNullPatterns() {
            PathNormalizer instance = new PathNormalizer(null);
            assertThat(instance).isNotNull();
        }

        @Test
        @DisplayName("커스텀 패턴 리스트로 생성할 수 있다")
        void shouldCreateWithCustomPatterns() {
            HttpLoggingProperties.PathPattern pattern = new HttpLoggingProperties.PathPattern(
                    "/api/orders/ORD-[A-Z]+-\\d+",
                    "/api/orders/{orderId}"
            );
            PathNormalizer instance = new PathNormalizer(List.of(pattern));

            String result = instance.normalize("/api/orders/ORD-ABC-123");
            assertThat(result).isEqualTo("/api/orders/{orderId}");
        }
    }

    @Nested
    @DisplayName("null 및 빈 문자열 처리 테스트")
    class NullAndEmptyTest {

        @Test
        @DisplayName("null 입력에 대해 null을 반환한다")
        void shouldReturnNullForNullInput() {
            String result = normalizer.normalize(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열에 대해 빈 문자열을 반환한다")
        void shouldReturnEmptyForEmptyInput() {
            String result = normalizer.normalize("");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("UUID 정규화 테스트")
    class UuidNormalizationTest {

        @Test
        @DisplayName("UUID가 {uuid}로 정규화된다")
        void shouldNormalizeUuid() {
            String path = "/api/users/550e8400-e29b-41d4-a716-446655440000";
            String result = normalizer.normalize(path);
            assertThat(result).isEqualTo("/api/users/{uuid}");
        }

        @Test
        @DisplayName("대문자 UUID도 정규화된다")
        void shouldNormalizeUppercaseUuid() {
            String path = "/api/users/550E8400-E29B-41D4-A716-446655440000";
            String result = normalizer.normalize(path);
            assertThat(result).isEqualTo("/api/users/{uuid}");
        }

        @Test
        @DisplayName("경로 중간의 UUID도 정규화된다")
        void shouldNormalizeUuidInMiddle() {
            String path = "/api/users/550e8400-e29b-41d4-a716-446655440000/orders";
            String result = normalizer.normalize(path);
            assertThat(result).isEqualTo("/api/users/{uuid}/orders");
        }
    }

    @Nested
    @DisplayName("숫자 ID 정규화 테스트")
    class NumericIdNormalizationTest {

        @Test
        @DisplayName("숫자 ID가 {id}로 정규화된다")
        void shouldNormalizeNumericId() {
            String path = "/api/users/12345";
            String result = normalizer.normalize(path);
            assertThat(result).isEqualTo("/api/users/{id}");
        }

        @Test
        @DisplayName("경로 중간의 숫자 ID도 정규화된다")
        void shouldNormalizeNumericIdInMiddle() {
            String path = "/api/users/12345/orders";
            String result = normalizer.normalize(path);
            assertThat(result).isEqualTo("/api/users/{id}/orders");
        }

        @Test
        @DisplayName("여러 숫자 ID가 모두 정규화된다")
        void shouldNormalizeMultipleNumericIds() {
            String path = "/api/users/123/orders/456";
            String result = normalizer.normalize(path);
            assertThat(result).isEqualTo("/api/users/{id}/orders/{id}");
        }

        @Test
        @DisplayName("경로 끝의 숫자 ID도 정규화된다")
        void shouldNormalizeNumericIdAtEnd() {
            String path = "/api/products/999";
            String result = normalizer.normalize(path);
            assertThat(result).isEqualTo("/api/products/{id}");
        }
    }

    @Nested
    @DisplayName("해시값 정규화 테스트")
    class HashNormalizationTest {

        @Test
        @DisplayName("32자 이상 해시값이 {hash}로 정규화된다")
        void shouldNormalizeHash() {
            String path = "/api/files/0123456789abcdef0123456789abcdef";
            String result = normalizer.normalize(path);
            assertThat(result).isEqualTo("/api/files/{hash}");
        }

        @Test
        @DisplayName("64자 해시값도 정규화된다")
        void shouldNormalize64CharHash() {
            String path = "/api/files/0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
            String result = normalizer.normalize(path);
            assertThat(result).isEqualTo("/api/files/{hash}");
        }
    }

    @Nested
    @DisplayName("커스텀 패턴 테스트")
    class CustomPatternTest {

        @Test
        @DisplayName("addPattern으로 커스텀 패턴을 추가할 수 있다")
        void shouldAddCustomPattern() {
            normalizer.addPattern("/api/orders/ORD-[A-Z]+-\\d+", "/api/orders/{orderId}");

            String result = normalizer.normalize("/api/orders/ORD-ABC-123");
            assertThat(result).isEqualTo("/api/orders/{orderId}");
        }

        @Test
        @DisplayName("커스텀 패턴이 기본 패턴보다 먼저 적용된다")
        void shouldApplyCustomPatternBeforeDefault() {
            normalizer.addPattern("/api/v\\d+", "/api/{version}");

            String result = normalizer.normalize("/api/v2/users/123");
            assertThat(result).isEqualTo("/api/{version}/users/{id}");
        }
    }

    @Nested
    @DisplayName("복합 정규화 테스트")
    class ComplexNormalizationTest {

        @Test
        @DisplayName("UUID와 숫자 ID가 함께 정규화된다")
        void shouldNormalizeUuidAndNumericId() {
            String path = "/api/users/550e8400-e29b-41d4-a716-446655440000/orders/123";
            String result = normalizer.normalize(path);
            assertThat(result).isEqualTo("/api/users/{uuid}/orders/{id}");
        }

        @Test
        @DisplayName("정규화 대상이 없는 경로는 그대로 반환된다")
        void shouldReturnUnchangedPathIfNoMatch() {
            String path = "/api/users";
            String result = normalizer.normalize(path);
            assertThat(result).isEqualTo("/api/users");
        }
    }
}
