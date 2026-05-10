# platform-lib — internal Spring Boot starter

The reason every backend service in this repo is ~200 LOC of plumbing instead of 5,000.

## Modules

- `core/` — `BaseEntity`, `AuditableEntity`, `TenantContext`, `OutboxEvent`, common error types
- `security/` — JWT validation filter, `TenantContextFilter` that sets the Postgres RLS GUC, RBAC `PermissionEvaluator`
- `web/` — RFC-7807 `ProblemDetail` exception handler, request-id filter, OpenAPI config
- `observability/` — Micrometer + OpenTelemetry auto-config, MDC propagation
- `messaging/` — Kafka producer wrapper with idempotency keys, outbox dispatcher
- `test-support/` — `@TmsIntegrationTest` annotation, Testcontainers Postgres fixture, `MockJwt` helper

## Using it in a service

`build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.handa.tms:platform-lib-security:0.1.0-SNAPSHOT")
    implementation("com.handa.tms:platform-lib-web:0.1.0-SNAPSHOT")
    implementation("com.handa.tms:platform-lib-observability:0.1.0-SNAPSHOT")
    implementation("com.handa.tms:platform-lib-messaging:0.1.0-SNAPSHOT")
    testImplementation("com.handa.tms:platform-lib-test-support:0.1.0-SNAPSHOT")
}
```

That's it. Auto-configuration wires JWT validation, tenant context, exception handlers, observability, and Kafka — every service gets the same behavior without copy-paste.

## Publishing

```bash
./gradlew :platform-lib:publishToMavenLocal      # for local dev
./gradlew :platform-lib:publish                  # to internal registry (CI only)
```
