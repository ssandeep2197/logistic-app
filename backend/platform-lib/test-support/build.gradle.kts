plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":platform-lib:core"))
    api(project(":platform-lib:security"))
    api("org.springframework.boot:spring-boot-starter-test")
    api("org.testcontainers:junit-jupiter:1.20.2")
    api("org.testcontainers:postgresql:1.20.2")
    api("org.testcontainers:kafka:1.20.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "platform-lib-test-support"
            from(components["java"])
        }
    }
}
