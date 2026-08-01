import { useEffect, useState } from 'react';
import { Card, Tabs, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { fetchAdminTables } from '../../../api/adminApi';
import type { AdminTable, AdminUser } from '../../../types/admin';
import { RAW_TABLE_KEYS, toCamelKey } from './tableFormat';
import { AdminTableTab } from './AdminTableTab';
import { UserFunctionAccessView } from './UserFunctionAccessView';

export function DatabaseTablesBrowser({ selectedUser }: { selectedUser: AdminUser | null }) {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const [tables, setTables] = useState<AdminTable[]>([]);

  useEffect(() => {
    if (!accessToken || !selectedUser) {
      setTables([]);
      return;
    }
    fetchAdminTables(accessToken).then((data) => setTables(data.filter((table) => RAW_TABLE_KEYS.has(table.key))));
  }, [accessToken, selectedUser]);

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
            items={[
              ...tables.map((table) => ({
                key: table.key,
                label: t(`admin.tables.${toCamelKey(table.key)}`),
                children: (
                  <AdminTableTab
                    tableKey={table.key}
                    hasAudit={table.hasAudit}
                    editableColumns={table.editableColumns}
                    filterUserId={selectedUser.id}
                  />
                ),
              })),
              {
                key: 'FUNCTION_ACCESS',
                label: t('admin.functionAccess.tabLabel'),
                children: <UserFunctionAccessView userId={selectedUser.id} userRoles={selectedUser.roles} />,
              }
            ]}
          />
        </>
      )}
    </Card>
  );
}
