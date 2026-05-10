# Deployment

## Target

Single-cluster k3s (or any conformant Kubernetes) on a VPS. Works on as little as one 8-vCPU / 16 GB node for a few hundred trucks; horizontal scale by adding nodes.

## One-time cluster setup

1. Install k3s on the VPS (`curl -sfL https://get.k3s.io | sh -`).
2. Install nginx-ingress and cert-manager.
3. Create the namespace + secrets:
   ```bash
   kubectl apply -f infra/k8s/base/namespace.yaml
   # Create real secrets — NEVER commit decrypted values.
   kubectl create secret generic tms-jwt -n tms \
     --from-literal=signing-key="$(openssl rand -hex 64)"
   kubectl create secret generic tms-postgres -n tms \
     --from-literal=identity-password="$(openssl rand -hex 24)" \
     --from-literal=operations-password="$(openssl rand -hex 24)" \
     ...etc
   kubectl create secret generic tms-anthropic -n tms --from-literal=api-key="sk-..."
   ```
4. Run the Postgres init SQL (`infra/postgres/init/01-extensions-and-schemas.sql`) against the prod Postgres. This creates schemas + per-service roles.
5. Apply shared infra:
   ```bash
   kustomize build infra/k8s/overlays/prod | kubectl apply -f -
   ```
   This brings up everything currently listed in `overlays/prod/kustomization.yaml`. Initially that's identity, tracking, shell, mfe-identity — extend as services come online.

## GitHub Actions secrets

In repo Settings → Secrets and variables → Actions, add:

| Name | Value |
|---|---|
| `KUBECONFIG_PROD` | base64 of a kubeconfig file scoped to a deploy ServiceAccount |

(GHCR auth uses `GITHUB_TOKEN` automatically — no setup needed.)

## How CI deploys ONLY the changed service

`.github/workflows/ci.yml`:

1. `dorny/paths-filter` outputs a boolean per service.
2. The matrix job runs `_reusable-springboot-deploy.yml` (or `_reusable-node-deploy.yml`, or `_reusable-mfe-deploy.yml`) once per service.
3. Each invocation early-returns if its `changed` input is false.
4. For services that DID change: build → test → image push → `kustomize edit set image` → `kubectl apply -k`.
5. `kubectl rollout status` confirms the new pods are healthy before the workflow exits.

A push that touches only `backend/dispatch-service/...` triggers exactly one image build (dispatch) and exactly one `kubectl apply` (dispatch). Every other deployment in the namespace keeps running unchanged.

## Rollback

The previous image tag is still in GHCR; rollback is one command:

```bash
kubectl set image deployment/identity-service identity-service=ghcr.io/handatransportation/identity-service:<previous-sha> -n tms
kubectl rollout status deployment/identity-service -n tms
```

(`kubectl rollout undo` also works if the previous ReplicaSet is still around.)

## Database migrations on deploy

Liquibase runs at service startup. The new pod attempts migrations before reporting `Ready`; if a migration fails, the rollout halts via `maxUnavailable: 0` + readiness probe. This is the right behavior — never let a half-migrated cluster serve traffic.

For long migrations (rare), run them out-of-band as a Kubernetes Job before rolling the deployment.

## Observability

Each pod exposes Prometheus at `/actuator/prometheus`. A future `infra/observability/` directory will hold the Prometheus + Grafana + Loki + Tempo stack. Until then, `kubectl logs -f` is your friend.

## Scaling truths

- **identity-service**: small. 2 replicas covers ~1k tenants.
- **tracking-service**: scales with truck count + WS connections. Plan ~1 replica per 500 active trucks.
- **operations / dispatch / finance**: scale with active dispatchers, not trucks. Usually 2-3 replicas.
- **reporting-service**: pull off a read replica from day one — heavy queries WILL hurt OLTP if they share the primary.
- **MFE pods**: tiny nginx pods, ~30 MB each. They're effectively a CDN-replacement.
