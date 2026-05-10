#!/usr/bin/env bash
# Scaffold a new frontend MFE.
#   ./scripts/new-mfe.sh feature-name 5183
# Creates frontend/mfe-feature-name/ with the same shape as mfe-operations.
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 <name-without-mfe-prefix> <port>" >&2
  echo "example: $0 settings 5190 → frontend/mfe-settings on port 5190" >&2
  exit 1
fi

NAME="$1"
PORT="$2"
DIR="frontend/mfe-${NAME}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -d "$ROOT/$DIR" ]]; then
  echo "already exists: $DIR" >&2; exit 1
fi

mkdir -p "$ROOT/$DIR/src"

cat > "$ROOT/$DIR/package.json" <<JSON
{
  "name": "@tms/mfe-${NAME}",
  "version": "0.1.0", "private": true, "type": "module",
  "scripts": {
    "dev": "vite --port ${PORT}",
    "build": "vite build",
    "preview": "vite preview --port ${PORT}",
    "typecheck": "tsc --noEmit"
  },
  "dependencies": {
    "@tms/design-system": "workspace:*",
    "@tms/shared": "workspace:*",
    "react": "^18.3.1", "react-dom": "^18.3.1",
    "react-router-dom": "^6.27.0",
    "@tanstack/react-query": "^5.59.16"
  },
  "devDependencies": {
    "@module-federation/vite": "^1.1.13",
    "@vitejs/plugin-react": "^4.3.3",
    "@types/react": "^18.3.12", "@types/react-dom": "^18.3.1",
    "typescript": "^5.6.3", "vite": "^5.4.10"
  }
}
JSON

cat > "$ROOT/$DIR/vite.config.ts" <<TS
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { federation } from '@module-federation/vite';

export default defineConfig({
  server: { port: ${PORT}, strictPort: true, cors: true },
  preview: { port: ${PORT}, strictPort: true },
  build: { target: 'es2022', cssCodeSplit: false, sourcemap: true, modulePreload: false },
  plugins: [react(), federation({
    name: 'mfe_${NAME}',
    filename: 'remoteEntry.js',
    exposes: { './App': './src/App.tsx' },
    shared: {
      react: { singleton: true, requiredVersion: '^18.3.0' },
      'react-dom': { singleton: true, requiredVersion: '^18.3.0' },
      'react-router-dom': { singleton: true, requiredVersion: '^6.27.0' },
      '@tanstack/react-query': { singleton: true, requiredVersion: '^5.0.0' },
      '@tms/design-system': { singleton: true, requiredVersion: '*' },
      '@tms/shared': { singleton: true, requiredVersion: '*' },
    },
  })],
});
TS

cat > "$ROOT/$DIR/tsconfig.json" <<JSON
{ "extends": "../tsconfig.base.json", "compilerOptions": { "noEmit": true, "types": ["vite/client"] }, "include": ["src/**/*", "vite.config.ts"] }
JSON

cat > "$ROOT/$DIR/src/App.tsx" <<TSX
import { Card } from '@tms/design-system';
export interface AppProps { basePath?: string }
export default function App(_p: AppProps) {
  return <Card className="p-8"><h2 className="text-lg font-semibold">${NAME}</h2><p className="text-slate-500">Coming soon.</p></Card>;
}
TSX

echo "✔ Created $DIR"
echo "Next steps:"
echo "  1. Add the route in frontend/shell/src/App.tsx"
echo "  2. Register the remote in frontend/shell/vite.config.ts"
echo "  3. Add to .github/workflows/ci.yml frontend matrix"
echo "  4. Add k8s overlay: cp -r infra/k8s/overlays/prod/mfe-identity infra/k8s/overlays/prod/mfe-${NAME}"
