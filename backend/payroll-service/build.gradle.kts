plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

description = "Payroll service — driver pay calc, settlements, W-2/1099, deductions"

dependencies {
    implementation(project(":platform-lib:core"))
    implementation(project(":platform-lib:security"))
    implementation(project(":platform-lib:web"))
    implementation(project(":platform-lib:observability"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    // Spring Batch for the weekly payroll run when implementation begins.

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
