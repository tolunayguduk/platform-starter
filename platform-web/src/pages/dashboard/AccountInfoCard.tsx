import { Card, Descriptions } from 'antd';
import { useTranslation } from 'react-i18next';
import type { CurrentUser } from '../../types/auth';

export function AccountInfoCard({ user }: { user: CurrentUser }) {
  const { t } = useTranslation();

  return (
    <Card title={t('dashboard.accountInfo')} size="small">
      <Descriptions column={1} size="small">
        <Descriptions.Item label={t('dashboard.username')}>{user.username}</Descriptions.Item>
        <Descriptions.Item label={t('dashboard.email')}>{user.email}</Descriptions.Item>
        {user.fullName && (
          <Descriptions.Item label={t('dashboard.fullName')}>{user.fullName}</Descriptions.Item>
        )}
      </Descriptions>
    </Card>
  );
}