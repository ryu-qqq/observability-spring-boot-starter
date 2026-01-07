package com.ryuqq.observability.integration.redis;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Redis 아웃바운드 통합 테스트를 위한 Spring 설정.
 *
 * <p>Testcontainers Redis와 연동하기 위한 빈 설정과
 * TraceId 전파를 지원하는 TracingRedisTemplate을 포함합니다.</p>
 */
@Configuration
@EnableAutoConfiguration
public class RedisOutTestConfiguration {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            org.springframework.core.env.Environment env) {
        String host = env.getProperty("spring.data.redis.host", "localhost");
        int port = env.getProperty("spring.data.redis.port", Integer.class, 6379);

        return new LettuceConnectionFactory(host, port);
    }

    /**
     * TraceId 전파를 지원하는 RedisTemplate.
     *
     * <p>메시지 발송 시 TraceIdHolder의 컨텍스트를 자동으로
     * 메시지 또는 Stream 필드에 추가합니다.</p>
     */
    @Bean
    public TracingRedisTemplate tracingRedisTemplate(LettuceConnectionFactory connectionFactory) {
        return new TracingRedisTemplate(connectionFactory);
    }
}
