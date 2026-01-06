/**
 * observability-core
 *
 * 순수 Java 모듈 - Domain Layer에서 사용 가능
 * Spring 의존성 없음!
 *
 * 포함 기능:
 * - TraceIdHolder (MDC 관리)
 * - TraceIdHeaders (헤더 상수)
 * - LogMasker (민감정보 마스킹)
 * - 공통 유틸리티
 */

description = "Observability Core - Pure Java (Domain Layer 호환)"

dependencies {
    // 순수 Java - SLF4J API만 의존
    api("org.slf4j:slf4j-api")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("ch.qos.logback:logback-classic")
}

// Domain Layer 순수성 검증 태스크
tasks.register("verifyPureJava") {
    group = "verification"
    description = "Verify that this module has no Spring/JPA/Lombok dependencies"

    doLast {
        val forbiddenDependencies = listOf(
            "org.springframework",
            "jakarta.persistence",
            "org.hibernate",
            "org.projectlombok"
        )

        val allDeps = configurations.runtimeClasspath.get().resolvedConfiguration
            .resolvedArtifacts.map { it.moduleVersion.id.toString() }

        val violations = allDeps.filter { dep ->
            forbiddenDependencies.any { forbidden -> dep.contains(forbidden) }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "observability-core must be pure Java! Found forbidden dependencies:\n" +
                violations.joinToString("\n") { "  - $it" }
            )
        }

        println("✅ observability-core is pure Java - no Spring/JPA/Lombok dependencies")
    }
}

tasks.named("check") {
    dependsOn("verifyPureJava")
}
