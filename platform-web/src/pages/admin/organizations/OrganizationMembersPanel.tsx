import { useEffect, useState } from 'react';
import { App, Button, Popconfirm, Space, Table, Tag } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { fetchOrganizationMembers, removeOrganizationMember } from '../../../api/adminApi';
import type { AdminUser } from '../../../types/admin';
import { AddOrganizationMemberModal } from './AddOrganizationMemberModal';

export function OrganizationMembersPanel({ organizationId, onMembershipChanged }: { organizationId: string; onMembershipChanged: () => void }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [members, setMembers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [removingId, setRemovingId] = useState<string | null>(null);

  function loadMembers() {
    if (!accessToken) return;
    setLoading(true);
    fetchOrganizationMembers(accessToken, organizationId)
      .then(setMembers)
      .finally(() => setLoading(false));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadMembers, [accessToken, organizationId]);

  async function handleRemove(user: AdminUser) {
    if (!accessToken) return;
    setRemovingId(user.id);
    try {
      await removeOrganizationMember(accessToken, organizationId, user.id);
      message.success(t('admin.organizations.memberRemoved'));
      loadMembers();
      onMembershipChanged();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.organizations.error')) : t('admin.organizations.error'));
    } finally {
      setRemovingId(null);
    }
  }

  return (
    <div>
      <Space style={{ marginBottom: 12 }}>
        <Button type="primary" size="small" onClick={() => setAddModalOpen(true)}>
          {t('admin.organizations.addMember.button')}
        </Button>
      </Space>
      <Table
        size="small"
        rowKey="id"
        loading={loading}
        dataSource={members}
        pagination={false}
        columns={[
          { title: t('admin.column.username'), dataIndex: 'username' },
          { title: t('admin.column.email'), dataIndex: 'email' },
          {
            title: t('admin.column.roles'),
            dataIndex: 'roles',
            render: (roles: string[]) => roles.map((r) => <Tag key={r}>{r}</Tag>),
          },
          {
            title: t('admin.editRow.actionsColumn'),
            key: 'actions',
            render: (_: unknown, user: AdminUser) => (
              <Popconfirm title={t('admin.organizations.confirmRemoveMember')} onConfirm={() => handleRemove(user)}>
                <Button size="small" danger loading={removingId === user.id}>
                  {t('admin.organizations.removeMemberAction')}
                </Button>
              </Popconfirm>
            ),
          },
        ]}
      />
      <AddOrganizationMemberModal
        open={addModalOpen}
        organizationId={organizationId}
        existingMemberIds={new Set(members.map((m) => m.id))}
        onClose={() => setAddModalOpen(false)}
        onAdded={() => {
          setAddModalOpen(false);
          loadMembers();
          onMembershipChanged();
        }}
      />
    </div>
  );
}
