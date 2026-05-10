// Entry used when running this MFE standalone for development.
// In production the shell loads ./App via remoteEntry.js, never main.tsx.
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App.js';
import './styles.css';

const qc = new QueryClient();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <QueryClientProvider client={qc}>
        <App basePath="" mode="login" />
      </QueryClientProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
