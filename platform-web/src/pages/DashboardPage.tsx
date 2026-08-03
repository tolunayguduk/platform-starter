import { useEffect, useState } from 'react';
import { Typography, Space } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../store/AuthContext';
import { fetchUiPermissions } from '../api/permissionsApi';
import type { UiPermissions } from '../types/permissions';
import { AccountInfoCard } from './dashboard/AccountInfoCard';
import { ActionsCard } from './dashboard/ActionsCard';
import { PendingInvitesCard } from './dashboard/PendingInvitesCard';
import styles from './DashboardPage.module.css';

export function DashboardPage() {
  const { t } = useTranslation();
  const { accessToken, user } = useAuth();
  const [uiPerms, setUiPerms] = useState<UiPermissions>({});

  useEffect(() => {
    if (!accessToken) return;
    fetchUiPermissions(accessToken).then(setUiPerms);
  }, [accessToken]);

  if (!user) {
    return null;
  }

  return (
    <Space orientation="vertical" size={24} className={styles.wrapper}>
      <Typography.Title level={3} className={styles.title}>
        {t('dashboard.welcome', { name: user.fullName ?? user.username })}
      </Typography.Title>

      <PendingInvitesCard />
      <AccountInfoCard user={user} />
      <ActionsCard uiPerms={uiPerms} />
    </Space>
  );
}