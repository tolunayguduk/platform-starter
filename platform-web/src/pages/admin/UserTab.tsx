import { useState } from 'react';
import { Space } from 'antd';
import { useAdminAccessScope } from '../../hooks/useAdminAccessScope';
import { AdminScopeNotice } from './AdminScopeNotice';
import { RegistrationStatsCards } from './RegistrationStatsCards';
import { RegistrationTrendChart } from './RegistrationTrendChart';
import { RecentActivityFeed } from './RecentActivityFeed';
import { UsersTable } from './users/UsersTable';
import { DatabaseTablesBrowser } from './databaseTables/DatabaseTablesBrowser';
import type { AdminUser } from '../../types/admin';

export function UserTab() {
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const { scope } = useAdminAccessScope();

  return (
    <Space orientation="vertical" size={24} style={{ width: '100%' }}>
      <AdminScopeNotice />
      <RegistrationStatsCards />
      <RegistrationTrendChart />
      {/* Realm-wide admin-event feed - even org-filtered, a MANAGER doesn't need this; only ADMIN
          (PLATFORM scope) manages the platform as a whole. */}
      {scope?.platformScoped && <RecentActivityFeed />}
      <UsersTable selectedUserId={selectedUser?.id ?? null} onSelectUser={setSelectedUser} />
      <DatabaseTablesBrowser selectedUser={selectedUser} />
    </Space>
  );
}
