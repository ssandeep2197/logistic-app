import { Route, Routes } from 'react-router-dom';
import { Login } from './Login.js';
import { Signup } from './Signup.js';
import { AdminUsers } from './admin/AdminUsers.js';
import { AdminRoles } from './admin/AdminRoles.js';
import { AdminGroups } from './admin/AdminGroups.js';
import { OAuthCallback } from './oauth/OAuthCallback.js';
import { WorkspaceSetup } from './oauth/WorkspaceSetup.js';
import { PlatformDashboard } from './platform/PlatformDashboard.js';

export interface AppProps {
  basePath: string;
  mode?: 'login' | 'signup' | 'admin' | 'platform';
}

/**
 * Single entry component for the identity MFE.  The shell mounts it under
 * different routes; we read `mode` to pick which sub-tree to render.
 *
 * `login` mode handles both the password form and /login/oauth-callback.
 * `signup` mode handles both the signup form and /signup/workspace.
 * `admin` mode owns Users / Roles / Groups under /admin/*.
 * `platform` mode shows the cross-tenant operator dashboard at /platform.
 *   The shell only mounts this when principal.platformAdmin === true; the
 *   backend ALSO gates /platform/* on the same flag, so a non-platform user
 *   typing /platform in the URL gets blank queries (403) — fail closed.
 */
export default function App({ mode = 'login' }: AppProps) {
  if (mode === 'login') {
    return (
      <Routes>
        <Route path="oauth-callback" element={<OAuthCallback />} />
        <Route path="*" element={<Login />} />
      </Routes>
    );
  }
  if (mode === 'signup') {
    return (
      <Routes>
        <Route path="workspace" element={<WorkspaceSetup />} />
        <Route path="*" element={<Signup />} />
      </Routes>
    );
  }
  if (mode === 'platform') {
    return <PlatformDashboard />;
  }

  return (
    <Routes>
      <Route index           element={<AdminUsers  />} />
      <Route path="users/*"  element={<AdminUsers  />} />
      <Route path="roles/*"  element={<AdminRoles  />} />
      <Route path="groups/*" element={<AdminGroups />} />
    </Routes>
  );
}
