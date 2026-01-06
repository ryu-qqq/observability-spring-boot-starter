/**
 * observability-message
 *
 * 메시지 큐 로깅 모듈 (adapter-in/out 메시지용)
 * SQS, Redis Pub/Sub, Redis Stream 지원
 *
 * 포함 기능:
 * - SQS Listener AOP 로깅 (adapter-in)
 * - SQS Template TraceId 전파 (adapter-out)
 * - Redis MessageListener AOP 로깅 (adapter-in)
 * - Redis Template TraceId 전파 (adapter-out)
 * - 메시지 속성에서 TraceId 추출
 */

description = "Observability Message - SQS/Redis (adapter-in/out 메시지용)"

dependencies {
    // Core 모듈 의존
    api(project(":observability-core"))

    // Spring AOP
    compileOnly("org.springframework:spring-aop")
    compileOnly("org.aspectj:aspectjweaver")

    // AWS SQS (Spring Cloud AWS) - 선택적
    compileOnly("io.awspring.cloud:spring-cloud-aws-sqs:3.2.1")

    // Spring Data Redis - 선택적
    compileOnly("org.springframework.data:spring-data-redis")
    compileOnly("org.springframework.boot:spring-boot-starter-data-redis")

    // Spring Boot AutoConfiguration
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-aop")
    testImplementation("org.springframework.data:spring-data-redis")
    testImplementation("io.awspring.cloud:spring-cloud-aws-sqs:3.2.1")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
