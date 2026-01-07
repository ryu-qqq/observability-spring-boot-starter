package com.ryuqq.observability.integration.sqs;

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

/**
 * SQS 아웃바운드 통합 테스트를 위한 Spring 설정.
 *
 * <p>LocalStack SQS와 연동하기 위한 빈 설정과
 * TraceId 전파를 지원하는 TracingSqsTemplate을 포함합니다.</p>
 *
 * <p>SqsAutoConfiguration은 sqsTemplate 빈 충돌을 방지하기 위해 제외합니다.</p>
 */
@Configuration
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
@Import(SqsBootstrapConfiguration.class)
public class SqsOutTestConfiguration {

    @Bean
    public SqsAsyncClient sqsAsyncClient(
            org.springframework.core.env.Environment env) {
        String endpoint = env.getProperty("spring.cloud.aws.sqs.endpoint");
        String region = env.getProperty("spring.cloud.aws.region.static", "us-east-1");

        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();
    }

    /**
     * TraceId 전파를 지원하는 SqsTemplate.
     *
     * <p>메시지 발송 시 TraceIdHolder의 컨텍스트를 자동으로
     * 메시지 헤더(속성)에 추가합니다.</p>
     */
    @Bean
    public TracingSqsTemplate tracingSqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return new TracingSqsTemplate(sqsAsyncClient);
    }
}
