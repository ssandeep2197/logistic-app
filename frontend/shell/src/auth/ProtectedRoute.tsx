import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './AuthProvider.js';

/**
 * Redirects to /login if not authed, preserving the original target as the
 * `from` query param so we can return after sign-in.
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { principal } = useAuth();
  const location = useLocation();
  if (!principal) {
    const from = location.pathname + location.search;
    return <Navigate to={`/login?from=${encodeURIComponent(from)}`} replace />;
  }
  return <>{children}</>;
}
