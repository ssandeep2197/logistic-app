// Root build file — common config for every subproject.
//
// In `subprojects {}` with `apply(plugin = ...)`, the type-safe DSL accessors
// (like `dependencyManagement {}` or `java {}`) are NOT generated, so we use
// the explicit `configure<ExtensionType>()` form. The extension is registered
// the moment the plugin is applied, so this works without afterEvaluate.

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
    id("io.spring.dependency-management") version "1.1.6" apply false
    id("org.springframework.boot") version "3.3.4" apply false
}

allprojects {
    group = "com.handa.tms"
    version = (System.getenv("SERVICE_VERSION") ?: "0.1.0-SNAPSHOT")

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    configure<JavaPluginExtension> {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }

    // Imports BOMs for every subproject so they don't repeat versions.
    // - spring-boot-dependencies: every Spring artifact's version
    // - spring-cloud-dependencies: gateway, etc. (only api-gateway uses it)
    // - testcontainers-bom: aligns testcontainers + JUnit + jackson versions
    configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.4")
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
            mavenBom("org.testcontainers:testcontainers-bom:1.20.2")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // -Werror is intentionally OFF: Spring Boot 3.x emits a couple of
        // warnings under JDK 21 that we cannot suppress.  Re-enable per
        // service if you want stricter local code.
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
            showStackTraces = true
        }
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok:1.18.34")
        "annotationProcessor"("org.projectlombok:lombok:1.18.34")
        "testCompileOnly"("org.projectlombok:lombok:1.18.34")
        "testAnnotationProcessor"("org.projectlombok:lombok:1.18.34")
    }

    // Any subproject that publishes (i.e. platform-lib/*) writes resolved
    // versions into its POM.  Without this, BOM-managed deps would publish
    // with no version and downstream consumers couldn't resolve them.
    plugins.withId("maven-publish") {
        configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                versionMapping {
                    usage("java-api")     { fromResolutionOf("runtimeClasspath") }
                    usage("java-runtime") { fromResolutionResult() }
                }
            }
        }
    }
}
