/**
 * observability-web
 *
 * HTTP 진입점용 모듈 (adapter-in REST API용)
 * Servlet API + Spring Web 의존
 *
 * 포함 기능:
 * - TraceIdFilter (TraceId 추출/생성)
 * - HttpLoggingFilter (요청/응답 로깅)
 * - PathNormalizer (URL 정규화)
 * - Gateway 헤더 처리 (X-User-Id, X-Tenant-Id 등)
 */

description = "Observability Web - HTTP Entry Point (adapter-in REST API용)"

dependencies {
    // Core 모듈 의존
    api(project(":observability-core"))

    // Servlet API
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    // Spring Web
    compileOnly("org.springframework:spring-web")
    compileOnly("org.springframework:spring-webmvc")

    // Spring Boot AutoConfiguration (선택적)
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
