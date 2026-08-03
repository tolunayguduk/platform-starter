import { useEffect, useState } from 'react';
import { App, Button, Popconfirm, Space, Table, Tag } from 'antd';
import { CrownOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import {
  addOrganizationManager,
  fetchOrganizationManagers,
  fetchOrganizationMembers,
  removeOrganizationManager,
  removeOrganizationMember,
} from '../../../api/adminApi';
import type { AdminUser } from '../../../types/admin';
import { InviteOrganizationMemberModal } from './InviteOrganizationMemberModal';

export function OrganizationMembersTab({ organizationId, onMembershipChanged }: { organizationId: string; onMembershipChanged: () => void }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [members, setMembers] = useState<AdminUser[]>([]);
  const [managerIds, setManagerIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [inviteModalOpen, setInviteModalOpen] = useState(false);
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [togglingManagerId, setTogglingManagerId] = useState<string | null>(null);

  function loadMembers() {
    if (!accessToken) return;
    setLoading(true);
    Promise.all([fetchOrganizationMembers(accessToken, organizationId), fetchOrganizationManagers(accessToken, organizationId)])
      .then(([memberList, managerList]) => {
        setMembers(memberList);
        setManagerIds(new Set(managerList.map((m) => m.id)));
      })
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

  async function handleToggleManager(user: AdminUser) {
    if (!accessToken) return;
    setTogglingManagerId(user.id);
    try {
      if (managerIds.has(user.id)) {
        await removeOrganizationManager(accessToken, organizationId, user.id);
        message.success(t('admin.organizations.managerRemoved'));
      } else {
        await addOrganizationManager(accessToken, organizationId, user.id);
        message.success(t('admin.organizations.managerAdded'));
      }
      loadMembers();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.organizations.error')) : t('admin.organizations.error'));
    } finally {
      setTogglingManagerId(null);
    }
  }

  return (
    <div>
      <Space style={{ marginBottom: 12 }}>
        <Button type="primary" size="small" onClick={() => setInviteModalOpen(true)}>
          {t('admin.organizations.invite.button')}
        </Button>
      </Space>
      <Table
        size="small"
        rowKey="id"
        loading={loading}
        dataSource={members}
        pagination={false}
        columns={[
          {
            title: t('admin.column.username'),
            dataIndex: 'username',
            render: (username: string, user: AdminUser) => (
              <Space size={6}>
                {username}
                {managerIds.has(user.id) && (
                  <Tag icon={<CrownOutlined />} color="gold">
                    {t('admin.organizations.managerTag')}
                  </Tag>
                )}
              </Space>
            ),
          },
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
              <Space>
                <Button size="small" loading={togglingManagerId === user.id} onClick={() => handleToggleManager(user)}>
                  {managerIds.has(user.id) ? t('admin.organizations.removeManagerAction') : t('admin.organizations.makeManagerAction')}
                </Button>
                <Popconfirm title={t('admin.organizations.confirmRemoveMember')} onConfirm={() => handleRemove(user)}>
                  <Button size="small" danger loading={removingId === user.id}>
                    {t('admin.organizations.removeMemberAction')}
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <InviteOrganizationMemberModal
        open={inviteModalOpen}
        organizationId={organizationId}
        existingMemberIds={new Set(members.map((m) => m.id))}
        onClose={() => setInviteModalOpen(false)}
        onInvited={() => setInviteModalOpen(false)}
      />
    </div>
  );
}
