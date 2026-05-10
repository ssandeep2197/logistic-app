import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider.js';
import { cn } from '@tms/design-system';

interface NavItem { to: string; label: string; permission?: string }

const NAV: NavItem[] = [
  { to: '/dispatch',   label: 'Dispatch'   },
  { to: '/tracking',   label: 'Tracking',   permission: 'tracking:view:all' },
  { to: '/operations', label: 'Operations', permission: 'load:read:all' },
  { to: '/finance',    label: 'Finance',    permission: 'invoice:read:all' },
  { to: '/payroll',    label: 'Payroll',    permission: 'payroll:manage:all' },
  { to: '/compliance', label: 'Compliance' },
  { to: '/documents',  label: 'Documents'  },
  { to: '/reports',    label: 'Reports',    permission: 'report:view:all' },
  { to: '/admin',      label: 'Admin',      permission: 'user:read:all' },
];

export function Sidebar() {
  const { principal } = useAuth();
  const visible = NAV.filter(n => !n.permission || principal?.permissions.includes(n.permission));

  return (
    <aside className="flex w-56 flex-col border-r border-slate-200 bg-white">
      <div className="px-6 py-5 text-lg font-semibold text-blue-700">Handa TMS</div>
      <nav className="flex flex-col gap-0.5 px-3 pb-6">
        {visible.map(item => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => cn(
              'rounded-md px-3 py-2 text-sm transition-colors',
              isActive
                ? 'bg-blue-50 text-blue-700 font-medium'
                : 'text-slate-700 hover:bg-slate-100',
            )}
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
