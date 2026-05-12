import { useQuery } from '@tanstack/react-query';
import { Card } from '@tms/design-system';
import { api } from '@tms/shared';
import { BackupToggle } from '../settings/BackupToggle.js';

/**
 * Cross-tenant snapshot for the SaaS operator.  Backed by /platform/* which
 * gates on the JWT's pla=true claim.  Non-platform-admin users can't reach
 * this page (shell hides the link AND the backend returns 403 even if they
 * spoof the URL — defense in depth).
 */
interface Stats {
  tenants: number;
  tenantsActive: number;
  tenantsNew7d: number;
  tenantsNew30d: number;
  users: number;
  usersActive: number;
  usersNew7d: number;
  usersNew30d: number;
  usersWithGoogle: number;
  activeSessions: number;
  usersActive30d: number;
}

interface TenantRow {
  id: string;
  slug: string;
  name: string;
  plan: string;
  status: string;
  createdAt: string;
  userCount: number;
  lastLogin: string | null;
}

interface UserRow {
  id: string;
  tenantId: string;
  tenantSlug: string;
  email: string;
  fullName: string | null;
  status: string;
  createdAt: string;
  lastLoginAt: string | null;
  platformAdmin: boolean;
  hasGoogle: boolean;
}

export function PlatformDashboard() {
  const stats   = useQuery({ queryKey: ['platform.stats'],   queryFn: () => api<Stats>('/identity/platform/stats') });
  const tenants = useQuery({ queryKey: ['platform.tenants'], queryFn: () => api<TenantRow[]>('/identity/platform/tenants?limit=20') });
  const users   = useQuery({ queryKey: ['platform.users'],   queryFn: () => api<UserRow[]>('/identity/platform/users?limit=20') });

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold text-slate-900">Platform</h1>
        <p className="text-sm text-slate-500">SaaS operator view — visible only to platform admins.</p>
      </header>

      {/* KPI cards */}
      <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi label="Tenants"            value={stats.data?.tenants}
             subtext={stats.data ? `${stats.data.tenantsActive} active · +${stats.data.tenantsNew7d} (7d) · +${stats.data.tenantsNew30d} (30d)` : '…'} />
        <Kpi label="Users"              value={stats.data?.users}
             subtext={stats.data ? `${stats.data.usersActive} active · +${stats.data.usersNew7d} (7d) · +${stats.data.usersNew30d} (30d)` : '…'} />
        <Kpi label="Linked w/ Google"   value={stats.data?.usersWithGoogle}
             subtext={stats.data ? `${pct(stats.data.usersWithGoogle, stats.data.users)} of users` : '…'} />
        <Kpi label="Active sessions"    value={stats.data?.activeSessions}
             subtext={stats.data ? `${stats.data.usersActive30d} users logged in last 30d` : '…'} />
      </section>

      {/* Recent tenants */}
      <Card className="p-6">
        <h2 className="mb-4 text-lg font-semibold">Recent tenants</h2>
        {tenants.isLoading && <div className="text-slate-500">Loading…</div>}
        {tenants.error && <div className="text-red-600">Failed to load tenants.</div>}
        {tenants.data && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-left text-slate-500">
                <tr>
                  <th className="py-2 pr-4">Slug</th>
                  <th className="pr-4">Name</th>
                  <th className="pr-4">Plan</th>
                  <th className="pr-4">Users</th>
                  <th className="pr-4">Created</th>
                  <th className="pr-4">Last login</th>
                </tr>
              </thead>
              <tbody>
                {tenants.data.map(t => (
                  <tr key={t.id} className="border-t border-slate-100">
                    <td className="py-2 pr-4 font-mono text-slate-700">{t.slug}</td>
                    <td className="pr-4">{t.name}</td>
                    <td className="pr-4"><Pill>{t.plan}</Pill></td>
                    <td className="pr-4 tabular-nums">{t.userCount}</td>
                    <td className="pr-4 text-slate-500">{relative(t.createdAt)}</td>
                    <td className="pr-4 text-slate-500">{t.lastLogin ? relative(t.lastLogin) : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Platform settings — owner kill-switches go here. */}
      <BackupToggle scope="platform" />

      {/* Recent users */}
      <Card className="p-6">
        <h2 className="mb-4 text-lg font-semibold">Recent users</h2>
        {users.isLoading && <div className="text-slate-500">Loading…</div>}
        {users.error && <div className="text-red-600">Failed to load users.</div>}
        {users.data && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-left text-slate-500">
                <tr>
                  <th className="py-2 pr-4">Email</th>
                  <th className="pr-4">Tenant</th>
                  <th className="pr-4">Sign-in</th>
                  <th className="pr-4">Created</th>
                  <th className="pr-4">Last login</th>
                </tr>
              </thead>
              <tbody>
                {users.data.map(u => (
                  <tr key={u.id} className="border-t border-slate-100">
                    <td className="py-2 pr-4 font-mono text-slate-700">
                      {u.email}
                      {u.platformAdmin && <span className="ml-2 rounded bg-amber-100 px-1.5 py-0.5 text-xs text-amber-800">platform</span>}
                    </td>
                    <td className="pr-4 font-mono text-slate-500">{u.tenantSlug}</td>
                    <td className="pr-4">{u.hasGoogle ? <Pill>Google</Pill> : <Pill>password</Pill>}</td>
                    <td className="pr-4 text-slate-500">{relative(u.createdAt)}</td>
                    <td className="pr-4 text-slate-500">{u.lastLoginAt ? relative(u.lastLoginAt) : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}

function Kpi({ label, value, subtext }: { label: string; value: number | undefined; subtext: string }) {
  return (
    <Card className="p-4">
      <div className="text-xs uppercase tracking-wider text-slate-500">{label}</div>
      <div className="mt-1 text-3xl font-semibold tabular-nums text-slate-900">
        {value === undefined ? '—' : value.toLocaleString()}
      </div>
      <div className="mt-1 text-xs text-slate-500">{subtext}</div>
    </Card>
  );
}

function Pill({ children }: { children: React.ReactNode }) {
  return <span className="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-700">{children}</span>;
}

function pct(num: number, denom: number) {
  if (!denom) return '0%';
  return Math.round((num * 100) / denom) + '%';
}

/** "2 hours ago" style timestamp. */
function relative(iso: string): string {
  const then = new Date(iso).getTime();
  const sec  = Math.round((Date.now() - then) / 1000);
  if (sec < 60)        return sec + 's ago';
  const min = Math.round(sec / 60);
  if (min < 60)        return min + 'm ago';
  const hr  = Math.round(min / 60);
  if (hr < 24)         return hr + 'h ago';
  const d   = Math.round(hr / 24);
  if (d < 30)          return d + 'd ago';
  return new Date(iso).toLocaleDateString();
}
