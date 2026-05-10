import { useQuery } from '@tanstack/react-query';
import { Card } from '@tms/design-system';
import { api } from '@tms/shared';

interface RoleSummary {
  id: string; name: string; description: string | null;
  systemRole: boolean; permissions: string[];
}

export function AdminRoles() {
  const { data, isLoading } = useQuery({
    queryKey: ['roles'],
    queryFn: () => api<RoleSummary[]>('/identity/roles'),
  });

  return (
    <Card className="p-6">
      <h2 className="mb-4 text-lg font-semibold">Roles</h2>
      {isLoading && <div className="text-slate-500">Loading…</div>}
      {data?.map(r => (
        <div key={r.id} className="border-b border-slate-100 py-3 last:border-0">
          <div className="flex items-center gap-2">
            <span className="font-medium">{r.name}</span>
            {r.systemRole && (
              <span className="rounded bg-blue-50 px-2 py-0.5 text-xs text-blue-700">system</span>
            )}
          </div>
          {r.description && <div className="text-sm text-slate-500">{r.description}</div>}
          <div className="mt-1 flex flex-wrap gap-1">
            {r.permissions.map(p => (
              <span key={p} className="rounded bg-slate-100 px-1.5 py-0.5 font-mono text-xs">{p}</span>
            ))}
          </div>
        </div>
      ))}
    </Card>
  );
}
