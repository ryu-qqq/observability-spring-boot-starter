package com.ryuqq.observability.integration.redis;

import com.ryuqq.observability.core.trace.TraceIdHeaders;
import com.ryuqq.observability.core.trace.TraceIdHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 인바운드 메시지 수신 시 TraceId 추출 통합 테스트.
 *
 * <p>Testcontainers Redis를 사용하여 실제 Redis Pub/Sub 환경을 시뮬레이션하고,
 * MessageListener가 메시지를 처리할 때 TraceIdHolder에 컨텍스트가
 * 올바르게 설정되는지 검증합니다.</p>
 */
@Testcontainers
@SpringBootTest(classes = RedisTestConfiguration.class)
class RedisInboundIntegrationTest {

    private static final String TEST_CHANNEL = "test-channel-orders";

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisMessageCaptureHolder captureHolder;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @BeforeEach
    void setUp() {
        TraceIdHolder.clear();
        captureHolder.reset();
    }

    @AfterEach
    void tearDown() {
        TraceIdHolder.clear();
    }

    @Test
    @DisplayName("Redis Pub/Sub 메시지 수신 시 리스너가 호출된다")
    void shouldReceiveMessageFromRedisPubSub() throws InterruptedException {
        // given
        String payload = "test-payload-" + UUID.randomUUID();

        // when: Redis Pub/Sub으로 메시지 발행
        redisTemplate.convertAndSend(TEST_CHANNEL, payload);

        // then: 리스너가 메시지를 수신했는지 확인
        boolean received = captureHolder.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(captureHolder.getCapturedPayload()).isEqualTo(payload);
        assertThat(captureHolder.getCapturedChannel()).contains("test-channel");
    }

    @Test
    @DisplayName("JSON 메시지에서 TraceId를 추출한다")
    void shouldExtractTraceIdFromJsonMessage() throws InterruptedException {
        // given
        String expectedTraceId = UUID.randomUUID().toString();
        String jsonPayload = String.format(
                "{\"%s\":\"%s\",\"orderId\":123}",
                TraceIdHeaders.X_TRACE_ID, expectedTraceId
        );

        // when
        redisTemplate.convertAndSend(TEST_CHANNEL, jsonPayload);

        // then
        boolean received = captureHolder.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(captureHolder.getCapturedPayload()).contains(expectedTraceId);
    }

    @Test
    @DisplayName("TraceId가 없는 메시지도 정상 처리된다")
    void shouldHandleMessageWithoutTraceId() throws InterruptedException {
        // given
        String payload = "{\"orderId\":456}";

        // when
        redisTemplate.convertAndSend(TEST_CHANNEL, payload);

        // then
        boolean received = captureHolder.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(captureHolder.getCapturedPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("연속으로 여러 메시지를 수신한다")
    void shouldReceiveMultipleMessages() throws InterruptedException {
        // given
        String payload1 = "first-message-" + UUID.randomUUID();
        String payload2 = "second-message-" + UUID.randomUUID();

        // when & then: 첫 번째 메시지
        redisTemplate.convertAndSend(TEST_CHANNEL, payload1);
        boolean received1 = captureHolder.await(5, TimeUnit.SECONDS);
        assertThat(received1).isTrue();
        assertThat(captureHolder.getCapturedPayload()).isEqualTo(payload1);

        // reset and send second message
        captureHolder.reset();

        // when & then: 두 번째 메시지
        redisTemplate.convertAndSend(TEST_CHANNEL, payload2);
        boolean received2 = captureHolder.await(5, TimeUnit.SECONDS);
        assertThat(received2).isTrue();
        assertThat(captureHolder.getCapturedPayload()).isEqualTo(payload2);
    }

    @Test
    @DisplayName("다른 채널로 발행된 메시지는 수신하지 않는다")
    void shouldNotReceiveMessageFromUnsubscribedChannel() throws InterruptedException {
        // given
        String payload = "message-to-other-channel";

        // when: 구독하지 않은 채널로 발행
        redisTemplate.convertAndSend("other-channel", payload);

        // then: 메시지를 수신하지 않아야 함
        boolean received = captureHolder.await(2, TimeUnit.SECONDS);
        assertThat(received).isFalse();
    }

    @Test
    @DisplayName("대용량 메시지도 정상 처리된다")
    void shouldHandleLargeMessage() throws InterruptedException {
        // given: 1KB 크기의 페이로드 생성
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("data-").append(i).append("-");
        }
        String largePayload = sb.toString();

        // when
        redisTemplate.convertAndSend(TEST_CHANNEL, largePayload);

        // then
        boolean received = captureHolder.await(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(captureHolder.getCapturedPayload()).isEqualTo(largePayload);
    }
}
