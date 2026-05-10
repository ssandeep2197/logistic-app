# Backend services

Each subdirectory is an independently deployable microservice. They share `platform-lib/` (a published Spring Boot starter that bundles tenant context, JWT validation, RBAC, observability, exception handling) so each service is roughly 200 LOC of plumbing instead of thousands.

## Services

| Service | Port | Owns | Status |
|---|---|---|---|
| api-gateway | 8000 | edge routing, JWT validation, rate limit | stub |
| identity-service | 8001 | tenants, users, groups, roles, permissions, JWT issuance | **implemented** |
| operations-service | 8002 | loads, stops, customers, carriers | stub |
| dispatch-service | 8003 | assignment, auto-scheduler | stub |
| tracking-service | 8004 | GPS ingest @ 30s, WebSocket, ETA *(Node + TS)* | runnable skeleton |
| finance-service | 8005 | invoicing, A/R, A/P, settlements | stub |
| payroll-service | 8006 | driver pay, settlements, W-2/1099 | stub |
| compliance-service | 8007 | IFTA, DOT Oregon WMT, HOS | stub |
| documents-service | 8008 | uploads, OCR, rate-con extraction | stub |
| reporting-service | 8009 | canned + custom reports | stub |
| notification-service | 8010 | email, SMS, push, webhooks | stub |

## Building

```bash
./gradlew build                          # build all services
./gradlew :identity-service:build        # build only identity
./gradlew :identity-service:bootRun      # run identity locally
./gradlew :platform-lib:publishToMavenLocal   # publish shared lib for local consumption
```

## Per-service deploys

CI uses `dorny/paths-filter` to detect which subdirectories changed in a push, then runs only the corresponding deploy workflows. See `.github/workflows/`.

## Shared library

`platform-lib/` is published as `com.handa.tms:platform-lib-*` artifacts. Bump the version in `gradle/libs.versions.toml` and publish; downstream services consume the new version on their next build.
