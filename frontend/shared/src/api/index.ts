import type { ProblemDetail, TokenResponse } from '../types/index.js';
import { tokenStorage, tokenSecondsLeft } from '../auth/index.js';

/** Base URL of the api-gateway.  Configurable per environment. */
export const API_BASE = (import.meta as ImportMeta & { env?: { VITE_API_BASE?: string } })
                          .env?.VITE_API_BASE ?? '/api';

export class ApiError extends Error {
  constructor(public status: number, public problem: ProblemDetail) {
    super(problem.detail ?? problem.title);
  }
}

let refreshInFlight: Promise<TokenResponse> | null = null;

/** Refresh the access token if it has < 60s left.  Coalesced so concurrent
 *  callers share one network round-trip. */
async function ensureFreshToken(): Promise<string | null> {
  const access = tokenStorage.getAccess();
  if (!access) return null;
  if (tokenSecondsLeft(access) > 60) return access;

  const refresh = tokenStorage.getRefresh();
  if (!refresh) return null;

  refreshInFlight ??= fetch(`${API_BASE}/identity/auth/refresh`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ refreshToken: refresh }),
  }).then(async (r) => {
    if (!r.ok) throw new ApiError(r.status, await r.json());
    const tokens = (await r.json()) as TokenResponse;
    tokenStorage.setTokens(tokens.accessToken, tokens.refreshToken);
    return tokens;
  }).finally(() => { refreshInFlight = null; });

  try {
    const t = await refreshInFlight;
    return t.accessToken;
  } catch {
    tokenStorage.clear();
    return null;
  }
}

export interface ApiOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  /** When true, do not attach Authorization header.  Used for /auth/login etc. */
  unauth?: boolean;
}

export async function api<T>(path: string, opts: ApiOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    'content-type': 'application/json',
    accept: 'application/json',
    ...(opts.headers as Record<string, string> | undefined),
  };

  if (!opts.unauth) {
    const token = await ensureFreshToken();
    if (token) headers.authorization = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...opts,
    headers,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
  });

  if (res.status === 204) return undefined as T;

  const ct = res.headers.get('content-type') ?? '';
  if (!res.ok) {
    const problem = ct.includes('json') ? await res.json() : { title: res.statusText, status: res.status };
    throw new ApiError(res.status, problem);
  }
  return ct.includes('json') ? res.json() : ((await res.text()) as unknown as T);
}
