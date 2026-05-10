plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":platform-lib:core"))
    api("org.springframework.boot:spring-boot-starter-security")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")

    api("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "platform-lib-security"
            from(components["java"])
        }
    }
}
