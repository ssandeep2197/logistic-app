import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  type AuthPrincipal,
  principalFromAccessToken,
  tokenStorage,
} from '@tms/shared';

interface AuthState {
  principal: AuthPrincipal | null;
  loading: boolean;
  setTokens: (access: string, refresh: string) => void;
  signOut: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

/**
 * Wraps the app and exposes the current AuthPrincipal.  The principal is
 * derived from the JWT in localStorage — there's no extra /me round-trip on
 * page load, the token already carries everything we need.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [principal, setPrincipal] = useState<AuthPrincipal | null>(() => {
    const token = tokenStorage.getAccess();
    return token ? principalFromAccessToken(token) : null;
  });

  // Keep principal in sync if other tabs sign in / out.
  useEffect(() => {
    const handler = () => {
      const token = tokenStorage.getAccess();
      setPrincipal(token ? principalFromAccessToken(token) : null);
    };
    window.addEventListener('storage', handler);
    return () => window.removeEventListener('storage', handler);
  }, []);

  const value = useMemo<AuthState>(() => ({
    principal,
    loading: false,
    setTokens: (a, r) => {
      tokenStorage.setTokens(a, r);
      setPrincipal(principalFromAccessToken(a));
    },
    signOut: () => {
      tokenStorage.clear();
      setPrincipal(null);
    },
  }), [principal]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

/** True if the principal has the given permission code. */
export function useHasPermission(code: string): boolean {
  const { principal } = useAuth();
  return principal?.permissions.includes(code) ?? false;
}
