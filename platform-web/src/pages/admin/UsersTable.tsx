import { useEffect, useState } from 'react';
import { Card, Table, Select, Tag, Space, App } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../auth/AuthContext';
import { fetchAdminUsers, updateUserRoles, type AdminUser } from '../../api/admin';
import { ApiError } from '../../api/client';

const ALL_ROLES = ['ADMIN', 'MANAGER', 'USER'];

export function UsersTable() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken, user: currentUser } = useAuth();
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState<string | null>(null);

  function loadUsers() {
    if (!accessToken) return;
    setLoading(true);
    fetchAdminUsers(accessToken).then((data) => {
      setUsers(data);
      setLoading(false);
    });
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadUsers, [accessToken]);

  async function handleRolesChange(userId: string, roles: string[]) {
    if (!accessToken) return;
    setSavingId(userId);
    try {
      await updateUserRoles(accessToken, userId, roles);
      message.success(t('admin.rolesUpdated'));
      loadUsers();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.rolesUpdateError')) : t('admin.rolesUpdateError'));
    } finally {
      setSavingId(null);
    }
  }

  return (
    <Card title={t('admin.usersTitle')}>
      <Table
        rowKey="id"
        loading={loading}
        dataSource={users}
        pagination={{ pageSize: 10 }}
        columns={[
          {
            title: t('admin.column.username'),
            dataIndex: 'username',
            render: (value: string, record: AdminUser) => (
              <Space size={6}>
                {value}
                {record.username === currentUser?.username && <Tag>{t('admin.you')}</Tag>}
              </Space>
            ),
          },
          { title: t('admin.column.fullName'), dataIndex: 'fullName', render: (v: string | null) => v ?? '-' },
          { title: t('admin.column.email'), dataIndex: 'email' },
          { title: t('admin.column.status'), dataIndex: 'status' },
          {
            title: t('admin.column.createdAt'),
            dataIndex: 'createdAt',
            render: (v: string) => new Date(v).toLocaleString(),
          },
          {
            title: t('admin.column.roles'),
            dataIndex: 'roles',
            width: 260,
            render: (roles: string[], record: AdminUser) => (
              <Select
                mode="multiple"
                size="small"
                style={{ width: '100%' }}
                value={roles}
                loading={savingId === record.id}
                disabled={savingId === record.id}
                options={ALL_ROLES.map((r) => ({ value: r, label: r }))}
                onChange={(newRoles) => handleRolesChange(record.id, newRoles)}
              />
            ),
          },
        ]}
      />
    </Card>
  );
}