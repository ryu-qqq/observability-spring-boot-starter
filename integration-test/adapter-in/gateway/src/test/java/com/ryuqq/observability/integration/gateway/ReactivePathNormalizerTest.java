package com.ryuqq.observability.integration.gateway;

import com.ryuqq.observability.webflux.config.ReactiveHttpLoggingProperties;
import com.ryuqq.observability.webflux.http.ReactivePathNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReactivePathNormalizer 단위 테스트.
 *
 * <p>검증 항목:</p>
 * <ul>
 *   <li>UUID 경로 정규화</li>
 *   <li>숫자 ID 경로 정규화</li>
 *   <li>해시값 경로 정규화</li>
 *   <li>커스텀 패턴 적용</li>
 *   <li>복합 경로 정규화</li>
 *   <li>Edge case 처리</li>
 * </ul>
 */
class ReactivePathNormalizerTest {

    private ReactivePathNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new ReactivePathNormalizer();
    }

    @Nested
    @DisplayName("UUID 경로 정규화 테스트")
    class UuidNormalizationTests {

        @Test
        @DisplayName("UUID가 {uuid}로 정규화되어야 한다")
        void shouldNormalizeUuid() {
            String uuid = UUID.randomUUID().toString();
            String path = "/api/orders/" + uuid;

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/orders/{uuid}");
        }

        @Test
        @DisplayName("대문자 UUID도 정규화되어야 한다")
        void shouldNormalizeUppercaseUuid() {
            String path = "/api/orders/550E8400-E29B-41D4-A716-446655440000";

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/orders/{uuid}");
        }

        @Test
        @DisplayName("여러 UUID가 모두 정규화되어야 한다")
        void shouldNormalizeMultipleUuids() {
            String uuid1 = UUID.randomUUID().toString();
            String uuid2 = UUID.randomUUID().toString();
            String path = "/api/users/" + uuid1 + "/orders/" + uuid2;

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/users/{uuid}/orders/{uuid}");
        }
    }

    @Nested
    @DisplayName("숫자 ID 경로 정규화 테스트")
    class NumericIdNormalizationTests {

        @Test
        @DisplayName("숫자 ID가 {id}로 정규화되어야 한다")
        void shouldNormalizeNumericId() {
            String path = "/api/users/12345";

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/users/{id}");
        }

        @Test
        @DisplayName("여러 숫자 ID가 모두 정규화되어야 한다")
        void shouldNormalizeMultipleNumericIds() {
            String path = "/api/users/123/posts/456";

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/users/{id}/posts/{id}");
        }

        @Test
        @DisplayName("경로 끝의 숫자 ID가 정규화되어야 한다")
        void shouldNormalizeNumericIdAtEnd() {
            String path = "/api/products/99999";

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/products/{id}");
        }

        @Test
        @DisplayName("긴 숫자 ID도 정규화되어야 한다")
        void shouldNormalizeLongNumericId() {
            String path = "/api/transactions/1234567890123456789";

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/transactions/{id}");
        }
    }

    @Nested
    @DisplayName("해시값 경로 정규화 테스트")
    class HashNormalizationTests {

        @Test
        @DisplayName("32자 이상 해시값이 {hash}로 정규화되어야 한다")
        void shouldNormalizeLongHash() {
            String hash = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"; // 32자
            String path = "/api/files/" + hash;

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/files/{hash}");
        }

        @Test
        @DisplayName("SHA-256 해시가 정규화되어야 한다")
        void shouldNormalizeSha256Hash() {
            String sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            String path = "/api/checksums/" + sha256;

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/checksums/{hash}");
        }
    }

    @Nested
    @DisplayName("커스텀 패턴 테스트")
    class CustomPatternTests {

        @Test
        @DisplayName("커스텀 패턴이 적용되어야 한다")
        void shouldApplyCustomPattern() {
            List<ReactiveHttpLoggingProperties.PathPattern> customPatterns = List.of(
                    new ReactiveHttpLoggingProperties.PathPattern("ORD-[A-Z0-9]+", "{orderId}")
            );
            ReactivePathNormalizer customNormalizer = new ReactivePathNormalizer(customPatterns);

            String path = "/api/orders/ORD-ABC123";

            String normalized = customNormalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/orders/{orderId}");
        }

        @Test
        @DisplayName("여러 커스텀 패턴이 순서대로 적용되어야 한다")
        void shouldApplyMultipleCustomPatterns() {
            List<ReactiveHttpLoggingProperties.PathPattern> customPatterns = List.of(
                    new ReactiveHttpLoggingProperties.PathPattern("SKU-\\d+", "{sku}"),
                    new ReactiveHttpLoggingProperties.PathPattern("CAT-[A-Z]+", "{category}")
            );
            ReactivePathNormalizer customNormalizer = new ReactivePathNormalizer(customPatterns);

            String path = "/api/products/SKU-12345/category/CAT-ELECTRONICS";

            String normalized = customNormalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/products/{sku}/category/{category}");
        }

        @Test
        @DisplayName("addPattern으로 추가한 패턴이 적용되어야 한다")
        void shouldApplyPatternAddedViaMethod() {
            normalizer.addPattern("v\\d+", "{version}");

            String path = "/api/v2/users";

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/{version}/users");
        }
    }

    @Nested
    @DisplayName("복합 경로 정규화 테스트")
    class ComplexPathNormalizationTests {

        @Test
        @DisplayName("UUID와 숫자 ID가 혼합된 경로가 정규화되어야 한다")
        void shouldNormalizeMixedPath() {
            String uuid = UUID.randomUUID().toString();
            String path = "/api/organizations/123/projects/" + uuid + "/tasks/456";

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api/organizations/{id}/projects/{uuid}/tasks/{id}");
        }

        @Test
        @DisplayName("쿼리 파라미터가 있는 경로도 정규화되어야 한다")
        void shouldNormalizePathWithQueryParams() {
            String path = "/api/users/12345?include=profile&expand=orders";

            String normalized = normalizer.normalize(path);

            // 쿼리 파라미터는 경로의 일부가 아니므로 일반적으로 별도 처리
            // 하지만 이 구현에서는 경로만 처리하므로 쿼리 부분은 그대로 유지
            assertThat(normalized).contains("{id}");
        }
    }

    @Nested
    @DisplayName("Edge case 테스트")
    class EdgeCaseTests {

        @Test
        @DisplayName("null 경로는 null을 반환해야 한다")
        void shouldReturnNullForNullPath() {
            String normalized = normalizer.normalize(null);

            assertThat(normalized).isNull();
        }

        @Test
        @DisplayName("빈 경로는 빈 문자열을 반환해야 한다")
        void shouldReturnEmptyForEmptyPath() {
            String normalized = normalizer.normalize("");

            assertThat(normalized).isEmpty();
        }

        @Test
        @DisplayName("정규화 대상이 없는 경로는 그대로 반환되어야 한다")
        void shouldReturnOriginalForNonMatchingPath() {
            String path = "/api/health";

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo(path);
        }

        @Test
        @DisplayName("루트 경로는 그대로 반환되어야 한다")
        void shouldReturnRootPath() {
            String path = "/";

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/");
        }

        @Test
        @DisplayName("문자열 중간의 숫자는 정규화되지 않아야 한다")
        void shouldNotNormalizeNumbersInMiddleOfString() {
            String path = "/api/user123name";

            String normalized = normalizer.normalize(path);

            // user123name은 경로 세그먼트 전체가 숫자가 아니므로 정규화되지 않음
            assertThat(normalized).isEqualTo("/api/user123name");
        }

        @Test
        @DisplayName("슬래시로 시작하지 않는 경로도 처리되어야 한다")
        void shouldHandlePathWithoutLeadingSlash() {
            String path = "api/users/12345";

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("api/users/{id}");
        }

        @Test
        @DisplayName("연속된 슬래시가 있는 경로도 처리되어야 한다")
        void shouldHandleConsecutiveSlashes() {
            String path = "/api//users/12345";

            String normalized = normalizer.normalize(path);

            assertThat(normalized).isEqualTo("/api//users/{id}");
        }
    }

    @Nested
    @DisplayName("성능 테스트")
    class PerformanceTests {

        @Test
        @DisplayName("많은 경로를 빠르게 정규화할 수 있어야 한다")
        void shouldNormalizeManyPathsQuickly() {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 10000; i++) {
                String uuid = UUID.randomUUID().toString();
                String path = "/api/users/" + i + "/orders/" + uuid;
                normalizer.normalize(path);
            }

            long duration = System.currentTimeMillis() - startTime;

            // 10000번 정규화가 1초 이내에 완료되어야 함
            assertThat(duration).isLessThan(1000);
        }
    }
}
