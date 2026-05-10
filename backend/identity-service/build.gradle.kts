plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

description = "Identity service — tenants, users, groups, roles, permissions, JWT issuance"

dependencies {
    // Shared platform code — this is what keeps the service slim.
    implementation(project(":platform-lib:core"))
    implementation(project(":platform-lib:security"))
    implementation(project(":platform-lib:web"))
    implementation(project(":platform-lib:observability"))
    implementation(project(":platform-lib:messaging"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    // Password hashing — Spring Security pulls bcrypt by default but argon2 is better.
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")

    testImplementation(project(":platform-lib:test-support"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.bootBuildImage {
    imageName.set("ghcr.io/helloworlds/identity-service:${project.version}")
    builder.set("paketobuildpacks/builder-jammy-base:latest")
}
