import { useState } from 'react';
import { Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { RegistrationStatsCards } from './admin/RegistrationStatsCards';
import { UsersTable } from './admin/UsersTable';
import { DatabaseTablesBrowser } from './admin/DatabaseTablesBrowser';
import type { AdminUser } from '../api/admin';
import styles from './AdminPage.module.css';

export function AdminPage() {
  const { t } = useTranslation();
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);

  return (
    <div className={styles.wrapper}>
      <Typography.Title level={2} className={styles.title}>
        {t('admin.title')}
      </Typography.Title>

      <RegistrationStatsCards />
      <UsersTable selectedUserId={selectedUser?.id ?? null} onSelectUser={setSelectedUser} />
      <DatabaseTablesBrowser selectedUser={selectedUser} />
    </div>
  );
}