/**
 * "Sign in / Sign up with Google" button.  Does a full-page navigation to
 * the backend's start endpoint — NOT a fetch — because the response is a
 * 302 to accounts.google.com that the browser must follow.
 */
export function GoogleSignInButton(props: {
  tenantSlug: string;
  mode: 'login' | 'signup';
  disabled?: boolean;
}) {
  const label = props.mode === 'login' ? 'Sign in with Google' : 'Sign up with Google';
  const apiBase = (import.meta as ImportMeta & { env?: { VITE_API_BASE?: string } })
                    .env?.VITE_API_BASE ?? '/api';

  const onClick = () => {
    if (props.disabled) return;
    const slug = encodeURIComponent(props.tenantSlug.trim());
    window.location.href =
      `${apiBase}/identity/auth/oauth/google/start?tenantSlug=${slug}&mode=${props.mode}`;
  };

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={props.disabled}
      className={
        'flex h-10 w-full items-center justify-center gap-2 rounded-md border border-slate-300 ' +
        'bg-white px-4 text-sm font-medium text-slate-700 transition-colors hover:bg-slate-50 ' +
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 ' +
        'disabled:cursor-not-allowed disabled:opacity-50'
      }
      title={props.disabled ? 'Enter your tenant slug first' : undefined}
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
      <path fill="#34A853" d="M3.95 7.34l2.98 2.19C7.7 7.6 9.69 6.32 12 6.32c1.72 0 2.88.73 3.54 1.36l2.41-2.33C16.42 4.06 14.41 3 12 3 8.4 3 5.3 5.06 3.95 7.34z" opacity="0" />
      <path fill="#4285F4" d="M21.6 12.27c0-.6-.06-1.06-.14-1.52H12v3.6h5.04c-.21 1.32-1.5 3.88-5.04 3.88v3.85c4.05 0 7.46-2.66 8.6-6.5l.6-1.83c.27-.5.4-1.05.4-1.48z" opacity="0" />
    </svg>
  );
}
