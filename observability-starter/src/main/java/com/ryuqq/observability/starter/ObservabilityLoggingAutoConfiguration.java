package com.ryuqq.observability.starter;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.logging.aspect.BusinessLogAspect;
import com.ryuqq.observability.logging.aspect.LoggableAspect;
import com.ryuqq.observability.logging.config.BusinessLoggingProperties;
import com.ryuqq.observability.logging.event.BusinessEventListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * observability-logging 모듈 자동 설정.
 *
 * <p>비즈니스 로깅 어노테이션과 이벤트 리스너를 자동으로 구성합니다.</p>
 */
@AutoConfiguration(after = ObservabilityCoreAutoConfiguration.class)
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@ConditionalOnProperty(prefix = "observability.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableAspectJAutoProxy
public class ObservabilityLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LoggableAspect loggableAspect(
            ObservabilityProperties properties,
            LogMasker logMasker) {

        BusinessLoggingProperties loggingProps = properties.getLogging();
        return new LoggableAspect(loggingProps, logMasker);
    }

    @Bean
    @ConditionalOnMissingBean
    public BusinessLogAspect businessLogAspect(ObservabilityProperties properties) {
        BusinessLoggingProperties loggingProps = properties.getLogging();
        return new BusinessLogAspect(loggingProps);
    }

    @Bean
    @ConditionalOnMissingBean
    public BusinessEventListener businessEventListener(ObservabilityProperties properties) {
        BusinessLoggingProperties loggingProps = properties.getLogging();
        return new BusinessEventListener(loggingProps);
    }
}
