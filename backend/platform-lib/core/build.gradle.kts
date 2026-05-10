plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("jakarta.persistence:jakarta.persistence-api")
    api("com.fasterxml.jackson.core:jackson-databind")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "platform-lib-core"
            from(components["java"])
        }
    }
}
