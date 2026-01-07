package com.ryuqq.observability.webflux.config;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.webflux.http.ReactiveHttpLoggingFilter;
import com.ryuqq.observability.webflux.http.ReactivePathNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.WebFilter;

/**
 * WebFlux HTTP 로깅 자동 설정.
 *
 * <p>Spring WebFlux 환경에서만 활성화되며, 다음 기능을 제공합니다:</p>
 * <ul>
 *   <li>ReactiveHttpLoggingFilter - HTTP 요청/응답 로깅 WebFilter</li>
 *   <li>ReactivePathNormalizer - URL 경로 정규화</li>
 *   <li>LogMasker - 민감정보 마스킹 (observability-core 의존)</li>
 * </ul>
 *
 * <p>설정 예시:</p>
 * <pre>
 * observability:
 *   reactive-http:
 *     enabled: true
 *     log-request-body: false
 *     log-response-body: false
 *     max-body-length: 1000
 *     slow-request-threshold-ms: 3000
 * </pre>
 *
 * <p>이 설정은 {@link WebFluxTraceAutoConfiguration} 이후에 적용되어야 합니다.
 * HTTP 로깅 필터는 TraceId 필터 다음에 실행됩니다.</p>
 */
@AutoConfiguration(after = WebFluxTraceAutoConfiguration.class)
@ConditionalOnClass({WebFilter.class, WebFluxConfigurer.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "observability.reactive-http", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties({ReactiveHttpLoggingProperties.class, ReactiveMaskingProperties.class})
public class WebFluxHttpLoggingAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WebFluxHttpLoggingAutoConfiguration.class);

    /**
     * ReactivePathNormalizer를 등록합니다.
     *
     * <p>커스텀 구현이 있으면 대체됩니다.</p>
     *
     * @param properties HTTP 로깅 설정
     * @return ReactivePathNormalizer 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactivePathNormalizer reactivePathNormalizer(ReactiveHttpLoggingProperties properties) {
        log.debug("Creating ReactivePathNormalizer with {} custom patterns",
                properties.getPathPatterns().size());
        return new ReactivePathNormalizer(properties.getPathPatterns());
    }

    /**
     * LogMasker를 등록합니다.
     *
     * <p>observability-core 모듈의 LogMasker를 재사용합니다.
     * 커스텀 구현이 있으면 대체됩니다.</p>
     *
     * @param maskingProperties 마스킹 설정
     * @return LogMasker 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public LogMasker logMasker(ReactiveMaskingProperties maskingProperties) {
        log.debug("Creating LogMasker with masking enabled: {}", maskingProperties.isEnabled());
        return new LogMasker(maskingProperties);
    }

    /**
     * ReactiveHttpLoggingFilter를 등록합니다.
     *
     * <p>ReactiveTraceIdFilter 다음에 실행됩니다 (ORDER = HIGHEST_PRECEDENCE + 200).</p>
     *
     * @param properties     HTTP 로깅 설정
     * @param pathNormalizer 경로 정규화기
     * @param logMasker      민감정보 마스킹 유틸리티
     * @return ReactiveHttpLoggingFilter 인스턴스
     */
    @Bean
    @ConditionalOnMissingBean
    public ReactiveHttpLoggingFilter reactiveHttpLoggingFilter(ReactiveHttpLoggingProperties properties,
                                                               ReactivePathNormalizer pathNormalizer,
                                                               LogMasker logMasker) {
        log.info("Registering ReactiveHttpLoggingFilter for WebFlux application. " +
                        "logRequestBody={}, logResponseBody={}, maxBodyLength={}, slowThreshold={}ms",
                properties.isLogRequestBody(),
                properties.isLogResponseBody(),
                properties.getMaxBodyLength(),
                properties.getSlowRequestThresholdMs());

        return new ReactiveHttpLoggingFilter(properties, pathNormalizer, logMasker);
    }
}
