package com.ryuqq.observability.core.masking;

import java.util.regex.Pattern;

/**
 * 기본 제공되는 민감정보 마스킹 패턴들.
 *
 * <p>이 클래스는 순수 Java로 구현되어 Domain Layer에서도 사용할 수 있습니다.</p>
 */
public final class MaskingPatterns {

    private MaskingPatterns() {
    }

    /**
     * 이메일 주소 패턴.
     * example@domain.com → ex***@domain.com
     */
    public static final Pattern EMAIL = Pattern.compile(
            "([a-zA-Z0-9._%+-]{2})[a-zA-Z0-9._%+-]*(@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
    );
    public static final String EMAIL_REPLACEMENT = "$1***$2";

    /**
     * 신용카드 번호 패턴 (하이픈 구분).
     * 1234-5678-9012-3456 → ****-****-****-3456
     */
    public static final Pattern CREDIT_CARD_DASH = Pattern.compile(
            "\\d{4}-\\d{4}-\\d{4}-(\\d{4})"
    );
    public static final String CREDIT_CARD_DASH_REPLACEMENT = "****-****-****-$1";

    /**
     * 신용카드 번호 패턴 (연속).
     * 1234567890123456 → ************3456
     */
    public static final Pattern CREDIT_CARD_PLAIN = Pattern.compile(
            "\\d{12}(\\d{4})"
    );
    public static final String CREDIT_CARD_PLAIN_REPLACEMENT = "************$1";

    /**
     * 한국 휴대폰 번호 패턴.
     * 010-1234-5678 → 010-****-5678
     */
    public static final Pattern PHONE_KR = Pattern.compile(
            "(01[0-9])[-\\s]?(\\d{4})[-\\s]?(\\d{4})"
    );
    public static final String PHONE_KR_REPLACEMENT = "$1-****-$3";

    /**
     * 주민등록번호 패턴.
     * 900101-1234567 → 900101-*******
     */
    public static final Pattern SSN_KR = Pattern.compile(
            "(\\d{6})[-\\s]?([1-4]\\d{6})"
    );
    public static final String SSN_KR_REPLACEMENT = "$1-*******";

    /**
     * Bearer 토큰 패턴.
     * Bearer eyJhbGciOi... → Bearer [MASKED]
     */
    public static final Pattern BEARER_TOKEN = Pattern.compile(
            "(Bearer\\s+)[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]*"
    );
    public static final String BEARER_TOKEN_REPLACEMENT = "$1[MASKED]";

    /**
     * 일반 JWT 토큰 패턴 (JSON 값으로 있을 때).
     * "token":"eyJ..." → "token":"[MASKED]"
     */
    public static final Pattern JWT_IN_JSON = Pattern.compile(
            "(\"(?:token|accessToken|access_token|refreshToken|refresh_token)\"\\s*:\\s*\")[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]*(\")"
    );
    public static final String JWT_IN_JSON_REPLACEMENT = "$1[MASKED]$2";

    /**
     * JSON의 password 필드.
     * "password":"secret123" → "password":"[MASKED]"
     */
    public static final Pattern PASSWORD_IN_JSON = Pattern.compile(
            "(\"(?:password|passwd|secret|apiKey|api_key)\"\\s*:\\s*\")[^\"]*(\")");
    public static final String PASSWORD_IN_JSON_REPLACEMENT = "$1[MASKED]$2";

    /**
     * IPv4 주소 패턴 (부분 마스킹).
     * 192.168.1.100 → 192.168.*.*
     */
    public static final Pattern IPV4 = Pattern.compile(
            "(\\d{1,3}\\.\\d{1,3})\\.\\d{1,3}\\.\\d{1,3}"
    );
    public static final String IPV4_REPLACEMENT = "$1.*.*";
}
