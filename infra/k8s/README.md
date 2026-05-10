# Kubernetes manifests

Kustomize-based: a small `base/` per service plus a `prod/` overlay that injects the image tag and prod-specific config (replicas, resources, ingress host).

## Layout

```
infra/k8s/
├── base/
│   ├── namespace.yaml          # tms namespace + base labels
│   ├── ingress.yaml            # single nginx ingress fronts the gateway + every MFE
│   ├── secrets.example.yaml    # template for prod secrets (NEVER commit real ones)
│   ├── configmap-shared.yaml   # values used by every service (kafka, postgres host)
│   ├── identity-service/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── kustomization.yaml
│   ├── tracking-service/
│   ├── shell/
│   ├── mfe-identity/
│   └── ... (one per deployable)
└── overlays/
    └── prod/
        ├── kustomization.yaml      # composes all the per-service overlays
        ├── identity-service/
        │   └── kustomization.yaml  # image: tag pinned by CI
        ├── tracking-service/
        └── ...
```

## How CI redeploys ONE service

The reusable deploy workflows do:

```bash
cd infra/k8s/overlays/prod/<service>
kustomize edit set image <service>=ghcr.io/.../<service>:<sha>
kustomize build . | kubectl apply -f -
kubectl rollout status deployment/<service> -n tms
```

Only the named deployment is touched; every other deployment in the namespace keeps running its current image.

## Adding a new service

```bash
cp -r infra/k8s/base/identity-service infra/k8s/base/<new-service>
cp -r infra/k8s/overlays/prod/identity-service infra/k8s/overlays/prod/<new-service>
# Edit the resource names + ports + env in the new files.
# Add the new dir to overlays/prod/kustomization.yaml.
```

A `scripts/new-k8s-service.sh` helper is on the TODO list.

## Secrets

Use `kubectl create secret` (or `kubeseal`) directly on the cluster — never commit decrypted YAML. The `*-secret.example.yaml` files in this repo are templates only; the real ones live in the cluster.

Required secrets in the `tms` namespace:
- `tms-jwt`            — `signing-key`
- `tms-postgres`       — `password` per service (`identity-password`, `operations-password`, …)
- `tms-anthropic`      — `api-key`
- `tms-stripe`         — `secret-key` (when billing rolls out)
- `tms-s3`             — `access-key`, `secret-key`
