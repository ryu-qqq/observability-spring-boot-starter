package com.ryuqq.observability.webflux.config;

import com.ryuqq.observability.core.masking.MaskingProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WebFlux용 민감정보 마스킹 설정.
 *
 * <p>observability-core의 MaskingProperties를 상속하여
 * Spring Boot ConfigurationProperties로 바인딩합니다.</p>
 *
 * <pre>
 * observability:
 *   masking:
 *     enabled: true
 *     mask-fields:
 *       - password
 *       - secret
 *       - apiKey
 * </pre>
 */
@ConfigurationProperties(prefix = "observability.masking")
public class ReactiveMaskingProperties extends MaskingProperties {
    // MaskingProperties의 모든 필드와 메서드를 그대로 상속
}
