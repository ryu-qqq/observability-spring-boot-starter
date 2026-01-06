package com.ryuqq.observability.message.config;

/**
 * 메시지 큐 로깅 설정.
 *
 * <pre>
 * observability:
 *   message:
 *     enabled: true
 *     log-payload: false
 *     max-payload-length: 500
 * </pre>
 */
public class MessageLoggingProperties {

    /**
     * 메시지 로깅 활성화 여부
     */
    private boolean enabled = true;

    /**
     * 메시지 페이로드 로깅 여부 (민감정보 주의)
     */
    private boolean logPayload = false;

    /**
     * 페이로드 로깅 시 최대 길이
     */
    private int maxPayloadLength = 500;


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogPayload() {
        return logPayload;
    }

    public void setLogPayload(boolean logPayload) {
        this.logPayload = logPayload;
    }

    public int getMaxPayloadLength() {
        return maxPayloadLength;
    }

    public void setMaxPayloadLength(int maxPayloadLength) {
        this.maxPayloadLength = maxPayloadLength;
    }
}
