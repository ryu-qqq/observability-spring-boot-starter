/**
 * observability-starter
 *
 * Spring Boot Starter - 전체 통합 모듈 (bootstrap용)
 * 모든 모듈을 포함하여 자동 설정 제공
 *
 * 사용자가 이 모듈만 의존하면 전체 기능 사용 가능
 * 또는 필요한 모듈만 선택적으로 의존 가능
 */

description = "Observability Spring Boot Starter - 전체 통합 (bootstrap용)"

dependencies {
    // 모든 모듈 포함
    api(project(":observability-core"))
    api(project(":observability-logging"))
    api(project(":observability-web"))
    api(project(":observability-client"))
    api(project(":observability-message"))

    // Spring Boot AutoConfiguration
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Spring Boot Web (기본 포함)
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-aop")

    // Logging
    api("org.slf4j:slf4j-api")
    compileOnly("ch.qos.logback:logback-classic")
    compileOnly("net.logstash.logback:logstash-logback-encoder:8.0")

    // Jackson
    compileOnly("com.fasterxml.jackson.core:jackson-databind")

    // Optional dependencies (사용자 환경에 따라 활성화)
    compileOnly("io.awspring.cloud:spring-cloud-aws-sqs:3.2.1")
    compileOnly("org.springframework.boot:spring-boot-starter-data-redis")
    compileOnly("org.springframework:spring-webflux")
    compileOnly("org.springframework.cloud:spring-cloud-openfeign-core:4.2.1")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-aop")
    testImplementation("net.logstash.logback:logstash-logback-encoder:8.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
