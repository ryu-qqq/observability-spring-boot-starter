package com.ryuqq.observability.integration.redis;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 아웃바운드 메시지 발행 시 TraceId 전파 통합 테스트.
 *
 * <p>Testcontainers Redis를 사용하여 실제 Redis 환경을 시뮬레이션하고,
 * TracingRedisTemplate으로 메시지 발행 시 TraceIdHolder의 컨텍스트가
 * 메시지에 올바르게 전파되는지 검증합니다.</p>
 */
@Testcontainers
@SpringBootTest(classes = RedisOutTestConfiguration.class)
class RedisOutboundIntegrationTest {

    private static final String TEST_CHANNEL = "test-outbound-channel";
    private static final String TEST_STREAM_KEY = "test-outbound-stream";

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private TracingRedisTemplate tracingRedisTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @BeforeEach
    void setUp() {
        TraceIdHolder.clear();
        // Stream이 존재하면 삭제
        try {
            tracingRedisTemplate.delete(TEST_STREAM_KEY);
        } catch (Exception ignored) {
        }
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Test
    @DisplayName("Pub/Sub 메시지 발행 시 TraceId가 메시지에 포함된다")
    void shouldPropagateTraceIdWhenPublishingPubSubMessage() throws InterruptedException {
        // given
        String expectedTraceId = UUID.randomUUID().toString();
        String expectedUserId = "user-out-123";
        String payload = "test-payload";

        TraceIdHolder.set(expectedTraceId);
        TraceIdHolder.setUserId(expectedUserId);

        AtomicReference<String> receivedMessage = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // Redis Pub/Sub 리스너 설정
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(tracingRedisTemplate.getConnectionFactory());
        container.addMessageListener((message, pattern) -> {
            receivedMessage.set(new String(message.getBody()));
            latch.countDown();
        }, new PatternTopic(TEST_CHANNEL));
        container.afterPropertiesSet();
        container.start();

        // when: TracingRedisTemplate으로 메시지 발행
        String publishedMessage = tracingRedisTemplate.convertAndSendWithTrace(TEST_CHANNEL, payload);

        // then: 발행된 메시지에 TraceId 포함 확인
        assertThat(publishedMessage).contains(expectedTraceId);
        assertThat(publishedMessage).contains(expectedUserId);
        assertThat(publishedMessage).contains(TraceIdHeaders.X_TRACE_ID);
        assertThat(publishedMessage).contains(TraceIdHeaders.X_USER_ID);

        // 리스너가 메시지를 수신했는지도 확인
        boolean received = latch.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(receivedMessage.get()).contains(expectedTraceId);

        container.stop();
    }

    @Test
    @DisplayName("Redis Stream 발행 시 TraceId가 필드에 포함된다")
    void shouldPropagateTraceIdWhenPublishingStreamRecord() {
        // given
        String expectedTraceId = UUID.randomUUID().toString();
        String expectedUserId = "stream-user-456";
        String expectedTenantId = "stream-tenant-789";
        String expectedOrgId = "stream-org-012";

        TraceIdHolder.set(expectedTraceId);
        TraceIdHolder.setUserId(expectedUserId);
        TraceIdHolder.setTenantId(expectedTenantId);
        TraceIdHolder.setOrganizationId(expectedOrgId);

        Map<String, String> payload = new HashMap<>();
        payload.put("orderId", "12345");
        payload.put("status", "CREATED");

        // when: TracingRedisTemplate으로 Stream 레코드 발행
        RecordId recordId = tracingRedisTemplate.addToStreamWithTrace(TEST_STREAM_KEY, payload);

        // then: Stream에서 레코드를 읽어 TraceId 포함 확인
        assertThat(recordId).isNotNull();

        List<MapRecord<String, Object, Object>> records = tracingRedisTemplate.opsForStream()
                .read(StreamOffset.fromStart(TEST_STREAM_KEY));

        assertThat(records).hasSize(1);
        Map<Object, Object> fields = records.get(0).getValue();

        assertThat(fields)
                .containsEntry(TraceIdHeaders.X_TRACE_ID, expectedTraceId)
                .containsEntry(TraceIdHeaders.X_USER_ID, expectedUserId)
                .containsEntry(TraceIdHeaders.X_TENANT_ID, expectedTenantId)
                .containsEntry(TraceIdHeaders.X_ORGANIZATION_ID, expectedOrgId)
                .containsEntry("orderId", "12345")
                .containsEntry("status", "CREATED");
    }

    @Test
    @DisplayName("TraceId가 없는 상태에서 메시지 발행 시 TraceId 필드가 없다")
    void shouldNotIncludeTraceIdWhenNotSet() {
        // given: TraceIdHolder가 비어있음
        Map<String, String> payload = new HashMap<>();
        payload.put("data", "test-data");

        // when
        RecordId recordId = tracingRedisTemplate.addToStreamWithTrace(TEST_STREAM_KEY, payload);

        // then
        assertThat(recordId).isNotNull();

        List<MapRecord<String, Object, Object>> records = tracingRedisTemplate.opsForStream()
                .read(StreamOffset.fromStart(TEST_STREAM_KEY));

        assertThat(records).hasSize(1);
        Map<Object, Object> fields = records.get(0).getValue();

        assertThat(fields).containsEntry("data", "test-data");
        assertThat(fields).doesNotContainKey(TraceIdHeaders.X_TRACE_ID);
        assertThat(fields).doesNotContainKey(TraceIdHeaders.X_USER_ID);
    }

    @Test
    @DisplayName("부분적인 컨텍스트만 설정해도 해당 속성만 전파된다")
    void shouldPropagatePartialContext() {
        // given: TraceId와 UserId만 설정
        String expectedTraceId = UUID.randomUUID().toString();
        String expectedUserId = "partial-user-id";

        TraceIdHolder.set(expectedTraceId);
        TraceIdHolder.setUserId(expectedUserId);

        Map<String, String> payload = new HashMap<>();
        payload.put("action", "test");

        // when
        RecordId recordId = tracingRedisTemplate.addToStreamWithTrace(TEST_STREAM_KEY, payload);

        // then
        List<MapRecord<String, Object, Object>> records = tracingRedisTemplate.opsForStream()
                .read(StreamOffset.fromStart(TEST_STREAM_KEY));

        assertThat(records).hasSize(1);
        Map<Object, Object> fields = records.get(0).getValue();

        assertThat(fields)
                .containsEntry(TraceIdHeaders.X_TRACE_ID, expectedTraceId)
                .containsEntry(TraceIdHeaders.X_USER_ID, expectedUserId);

        // TenantId와 OrganizationId는 설정하지 않았으므로 없어야 함
        assertThat(fields).doesNotContainKey(TraceIdHeaders.X_TENANT_ID);
        assertThat(fields).doesNotContainKey(TraceIdHeaders.X_ORGANIZATION_ID);
    }

    @Test
    @DisplayName("연속으로 여러 Stream 레코드를 발행한다")
    void shouldPublishMultipleStreamRecords() {
        // given
        String traceId1 = "trace-1-" + UUID.randomUUID();
        String traceId2 = "trace-2-" + UUID.randomUUID();

        // when: 첫 번째 메시지
        TraceIdHolder.set(traceId1);
        Map<String, String> payload1 = new HashMap<>();
        payload1.put("sequence", "1");
        tracingRedisTemplate.addToStreamWithTrace(TEST_STREAM_KEY, payload1);

        // 두 번째 메시지
        TraceIdHolder.clear();
        TraceIdHolder.set(traceId2);
        Map<String, String> payload2 = new HashMap<>();
        payload2.put("sequence", "2");
        tracingRedisTemplate.addToStreamWithTrace(TEST_STREAM_KEY, payload2);

        // then
        List<MapRecord<String, Object, Object>> records = tracingRedisTemplate.opsForStream()
                .read(StreamOffset.fromStart(TEST_STREAM_KEY));

        assertThat(records).hasSize(2);

        // 첫 번째 레코드
        Map<Object, Object> firstFields = records.get(0).getValue();
        assertThat(firstFields)
                .containsEntry("sequence", "1")
                .containsEntry(TraceIdHeaders.X_TRACE_ID, traceId1);

        // 두 번째 레코드
        Map<Object, Object> secondFields = records.get(1).getValue();
        assertThat(secondFields)
                .containsEntry("sequence", "2")
                .containsEntry(TraceIdHeaders.X_TRACE_ID, traceId2);
    }

    @Test
    @DisplayName("JSON 페이로드가 올바르게 포함된다")
    void shouldIncludeJsonPayloadCorrectly() {
        // given
        String expectedTraceId = UUID.randomUUID().toString();
        TraceIdHolder.set(expectedTraceId);

        String jsonPayload = "{\"orderId\":123,\"status\":\"PENDING\"}";

        // when
        String publishedMessage = tracingRedisTemplate.convertAndSendWithTrace(TEST_CHANNEL, jsonPayload);

        // then
        assertThat(publishedMessage).contains(expectedTraceId);
        assertThat(publishedMessage).contains("orderId");
        assertThat(publishedMessage).contains("123");
        assertThat(publishedMessage).contains("PENDING");
    }
}
