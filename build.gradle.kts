plugins {
    id("java-library")
    id("maven-publish")
    id("jacoco")
    id("org.springframework.boot") version "3.5.0" apply false
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.ryuqq"
version = "1.0.0-SNAPSHOT"

// 모든 프로젝트 공통 설정
allprojects {
    repositories {
        mavenCentral()
    }
}

// 하위 모듈 공통 설정
subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")

    group = rootProject.group
    version = rootProject.version

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
        withJavadocJar()
        withSourcesJar()
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.0")
        }
    }

    dependencies {
        // 테스트 공통 의존성 (Spring Boot BOM에서 버전 관리)
        "testImplementation"(platform("org.junit:junit-bom:5.10.3"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
        "testImplementation"("org.assertj:assertj-core")
        "testImplementation"("org.mockito:mockito-core")
        "testImplementation"("org.mockito:mockito-junit-jupiter")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.withType<JacocoReport> {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.withType<JacocoCoverageVerification> {
        violationRules {
            rule {
                limit {
                    minimum = "0.95".toBigDecimal()
                }
            }
        }
    }

    // Javadoc 설정
    tasks.withType<Javadoc> {
        options {
            (this as StandardJavadocDocletOptions).apply {
                addStringOption("Xdoclint:none", "-quiet")
                encoding = "UTF-8"
                charSet = "UTF-8"
            }
        }
    }

    // Maven Publishing 공통 설정
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {
                    url = "https://github.com/ryu-qqq/observability-spring-boot-starter"

                    licenses {
                        license {
                            name = "MIT License"
                            url = "https://opensource.org/licenses/MIT"
                        }
                    }

                    developers {
                        developer {
                            id = "ryuqq"
                            name = "Sangwon Ryu"
                        }
                    }

                    scm {
                        connection = "scm:git:git://github.com/ryu-qqq/observability-spring-boot-starter.git"
                        developerConnection = "scm:git:ssh://github.com/ryu-qqq/observability-spring-boot-starter.git"
                        url = "https://github.com/ryu-qqq/observability-spring-boot-starter"
                    }
                }
            }
        }
    }
}

// 루트 프로젝트는 빌드하지 않음
tasks.named<Jar>("jar") {
    enabled = false
}

tasks.findByName("javadocJar")?.let {
    it.enabled = false
}

tasks.findByName("sourcesJar")?.let {
    it.enabled = false
}
