import { Tabs, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { UserTab } from './admin/UserTab';
import { RoleFunctionManager } from './admin/roleFunctions/RoleFunctionManager';
import styles from './AdminPage.module.css';

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
