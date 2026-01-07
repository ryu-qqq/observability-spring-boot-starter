package com.ryuqq.observability.integration.redis;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.message.config.MessageLoggingProperties;
import com.ryuqq.observability.message.interceptor.MessageLoggingInterceptor;
import com.ryuqq.observability.message.redis.RedisMessageLoggingAspect;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.UUID;

/**
 * Redis 인바운드 통합 테스트를 위한 Spring 설정.
 *
 * <p>Testcontainers Redis와 연동하기 위한 빈 설정과
 * observability-message 모듈의 RedisMessageLoggingAspect를 포함합니다.</p>
 */
@Configuration
@EnableAutoConfiguration
@EnableAspectJAutoProxy
public class RedisTestConfiguration {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            org.springframework.core.env.Environment env) {
        String host = env.getProperty("spring.data.redis.host", "localhost");
        int port = env.getProperty("spring.data.redis.port", Integer.class, 6379);

        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        return factory;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RedisMessageCaptureHolder redisMessageCaptureHolder() {
        return new RedisMessageCaptureHolder();
    }

    @Bean
    public RedisTestListener redisTestListener(RedisMessageCaptureHolder captureHolder) {
        return new RedisTestListener(captureHolder);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisTestListener redisTestListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(redisTestListener, new PatternTopic("test-channel-*"));
        return container;
    }

    @Bean
    public MessageLoggingProperties messageLoggingProperties() {
        MessageLoggingProperties properties = new MessageLoggingProperties();
        properties.setEnabled(true);
        properties.setLogPayload(true);
        return properties;
    }

    @Bean
    public LogMasker logMasker() {
        return new LogMasker();
    }

    @Bean
    public MessageLoggingInterceptor messageLoggingInterceptor(
            MessageLoggingProperties properties,
            LogMasker logMasker) {
        return new MessageLoggingInterceptor(
                properties,
                () -> UUID.randomUUID().toString(),
                logMasker,
                "redis-in-test"
        );
    }

    @Bean
    public RedisMessageLoggingAspect redisMessageLoggingAspect(
            MessageLoggingInterceptor interceptor) {
        return new RedisMessageLoggingAspect(interceptor);
    }
}
