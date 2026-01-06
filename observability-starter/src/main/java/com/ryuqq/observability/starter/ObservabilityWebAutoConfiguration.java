package com.ryuqq.observability.starter;

import com.ryuqq.observability.core.masking.LogMasker;
import com.ryuqq.observability.web.config.HttpLoggingProperties;
import com.ryuqq.observability.web.config.TraceProperties;
import com.ryuqq.observability.web.http.HttpLoggingFilter;
import com.ryuqq.observability.web.http.PathNormalizer;
import com.ryuqq.observability.web.trace.DefaultTraceIdProvider;
import com.ryuqq.observability.web.trace.TraceIdFilter;
import com.ryuqq.observability.web.trace.TraceIdProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * observability-web 모듈 자동 설정.
 *
 * <p>HTTP 요청/응답 로깅 및 TraceId 필터를 자동으로 구성합니다.</p>
 */
@AutoConfiguration(after = ObservabilityCoreAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "jakarta.servlet.Filter")
public class ObservabilityWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TraceIdProvider traceIdProvider(ObservabilityProperties properties) {
        return new DefaultTraceIdProvider(properties.getTrace().getHeaderNames());
    }

    @Bean
    @ConditionalOnMissingBean
    public PathNormalizer pathNormalizer() {
        return new PathNormalizer();
    }

    @Bean
    @ConditionalOnProperty(prefix = "observability.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(
            TraceIdProvider traceIdProvider,
            ObservabilityProperties properties) {

        TraceProperties traceProps = properties.getTrace();
        TraceIdFilter filter = new TraceIdFilter(
                traceIdProvider,
                traceProps,
                properties.getServiceName()
        );

        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");

        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "observability.http", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<HttpLoggingFilter> httpLoggingFilterRegistration(
            LogMasker logMasker,
            PathNormalizer pathNormalizer,
            ObservabilityProperties properties) {

        HttpLoggingProperties httpProps = properties.getHttp();
        HttpLoggingFilter filter = new HttpLoggingFilter(httpProps, pathNormalizer, logMasker);

        FilterRegistrationBean<HttpLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10); // TraceIdFilter 다음
        registration.addUrlPatterns("/*");
        registration.setName("httpLoggingFilter");

        return registration;
    }
}
