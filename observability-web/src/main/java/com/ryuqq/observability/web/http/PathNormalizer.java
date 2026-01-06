package com.ryuqq.observability.web.http;

import com.ryuqq.observability.web.config.HttpLoggingProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * URL 경로를 정규화하여 메트릭 폭발을 방지합니다.
 *
 * <p>예시:</p>
 * <ul>
 *   <li>/api/users/12345 → /api/users/{id}</li>
 *   <li>/api/orders/ORD-ABC-123 → /api/orders/{orderId}</li>
 * </ul>
 */
public class PathNormalizer {

    private final List<PatternReplacement> customPatterns = new ArrayList<>();

    // 기본 패턴들
    private static final List<PatternReplacement> DEFAULT_PATTERNS = List.of(
            // UUID 패턴
            new PatternReplacement(
                    Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"),
                    "{uuid}"
            ),
            // 숫자만 있는 ID
            new PatternReplacement(
                    Pattern.compile("/\\d+(?=/|$)"),
                    "/{id}"
            ),
            // 긴 해시값 (32자 이상)
            new PatternReplacement(
                    Pattern.compile("[0-9a-fA-F]{32,}"),
                    "{hash}"
            )
    );

    public PathNormalizer() {
    }

    public PathNormalizer(List<HttpLoggingProperties.PathPattern> patterns) {
        if (patterns != null) {
            for (HttpLoggingProperties.PathPattern pattern : patterns) {
                addPattern(pattern.getPattern(), pattern.getReplacement());
            }
        }
    }

    /**
     * 커스텀 정규화 패턴을 추가합니다.
     *
     * @param pattern     정규표현식 패턴
     * @param replacement 대체 문자열
     */
    public void addPattern(String pattern, String replacement) {
        customPatterns.add(new PatternReplacement(Pattern.compile(pattern), replacement));
    }

    /**
     * 경로를 정규화합니다.
     *
     * @param path 원본 경로
     * @return 정규화된 경로
     */
    public String normalize(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        String result = path;

        // 커스텀 패턴 먼저 적용
        for (PatternReplacement pr : customPatterns) {
            result = pr.pattern.matcher(result).replaceAll(pr.replacement);
        }

        // 기본 패턴 적용
        for (PatternReplacement pr : DEFAULT_PATTERNS) {
            result = pr.pattern.matcher(result).replaceAll(pr.replacement);
        }

        return result;
    }


    private record PatternReplacement(Pattern pattern, String replacement) {
    }
}
