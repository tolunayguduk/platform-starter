import { Tabs, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { UserTab } from './admin/UserTab';
import { RoleFunctionManager } from './admin/roleFunctions/RoleFunctionManager';
import { OrganizationsTab } from './admin/organizations/OrganizationsTab';
import { useAdminAccessScope } from '../hooks/useAdminAccessScope';
import styles from './AdminPage.module.css';

export function AdminPage() {
  const { t } = useTranslation();
  const { scope } = useAdminAccessScope();

  return (
    <div className={styles.wrapper}>
      <Typography.Title level={2} className={styles.title}>
        {t('admin.title')}
      </Typography.Title>

      <Tabs
        items={[
          { key: 'USER', label: t('admin.tab.user'), children: <UserTab /> },
          { key: 'ORGANIZATIONS', label: t('admin.tab.organizations'), children: <OrganizationsTab /> },
          // Role/function definitions are platform-wide policy - organization-scoped admins never
          // touch them, only user↔role assignment and their own organization's membership.
          ...(scope?.platformScoped
            ? [{ key: 'ROLE_FUNCTIONS', label: t('admin.tab.roleFunctions'), children: <RoleFunctionManager /> }]
            : []),
        ]}
      />
    </div>
  );
}
