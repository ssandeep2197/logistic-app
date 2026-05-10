# Development

## Prerequisites

- JDK 21 (e.g. via `sdkman`)
- Node 20 + pnpm 9
- Docker + Docker Compose
- `kubectl` and `kustomize` (for prod parity testing)

## First-time setup

```bash
# 1. Start the infra stack — Postgres+Timescale+PostGIS, Redis, Kafka, MinIO, MailHog.
make up

# 2. Generate Gradle wrapper (one time only — not committed):
cd backend && gradle wrapper --gradle-version 8.10 && cd ..

# 3. Publish platform-lib to local Maven so services can resolve it.
cd backend && ./gradlew :platform-lib:publishToMavenLocal

# 4. Frontend deps.
cd frontend && pnpm install && cd ..

# 5. Run identity-service.
cd backend && ./gradlew :identity-service:bootRun

# 6. Run shell + every MFE in parallel (separate terminal).
cd frontend && pnpm dev

# 7. Open http://localhost:5173 and sign up your first tenant.
```

## Conventions

### Backend

- Every entity that is per-tenant data extends `TenantScopedEntity` (auto-stamps `tenant_id`).
- Every service-method that mutates state is `@Transactional`. The `RlsGucInterceptor` sets the GUC for free.
- Write the outbox event in the SAME transaction as the business change. Never publish to Kafka directly.
- Money is `BigDecimal` with explicit `RoundingMode`. Never `double`.
- Time is `Instant` at boundaries; `LocalDate` only when a domain genuinely doesn't have a time-of-day (settlement period, IFTA quarter).
- DTOs live next to the controller (records, validated with `jakarta.validation`).

### Frontend

- API calls go through `@tms/shared`'s `api()` — never raw `fetch`. It refreshes tokens, sets headers, and converts `ProblemDetail` to `ApiError`.
- Server state lives in TanStack Query, not Zustand. UI state can live in `useState` or Zustand.
- Components from `@tms/design-system` only. Don't import `lucide-react` or `clsx` directly in MFEs — go through the design system so we can swap libraries later.
- Each MFE exports exactly one entry: `default function App({ basePath, ... })` from `src/App.tsx`. The shell mounts it at the right route.

### Tests

- Backend integration tests use `@SpringBootTest` + `TmsPostgresExtension` (Testcontainers + reuse).
- Don't mock the database. Use a real Postgres in tests; CI runs the same containers.
- Frontend: vitest + @testing-library. Component tests for critical flows; we don't chase coverage.

## Adding a new MFE

```bash
./scripts/new-mfe.sh feature-name 5183     # name, port
# Edit frontend/shell/src/App.tsx to add a route.
# Edit frontend/shell/vite.config.ts to register the remote.
# Edit infra/k8s/overlays/prod/kustomization.yaml to include the new overlay.
# Add the matrix entry to .github/workflows/ci.yml.
```

## Adding a new backend service

```bash
./scripts/new-service.sh new-service 8011  # name, port
# Edit backend/settings.gradle.kts to include it.
# Add k8s base + overlay (copy from identity-service).
# Add the matrix entry to .github/workflows/ci.yml.
```

## Useful URLs (local dev)

| Service | URL |
|---|---|
| Shell | http://localhost:5173 |
| MFE: identity (standalone) | http://localhost:5174 |
| identity-service | http://localhost:8001 |
| identity-service Swagger | http://localhost:8001/swagger-ui.html |
| api-gateway | http://localhost:8000 |
| tracking-service | http://localhost:8004 |
| Kafka UI | http://localhost:8080 |
| MinIO console | http://localhost:9001 |
| MailHog | http://localhost:8025 |
