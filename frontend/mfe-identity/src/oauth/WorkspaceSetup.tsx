import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Card, Input, Label } from '@tms/design-system';
import { api, type TokenResponse, ApiError } from '@tms/shared';
import { oauthErrorMessage } from './errors.js';

/**
 * Last leg of the Google-sign-up flow when the user did NOT type a tenant
 * slug on the signup form.  Reads the {@code pendingSignup} JWT from the URL
 * fragment (the backend put it there after Google identified the user),
 * collects a workspace slug, then POSTs to
 * {@code /auth/oauth/google/complete-signup}.  On success the response is
 * identical to /auth/login — we stash tokens and land in /admin.
 *
 * Token in the fragment never reaches the server (no referer, no logs).
 * If the user reloads or shares the URL after completion, the URL is
 * scrubbed in {@link OAuthCallback}-style with history.replaceState.
 */
export function WorkspaceSetup() {
  const nav = useNavigate();

  // Parse the URL fragment ONCE on mount.
  const fragment = useMemo(() => {
    const hash = window.location.hash.startsWith('#')
      ? window.location.hash.slice(1) : window.location.hash;
    const p = new URLSearchParams(hash);
    return {
      pendingSignup: p.get('pendingSignup') ?? '',
      email:         p.get('email')         ?? '',
      name:          p.get('name')          ?? '',
    };
  }, []);

  const suggestedSlug = useMemo(() => slugify(fragment.email || fragment.name), [fragment]);

  const [slug, setSlug]         = useState(suggestedSlug);
  const [displayName, setName]  = useState(fragment.name);
  const [error, setError]       = useState<string | null>(null);
  const [busy, setBusy]         = useState(false);

  // If the user arrived here without a token, send them back.
  useEffect(() => {
    if (!fragment.pendingSignup) {
      nav('/signup?oauth_error=oauth_pending_invalid', { replace: true });
    }
  }, [fragment.pendingSignup, nav]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const tokens = await api<TokenResponse>('/identity/auth/oauth/google/complete-signup', {
        method: 'POST',
        unauth: true,
        body: {
          tenantSlug: slug.trim().toLowerCase(),
          pendingSignupToken: fragment.pendingSignup,
          fullName: displayName.trim() || null,
        },
      });
      localStorage.setItem('tms.access',  tokens.accessToken);
      localStorage.setItem('tms.refresh', tokens.refreshToken);
      window.dispatchEvent(new StorageEvent('storage', { key: 'tms.access' }));
      window.history.replaceState({}, '', '/admin');
      nav('/admin', { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        const code = (err.problem as { code?: string }).code;
        setError(code ? oauthErrorMessage(code)
                      : (err.problem.detail ?? err.problem.title ?? 'Could not finish sign-up.'));
      } else {
        setError('Could not finish sign-up. Please try again.');
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-6">
      <Card className="w-full max-w-md p-8">
        <h1 className="mb-1 text-2xl font-semibold">Name your workspace</h1>
        <p className="mb-6 text-sm text-slate-500">
          Signed in as <span className="font-medium text-slate-700">{fragment.email || 'your Google account'}</span>.
          Pick a short slug that will appear in URLs.
        </p>

        <form onSubmit={submit} className="space-y-4">
          <div>
            <Label htmlFor="slug">Workspace slug</Label>
            <Input id="slug" value={slug} onChange={e => setSlug(e.target.value)}
                   required pattern="[a-z0-9\-]{2,64}"
                   placeholder="acme-trucking" autoFocus />
            <p className="mt-1 text-xs text-slate-500">
              Lowercase letters, numbers, and hyphens — 2 to 64 chars.
            </p>
          </div>
          <div>
            <Label htmlFor="fullName">Your full name (optional)</Label>
            <Input id="fullName" value={displayName} onChange={e => setName(e.target.value)} />
          </div>

          {error && (
            <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>
          )}

          <Button type="submit" className="w-full" disabled={busy || !slug.trim()}>
            {busy ? 'Creating workspace…' : 'Create workspace'}
          </Button>
        </form>
      </Card>
    </div>
  );
}

/**
 * Turn an email or display name into a plausible slug.  Used only as a
 * suggestion — the user can edit it before submitting.
 */
function slugify(input: string): string {
  if (!input) return '';
  // Prefer the part before the @ for emails, but for personal Gmail-style
  // addresses ("alice.smith@gmail.com") use the local part — for company
  // emails we'd ideally use the domain, but the user can override.
  const local = input.includes('@') ? input.split('@')[0]! : input;
  return local
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 64);
}
