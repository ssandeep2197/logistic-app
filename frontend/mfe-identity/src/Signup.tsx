import { useState, type FormEvent } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import { Button, Card, Input, Label } from '@tms/design-system';
import { api, type TokenResponse, ApiError } from '@tms/shared';
import { GoogleSignInButton } from './oauth/GoogleSignInButton.js';
import { oauthErrorMessage } from './oauth/errors.js';

/**
 * First-tenant signup.  The backend creates the tenant + admin user
 * atomically and returns tokens — we navigate straight into the app.
 *
 * Also offers Google sign-up: the backend's /auth/oauth/google/start with
 * mode=signup creates a new tenant with the typed slug + an admin user
 * linked to the Google identity (no TMS password needed).
 */
export function Signup() {
  const nav = useNavigate();
  const [params] = useSearchParams();
  const oauthError = params.get('oauth_error');

  const [form, setForm] = useState({
    tenantSlug: '', tenantName: '', email: '', password: '', fullName: '',
  });
  const [error, setError] = useState<string | null>(
    oauthError ? oauthErrorMessage(oauthError) : null,
  );
  const [busy, setBusy] = useState(false);

  const update = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [k]: e.target.value });

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const tokens = await api<TokenResponse>('/identity/auth/signup', {
        method: 'POST', unauth: true, body: form,
      });
      localStorage.setItem('tms.access', tokens.accessToken);
      localStorage.setItem('tms.refresh', tokens.refreshToken);
      window.dispatchEvent(new StorageEvent('storage', { key: 'tms.access' }));
      nav('/', { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? (err.problem.detail ?? err.problem.title) : 'Signup failed');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-6">
      <Card className="w-full max-w-md p-8">
        <h1 className="mb-1 text-2xl font-semibold">Create your tenant</h1>
        <p className="mb-6 text-sm text-slate-500">You'll be the first admin.</p>

        <form onSubmit={submit} className="space-y-4">
          <div>
            <Label htmlFor="tenantName">Company name</Label>
            <Input id="tenantName" value={form.tenantName} onChange={update('tenantName')} required />
          </div>
          <div>
            <Label htmlFor="tenantSlug">URL slug</Label>
            <Input id="tenantSlug" value={form.tenantSlug} onChange={update('tenantSlug')}
                   required pattern="[a-z0-9\-]{2,64}" placeholder="acme" />
          </div>
          <div>
            <Label htmlFor="fullName">Your name</Label>
            <Input id="fullName" value={form.fullName} onChange={update('fullName')} />
          </div>
          <div>
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" value={form.email} onChange={update('email')} required />
          </div>
          <div>
            <Label htmlFor="password">Password (12+ chars)</Label>
            <Input id="password" type="password" value={form.password} onChange={update('password')}
                   minLength={12} required />
          </div>

          {error && (
            <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>
          )}

          <Button type="submit" className="w-full" disabled={busy}>
            {busy ? 'Creating…' : 'Create tenant'}
          </Button>
        </form>

        <div className="my-6 flex items-center gap-3 text-xs uppercase tracking-wider text-slate-400">
          <div className="h-px flex-1 bg-slate-200" />
          or
          <div className="h-px flex-1 bg-slate-200" />
        </div>

        {/* tenantSlug is optional on the Google signup path — if it's blank
            the backend bounces to /signup/workspace after Google auth so
            the user can pick the slug there. */}
        <GoogleSignInButton tenantSlug={form.tenantSlug} mode="signup" />

        <p className="mt-6 text-center text-sm text-slate-500">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-blue-600 hover:underline">Sign in</Link>
        </p>
      </Card>
    </div>
  );
}
