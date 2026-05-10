plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

description = "Compliance service — IFTA, DOT Oregon WMT, HOS, IRP, KYU/NM/CT/NY HUT"

dependencies {
    implementation(project(":platform-lib:core"))
    implementation(project(":platform-lib:security"))
    implementation(project(":platform-lib:web"))
    implementation(project(":platform-lib:observability"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    // PostGIS via hibernate-spatial added when domain work begins (state-mile calc).

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
