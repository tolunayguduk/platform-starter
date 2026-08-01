import { useEffect, useState } from 'react';
import { App, Button, Card, Input, Segmented, Space, Table, Tag, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { fetchAdminRoles, fetchAdminUsers, updateUserStatus } from '../../../api/adminApi';
import type { AdminRole, AdminUser } from '../../../types/admin';
import { AuditEventsView } from './AuditEventsView';
import { EditUserModal } from './EditUserModal';

interface UsersTableProps {
  selectedUserId: string | null;
  onSelectUser: (user: AdminUser) => void;
}

export function UsersTable({ selectedUserId, onSelectUser }: UsersTableProps) {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { accessToken, user: currentUser } = useAuth();
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [roles, setRoles] = useState<AdminRole[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
  const [statusSavingId, setStatusSavingId] = useState<string | null>(null);
  const [search, setSearch] = useState('');

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

  useEffect(() => {
    if (!accessToken) return;
    fetchAdminRoles(accessToken).then(setRoles);
  }, [accessToken]);

  async function handleStatusToggle(userId: string, enabled: boolean) {
    if (!accessToken) return;
    setStatusSavingId(userId);
    try {
      await updateUserStatus(accessToken, userId, enabled);
      message.success(enabled ? t('admin.status.enabled') : t('admin.status.disabled'));
      loadUsers();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.status.error')) : t('admin.status.error'));
    } finally {
      setStatusSavingId(null);
    }
  }

  const visibleUsers = users.filter((u) => {
    const needle = search.toLowerCase();
    return (
      u.username.toLowerCase().includes(needle) ||
      u.email.toLowerCase().includes(needle) ||
      (u.fullName ?? '').toLowerCase().includes(needle)
    );
  });

  return (
    <Card title={t('admin.usersTitle')}>
      <Typography.Paragraph type="secondary">{t('admin.usersHint')}</Typography.Paragraph>
      <Input.Search
        style={{ width: 240, marginBottom: 16 }}
        allowClear
        placeholder={t('admin.searchUsers')}
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      <Table
        rowKey="id"
        loading={loading}
        dataSource={visibleUsers}
        pagination={{ pageSize: 10 }}
        expandable={{ expandedRowRender: (record) => <AuditEventsView userId={record.id} /> }}
        onRow={(record) => ({
          onClick: () => onSelectUser(record),
          style: { cursor: 'pointer', backgroundColor: record.id === selectedUserId ? 'rgba(22, 119, 255, 0.08)' : undefined },
        })}
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
          {
            title: t('admin.column.status'),
            dataIndex: 'status',
            render: (status: string, record: AdminUser) => {
              const enabled = status === 'ACTIVE';
              const isSelf = record.username === currentUser?.username;
              return (
                <div onClick={(e) => e.stopPropagation()}>
                  <Segmented
                    size="small"
                    value={enabled ? 'enabled' : 'disabled'}
                    disabled={statusSavingId === record.id || (isSelf && enabled)}
                    onChange={(value) => {
                      if (value === 'enabled') {
                        handleStatusToggle(record.id, true);
                        return;
                      }
                      modal.confirm({
                        title: t('admin.status.confirmDisable'),
                        okButtonProps: { danger: true },
                        okText: t('admin.roleFunctions.disableAction'),
                        cancelText: t('admin.editRow.cancel'),
                        onOk: () => handleStatusToggle(record.id, false),
                      });
                    }}
                    options={[
                      { label: t('admin.roleFunctions.enabledTag'), value: 'enabled' },
                      { label: t('admin.roleFunctions.disabledTag'), value: 'disabled' },
                    ]}
                  />
                </div>
              );
            },
          },
          {
            title: t('admin.column.createdAt'),
            dataIndex: 'createdAt',
            render: (v: string) => new Date(v).toLocaleString(),
          },
          {
            title: t('admin.column.roles'),
            dataIndex: 'roles',
            render: (roles: string[]) => roles.map((r) => <Tag key={r}>{r}</Tag>),
          },
          {
            title: t('admin.editRow.actionsColumn'),
            key: 'actions',
            render: (_: unknown, record: AdminUser) => (
              <Button
                size="small"
                onClick={(e) => {
                  e.stopPropagation();
                  setEditingUser(record);
                }}
              >
                {t('admin.editRow.editAction')}
              </Button>
            ),
          },
        ]}
      />
      {editingUser && (
        <EditUserModal
          open
          user={editingUser}
          isSelf={editingUser.username === currentUser?.username}
          availableRoles={roles}
          onClose={() => setEditingUser(null)}
          onSaved={loadUsers}
        />
      )}
    </Card>
  );
}
