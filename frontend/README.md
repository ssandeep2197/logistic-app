# Frontend — micro-frontends via Module Federation

The frontend is a host (`shell`) plus N independently-deployed remotes (`mfe-*`). Changing the dispatch UI rebuilds and redeploys only `mfe-dispatch`; the shell and every other remote keep running.

## Why Module Federation 2.0 (Vite)

- **Per-feature deploys.** Each `mfe-*` builds to its own `remoteEntry.js`; CDN-cached, swapped at runtime.
- **Shared deps as singletons.** React, design-system, and TanStack Query are loaded once across all remotes.
- **TypeScript across boundaries.** MF 2.0 emits `.d.ts` for remote modules so the shell's TSC is happy without a type stub.

## Packages

| Package | Type | Port | Purpose |
|---|---|---|---|
| `shell` | host | 5173 | Auth shell, layout, routing, MF host config |
| `mfe-identity` | remote | 5174 | Login + users/roles/groups/tenant admin |
| `mfe-operations` | remote | 5175 | Loads, customers, carriers |
| `mfe-dispatch` | remote | 5176 | Dispatch board |
| `mfe-tracking` | remote | 5177 | Live map |
| `mfe-finance` | remote | 5178 | Invoices, A/R, A/P |
| `mfe-payroll` | remote | 5179 | Payroll runs, settlements |
| `mfe-compliance` | remote | 5180 | IFTA, DOT |
| `mfe-documents` | remote | 5181 | Rate-con upload, doc vault |
| `mfe-reports` | remote | 5182 | Reports + custom builder |
| `design-system` | lib | — | shadcn/ui-based shared components |
| `shared` | lib | — | Types, API client, hooks, MF runtime helpers |

## Dev

```bash
pnpm install
pnpm dev          # starts shell + every mfe in parallel
```

Open http://localhost:5173. Shell discovers remotes from a config map; in dev they're at `http://localhost:<port>/remoteEntry.js`. In prod the URLs are read from `VITE_MFE_*` env at deploy time.
