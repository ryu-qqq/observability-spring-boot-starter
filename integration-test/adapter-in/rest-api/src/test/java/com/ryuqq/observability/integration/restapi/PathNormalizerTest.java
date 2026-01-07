package com.ryuqq.observability.integration.restapi;

import com.ryuqq.observability.web.http.PathNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PathNormalizer 단위 테스트.
 *
 * <p>테스트 항목:</p>
 * <ul>
 *   <li>숫자 ID 정규화</li>
 *   <li>UUID 정규화</li>
 *   <li>해시값 정규화</li>
 *   <li>토큰 정규화</li>
 *   <li>커스텀 패턴</li>
 * </ul>
 */
class PathNormalizerTest {

    private PathNormalizer pathNormalizer;

    @BeforeEach
    void setUp() {
        pathNormalizer = new PathNormalizer();
    }

    @Nested
    @DisplayName("숫자 ID 정규화")
    class NumericIdNormalization {

        @Test
        @DisplayName("경로의 숫자 ID가 {id}로 정규화되어야 함")
        void shouldNormalizeNumericId() {
            // given
            String path = "/api/users/12345";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/users/{id}");
        }

        @Test
        @DisplayName("여러 숫자 ID가 모두 정규화되어야 함")
        void shouldNormalizeMultipleNumericIds() {
            // given
            String path = "/api/orders/123/items/456";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/orders/{id}/items/{id}");
        }

        @Test
        @DisplayName("긴 숫자 ID도 정규화되어야 함")
        void shouldNormalizeLongNumericId() {
            // given
            String path = "/api/transactions/9876543210123456789";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/transactions/{id}");
        }
    }

    @Nested
    @DisplayName("UUID 정규화")
    class UuidNormalization {

        @Test
        @DisplayName("UUID가 {uuid}로 정규화되어야 함")
        void shouldNormalizeUuid() {
            // given
            String path = "/api/users/550e8400-e29b-41d4-a716-446655440000";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/users/{uuid}");
        }

        @Test
        @DisplayName("대문자 UUID도 정규화되어야 함")
        void shouldNormalizeUppercaseUuid() {
            // given
            String path = "/api/orders/550E8400-E29B-41D4-A716-446655440000";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/orders/{uuid}");
        }
    }

    @Nested
    @DisplayName("해시값 정규화")
    class HashNormalization {

        @Test
        @DisplayName("32자 이상 해시가 {hash}로 정규화되어야 함")
        void shouldNormalizeHash() {
            // given - 32자 해시
            String path = "/api/files/abc123def456789abc123def456789ab";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/files/{hash}");
        }

        @Test
        @DisplayName("SHA-256 해시가 정규화되어야 함")
        void shouldNormalizeSha256Hash() {
            // given - 64자 해시
            String path = "/api/documents/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/documents/{hash}");
        }
    }

    @Nested
    @DisplayName("커스텀 패턴 테스트")
    class CustomPatternTests {

        @Test
        @DisplayName("커스텀 패턴을 추가하면 해당 패턴이 정규화되어야 함")
        void shouldNormalizeWithCustomPattern() {
            // given - 커스텀 토큰 패턴 추가
            pathNormalizer.addPattern("eyJ[a-zA-Z0-9_-]+", "{token}");
            String path = "/api/verify/eyJhbGciOiJIUzI1NiIsInR5";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/verify/{token}");
        }

        @Test
        @DisplayName("주문번호 패턴 커스텀 정규화")
        void shouldNormalizeOrderIdPattern() {
            // given
            pathNormalizer.addPattern("ORD-[A-Z]+-\\d+", "{orderId}");
            String path = "/api/orders/ORD-ABC-123";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/orders/{orderId}");
        }
    }

    @Nested
    @DisplayName("복합 경로 정규화")
    class ComplexPathNormalization {

        @Test
        @DisplayName("숫자와 UUID가 혼합된 경로가 정규화되어야 함")
        void shouldNormalizeMixedPath() {
            // given
            String path = "/api/users/550e8400-e29b-41d4-a716-446655440000/orders/123";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/users/{uuid}/orders/{id}");
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("null 경로는 null을 반환해야 함")
        void shouldHandleNullPath() {
            // when
            String normalized = pathNormalizer.normalize(null);

            // then
            assertThat(normalized).isNull();
        }

        @Test
        @DisplayName("빈 경로는 빈 문자열을 반환해야 함")
        void shouldHandleEmptyPath() {
            // when
            String normalized = pathNormalizer.normalize("");

            // then
            assertThat(normalized).isEmpty();
        }

        @Test
        @DisplayName("정규화할 것이 없는 경로는 그대로 반환")
        void shouldReturnOriginalPathWhenNoNormalizationNeeded() {
            // given
            String path = "/api/users/profile";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/users/profile");
        }

        @Test
        @DisplayName("쿼리 파라미터 앞의 숫자 ID는 정규화 패턴에서 제외됨 (패턴이 /|$ 앞만 매칭)")
        void shouldNotNormalizeIdBeforeQueryParams() {
            // given - 기본 패턴은 /\d+(?=/|$) 이므로 ? 앞 숫자는 정규화되지 않음
            String path = "/api/users/123?include=orders";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then - 쿼리 파라미터 앞 숫자는 정규화되지 않음
            assertThat(normalized).isEqualTo("/api/users/123?include=orders");
        }

        @Test
        @DisplayName("경로 중간의 숫자 ID는 정규화됨")
        void shouldNormalizeIdInMiddleOfPath() {
            // given
            String path = "/api/users/123/orders";

            // when
            String normalized = pathNormalizer.normalize(path);

            // then
            assertThat(normalized).isEqualTo("/api/users/{id}/orders");
        }
    }
}
