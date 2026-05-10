plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

description = "Finance service — A/R invoicing, A/P, settlements, GL posting"

dependencies {
    implementation(project(":platform-lib:core"))
    implementation(project(":platform-lib:security"))
    implementation(project(":platform-lib:web"))
    implementation(project(":platform-lib:observability"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
