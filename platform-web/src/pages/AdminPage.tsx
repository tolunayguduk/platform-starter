import { useState } from 'react';
import { Tabs, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { RegistrationStatsCards } from './admin/RegistrationStatsCards';
import { UsersTable } from './admin/UsersTable';
import { DatabaseTablesBrowser } from './admin/DatabaseTablesBrowser';
import { RoleFunctionManager } from './admin/RoleFunctionManager';
import type { AdminUser } from '../api/admin';
import styles from './AdminPage.module.css';

function UserTab() {
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);

  return (
    <>
      <RegistrationStatsCards />
      <UsersTable selectedUserId={selectedUser?.id ?? null} onSelectUser={setSelectedUser} />
      <DatabaseTablesBrowser selectedUser={selectedUser} />
    </>
  );
}

export function AdminPage() {
  const { t } = useTranslation();

  return (
    <div className={styles.wrapper}>
      <Typography.Title level={2} className={styles.title}>
        {t('admin.title')}
      </Typography.Title>

      <Tabs
        items={[
          { key: 'USER', label: t('admin.tab.user'), children: <UserTab /> },
          { key: 'ROLE_FUNCTIONS', label: t('admin.tab.roleFunctions'), children: <RoleFunctionManager /> },
        ]}
      />
    </div>
  );
}
