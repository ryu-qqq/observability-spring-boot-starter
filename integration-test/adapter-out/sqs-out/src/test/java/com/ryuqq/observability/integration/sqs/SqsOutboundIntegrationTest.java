package com.ryuqq.observability.integration.sqs;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * SQS 아웃바운드 메시지 발행 시 TraceId 전파 통합 테스트.
 *
 * <p>LocalStack을 사용하여 실제 SQS 환경을 시뮬레이션하고,
 * SqsTemplate으로 메시지 발행 시 TraceIdHolder의 컨텍스트가
 * 메시지 속성으로 올바르게 전파되는지 검증합니다.</p>
 */
@Testcontainers
@SpringBootTest(classes = SqsOutTestConfiguration.class)
class SqsOutboundIntegrationTest {

    private static final String TEST_QUEUE_NAME = "test-outbound-queue";

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(SQS);

    @Autowired
    private TracingSqsTemplate tracingSqsTemplate;

    private SqsClient sqsClient;
    private String queueUrl;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.credentials.access-key", () -> "test");
        registry.add("spring.cloud.aws.credentials.secret-key", () -> "test");
        registry.add("spring.cloud.aws.region.static", () -> localStack.getRegion());
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localStack.getEndpointOverride(SQS).toString());
    }

    @BeforeEach
    void setUp() {
        TraceIdHolder.clear();

        sqsClient = SqsClient.builder()
                .endpointOverride(localStack.getEndpointOverride(SQS))
                .region(Region.of(localStack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();

        // 테스트 큐 생성
        try {
            sqsClient.createQueue(CreateQueueRequest.builder()
                    .queueName(TEST_QUEUE_NAME)
                    .build());
        } catch (Exception e) {
            // 큐가 이미 존재하는 경우 무시
        }

        queueUrl = sqsClient.getQueueUrl(r -> r.queueName(TEST_QUEUE_NAME)).queueUrl();
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Test
    @DisplayName("SqsTemplate으로 메시지 발행 시 TraceId가 메시지 속성에 포함된다")
    void shouldPropagateTraceIdWhenSendingMessage() {
        // given
        String expectedTraceId = UUID.randomUUID().toString();
        String expectedUserId = "user-out-123";
        String expectedTenantId = "tenant-out-456";
        String expectedOrgId = "org-out-789";
        String payload = "test-outbound-payload";

        TraceIdHolder.set(expectedTraceId);
        TraceIdHolder.setUserId(expectedUserId);
        TraceIdHolder.setTenantId(expectedTenantId);
        TraceIdHolder.setOrganizationId(expectedOrgId);

        // when: TracingSqsTemplate으로 메시지 발행
        tracingSqsTemplate.send(TEST_QUEUE_NAME, payload);

        // then: 큐에서 메시지를 받아 속성 검증
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageAttributeNames("All")
                        .waitTimeSeconds(5)
                        .maxNumberOfMessages(1)
                        .build())
                .messages();

        assertThat(messages).hasSize(1);

        Message message = messages.get(0);
        assertThat(message.body()).contains(payload);

        // 메시지 속성에 TraceId 헤더가 포함되어 있는지 검증
        assertThat(message.messageAttributes())
                .containsKey(TraceIdHeaders.X_TRACE_ID);
        assertThat(message.messageAttributes().get(TraceIdHeaders.X_TRACE_ID).stringValue())
                .isEqualTo(expectedTraceId);

        assertThat(message.messageAttributes())
                .containsKey(TraceIdHeaders.X_USER_ID);
        assertThat(message.messageAttributes().get(TraceIdHeaders.X_USER_ID).stringValue())
                .isEqualTo(expectedUserId);

        assertThat(message.messageAttributes())
                .containsKey(TraceIdHeaders.X_TENANT_ID);
        assertThat(message.messageAttributes().get(TraceIdHeaders.X_TENANT_ID).stringValue())
                .isEqualTo(expectedTenantId);

        assertThat(message.messageAttributes())
                .containsKey(TraceIdHeaders.X_ORGANIZATION_ID);
        assertThat(message.messageAttributes().get(TraceIdHeaders.X_ORGANIZATION_ID).stringValue())
                .isEqualTo(expectedOrgId);
    }

    @Test
    @DisplayName("TraceId가 없는 상태에서 메시지 발행 시 TraceId 속성이 없다")
    void shouldNotIncludeTraceIdWhenNotSet() {
        // given: TraceIdHolder가 비어있음
        String payload = "message-without-trace";

        // when
        tracingSqsTemplate.send(TEST_QUEUE_NAME, payload);

        // then
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageAttributeNames("All")
                        .waitTimeSeconds(5)
                        .maxNumberOfMessages(1)
                        .build())
                .messages();

        assertThat(messages).hasSize(1);
        Message message = messages.get(0);

        // TraceId 속성이 없거나 null이어야 함
        assertThat(message.messageAttributes().get(TraceIdHeaders.X_TRACE_ID))
                .isNull();
    }

    @Test
    @DisplayName("부분적인 컨텍스트만 설정해도 해당 속성만 전파된다")
    void shouldPropagatePartialContext() {
        // given: TraceId와 UserId만 설정
        String expectedTraceId = UUID.randomUUID().toString();
        String expectedUserId = "partial-user-id";
        String payload = "partial-context-message";

        TraceIdHolder.set(expectedTraceId);
        TraceIdHolder.setUserId(expectedUserId);

        // when
        tracingSqsTemplate.send(TEST_QUEUE_NAME, payload);

        // then
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageAttributeNames("All")
                        .waitTimeSeconds(5)
                        .maxNumberOfMessages(1)
                        .build())
                .messages();

        assertThat(messages).hasSize(1);
        Message message = messages.get(0);

        assertThat(message.messageAttributes().get(TraceIdHeaders.X_TRACE_ID).stringValue())
                .isEqualTo(expectedTraceId);
        assertThat(message.messageAttributes().get(TraceIdHeaders.X_USER_ID).stringValue())
                .isEqualTo(expectedUserId);

        // TenantId와 OrganizationId는 설정하지 않았으므로 없어야 함
        assertThat(message.messageAttributes().get(TraceIdHeaders.X_TENANT_ID))
                .isNull();
        assertThat(message.messageAttributes().get(TraceIdHeaders.X_ORGANIZATION_ID))
                .isNull();
    }
}
