package com.ryuqq.observability.starter;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.message.config.MessageLoggingProperties;
import com.ryuqq.observability.message.interceptor.MessageLoggingInterceptor;
import com.ryuqq.observability.message.redis.RedisMessageLoggingAspect;
import com.ryuqq.observability.message.sqs.SqsMessageLoggingAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.UUID;

/**
 * observability-message 모듈 자동 설정.
 *
 * <p>메시지 큐 리스너(SQS, Redis)에 자동 로깅을 구성합니다.</p>
 */
@AutoConfiguration(after = ObservabilityCoreAutoConfiguration.class)
@ConditionalOnProperty(prefix = "observability.message", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableAspectJAutoProxy
public class ObservabilityMessageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MessageLoggingInterceptor messageLoggingInterceptor(
            ObservabilityProperties properties,
            LogMasker logMasker) {

        MessageLoggingProperties messageProps = properties.getMessage();
        return new MessageLoggingInterceptor(
                messageProps,
                () -> UUID.randomUUID().toString(),
                logMasker,
                properties.getServiceName()
        );
    }

    /**
     * SQS 메시지 로깅 설정.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.awspring.cloud.sqs.annotation.SqsListener")
    static class SqsLoggingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public SqsMessageLoggingAspect sqsMessageLoggingAspect(
                MessageLoggingInterceptor interceptor) {
            return new SqsMessageLoggingAspect(interceptor);
        }
    }

    /**
     * Redis 메시지 로깅 설정.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.redis.connection.MessageListener")
    static class RedisLoggingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RedisMessageLoggingAspect redisMessageLoggingAspect(
                MessageLoggingInterceptor interceptor) {
            return new RedisMessageLoggingAspect(interceptor);
        }
    }
}
