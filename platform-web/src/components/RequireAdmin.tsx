import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAdminAccessScope } from '../hooks/useAdminAccessScope';

/** Nest inside RequireAuth - assumes accessToken/isLoading are already handled there. Gated on
 * GET /api/me/admin-scope (platform- or organization-scoped), never a hardcoded role name - see
 * AdminAccessScopeService on the backend for the same rule enforced server-side. */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { scope, loading } = useAdminAccessScope();

  if (loading) {
    return null; // scope still loading
  }
  if (!scope?.platformScoped && !scope?.organizationScoped) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}
