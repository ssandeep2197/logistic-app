import { Button } from '@tms/design-system';
import { api } from '@tms/shared';
import { useAuth } from '../auth/AuthProvider.js';
import { tokenStorage } from '@tms/shared';

export function Topbar() {
  const { principal, signOut } = useAuth();

  const handleSignOut = async () => {
    const refresh = tokenStorage.getRefresh();
    if (refresh) {
      try { await api('/identity/auth/logout', { method: 'POST', body: { refreshToken: refresh } }); }
      catch { /* ignore — we sign out locally regardless */ }
    }
    signOut();
  };

  return (
    <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
      <div className="text-sm text-slate-500">
        Tenant <span className="font-mono text-slate-700">{principal?.tenantId.slice(0, 8)}…</span>
      </div>
      <div className="flex items-center gap-3">
        <span className="text-sm text-slate-700">{principal?.email}</span>
        <Button variant="ghost" size="sm" onClick={handleSignOut}>Sign out</Button>
      </div>
    </header>
  );
}
