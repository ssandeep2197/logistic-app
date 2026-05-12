import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

/**
 * Lands here after the backend's OAuth callback redirects with tokens in the
 * URL fragment, e.g.
 *
 *   /login/oauth-callback#accessToken=…&refreshToken=…&tid=…&uid=…
 *
 * Fragments are NOT sent to servers, so the tokens never appear in access
 * logs or referrer headers.  We pull them out, drop them into localStorage,
 * and dispatch a `storage` event so AuthProvider re-reads the principal.
 */
export function OAuthCallback() {
  const nav = useNavigate();

  useEffect(() => {
    // The hash always begins with "#" — strip it before parsing.
    const hash = window.location.hash.startsWith('#')
      ? window.location.hash.slice(1)
      : window.location.hash;
    const params = new URLSearchParams(hash);

    const access  = params.get('accessToken');
    const refresh = params.get('refreshToken');

    if (!access || !refresh) {
      // Came here without tokens — bounce to /login so the user can retry.
      nav('/login?oauth_error=oauth_missing_params', { replace: true });
      return;
    }

    localStorage.setItem('tms.access', access);
    localStorage.setItem('tms.refresh', refresh);
    window.dispatchEvent(new StorageEvent('storage', { key: 'tms.access' }));

    // Scrub the URL so a copy/paste of the address doesn't leak tokens.
    window.history.replaceState({}, '', '/admin');
    nav('/admin', { replace: true });
  }, [nav]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-6">
      <div className="text-sm text-slate-500">Completing sign-in…</div>
    </div>
  );
}
