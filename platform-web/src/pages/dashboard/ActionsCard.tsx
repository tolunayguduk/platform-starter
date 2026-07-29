import { Card, Space } from 'antd';
import { useTranslation } from 'react-i18next';
import { PermissionButton } from '../../components/PermissionButton';
import type { UiPermissions } from '../../api/permissions';

export function ActionsCard({ uiPerms }: { uiPerms: UiPermissions }) {
  const { t } = useTranslation();

  return (
    <Card title={t('dashboard.actions')} size="small">
      <Space wrap>
        <PermissionButton
          state={uiPerms['report:list']}
          label={t('dashboard.listReports')}
          onClick={() => console.log('list reports')}
        />
        <PermissionButton
          state={uiPerms['report:approve']}
          label={t('dashboard.approveReport')}
          onClick={() => console.log('approve report')}
        />
        <PermissionButton
          state={uiPerms['billing:refund']}
          label={t('dashboard.refund')}
          onClick={() => console.log('refund')}
        />
      </Space>
    </Card>
  );
}