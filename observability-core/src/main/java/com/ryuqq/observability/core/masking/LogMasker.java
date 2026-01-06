package com.ryuqq.observability.core.masking;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 로그 내 민감정보를 마스킹하는 유틸리티.
 *
 * <p>이 클래스는 순수 Java로 구현되어 Domain Layer에서도 사용할 수 있습니다.</p>
 *
 * <p>기본 제공 패턴과 커스텀 패턴을 모두 적용합니다.</p>
 *
 * <pre>
 * {@code
 * String masked = logMasker.mask("card=1234-5678-9012-3456");
 * // 결과: "card=****-****-****-3456"
 * }
 * </pre>
 */
public class LogMasker {

    private final List<PatternReplacement> patterns = new ArrayList<>();
    private final boolean enabled;

    public LogMasker(MaskingProperties properties) {
        this.enabled = properties.isEnabled();

        if (enabled) {
            // 기본 패턴 등록
            addDefaultPatterns();

            // 커스텀 패턴 등록
            for (MaskingProperties.MaskingPattern custom : properties.getPatterns()) {
                addPattern(custom.getPattern(), custom.getReplacement());
            }

            // 필드명 기반 마스킹 패턴 등록
            for (String field : properties.getMaskFields()) {
                addFieldPattern(field);
            }
        }
    }

    /**
     * 기본 설정으로 LogMasker를 생성합니다.
     */
    public LogMasker() {
        this(new MaskingProperties());
    }

    /**
     * 문자열 내 민감정보를 마스킹합니다.
     *
     * @param input 원본 문자열
     * @return 마스킹된 문자열
     */
    public String mask(String input) {
        if (!enabled || input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        for (PatternReplacement pr : patterns) {
            result = pr.pattern.matcher(result).replaceAll(pr.replacement);
        }
        return result;
    }

    /**
     * 마스킹이 활성화되어 있는지 확인합니다.
     *
     * @return 활성화 여부
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 커스텀 패턴을 추가합니다.
     *
     * @param pattern     정규표현식 패턴
     * @param replacement 대체 문자열
     */
    public void addPattern(String pattern, String replacement) {
        patterns.add(new PatternReplacement(Pattern.compile(pattern), replacement));
    }

    private void addDefaultPatterns() {
        // JSON 내 민감 필드 (가장 먼저 적용)
        patterns.add(new PatternReplacement(
                MaskingPatterns.PASSWORD_IN_JSON,
                MaskingPatterns.PASSWORD_IN_JSON_REPLACEMENT
        ));
        patterns.add(new PatternReplacement(
                MaskingPatterns.JWT_IN_JSON,
                MaskingPatterns.JWT_IN_JSON_REPLACEMENT
        ));

        // Bearer 토큰
        patterns.add(new PatternReplacement(
                MaskingPatterns.BEARER_TOKEN,
                MaskingPatterns.BEARER_TOKEN_REPLACEMENT
        ));

        // 신용카드
        patterns.add(new PatternReplacement(
                MaskingPatterns.CREDIT_CARD_DASH,
                MaskingPatterns.CREDIT_CARD_DASH_REPLACEMENT
        ));
        patterns.add(new PatternReplacement(
                MaskingPatterns.CREDIT_CARD_PLAIN,
                MaskingPatterns.CREDIT_CARD_PLAIN_REPLACEMENT
        ));

        // 이메일
        patterns.add(new PatternReplacement(
                MaskingPatterns.EMAIL,
                MaskingPatterns.EMAIL_REPLACEMENT
        ));

        // 한국 전화번호
        patterns.add(new PatternReplacement(
                MaskingPatterns.PHONE_KR,
                MaskingPatterns.PHONE_KR_REPLACEMENT
        ));

        // 주민등록번호
        patterns.add(new PatternReplacement(
                MaskingPatterns.SSN_KR,
                MaskingPatterns.SSN_KR_REPLACEMENT
        ));
    }

    /**
     * JSON 필드명 기반 마스킹 패턴을 추가합니다.
     * "fieldName":"value" → "fieldName":"[MASKED]"
     */
    private void addFieldPattern(String fieldName) {
        String pattern = "(\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\")[^\"]*(\")";
        patterns.add(new PatternReplacement(
                Pattern.compile(pattern, Pattern.CASE_INSENSITIVE),
                "$1[MASKED]$2"
        ));
    }


    private record PatternReplacement(Pattern pattern, String replacement) {
    }
}
