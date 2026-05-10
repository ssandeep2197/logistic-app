plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

description = "Reporting service — canned reports + custom report builder, scheduled exports"

dependencies {
    implementation(project(":platform-lib:core"))
    implementation(project(":platform-lib:security"))
    implementation(project(":platform-lib:web"))
    implementation(project(":platform-lib:observability"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    // Add when implementation begins:
    //   implementation("org.jooq:jooq:3.19.x")    // safe dynamic SQL for custom report builder
    //   implementation("org.apache.poi:poi-ooxml:5.x") // XLSX export

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
