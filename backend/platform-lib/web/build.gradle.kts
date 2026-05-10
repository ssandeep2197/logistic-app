plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":platform-lib:core"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "platform-lib-web"
            from(components["java"])
        }
    }
}
