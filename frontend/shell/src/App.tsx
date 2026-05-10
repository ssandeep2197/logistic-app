import { Suspense, lazy } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './layout/Layout.js';
import { ProtectedRoute } from './auth/ProtectedRoute.js';
import { useAuth } from './auth/AuthProvider.js';

// ── Module Federation remotes ──────────────────────────────────────────
// React.lazy + Suspense gives us code-splitting per MFE: nothing for the
// finance UI is downloaded until a user navigates to /finance.
//
// `// @ts-expect-error` because MF 2.0 emits .d.ts asynchronously; once the
// remote has built once in the same dev session the import is fully typed.

// @ts-expect-error  remote types
const IdentityApp   = lazy(() => import('mfe_identity/App'));
// @ts-expect-error
const OperationsApp = lazy(() => import('mfe_operations/App'));
// @ts-expect-error
const DispatchApp   = lazy(() => import('mfe_dispatch/App'));
// @ts-expect-error
const TrackingApp   = lazy(() => import('mfe_tracking/App'));
// @ts-expect-error
const FinanceApp    = lazy(() => import('mfe_finance/App'));
// @ts-expect-error
const PayrollApp    = lazy(() => import('mfe_payroll/App'));
// @ts-expect-error
const ComplianceApp = lazy(() => import('mfe_compliance/App'));
// @ts-expect-error
const DocumentsApp  = lazy(() => import('mfe_documents/App'));
// @ts-expect-error
const ReportsApp    = lazy(() => import('mfe_reports/App'));

export function App() {
  const { principal } = useAuth();
  const isAuthed = principal !== null;

  return (
    <Suspense fallback={<div className="p-8 text-slate-500">Loading…</div>}>
      <Routes>
        <Route path="/login/*" element={isAuthed ? <Navigate to="/" replace /> : <IdentityApp basePath="/login" mode="login" />} />
        <Route path="/signup/*" element={<IdentityApp basePath="/signup" mode="signup" />} />

        <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route index element={<Navigate to="/dispatch" replace />} />
          <Route path="/admin/*"      element={<IdentityApp   basePath="/admin" mode="admin" />} />
          <Route path="/operations/*" element={<OperationsApp basePath="/operations" />} />
          <Route path="/dispatch/*"   element={<DispatchApp   basePath="/dispatch" />} />
          <Route path="/tracking/*"   element={<TrackingApp   basePath="/tracking" />} />
          <Route path="/finance/*"    element={<FinanceApp    basePath="/finance" />} />
          <Route path="/payroll/*"    element={<PayrollApp    basePath="/payroll" />} />
          <Route path="/compliance/*" element={<ComplianceApp basePath="/compliance" />} />
          <Route path="/documents/*"  element={<DocumentsApp  basePath="/documents" />} />
          <Route path="/reports/*"    element={<ReportsApp    basePath="/reports" />} />
        </Route>

        <Route path="*" element={<Navigate to={isAuthed ? '/' : '/login'} replace />} />
      </Routes>
    </Suspense>
  );
}
