import { useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { Button, Card, Input, Label } from '@tms/design-system';
import { api, type TokenResponse, type ProblemDetail, ApiError } from '@tms/shared';
import { GoogleSignInButton } from './oauth/GoogleSignInButton.js';
import { oauthErrorMessage } from './oauth/errors.js';

/**
 * Login form.  On success it stashes tokens in localStorage and dispatches a
 * `storage` event; the shell's AuthProvider listens for that and re-renders
 * the layout with the authed routes available.
 *
 * Also surfaces OAuth errors propagated back via ?oauth_error=&lt;code&gt;
 * (the backend's OAuthController redirects here on any flow failure).
 */
export function Login() {
  const nav = useNavigate();
  const [params] = useSearchParams();
  const returnTo = params.get('from') ?? '/';
  const oauthError = params.get('oauth_error');

  const [tenantSlug, setTenantSlug] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(
    oauthError ? oauthErrorMessage(oauthError) : null,
  );
  const [busy, setBusy] = useState(false);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const tokens = await api<TokenResponse>('/identity/auth/login', {
        method: 'POST',
        unauth: true,
        body: { tenantSlug, email, password },
      });
      localStorage.setItem('tms.access', tokens.accessToken);
      localStorage.setItem('tms.refresh', tokens.refreshToken);
      window.dispatchEvent(new StorageEvent('storage', { key: 'tms.access' }));
      nav(returnTo, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        const p = err.problem as ProblemDetail;
        setError(p.detail ?? p.title);
      } else {
        setError('Login failed. Please try again.');
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-6">
      <Card className="w-full max-w-md p-8">
        <h1 className="mb-1 text-2xl font-semibold">Sign in</h1>
        <p className="mb-6 text-sm text-slate-500">Welcome back. Sign in to your tenant.</p>

        <form onSubmit={submit} className="space-y-4">
          <div>
            <Label htmlFor="tenantSlug">Tenant</Label>
            <Input id="tenantSlug" autoComplete="organization"
                   value={tenantSlug} onChange={e => setTenantSlug(e.target.value)} required />
          </div>
          <div>
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" autoComplete="email"
                   value={email} onChange={e => setEmail(e.target.value)} required />
          </div>
          <div>
            <Label htmlFor="password">Password</Label>
            <Input id="password" type="password" autoComplete="current-password"
                   value={password} onChange={e => setPassword(e.target.value)} required />
          </div>

          {error && (
            <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>
          )}

          <Button type="submit" className="w-full" disabled={busy}>
            {busy ? 'Signing in…' : 'Sign in'}
          </Button>
        </form>

        {/* Visual divider between password + SSO providers. */}
        <div className="my-6 flex items-center gap-3 text-xs uppercase tracking-wider text-slate-400">
          <div className="h-px flex-1 bg-slate-200" />
          or
          <div className="h-px flex-1 bg-slate-200" />
        </div>

        <GoogleSignInButton tenantSlug={tenantSlug} mode="login"
                            disabled={!tenantSlug.trim()} />

        <p className="mt-6 text-center text-sm text-slate-500">
          New to Helloworlds TMS?{' '}
          <Link to="/signup" className="font-medium text-blue-600 hover:underline">
            Create a tenant
          </Link>
        </p>
      </Card>
    </div>
  );
}
