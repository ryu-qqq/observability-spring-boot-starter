package com.ryuqq.observability.integration.sqs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 비동기 메시지 수신 검증을 위한 캡처 유틸리티.
 *
 * <p>리스너가 메시지를 처리할 때 컨텍스트 정보를 캡처하고,
 * 테스트에서 검증할 수 있도록 합니다.</p>
 */
public class TestMessageCaptureHolder {

    private final AtomicReference<String> capturedTraceId = new AtomicReference<>();
    private final AtomicReference<String> capturedUserId = new AtomicReference<>();
    private final AtomicReference<String> capturedTenantId = new AtomicReference<>();
    private final AtomicReference<String> capturedOrganizationId = new AtomicReference<>();
    private final AtomicReference<String> capturedPayload = new AtomicReference<>();
    private CountDownLatch latch = new CountDownLatch(1);

    public void capture(String traceId, String userId, String tenantId, String organizationId, String payload) {
        capturedTraceId.set(traceId);
        capturedUserId.set(userId);
        capturedTenantId.set(tenantId);
        capturedOrganizationId.set(organizationId);
        capturedPayload.set(payload);
        latch.countDown();
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

    public String getCapturedTraceId() {
        return capturedTraceId.get();
    }

    public String getCapturedUserId() {
        return capturedUserId.get();
    }

    public String getCapturedTenantId() {
        return capturedTenantId.get();
    }

    public String getCapturedOrganizationId() {
        return capturedOrganizationId.get();
    }

    public String getCapturedPayload() {
        return capturedPayload.get();
    }

    public void reset() {
        capturedTraceId.set(null);
        capturedUserId.set(null);
        capturedTenantId.set(null);
        capturedOrganizationId.set(null);
        capturedPayload.set(null);
        latch = new CountDownLatch(1);
    }
}
