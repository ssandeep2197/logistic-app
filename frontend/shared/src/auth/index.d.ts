import type { AuthPrincipal } from '../types/index.js';
/**
 * Decodes the payload of a JWT.  We only need the payload for routing /
 * permission checks; signature verification happens on every API call by
 * the gateway, so a tampered token simply gets a 401 in response.
 */
export declare function decodeJwt<T = unknown>(token: string): T | null;
export declare function principalFromAccessToken(token: string): AuthPrincipal | null;
/** Returns the seconds remaining until expiry, or 0 if expired/invalid. */
export declare function tokenSecondsLeft(token: string): number;
export declare const tokenStorage: {
    getAccess: () => string | null;
    getRefresh: () => string | null;
    setTokens: (access: string, refresh: string) => void;
    clear: () => void;
};
