package com.ryuqq.observability.webflux.http;

import com.ryuqq.observability.webflux.config.ReactiveHttpLoggingProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * URL 경로를 정규화하여 메트릭 폭발을 방지합니다.
 *
 * <p>동적 경로 파라미터(UUID, 숫자 ID, 해시값 등)를 일반화된 플레이스홀더로 변환합니다.</p>
 *
 * <p>예시:</p>
 * <ul>
 *   <li>/api/users/12345 → /api/users/{id}</li>
 *   <li>/api/orders/550e8400-e29b-41d4-a716-446655440000 → /api/orders/{uuid}</li>
 *   <li>/api/files/abc123def456789 → /api/files/{hash}</li>
 * </ul>
 *
 * <p>이는 Spring WebFlux 환경에서 사용되며, 로그 및 메트릭의 카디널리티를 제어합니다.</p>
 */
public class ReactivePathNormalizer {

    private final List<PatternReplacement> customPatterns = new ArrayList<>();

    /**
     * 기본 정규화 패턴들.
     * 순서가 중요: 더 구체적인 패턴이 먼저 적용되어야 함.
     */
    private static final List<PatternReplacement> DEFAULT_PATTERNS = List.of(
            // UUID 패턴 (8-4-4-4-12 형식)
            new PatternReplacement(
                    Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"),
                    "{uuid}"
            ),
            // 숫자만 있는 ID (경로 세그먼트 전체가 숫자인 경우, 쿼리 파라미터도 처리)
            new PatternReplacement(
                    Pattern.compile("/\\d+(?=/|\\?|$)"),
                    "/{id}"
            ),
            // 긴 해시값 (32자 이상의 16진수)
            new PatternReplacement(
                    Pattern.compile("[0-9a-fA-F]{32,}"),
                    "{hash}"
            ),
            // Base64 URL-safe 토큰 (20자 이상)
            new PatternReplacement(
                    Pattern.compile("[A-Za-z0-9_-]{20,}"),
                    "{token}"
            )
    );

    /**
     * 기본 패턴만으로 PathNormalizer를 생성합니다.
     */
    public ReactivePathNormalizer() {
    }

    /**
     * 커스텀 패턴을 포함하여 PathNormalizer를 생성합니다.
     *
     * @param patterns 커스텀 정규화 패턴 목록
     */
    public ReactivePathNormalizer(List<ReactiveHttpLoggingProperties.PathPattern> patterns) {
        if (patterns != null) {
            for (ReactiveHttpLoggingProperties.PathPattern pattern : patterns) {
                addPattern(pattern.getPattern(), pattern.getReplacement());
            }
        }
    }

    /**
     * 커스텀 정규화 패턴을 추가합니다.
     *
     * <p>커스텀 패턴은 기본 패턴보다 먼저 적용됩니다.</p>
     *
     * @param pattern     정규표현식 패턴
     * @param replacement 대체 문자열 (예: "{orderId}")
     */
    public void addPattern(String pattern, String replacement) {
        customPatterns.add(new PatternReplacement(Pattern.compile(pattern), replacement));
    }

    /**
     * 경로를 정규화합니다.
     *
     * <p>커스텀 패턴이 먼저 적용되고, 그 다음 기본 패턴이 적용됩니다.</p>
     *
     * @param path 원본 경로
     * @return 정규화된 경로
     */
    public String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        String result = path;

        // 커스텀 패턴 먼저 적용 (사용자 정의 우선)
        for (PatternReplacement pr : customPatterns) {
            result = pr.pattern.matcher(result).replaceAll(pr.replacement);
        }

        // 기본 패턴 적용
        for (PatternReplacement pr : DEFAULT_PATTERNS) {
            result = pr.pattern.matcher(result).replaceAll(pr.replacement);
        }

        return result;
    }


    /**
     * 패턴과 대체 문자열을 담는 불변 레코드.
     */
    private record PatternReplacement(Pattern pattern, String replacement) {
    }
}
