/** Mirrors backend `AuthPrincipal` + JWT claims. */
export interface AuthPrincipal {
    userId: string;
    tenantId: string;
    email: string;
    permissions: string[];
    branchIds: string[];
}
export interface TokenResponse {
    tenantId: string;
    userId: string;
    accessToken: string;
    refreshToken: string;
}
export interface ProblemDetail {
    type?: string;
    title: string;
    status: number;
    detail?: string;
    code?: string;
    requestId?: string;
    fields?: Record<string, string>;
    [key: string]: unknown;
}
/** A permission code like {@code "load:read:own_branch"}. */
export type PermissionCode = `${string}:${string}:${'all' | 'own_branch' | 'own'}`;
