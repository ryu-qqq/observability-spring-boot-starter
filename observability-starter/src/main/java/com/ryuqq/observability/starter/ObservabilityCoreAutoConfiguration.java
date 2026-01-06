package com.ryuqq.observability.starter;

import com.ryuqq.observability.core.masking.LogMasker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * observability-core 모듈 자동 설정.
 *
 * <p>마스킹 설정 및 핵심 유틸리티를 자동으로 구성합니다.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties.class)
public class ObservabilityCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogMasker logMasker(ObservabilityProperties properties) {
        return new LogMasker(properties.getMasking());
    }
}
