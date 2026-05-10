import { useQuery } from '@tanstack/react-query';
import { Card } from '@tms/design-system';
import { api } from '@tms/shared';

interface GroupSummary { id: string; name: string; description: string | null; roles: string[] }

export function AdminGroups() {
  const { data, isLoading } = useQuery({
    queryKey: ['groups'],
    queryFn: () => api<GroupSummary[]>('/identity/groups'),
  });

  return (
    <Card className="p-6">
      <h2 className="mb-4 text-lg font-semibold">Groups</h2>
      {isLoading && <div className="text-slate-500">Loading…</div>}
      {data?.length === 0 && <div className="text-slate-500">No groups yet.</div>}
      {data?.map(g => (
        <div key={g.id} className="border-b border-slate-100 py-3 last:border-0">
          <div className="font-medium">{g.name}</div>
          {g.description && <div className="text-sm text-slate-500">{g.description}</div>}
          <div className="mt-1 flex flex-wrap gap-1">
            {g.roles.map(r => (
              <span key={r} className="rounded bg-blue-50 px-1.5 py-0.5 text-xs text-blue-700">{r}</span>
            ))}
          </div>
        </div>
      ))}
    </Card>
  );
}
