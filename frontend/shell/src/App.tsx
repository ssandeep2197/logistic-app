import { Suspense, lazy } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './layout/Layout.js';
import { ProtectedRoute } from './auth/ProtectedRoute.js';
import { useAuth } from './auth/AuthProvider.js';
import { ComingSoon } from './ComingSoon.js';

// ── Module Federation remotes ──────────────────────────────────────────
// Only the remotes that have a built artifact AND are listed in the shell's
// vite.config.ts `remotes` map are imported here.  The MF Vite plugin emits
// a bootstrap that does `Promise.all(loadRemote(...))` over the configured
// remotes BEFORE React renders, so any remote we declare must actually
// resolve at runtime — otherwise the app stays a white screen.
//
// To wire a new MFE:
//   1. add it to vite.config.ts `remotes`
//   2. import it here with `lazy(() => import('mfe_<name>/App'))`
//   3. replace the matching <ComingSoon/> below with the real route

// @ts-expect-error  remote types resolve once the remote has built
const IdentityApp = lazy(() => import('mfe_identity/App'));

export function App() {
  const { principal } = useAuth();
  const isAuthed = principal !== null;

  return (
    <Suspense fallback={<div className="p-8 text-slate-500">Loading…</div>}>
      <Routes>
        <Route
          path="/login/*"
          element={isAuthed ? <Navigate to="/" replace /> : <IdentityApp basePath="/login" mode="login" />}
        />
        <Route path="/signup/*" element={<IdentityApp basePath="/signup" mode="signup" />} />

        <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          {/* Land on the only authed surface that exists today; switch back to
              /dispatch once mfe-dispatch ships. */}
          <Route index element={<Navigate to="/admin" replace />} />
          <Route path="/admin/*"      element={<IdentityApp basePath="/admin" mode="admin" />} />
          <Route path="/operations/*" element={<ComingSoon name="Operations" />} />
          <Route path="/dispatch/*"   element={<ComingSoon name="Dispatch" />} />
          <Route path="/tracking/*"   element={<ComingSoon name="Tracking" />} />
          <Route path="/finance/*"    element={<ComingSoon name="Finance" />} />
          <Route path="/payroll/*"    element={<ComingSoon name="Payroll" />} />
          <Route path="/compliance/*" element={<ComingSoon name="Compliance" />} />
          <Route path="/documents/*"  element={<ComingSoon name="Documents" />} />
          <Route path="/reports/*"    element={<ComingSoon name="Reports" />} />
        </Route>

        <Route path="*" element={<Navigate to={isAuthed ? '/' : '/login'} replace />} />
      </Routes>
    </Suspense>
  );
}
