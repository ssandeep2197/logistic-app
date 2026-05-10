plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("io.micrometer:micrometer-registry-prometheus")
    api("io.micrometer:micrometer-tracing-bridge-otel")
    api("io.opentelemetry:opentelemetry-exporter-otlp:1.42.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "platform-lib-observability"
            from(components["java"])
        }
    }
}
