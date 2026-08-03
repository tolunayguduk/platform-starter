import { Alert } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAdminAccessScope } from '../../hooks/useAdminAccessScope';

/** Shown above the dashboard's stats/charts/tables for an ORGANIZATION-scope caller (MANAGER) so
 * it's visible, not just silently true, that everything below - registration stats, the activity
 * feed, the user table - is confined to their own organization. Renders nothing for a
 * PLATFORM-scope caller (ADMIN), who sees the whole realm. */
export function AdminScopeNotice() {
  const { t } = useTranslation();
  const { scope } = useAdminAccessScope();

  if (!scope || scope.platformScoped) {
    return null;
  }

  return <Alert type="info" showIcon message={t('admin.scopeNotice.organization')} />;
}
