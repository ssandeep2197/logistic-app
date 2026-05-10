# Logistics App — Multi-Tenant TMS

Enterprise Transport Management System for US carriers. Multi-tenant SaaS with full microservices on backend and Module-Federation micro-frontends on the frontend.

## Repo layout

```
Logistics App/
├── frontend/        # All micro-frontends (shell + 9 MFEs) + shared libs
├── backend/         # All backend microservices (11) + shared libs
├── infra/           # docker-compose for local dev, k8s manifests for prod
├── .github/         # Per-service CI/CD with path-filtered deploys
├── docs/            # Architecture, dev, and deployment docs
└── scripts/         # Dev helpers
```

## Quick start (local dev)

```bash
# 1. Start infra
make up

# 2. Start backend services (in separate terminals)
cd backend && ./gradlew :identity-service:bootRun
cd backend/tracking-service && pnpm dev

# 3. Start frontend (in another terminal)
cd frontend && pnpm install && pnpm dev
```

Open http://localhost:5173 — sign up your first tenant, log in.

## Documents

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — services, data ownership, contracts
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) — local dev, conventions, testing
- [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) — VPS k8s deploys, GitHub Actions, secrets

## Status

Phase 1 in progress. Identity service + auth flow scaffolded. All other services are runnable stubs awaiting domain implementation.
