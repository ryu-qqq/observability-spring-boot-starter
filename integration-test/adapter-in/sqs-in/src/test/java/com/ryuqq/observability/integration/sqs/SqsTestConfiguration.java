package com.ryuqq.observability.integration.sqs;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.message.config.MessageLoggingProperties;
import com.ryuqq.observability.message.interceptor.MessageLoggingInterceptor;
import com.ryuqq.observability.message.sqs.SqsMessageLoggingAspect;
import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;
import java.util.UUID;

/**
 * SQS 인바운드 통합 테스트를 위한 Spring 설정.
 *
 * <p>LocalStack SQS와 연동하기 위한 빈 설정과
 * observability-message 모듈의 SqsMessageLoggingAspect를 포함합니다.</p>
 */
@Configuration
@EnableAutoConfiguration
@EnableAspectJAutoProxy
@Import(SqsBootstrapConfiguration.class)
public class SqsTestConfiguration {

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

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }

    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
            SqsAsyncClient sqsAsyncClient) {
        return SqsMessageListenerContainerFactory.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
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
                "sqs-in-test"
        );
    }

    @Bean
    public SqsMessageLoggingAspect sqsMessageLoggingAspect(
            MessageLoggingInterceptor interceptor) {
        return new SqsMessageLoggingAspect(interceptor);
    }

    @Bean
    public TestMessageCaptureHolder testMessageCaptureHolder() {
        return new TestMessageCaptureHolder();
    }

    @Bean
    public SqsTestListener sqsTestListener(TestMessageCaptureHolder captureHolder) {
        return new SqsTestListener(captureHolder);
    }
}
