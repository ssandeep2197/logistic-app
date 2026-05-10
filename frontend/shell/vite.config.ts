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
        // Only remotes we actually deploy.  The MF Vite plugin emits a
        // bootstrap that does `Promise.all(loadRemote(...))` before React
        // renders, so a single missing remoteEntry.js produces a white screen.
        // Add a remote here only AFTER its build artifacts ship.
        //
        // type:"module" is REQUIRED — Vite emits ESM remoteEntry files
        // that contain top-level `import` statements.  The default
        // type:"var" injects a classic <script> tag and the browser throws
        // "Cannot use import statement outside a module" at runtime, which
        // shows up as a white screen because the host bootstrap awaits
        // Promise.all of all remote loads before React renders.
        mfe_identity: {
          type: 'module',
          name: 'mfe_identity',
          entry: process.env.VITE_MFE_IDENTITY_URL ?? 'http://localhost:5174/remoteEntry.js',
        },
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
