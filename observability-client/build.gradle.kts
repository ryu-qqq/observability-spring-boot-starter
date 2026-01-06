/**
 * observability-client
 *
 * HTTP 클라이언트 TraceId 전파 모듈 (adapter-out HTTP용)
 * RestClient, WebClient, Feign 지원
 *
 * 포함 기능:
 * - RestClient Interceptor
 * - WebClient ExchangeFilterFunction
 * - Feign RequestInterceptor
 * - 응답 로깅 (선택적)
 */

description = "Observability Client - HTTP Client TraceId 전파 (adapter-out HTTP용)"

dependencies {
    // Core 모듈 의존
    api(project(":observability-core"))

    // Spring Web (RestClient, RestTemplate)
    compileOnly("org.springframework:spring-web")

    // Spring WebFlux (WebClient) - 선택적
    compileOnly("org.springframework:spring-webflux")

    // OpenFeign - 선택적
    compileOnly("io.github.openfeign:feign-core:13.5")

    // Reactor (WebClient용) - 선택적
    compileOnly("io.projectreactor:reactor-core")

    // Spring Boot AutoConfiguration
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework:spring-webflux")
    testImplementation("io.projectreactor:reactor-core")
    testImplementation("io.github.openfeign:feign-core:13.5")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
