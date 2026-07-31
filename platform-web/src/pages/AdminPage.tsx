import { Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { RegistrationStatsCards } from './admin/RegistrationStatsCards';
import { UsersTable } from './admin/UsersTable';
import { DatabaseTablesBrowser } from './admin/DatabaseTablesBrowser';
import styles from './AdminPage.module.css';

export function AdminPage() {
  const { t } = useTranslation();

  return (
    <div className={styles.wrapper}>
      <Typography.Title level={2} className={styles.title}>
        {t('admin.title')}
      </Typography.Title>

      <RegistrationStatsCards />
      <UsersTable />
      <DatabaseTablesBrowser />
    </div>
  );
}