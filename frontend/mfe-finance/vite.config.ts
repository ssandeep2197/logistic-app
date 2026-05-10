import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { federation } from '@module-federation/vite';

export default defineConfig({
  server: { port: 5178, strictPort: true, cors: true },
  preview: { port: 5178, strictPort: true },
  build: { target: 'es2022', cssCodeSplit: false, sourcemap: true, modulePreload: false },
  plugins: [react(), federation({
    name: 'mfe_finance', filename: 'remoteEntry.js',
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
