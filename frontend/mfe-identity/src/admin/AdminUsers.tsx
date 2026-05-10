import { useQuery } from '@tanstack/react-query';
import { Card } from '@tms/design-system';
import { api } from '@tms/shared';

interface UserSummary {
  id: string; email: string; fullName: string | null;
  status: string; branchIds: string[];
}
interface Page<T> { content: T[]; totalElements: number }

export function AdminUsers() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['users'],
    queryFn: () => api<Page<UserSummary>>('/identity/users'),
  });

  return (
    <Card className="p-6">
      <h2 className="mb-4 text-lg font-semibold">Users</h2>
      {isLoading && <div className="text-slate-500">Loading…</div>}
      {error && <div className="text-red-600">Failed to load users.</div>}
      {data && (
        <table className="w-full text-sm">
          <thead className="text-left text-slate-500">
            <tr><th className="py-2">Email</th><th>Name</th><th>Status</th></tr>
          </thead>
          <tbody>
            {data.content.map(u => (
              <tr key={u.id} className="border-t border-slate-100">
                <td className="py-2 font-mono text-slate-700">{u.email}</td>
                <td>{u.fullName ?? '—'}</td>
                <td><span className="rounded bg-slate-100 px-2 py-0.5 text-xs">{u.status}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Card>
  );
}
