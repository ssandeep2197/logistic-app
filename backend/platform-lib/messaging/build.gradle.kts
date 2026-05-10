plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":platform-lib:core"))
    api("org.springframework.kafka:spring-kafka")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("com.fasterxml.jackson.core:jackson-databind")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "platform-lib-messaging"
            from(components["java"])
        }
    }
}
