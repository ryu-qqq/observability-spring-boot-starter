package com.ryuqq.observability.starter;

import com.ryuqq.observability.client.feign.TraceIdFeignRequestInterceptor;
import com.ryuqq.observability.client.rest.TraceIdRestClientInterceptor;
import com.ryuqq.observability.client.rest.TraceIdRestTemplateInterceptor;
import com.ryuqq.observability.client.webclient.TraceIdExchangeFilterFunction;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * observability-client 모듈 자동 설정.
 *
 * <p>HTTP 클라이언트에 TraceId 전파 인터셉터를 자동으로 구성합니다.</p>
 */
@AutoConfiguration(after = ObservabilityCoreAutoConfiguration.class)
public class ObservabilityClientAutoConfiguration {

    /**
     * RestTemplate 인터셉터 설정.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.http.client.ClientHttpRequestInterceptor")
    static class RestTemplateConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TraceIdRestTemplateInterceptor traceIdRestTemplateInterceptor() {
            return new TraceIdRestTemplateInterceptor();
        }
    }

    /**
     * RestClient 인터셉터 설정 (Spring 6.1+).
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.http.client.ClientHttpRequestInitializer")
    static class RestClientConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TraceIdRestClientInterceptor traceIdRestClientInterceptor() {
            return new TraceIdRestClientInterceptor();
        }
    }

    /**
     * WebClient 필터 설정.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    static class WebClientConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TraceIdExchangeFilterFunction traceIdExchangeFilterFunction() {
            return new TraceIdExchangeFilterFunction();
        }
    }

    /**
     * OpenFeign 인터셉터 설정.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    static class FeignConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public TraceIdFeignRequestInterceptor traceIdFeignRequestInterceptor() {
            return new TraceIdFeignRequestInterceptor();
        }
    }
}
