import { useState } from 'react';
import { RegistrationStatsCards } from './RegistrationStatsCards';
import { UsersTable } from './users/UsersTable';
import { DatabaseTablesBrowser } from './databaseTables/DatabaseTablesBrowser';
import type { AdminUser } from '../../types/admin';

export function UserTab() {
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);

  return (
    <>
      <RegistrationStatsCards />
      <UsersTable selectedUserId={selectedUser?.id ?? null} onSelectUser={setSelectedUser} />
      <DatabaseTablesBrowser selectedUser={selectedUser} />
    </>
  );
}
