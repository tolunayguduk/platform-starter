import { useEffect, useState } from 'react';
import { useAuth } from '../store/AuthContext';
import { fetchAdminAccessScope } from '../api/meApi';
import type { AdminAccessScope } from '../types/admin';

/** How far the current user's admin-panel access reaches - see GET /api/me/admin-scope. null
 * while loading (or logged out); every consumer should treat that as "not platform-scoped yet"
 * rather than flash platform-only UI before the real answer arrives. */
export function useAdminAccessScope(): { scope: AdminAccessScope | null; loading: boolean } {
  const { accessToken } = useAuth();
  const [scope, setScope] = useState<AdminAccessScope | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!accessToken) {
      setScope(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    fetchAdminAccessScope(accessToken)
      .then(setScope)
      .finally(() => setLoading(false));
  }, [accessToken]);

  return { scope, loading };
}
