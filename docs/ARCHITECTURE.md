# Architecture

## Topology

```
                                         ┌──────────────────┐
                              ╭──────────►│ identity-service │──┐
                              │           └──────────────────┘  │
   browser ─► nginx ingress ─►│           ┌──────────────────┐  │
                              ├──────────►│ operations       │──┤
                              │           └──────────────────┘  │
                              │           ┌──────────────────┐  │
                              ├──────────►│ dispatch         │──┤        ┌──────────────┐
                              │           └──────────────────┘  ├──────► │  Postgres    │
                              │           ┌──────────────────┐  │        │  (schemas:   │
                              ├──────────►│ tracking (Node)  │──┤        │   identity   │
                              │           └──────────────────┘  │        │   operations │
                              │           ┌──────────────────┐  │        │   dispatch   │
                              ├──────────►│ finance          │──┤        │   tracking   │
                              │           └──────────────────┘  │        │     ↑ TimescaleDB hypertable
                              │           ┌──────────────────┐  │        │   finance    │
                              ├──────────►│ payroll          │──┤        │   payroll    │
                              │           └──────────────────┘  │        │   compliance │
                              │           ┌──────────────────┐  │        │   documents  │
                              ├──────────►│ compliance       │──┤        │   reporting  │
                              │           └──────────────────┘  │        │   notification│
                              │           ┌──────────────────┐  │        └──────────────┘
                              ├──────────►│ documents        │──┤
                              │           └──────────────────┘  │
                              │           ┌──────────────────┐  │
   shell + 9 MFEs  (static) ◄─┤           │ reporting        │──┘
                              │           └──────────────────┘
                              │           ┌──────────────────┐
                              ╰───────────│ notification     │
                                          └──────────────────┘
                                                      │
                                          ┌──────────────────────┐
                                          │ Kafka                │
                                          │  truck.position.v1   │
                                          │  user.created.v1     │
                                          │  load.created.v1     │
                                          │  invoice.created.v1  │
                                          │  …                   │
                                          └──────────────────────┘
```

The api-gateway routes `/api/<service>/...` and `/ws/...` to the right backend; the shell and every MFE are served as static nginx pods at `/` and `/mfe/<name>/`.

## Service ownership

| Service | Owns (write) | Reads (via API or Kafka projection) |
|---|---|---|
| identity | tenants, users, groups, roles, permissions, refresh tokens | — |
| operations | loads, stops, customers, carriers, lanes | identity (user names projection) |
| dispatch | assignment, scheduler state, dispatch board state | operations (loads), tracking (positions) |
| tracking | gps positions, last-known per truck | identity |
| finance | invoices, payments, A/P bills, GL postings | operations (loads), identity, payroll (settlements) |
| payroll | driver pay rates, settlements, W-2/1099 | operations (driver-load history), finance |
| compliance | IFTA worksheets, DOT WMT, HOS aggregates | tracking (state-miles), finance (fuel) |
| documents | uploaded files, OCR results, extraction reviews | — |
| reporting | report definitions, scheduled exports | every service via projection |
| notification | preferences, send history | every service via Kafka |

Two rules to keep this honest:
1. **No service queries another service's tables.** Cross-service reads go through REST or Kafka events.
2. **Each service can be redeployed without touching any other.** Schema migrations live with the service that owns them.

## Multi-tenancy

- Every tenant-scoped table has `tenant_id UUID NOT NULL` + a Postgres RLS policy `USING (tenant_id::text = current_setting('app.current_tenant', true))`.
- `platform-lib-security` injects an AOP interceptor that runs `SET LOCAL app.current_tenant = '<uuid>'` at the start of every `@Transactional` method. **A missing filter call = no rows visible**, not "all rows visible".
- Cross-tenant operations (admin tooling, IFTA roll-up) use `TenantContext.runAs(uuid, () -> { ... })` which scopes the GUC for the closure.

## Eventual consistency

Cross-service updates are NOT distributed transactions. The pattern:

1. Service A commits its DB change AND writes an OutboxEvent row in the same transaction.
2. The OutboxDispatcher (already wired in `platform-lib-messaging`) polls and publishes to Kafka.
3. Service B consumes the event and updates its own state.

Failure modes are explicit:
- A's commit + outbox write are atomic — they succeed together or not at all.
- Dispatcher is at-least-once — consumers must be idempotent (use the event id as the idempotency key).
- Replays are safe — every consumer ends up at the same state.

## RBAC end-to-end

```
User ──*── GroupMembership ──*── Group ──*── GroupRole ──*── Role ──*── RolePermission ──*── Permission
          │                                                      ▲
          ╰── (or directly) UserRole ────────────────────────────╯
```

Each `Permission` is `(resource, action, scope)`. Scope is one of:
- `all` — any record in the tenant
- `own_branch` — records in one of the user's branch ids (`user_branch` table)
- `own` — records the user owns/is assigned to

At login, identity-service flattens the user's effective permissions and stuffs them into the JWT's `perm` claim. Every other service authorizes off the JWT alone — no RPC back to identity. Tradeoff: permission revocations take effect on the next token refresh (≤ 15 min).

## Tech stack summary

| Layer | Choice |
|---|---|
| Backend (most services) | Java 21 + Spring Boot 3.3 |
| Backend (gps ingest) | Node 20 + TypeScript + Fastify + KafkaJS |
| Frontend host + remotes | React 18 + TypeScript + Vite + Module Federation 2.0 |
| Styling | Tailwind + shadcn-style components in `design-system` |
| RDBMS | PostgreSQL 16 |
| Time-series | TimescaleDB (extension on the same Postgres cluster) |
| Geo | PostGIS |
| Cache + pub/sub | Redis 7 |
| Event bus | Kafka 3 |
| Object storage | S3-compatible (MinIO local, real S3 in prod) |
| Migrations | Liquibase (Java services) — services own their schemas |
| Messaging | Spring-Kafka + Outbox pattern |
| Observability | Micrometer + OpenTelemetry → OTLP → your backend of choice |
| Auth | JWT (HS256) + refresh tokens stored hashed |
| Container | Docker (multi-stage), nginx for static frontend |
| Orchestration | Kubernetes (k3s on VPS works fine) |
| Manifests | Kustomize: `base/` + `overlays/prod/` |
| CI | GitHub Actions, path-filtered, image to GHCR, kubectl apply via OIDC |
