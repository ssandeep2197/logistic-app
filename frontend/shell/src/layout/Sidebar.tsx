import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider.js';
import { cn } from '@tms/design-system';

/**
 * A nav item is visible when either:
 *  • it has no `permission` requirement (always visible to authed users), or
 *  • the principal carries the matching permission code, or
 *  • the item is `platformOnly` and the principal is a platform admin.
 *
 * Platform items are orthogonal to tenant permissions; a Tenant Admin with
 * every tenant-level permission still does NOT see /platform unless their
 * is_platform_admin flag is set.
 */
interface NavItem {
  to: string;
  label: string;
  permission?: string;
  platformOnly?: boolean;
}

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
  { to: '/platform',   label: 'Platform',   platformOnly: true },
];

export function Sidebar() {
  const { principal } = useAuth();
  const visible = NAV.filter(n => {
    if (n.platformOnly) return principal?.platformAdmin === true;
    if (n.permission)   return principal?.permissions.includes(n.permission);
    return true;
  });

  return (
    <aside className="flex w-56 flex-col border-r border-slate-200 bg-white">
      <div className="px-6 py-5 text-lg font-semibold text-blue-700">Helloworlds TMS</div>
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
              item.platformOnly && 'mt-2 border-t border-slate-100 pt-3',
            )}
          >
            {item.label}
            {item.platformOnly && (
              <span className="ml-2 rounded bg-amber-100 px-1.5 py-0.5 text-xs text-amber-800">owner</span>
            )}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
