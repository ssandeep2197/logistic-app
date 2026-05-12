#!/usr/bin/env bash
# Creates the per-environment secrets in the tms namespace.  Idempotent:
# `kubectl create secret --dry-run=client -o yaml | kubectl apply -f -`.
# Re-run any time you want to rotate.
set -euo pipefail

NS=tms
kubectl get ns "$NS" >/dev/null 2>&1 || kubectl create ns "$NS"

# Postgres superuser password.
PG_SU="$(openssl rand -hex 24)"

# Per-service login passwords.  Keys must match the role names without _svc.
declare -A SVC=(
  [identity]="$(openssl rand -hex 24)"
  [operations]="$(openssl rand -hex 24)"
  [dispatch]="$(openssl rand -hex 24)"
  [tracking]="$(openssl rand -hex 24)"
  [finance]="$(openssl rand -hex 24)"
  [payroll]="$(openssl rand -hex 24)"
  [compliance]="$(openssl rand -hex 24)"
  [documents]="$(openssl rand -hex 24)"
  [reporting]="$(openssl rand -hex 24)"
  [notification]="$(openssl rand -hex 24)"
)

# JWT signing key — 64 bytes hex = 256 bits, plenty for HS256.
JWT_KEY="$(openssl rand -hex 64)"

# tms-postgres-secret: superuser
kubectl -n "$NS" create secret generic tms-postgres-secret \
  --from-literal=superuser-password="$PG_SU" \
  --dry-run=client -o yaml | kubectl apply -f -

# tms-svc-passwords: one key per service, also mounted into postgres pod
# at /run/secrets/svc-passwords/<service> for the init script.
SVC_ARGS=()
for k in "${!SVC[@]}"; do
  SVC_ARGS+=(--from-literal="$k=${SVC[$k]}")
done
kubectl -n "$NS" create secret generic tms-svc-passwords \
  "${SVC_ARGS[@]}" \
  --dry-run=client -o yaml | kubectl apply -f -

# tms-jwt: HS256 signing key
kubectl -n "$NS" create secret generic tms-jwt \
  --from-literal=signing-key="$JWT_KEY" \
  --dry-run=client -o yaml | kubectl apply -f -

# tms-oauth: Google OAuth client credentials.  Created EMPTY here so the
# Deployment can reference it without erroring on a missing Secret.  Replace
# with real values via a separate command once the Google Cloud Console app
# exists:
#
#   kubectl -n tms create secret generic tms-oauth \
#       --from-literal=google-client-id=12345.apps.googleusercontent.com \
#       --from-literal=google-client-secret=GOCSPX-... \
#       --dry-run=client -o yaml | kubectl apply -f -
#   kubectl -n tms rollout restart deployment/tms-identity
#
# Until then identity-service will reply 503 from /auth/oauth/google/start.
kubectl -n "$NS" get secret tms-oauth >/dev/null 2>&1 || \
  kubectl -n "$NS" create secret generic tms-oauth \
    --from-literal=google-client-id="" \
    --from-literal=google-client-secret="" \
    --dry-run=client -o yaml | kubectl apply -f -

echo "✓ secrets created (or rotated) in namespace $NS"
echo "✓ identity-service password length: ${#SVC[identity]}"
