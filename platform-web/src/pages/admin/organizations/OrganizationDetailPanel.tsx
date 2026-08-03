import { Tabs } from 'antd';
import { useTranslation } from 'react-i18next';
import type { Organization } from '../../../types/admin';
import { OrganizationMembersTab } from './OrganizationMembersTab';
import { PendingJoinRequestsTab } from './PendingJoinRequestsTab';
import { OrganizationSettingsTab } from './OrganizationSettingsTab';

export function OrganizationDetailPanel({ organization, onOrganizationChanged }: { organization: Organization; onOrganizationChanged: () => void }) {
  const { t } = useTranslation();

  return (
    <Tabs
      items={[
        {
          key: 'members',
          label: t('admin.organizations.tabs.members'),
          children: <OrganizationMembersTab organizationId={organization.id} onMembershipChanged={onOrganizationChanged} />,
        },
        {
          key: 'pendingRequests',
          label: t('admin.organizations.tabs.pendingRequests'),
          children: <PendingJoinRequestsTab organizationId={organization.id} onResolved={onOrganizationChanged} />,
        },
        {
          key: 'settings',
          label: t('admin.organizations.tabs.settings'),
          children: <OrganizationSettingsTab organization={organization} onSettingsChanged={onOrganizationChanged} />,
        },
      ]}
    />
  );
}
