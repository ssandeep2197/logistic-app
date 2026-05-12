import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Card, Button } from '@tms/design-system';
import { api, ApiError } from '@tms/shared';

/**
 * Cascading backup toggle, used in BOTH the platform-owner dashboard and
 * the tenant-admin settings page.  The shape of the data and the cascade
 * rules are the same in both places; the {@code scope} prop just changes
 * which endpoint we hit and what the labels say.
 *
 * cascade:
 *   - platform-level toggle controls every tenant; flip off and the
 *     nightly CronJob exits without dumping.
 *   - tenant-level toggle is the per-tenant override but, today, does
 *     NOT have a physical effect — the backup is cluster-wide, so this
 *     is stored as preference for the day we ship per-tenant filtered
 *     dumps.  We label it that way in the UI so nobody thinks they're
 *     opting out of a backup they actually still appear in.
 */
export function BackupToggle({ scope }: { scope: 'platform' | 'tenant' }) {
  const qc = useQueryClient();

  // Both endpoints return the same shape on the tenant side — TenantSettingsView
  // with both platform map + tenant map.  The platform endpoint returns just
  // the platform map.  We normalise here.
  const q = useQuery({
    queryKey: ['settings', scope],
    queryFn: async () => {
      if (scope === 'platform') {
        const p = await api<Record<string, string>>('/identity/platform/settings');
        return { platform: p, tenant: {} as Record<string, string> };
      }
      return api<{ platform: Record<string, string>; tenant: Record<string, string> }>('/identity/tenant/settings');
    },
  });

  const platformEnabled = q.data?.platform['backup.enabled'] !== 'false';  // default true
  const tenantEnabled   = q.data?.tenant['backup.enabled']   === 'true';   // default false
  const effective       = platformEnabled && (scope === 'platform' ? true : tenantEnabled);

  const [error, setError] = useState<string | null>(null);
  const set = useMutation({
    mutationFn: async (value: boolean) => {
      const path = scope === 'platform'
        ? '/identity/platform/settings/backup.enabled'
        : '/identity/tenant/settings/backup.enabled';
      await api(path, { method: 'PUT', body: { value: String(value) } });
    },
    onSuccess: () => { setError(null); qc.invalidateQueries({ queryKey: ['settings'] }); },
    onError: (err) => {
      if (err instanceof ApiError) setError(err.problem.detail ?? err.problem.title);
      else setError('Could not save.');
    },
  });

  if (q.isLoading) return <Card className="p-6"><div className="text-slate-500">Loading…</div></Card>;
  if (q.error)     return <Card className="p-6"><div className="text-red-600">Could not load settings.</div></Card>;

  const isCurrentlyOn = scope === 'platform' ? platformEnabled : tenantEnabled;
  const canFlip       = scope === 'platform' ? true : platformEnabled;

  return (
    <Card className="p-6">
      <div className="flex items-start justify-between gap-6">
        <div className="flex-1">
          <h2 className="text-lg font-semibold text-slate-900">
            Nightly database backup
            {scope === 'tenant' && (
              <span className="ml-2 rounded bg-slate-100 px-1.5 py-0.5 text-xs text-slate-600">
                preference
              </span>
            )}
          </h2>
          <p className="mt-1 text-sm text-slate-500">
            {scope === 'platform'
              ? <>The nightly CronJob dumps Postgres at 03:15 UTC. Toggling this off pauses every backup across the platform until you turn it back on.</>
              : <>Today the backup is cluster-wide and always includes this workspace; this toggle records your preference for when per-tenant backups ship. {!platformEnabled && <span className="font-medium text-slate-700">Currently disabled by the platform owner.</span>}</>}
          </p>
          <p className="mt-2 text-xs text-slate-500">
            Effective state:{' '}
            <span className={effective ? 'font-medium text-emerald-700' : 'font-medium text-slate-700'}>
              {effective ? 'Active' : 'Paused'}
            </span>
          </p>
        </div>
        <Switch
          on={isCurrentlyOn}
          disabled={!canFlip || set.isPending}
          onChange={(v) => set.mutate(v)}
        />
      </div>
      {error && <div className="mt-3 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>}
    </Card>
  );
}

/** Tailwind-only toggle switch.  Accessible via a checkbox input under the hood. */
function Switch({ on, disabled, onChange }: { on: boolean; disabled: boolean; onChange: (v: boolean) => void }) {
  return (
    <label className={'relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors ' +
                       (on ? 'bg-emerald-500' : 'bg-slate-300') +
                       (disabled ? ' cursor-not-allowed opacity-50' : ' cursor-pointer')}>
      <input
        type="checkbox"
        className="peer sr-only"
        checked={on}
        disabled={disabled}
        onChange={e => onChange(e.target.checked)}
      />
      <span className={'inline-block h-5 w-5 transform rounded-full bg-white shadow transition-transform ' +
                       (on ? 'translate-x-5' : 'translate-x-0.5')}
      />
    </label>
  );
}
