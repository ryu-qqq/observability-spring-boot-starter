/**
 * observability-logging
 *
 * Application Layer용 로깅 모듈
 * Spring Context 의존 (ApplicationEventPublisher 등)
 *
 * 포함 기능:
 * - @LogEvent 어노테이션 (선택적)
 * - Domain Event 자동 로깅
 * - 구조화된 비즈니스 로깅
 * - Logback 기본 설정
 */

description = "Observability Logging - Application Layer용"

dependencies {
    // Core 모듈 의존
    api(project(":observability-core"))

    // Spring Context (ApplicationEvent 지원)
    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework:spring-aop")
    compileOnly("org.springframework:spring-expression")
    compileOnly("org.aspectj:aspectjweaver")

    // Logback 설정
    compileOnly("ch.qos.logback:logback-classic")
    compileOnly("net.logstash.logback:logstash-logback-encoder:8.0")

    // Jackson (JSON 로깅)
    compileOnly("com.fasterxml.jackson.core:jackson-databind")

    // Test
    testImplementation("org.springframework:spring-context")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-aop")
    testImplementation("org.springframework:spring-expression")
    testImplementation("org.aspectj:aspectjweaver")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("ch.qos.logback:logback-classic")
}
