package com.ryuqq.observability.starter;

import com.ryuqq.observability.core.masking.MaskingProperties;
import com.ryuqq.observability.logging.config.BusinessLoggingProperties;
import com.ryuqq.observability.message.config.MessageLoggingProperties;
import com.ryuqq.observability.web.config.HttpLoggingProperties;
import com.ryuqq.observability.web.config.TraceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Observability 통합 설정.
 *
 * <pre>
 * observability:
 *   service-name: my-service
 *   trace:
 *     enabled: true
 *   http:
 *     enabled: true
 *     exclude-paths:
 *       - /health
 *       - /actuator/**
 *   message:
 *     enabled: true
 *   logging:
 *     business:
 *       enabled: true
 *   masking:
 *     enabled: true
 *     patterns:
 *       email: "(?i)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
 * </pre>
 */
@ConfigurationProperties(prefix = "observability")
public class ObservabilityProperties {

    /**
     * 서비스 이름 (로그에 포함)
     */
    private String serviceName = "unknown";

    /**
     * Trace 설정
     */
    @NestedConfigurationProperty
    private TraceProperties trace = new TraceProperties();

    /**
     * HTTP 로깅 설정
     */
    @NestedConfigurationProperty
    private HttpLoggingProperties http = new HttpLoggingProperties();

    /**
     * 메시지 로깅 설정
     */
    @NestedConfigurationProperty
    private MessageLoggingProperties message = new MessageLoggingProperties();

    /**
     * 비즈니스 로깅 설정
     */
    @NestedConfigurationProperty
    private BusinessLoggingProperties logging = new BusinessLoggingProperties();

    /**
     * 마스킹 설정
     */
    @NestedConfigurationProperty
    private MaskingProperties masking = new MaskingProperties();

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public TraceProperties getTrace() {
        return trace;
    }

    public void setTrace(TraceProperties trace) {
        this.trace = trace;
    }

    public HttpLoggingProperties getHttp() {
        return http;
    }

    public void setHttp(HttpLoggingProperties http) {
        this.http = http;
    }

    public MessageLoggingProperties getMessage() {
        return message;
    }

    public void setMessage(MessageLoggingProperties message) {
        this.message = message;
    }

    public BusinessLoggingProperties getLogging() {
        return logging;
    }

    public void setLogging(BusinessLoggingProperties logging) {
        this.logging = logging;
    }

    public MaskingProperties getMasking() {
        return masking;
    }

    public void setMasking(MaskingProperties masking) {
        this.masking = masking;
    }
}
