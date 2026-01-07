package com.ryuqq.observability.integration.sqs;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * SQS 인바운드 메시지에서 TraceId 추출 통합 테스트.
 *
 * <p>LocalStack을 사용하여 실제 SQS 환경을 시뮬레이션하고,
 * 메시지 헤더에서 TraceId가 올바르게 추출되어 TraceIdHolder에 설정되는지 검증합니다.</p>
 */
@Testcontainers
@SpringBootTest(classes = SqsTestConfiguration.class)
class SqsInboundIntegrationTest {

    private static final String TEST_QUEUE_NAME = "test-queue";

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(SQS);

    @Autowired
    private TestMessageCaptureHolder captureHolder;

    @Autowired
    private SqsTemplate sqsTemplate;

    private SqsClient sqsClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.credentials.access-key", () -> "test");
        registry.add("spring.cloud.aws.credentials.secret-key", () -> "test");
        registry.add("spring.cloud.aws.region.static", () -> localStack.getRegion());
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localStack.getEndpointOverride(SQS).toString());
        registry.add("test.sqs.queue-name", () -> TEST_QUEUE_NAME);
    }

    @BeforeEach
    void setUp() {
        captureHolder.reset();

        sqsClient = SqsClient.builder()
                .endpointOverride(localStack.getEndpointOverride(SQS))
                .region(Region.of(localStack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();

        // 테스트 큐 생성 (이미 존재하면 무시)
        try {
            sqsClient.createQueue(CreateQueueRequest.builder()
                    .queueName(TEST_QUEUE_NAME)
                    .build());
        } catch (Exception e) {
            // 큐가 이미 존재하는 경우 무시
        }
    }

    @Test
    @DisplayName("SQS 메시지에서 TraceId 헤더를 추출하여 TraceIdHolder에 설정한다")
    void shouldExtractTraceIdFromSqsMessageHeader() throws InterruptedException {
        // given
        String expectedTraceId = UUID.randomUUID().toString();
        String expectedUserId = "user-123";
        String expectedTenantId = "tenant-456";
        String expectedOrgId = "org-789";
        String payload = "test-message-payload";

        String queueUrl = sqsClient.getQueueUrl(r -> r.queueName(TEST_QUEUE_NAME)).queueUrl();

        // when: TraceId 헤더가 포함된 메시지 전송
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(payload)
                .messageAttributes(Map.of(
                        TraceIdHeaders.X_TRACE_ID, MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(expectedTraceId)
                                .build(),
                        TraceIdHeaders.X_USER_ID, MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(expectedUserId)
                                .build(),
                        TraceIdHeaders.X_TENANT_ID, MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(expectedTenantId)
                                .build(),
                        TraceIdHeaders.X_ORGANIZATION_ID, MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(expectedOrgId)
                                .build()
                ))
                .build());

        // then: 리스너가 메시지를 수신하고 TraceIdHolder에 컨텍스트를 설정했는지 검증
        boolean messageReceived = captureHolder.await(10, TimeUnit.SECONDS);

        assertThat(messageReceived).isTrue();
        assertThat(captureHolder.getCapturedTraceId()).isEqualTo(expectedTraceId);
        assertThat(captureHolder.getCapturedUserId()).isEqualTo(expectedUserId);
        assertThat(captureHolder.getCapturedTenantId()).isEqualTo(expectedTenantId);
        assertThat(captureHolder.getCapturedOrganizationId()).isEqualTo(expectedOrgId);
        assertThat(captureHolder.getCapturedPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("TraceId 헤더가 없는 메시지는 새로운 TraceId를 생성하거나 null로 처리한다")
    void shouldHandleMessageWithoutTraceIdHeader() throws InterruptedException {
        // given
        String payload = "message-without-trace-id";

        String queueUrl = sqsClient.getQueueUrl(r -> r.queueName(TEST_QUEUE_NAME)).queueUrl();

        // when: TraceId 헤더 없이 메시지 전송
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(payload)
                .build());

        // then: 메시지 수신 확인
        boolean messageReceived = captureHolder.await(10, TimeUnit.SECONDS);

        assertThat(messageReceived).isTrue();
        assertThat(captureHolder.getCapturedPayload()).isEqualTo(payload);
        // TraceId가 없는 경우의 처리는 구현에 따라 다를 수 있음
        // (새로 생성하거나 null)
    }

    @Test
    @DisplayName("여러 메시지를 순차적으로 처리하면서 각각의 TraceId를 올바르게 추출한다")
    void shouldExtractCorrectTraceIdForEachMessage() throws InterruptedException {
        // given
        String traceId1 = UUID.randomUUID().toString();
        String traceId2 = UUID.randomUUID().toString();

        String queueUrl = sqsClient.getQueueUrl(r -> r.queueName(TEST_QUEUE_NAME)).queueUrl();

        // when: 첫 번째 메시지 전송 및 검증
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody("message-1")
                .messageAttributes(Map.of(
                        TraceIdHeaders.X_TRACE_ID, MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(traceId1)
                                .build()
                ))
                .build());

        boolean firstReceived = captureHolder.await(10, TimeUnit.SECONDS);
        assertThat(firstReceived).isTrue();
        assertThat(captureHolder.getCapturedTraceId()).isEqualTo(traceId1);
        assertThat(captureHolder.getCapturedPayload()).isEqualTo("message-1");

        // 두 번째 메시지를 위해 캡처 홀더 리셋
        captureHolder.reset();

        // when: 두 번째 메시지 전송 및 검증
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody("message-2")
                .messageAttributes(Map.of(
                        TraceIdHeaders.X_TRACE_ID, MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(traceId2)
                                .build()
                ))
                .build());

        boolean secondReceived = captureHolder.await(10, TimeUnit.SECONDS);
        assertThat(secondReceived).isTrue();
        assertThat(captureHolder.getCapturedTraceId()).isEqualTo(traceId2);
        assertThat(captureHolder.getCapturedPayload()).isEqualTo("message-2");
    }
}
