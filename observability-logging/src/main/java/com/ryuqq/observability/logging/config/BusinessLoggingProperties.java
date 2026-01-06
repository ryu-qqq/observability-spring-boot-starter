package com.ryuqq.observability.logging.config;

/**
 * 비즈니스 로깅 설정.
 *
 * <pre>
 * observability:
 *   logging:
 *     business:
 *       enabled: true
 *       log-arguments: false
 *       log-result: false
 *       log-execution-time: true
 * </pre>
 */
public class BusinessLoggingProperties {

    /**
     * 비즈니스 로깅 활성화 여부
     */
    private boolean enabled = true;

    /**
     * 메서드 인자 로깅 여부
     */
    private boolean logArguments = false;

    /**
     * 메서드 결과 로깅 여부
     */
    private boolean logResult = false;

    /**
     * 실행 시간 로깅 여부
     */
    private boolean logExecutionTime = true;

    /**
     * 느린 실행 임계값 (밀리초)
     */
    private long slowExecutionThreshold = 1000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogArguments() {
        return logArguments;
    }

    public void setLogArguments(boolean logArguments) {
        this.logArguments = logArguments;
    }

    public boolean isLogResult() {
        return logResult;
    }

    public void setLogResult(boolean logResult) {
        this.logResult = logResult;
    }

    public boolean isLogExecutionTime() {
        return logExecutionTime;
    }

    public void setLogExecutionTime(boolean logExecutionTime) {
        this.logExecutionTime = logExecutionTime;
    }

    public long getSlowExecutionThreshold() {
        return slowExecutionThreshold;
    }

    public void setSlowExecutionThreshold(long slowExecutionThreshold) {
        this.slowExecutionThreshold = slowExecutionThreshold;
    }
}
