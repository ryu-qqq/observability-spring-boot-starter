package com.ryuqq.observability.core.masking;

import java.util.ArrayList;
import java.util.List;

/**
 * 민감정보 마스킹 설정.
 *
 * <p>이 클래스는 순수 Java POJO로 구현되어 Domain Layer에서도 사용할 수 있습니다.</p>
 *
 * <pre>
 * observability:
 *   masking:
 *     enabled: true
 *     patterns:
 *       - name: credit-card
 *         pattern: "\\d{4}-\\d{4}-\\d{4}-\\d{4}"
 *         replacement: "****-****-****-****"
 *     mask-fields:
 *       - password
 *       - secret
 * </pre>
 */
public class MaskingProperties {

    /**
     * 마스킹 기능 활성화 여부
     */
    private boolean enabled = true;

    /**
     * 커스텀 마스킹 패턴 목록
     */
    private List<MaskingPattern> patterns = new ArrayList<>();

    /**
     * 마스킹할 필드명 목록 (JSON 키)
     */
    private List<String> maskFields = new ArrayList<>(List.of(
            "password",
            "passwd",
            "secret",
            "token",
            "apiKey",
            "api_key",
            "accessToken",
            "access_token",
            "refreshToken",
            "refresh_token",
            "creditCard",
            "credit_card",
            "cardNumber",
            "card_number",
            "ssn",
            "socialSecurityNumber"
    ));


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<MaskingPattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<MaskingPattern> patterns) {
        this.patterns = patterns;
    }

    public List<String> getMaskFields() {
        return maskFields;
    }

    public void setMaskFields(List<String> maskFields) {
        this.maskFields = maskFields;
    }


    /**
     * 커스텀 마스킹 패턴 정의.
     */
    public static class MaskingPattern {
        private String name;
        private String pattern;
        private String replacement;

        public MaskingPattern() {
        }

        public MaskingPattern(String name, String pattern, String replacement) {
            this.name = name;
            this.pattern = pattern;
            this.replacement = replacement;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getReplacement() {
            return replacement;
        }

        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }
    }
}
