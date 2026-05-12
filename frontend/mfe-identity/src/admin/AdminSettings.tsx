import { BackupToggle } from '../settings/BackupToggle.js';

/**
 * Tenant-admin settings page.  Shows per-tenant toggles, each with their
 * cascade state baked in (a toggle whose platform parent is off is greyed).
 * One toggle today (backup); add more as new feature flags ship.
 */
export function AdminSettings() {
  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold text-slate-900">Settings</h1>
        <p className="text-sm text-slate-500">Workspace-level toggles.</p>
      </header>
      <BackupToggle scope="tenant" />
    </div>
  );
}
