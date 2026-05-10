import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar.js';
import { Topbar } from './Topbar.js';

/**
 * App chrome around every authed page.  The MFE rendered into <Outlet/> is
 * fully responsible for its own internal layout — the shell only owns the
 * topbar + sidebar so the user never sees inconsistent navigation when
 * switching between independently-deployed remotes.
 */
export function Layout() {
  return (
    <div className="flex h-screen bg-slate-50">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Topbar />
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
