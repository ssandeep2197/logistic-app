import { Route, Routes } from 'react-router-dom';
import { Login } from './Login.js';
import { Signup } from './Signup.js';
import { AdminUsers } from './admin/AdminUsers.js';
import { AdminRoles } from './admin/AdminRoles.js';
import { AdminGroups } from './admin/AdminGroups.js';

export interface AppProps {
  basePath: string;
  mode?: 'login' | 'signup' | 'admin';
}

/**
 * Single entry component for the identity MFE.  The shell mounts it under
 * different routes; we read `mode` to pick which sub-tree to render.
 *
 * In `admin` mode we use a nested <Routes> so the URL stays in sync with the
 * sub-page (Users / Roles / Groups).  React-Router's <Route path="..\/*">
 * in the shell forwards the unmatched suffix to us.
 */
export default function App({ mode = 'login' }: AppProps) {
  if (mode === 'login') return <Login />;
  if (mode === 'signup') return <Signup />;

  return (
    <Routes>
      <Route index           element={<AdminUsers  />} />
      <Route path="users/*"  element={<AdminUsers  />} />
      <Route path="roles/*"  element={<AdminRoles  />} />
      <Route path="groups/*" element={<AdminGroups />} />
    </Routes>
  );
}
