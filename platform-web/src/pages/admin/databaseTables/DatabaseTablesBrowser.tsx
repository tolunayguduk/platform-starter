import { useEffect, useState } from 'react';
import { Card, Tabs, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { useAdminAccessScope } from '../../../hooks/useAdminAccessScope';
import { fetchAdminTables } from '../../../api/adminApi';
import type { AdminTable, AdminUser } from '../../../types/admin';
import { RAW_TABLE_KEYS, toCamelKey } from './tableFormat';
import { AdminTableTab } from './AdminTableTab';
import { UserFunctionAccessView } from './UserFunctionAccessView';

// Consent history and computed function access are both a step removed from plain profile
// editing - a manager manages their organization's membership, not GDPR consent records or the
// role/function machinery behind what a user can click. PLATFORM-scope only, same as audit
// history (see AdminTableTab's hasAudit gating below).
const PLATFORM_SCOPE_ONLY_TABLE_KEYS = new Set(['USER_CONSENT']);

export function DatabaseTablesBrowser({ selectedUser }: { selectedUser: AdminUser | null }) {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const { scope } = useAdminAccessScope();
  const [tables, setTables] = useState<AdminTable[]>([]);
  const [activeTabKey, setActiveTabKey] = useState('USER_PROFILE');

  useEffect(() => {
    // A newly selected user re-opens this browser fresh - always land on User Profiles rather
    // than wherever the previous user's tab selection happened to be.
    setActiveTabKey('USER_PROFILE');
    if (!accessToken || !selectedUser) {
      setTables([]);
      return;
    }
    fetchAdminTables(accessToken).then((data) =>
      setTables(
        data.filter((table) => RAW_TABLE_KEYS.has(table.key) && (scope?.platformScoped || !PLATFORM_SCOPE_ONLY_TABLE_KEYS.has(table.key))),
      ),
    );
  }, [accessToken, selectedUser, scope?.platformScoped]);

  return (
    <Card
      title={
        selectedUser
          ? t('admin.databaseTablesTitleFor', { username: selectedUser.username })
          : t('admin.databaseTablesTitle')
      }
    >
      {!selectedUser ? (
        <Typography.Paragraph type="secondary">{t('admin.databaseTablesEmptyState')}</Typography.Paragraph>
      ) : (
        <>
          <Typography.Paragraph type="secondary">{t('admin.databaseTablesHint')}</Typography.Paragraph>
          <Tabs
            activeKey={activeTabKey}
            onChange={setActiveTabKey}
            items={[
              ...tables.map((table) => ({
                key: table.key,
                label: t(`admin.tables.${toCamelKey(table.key)}`),
                children: (
                  <AdminTableTab
                    tableKey={table.key}
                    hasAudit={table.hasAudit && !!scope?.platformScoped}
                    editableColumns={table.editableColumns}
                    filterUserId={selectedUser.id}
                    readOnly={table.key === 'USER_PROFILE' && !scope?.platformScoped}
                  />
                ),
              })),
              ...(scope?.platformScoped
                ? [
                    {
                      key: 'FUNCTION_ACCESS',
                      label: t('admin.functionAccess.tabLabel'),
                      children: <UserFunctionAccessView userId={selectedUser.id} userRoles={selectedUser.roles} />,
                    },
                  ]
                : []),
            ]}
          />
        </>
      )}
    </Card>
  );
}
