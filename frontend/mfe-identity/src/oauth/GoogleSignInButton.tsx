/**
 * "Sign in / Sign up with Google" button.  Does a full-page navigation to
 * the backend's start endpoint — NOT a fetch — because the response is a
 * 302 to accounts.google.com that the browser must follow.
 *
 * `tenantSlug` is optional:
 *  • when present (typed in the form), it's passed through to the backend
 *    so the post-Google flow short-circuits the workspace-setup interstitial;
 *  • when absent, signup mode bounces the user to /signup/workspace after
 *    Google sign-in to enter the slug there.
 */
export function GoogleSignInButton(props: {
  tenantSlug?: string;
  mode: 'login' | 'signup';
}) {
  const label = props.mode === 'login' ? 'Sign in with Google' : 'Sign up with Google';
  const apiBase = (import.meta as ImportMeta & { env?: { VITE_API_BASE?: string } })
                    .env?.VITE_API_BASE ?? '/api';

  const onClick = () => {
    const slug = (props.tenantSlug ?? '').trim();
    const qs = slug
      ? `?mode=${props.mode}&tenantSlug=${encodeURIComponent(slug)}`
      : `?mode=${props.mode}`;
    window.location.href = `${apiBase}/identity/auth/oauth/google/start${qs}`;
  };

  return (
    <button
      type="button"
      onClick={onClick}
      className={
        'flex h-10 w-full items-center justify-center gap-2 rounded-md border border-slate-300 ' +
        'bg-white px-4 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 ' +
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500'
      }
    >
      <GoogleGlyph />
      {label}
    </button>
  );
}

/** Official Google "G" glyph (multicolor). */
function GoogleGlyph() {
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
      <path fill="#EA4335" d="M12 10.2v3.6h5.04c-.21 1.32-1.5 3.88-5.04 3.88-3.03 0-5.5-2.5-5.5-5.6s2.47-5.6 5.5-5.6c1.72 0 2.88.73 3.54 1.36l2.41-2.33C16.42 4.06 14.41 3 12 3 6.96 3 2.9 7.06 2.9 12.08s4.06 9.08 9.1 9.08c5.26 0 8.74-3.7 8.74-8.9 0-.6-.06-1.06-.14-1.52H12z" />
    </svg>
  );
}
