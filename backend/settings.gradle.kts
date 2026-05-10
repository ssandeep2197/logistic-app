// Gradle multi-project root for ALL backend microservices.
// Each subproject below is independently deployable. CI uses path filters to
// detect which subproject(s) changed and only builds + deploys those.
// Shared code lives in :platform-lib and is published to the local Maven repo
// (or an internal registry in prod) — services depend on it as a normal artifact.

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Internal lib registry — replace with your own (Nexus / Artifactory / GitHub Packages).
        // mavenLocal() works for solo dev; prod CI publishes platform-lib to a registry.
        mavenLocal()
    }
}

rootProject.name = "tms-backend"

// Shared library — built and published, never deployed alone.
include(":platform-lib")
include(":platform-lib:core")
include(":platform-lib:security")
include(":platform-lib:web")
include(":platform-lib:observability")
include(":platform-lib:messaging")
include(":platform-lib:test-support")

// Independently deployable Spring Boot microservices.
include(":api-gateway")
include(":identity-service")
include(":operations-service")
include(":dispatch-service")
include(":finance-service")
include(":payroll-service")
include(":compliance-service")
include(":documents-service")
include(":reporting-service")
include(":notification-service")

// Note: tracking-service is Node + TypeScript — see backend/tracking-service.
// It is NOT a Gradle subproject; it has its own pnpm-managed build.

// Note: contracts/ holds Avro schemas + OpenAPI specs as a non-code asset
// directory; consumed by services via tasks in their build.gradle.kts.
