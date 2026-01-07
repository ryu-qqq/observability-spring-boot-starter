package com.ryuqq.observability.integration.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstrap 통합 테스트용 애플리케이션.
 *
 * <p>observability-starter의 모든 자동 설정이 활성화됩니다.</p>
 */
@SpringBootApplication
public class BootstrapTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootstrapTestApplication.class, args);
    }
}
