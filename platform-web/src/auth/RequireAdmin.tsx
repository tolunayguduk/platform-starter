import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';

/** Nest inside RequireAuth - assumes accessToken/isLoading are already handled there. */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { user } = useAuth();

  if (!user) {
    return null; // profile still loading
  }
  if (!user.roles.includes('ADMIN')) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}