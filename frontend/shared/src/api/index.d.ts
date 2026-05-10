import type { ProblemDetail } from '../types/index.js';
/** Base URL of the api-gateway.  Configurable per environment. */
export declare const API_BASE: string;
export declare class ApiError extends Error {
    status: number;
    problem: ProblemDetail;
    constructor(status: number, problem: ProblemDetail);
}
export interface ApiOptions extends Omit<RequestInit, 'body'> {
    body?: unknown;
    /** When true, do not attach Authorization header.  Used for /auth/login etc. */
    unauth?: boolean;
}
export declare function api<T>(path: string, opts?: ApiOptions): Promise<T>;
