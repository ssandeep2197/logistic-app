import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { federation } from '@module-federation/vite';

/**
 * The shell is the Module Federation HOST.  At runtime it loads
 * remoteEntry.js from each MFE URL — by default localhost ports in dev,
 * env-substituted CDN/k8s ingress URLs in prod.
 *
 * To deploy a new MFE: build it, upload its remoteEntry.js + chunks to a
 * stable URL, set the corresponding VITE_MFE_*_URL env var on the shell's
 * deployment, and roll the shell.  Existing remotes keep running unchanged.
 */
export default defineConfig({
  server: { port: 5173, strictPort: true },
  preview: { port: 5173, strictPort: true },
  build: {
    target: 'es2022',
    cssCodeSplit: true,
    sourcemap: true,
    rollupOptions: { output: { manualChunks: undefined } },
  },
  plugins: [
    react(),
    federation({
      name: 'shell',
      remotes: {
        // Each remote URL is overridable via env at build time so prod points
        // at the CDN-deployed copy instead of localhost.
        mfe_identity:   process.env.VITE_MFE_IDENTITY_URL   ?? 'http://localhost:5174/remoteEntry.js',
        mfe_operations: process.env.VITE_MFE_OPERATIONS_URL ?? 'http://localhost:5175/remoteEntry.js',
        mfe_dispatch:   process.env.VITE_MFE_DISPATCH_URL   ?? 'http://localhost:5176/remoteEntry.js',
        mfe_tracking:   process.env.VITE_MFE_TRACKING_URL   ?? 'http://localhost:5177/remoteEntry.js',
        mfe_finance:    process.env.VITE_MFE_FINANCE_URL    ?? 'http://localhost:5178/remoteEntry.js',
        mfe_payroll:    process.env.VITE_MFE_PAYROLL_URL    ?? 'http://localhost:5179/remoteEntry.js',
        mfe_compliance: process.env.VITE_MFE_COMPLIANCE_URL ?? 'http://localhost:5180/remoteEntry.js',
        mfe_documents:  process.env.VITE_MFE_DOCUMENTS_URL  ?? 'http://localhost:5181/remoteEntry.js',
        mfe_reports:    process.env.VITE_MFE_REPORTS_URL    ?? 'http://localhost:5182/remoteEntry.js',
      },
      shared: {
        // Singletons — every remote uses the same React + Query instance.
        react:               { singleton: true, requiredVersion: '^18.3.0' },
        'react-dom':         { singleton: true, requiredVersion: '^18.3.0' },
        'react-router-dom':  { singleton: true, requiredVersion: '^6.27.0' },
        '@tanstack/react-query': { singleton: true, requiredVersion: '^5.0.0' },
        '@tms/design-system': { singleton: true, requiredVersion: '*' },
        '@tms/shared':        { singleton: true, requiredVersion: '*' },
      },
    }),
  ],
});
