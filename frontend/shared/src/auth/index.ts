import type { AuthPrincipal } from '../types/index.js';

const ACCESS_KEY = 'tms.access';
const REFRESH_KEY = 'tms.refresh';

/**
 * Decodes the payload of a JWT.  We only need the payload for routing /
 * permission checks; signature verification happens on every API call by
 * the gateway, so a tampered token simply gets a 401 in response.
 */
export function decodeJwt<T = unknown>(token: string): T | null {
  try {
    const [, payload] = token.split('.');
    return JSON.parse(atob(payload!.replace(/-/g, '+').replace(/_/g, '/'))) as T;
  } catch {
    return null;
  }
}

export function principalFromAccessToken(token: string): AuthPrincipal | null {
  const claims = decodeJwt<{
    sub: string; tid: string; eml?: string;
    perm?: string[]; brn?: string[]; pla?: boolean;
  }>(token);
  if (!claims) return null;
  return {
    userId: claims.sub,
    tenantId: claims.tid,
    email: claims.eml ?? '',
    permissions: claims.perm ?? [],
    branchIds: claims.brn ?? [],
    platformAdmin: claims.pla === true,
  };
}

/** Returns the seconds remaining until expiry, or 0 if expired/invalid. */
export function tokenSecondsLeft(token: string): number {
  const claims = decodeJwt<{ exp: number }>(token);
  if (!claims) return 0;
  return Math.max(0, claims.exp - Math.floor(Date.now() / 1000));
}

export const tokenStorage = {
  getAccess: () => localStorage.getItem(ACCESS_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  setTokens: (access: string, refresh: string) => {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};
