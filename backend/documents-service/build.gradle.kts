plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

description = "Documents service — uploads (S3), OCR + Claude rate-confirmation extraction"

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
    //   implementation("software.amazon.awssdk:s3:2.28.x")
    //   implementation("org.apache.pdfbox:pdfbox:3.0.x")
    //   implementation("com.anthropic:anthropic-java:x.y.z")  // for Claude extraction

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
