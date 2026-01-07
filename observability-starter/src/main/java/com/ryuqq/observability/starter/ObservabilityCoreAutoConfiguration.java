package com.ryuqq.observability.starter;

import com.ryuqq.observability.core.masking.LogMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
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
public class ObservabilityCoreAutoConfiguration implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityCoreAutoConfiguration.class);
    private static final String DEFAULT_SERVICE_NAME = "unknown";

    private final ObservabilityProperties properties;

    public ObservabilityCoreAutoConfiguration(ObservabilityProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (DEFAULT_SERVICE_NAME.equals(properties.getServiceName())) {
            log.warn("Observability SDK: service-name is not configured. " +
                    "Set 'spring.application.name' or 'observability.service-name' for better log tracing.");
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public LogMasker logMasker(ObservabilityProperties properties) {
        return new LogMasker(properties.getMasking());
    }
}
