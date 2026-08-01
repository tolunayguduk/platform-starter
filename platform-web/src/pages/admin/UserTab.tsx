import { useState } from 'react';
import { Space } from 'antd';
import { RegistrationStatsCards } from './RegistrationStatsCards';
import { RegistrationTrendChart } from './RegistrationTrendChart';
import { RecentActivityFeed } from './RecentActivityFeed';
import { UsersTable } from './users/UsersTable';
import { DatabaseTablesBrowser } from './databaseTables/DatabaseTablesBrowser';
import type { AdminUser } from '../../types/admin';

export function UserTab() {
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);

  return (
    <Space orientation="vertical" size={24} style={{ width: '100%' }}>
      <RegistrationStatsCards />
      <RegistrationTrendChart />
      <RecentActivityFeed />
      <UsersTable selectedUserId={selectedUser?.id ?? null} onSelectUser={setSelectedUser} />
      <DatabaseTablesBrowser selectedUser={selectedUser} />
    </Space>
  );
}
