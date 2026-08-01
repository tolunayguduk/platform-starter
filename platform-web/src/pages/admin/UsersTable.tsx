import { useEffect, useState } from 'react';
import { App, Button, Card, Descriptions, Form, Input, Modal, Segmented, Select, Space, Table, Tag, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../auth/AuthContext';
import { ApiError } from '../../api/client';
import {
  fetchAdminRoles,
  fetchAdminUserAuditEvents,
  fetchAdminUsers,
  updateUserIdentity,
  updateUserRoles,
  updateUserStatus,
  type AdminRole,
  type AdminUser,
  type AdminUserAuditEvent,
} from '../../api/admin';

function sameRoleSet(a: string[], b: string[]): boolean {
  if (a.length !== b.length) return false;
  const sorted = [...b].sort();
  return [...a].sort().every((role, i) => role === sorted[i]);
}

interface EditUserModalProps {
  open: boolean;
  user: AdminUser;
  isSelf: boolean;
  availableRoles: AdminRole[];
  onClose: () => void;
  onSaved: () => void;
}

function EditUserModal({ open, user, isSelf, availableRoles, onClose, onSaved }: EditUserModalProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [form] = Form.useForm();
  const [mode, setMode] = useState<'edit' | 'confirm'>('edit');
  const [pendingChanges, setPendingChanges] = useState<{ username?: string; email?: string; roles?: string[] }>({});
  const [saving, setSaving] = useState(false);

  function handleClose() {
    setMode('edit');
    setPendingChanges({});
    onClose();
  }

  function handleReview() {
    const values = form.getFieldsValue();
    const changes: { username?: string; email?: string; roles?: string[] } = {};
    if (values.username !== user.username) changes.username = values.username;
    if (values.email !== user.email) changes.email = values.email;
    if (!sameRoleSet(values.roles ?? [], user.roles)) changes.roles = values.roles;
    setPendingChanges(changes);
    setMode('confirm');
  }

  async function handleConfirm() {
    if (!accessToken) return;
    setSaving(true);
    try {
      if (pendingChanges.username !== undefined || pendingChanges.email !== undefined) {
        await updateUserIdentity(accessToken, user.id, {
          username: pendingChanges.username ?? user.username,
          email: pendingChanges.email ?? user.email,
        });
      }
      if (pendingChanges.roles !== undefined) {
        await updateUserRoles(accessToken, user.id, pendingChanges.roles);
      }
      message.success(t('admin.editUser.success'));
      onSaved();
      handleClose();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.editUser.error')) : t('admin.editUser.error'));
    } finally {
      setSaving(false);
    }
  }

  const hasChanges = Object.keys(pendingChanges).length > 0;

  return (
    <Modal
      open={open}
      onCancel={handleClose}
      destroyOnHidden
      title={mode === 'edit' ? t('admin.editUser.title') : t('admin.editUser.confirmTitle')}
      footer={
        mode === 'edit'
          ? [
              <Button key="cancel" onClick={handleClose}>
                {t('admin.editRow.cancel')}
              </Button>,
              <Button key="save" type="primary" onClick={handleReview}>
                {t('admin.editRow.save')}
              </Button>,
            ]
          : [
              <Button key="back" onClick={() => setMode('edit')}>
                {t('admin.editRow.back')}
              </Button>,
              <Button key="confirm" type="primary" loading={saving} disabled={!hasChanges} onClick={handleConfirm}>
                {t('admin.editRow.confirm')}
              </Button>,
            ]
      }
    >
      {mode === 'edit' ? (
        <Form
          form={form}
          layout="vertical"
          initialValues={{ username: user.username, email: user.email, roles: user.roles }}
        >
          <Form.Item name="username" label={t('admin.editUser.fields.username')}>
            <Input />
          </Form.Item>
          <Form.Item name="email" label={t('admin.editUser.fields.email')}>
            <Input />
          </Form.Item>
          <Form.Item
            name="roles"
            label={t('admin.editUser.fields.roles')}
            extra={isSelf ? t('admin.editUser.selfRoleHint') : undefined}
          >
            <Select
              mode="multiple"
              options={availableRoles.map((r) => ({
                value: r.name,
                label: r.enabled ? r.name : t('admin.editUser.roleDisabledLabel', { role: r.name }),
                disabled: !r.enabled,
              }))}
            />
          </Form.Item>
        </Form>
      ) : hasChanges ? (
        <Descriptions column={1} bordered size="small">
          {pendingChanges.username !== undefined && (
            <Descriptions.Item label={t('admin.editUser.fields.username')}>
              <span style={{ textDecoration: 'line-through', opacity: 0.6 }}>{user.username}</span>
              {' → '}
              <strong>{pendingChanges.username}</strong>
            </Descriptions.Item>
          )}
          {pendingChanges.email !== undefined && (
            <Descriptions.Item label={t('admin.editUser.fields.email')}>
              <span style={{ textDecoration: 'line-through', opacity: 0.6 }}>{user.email}</span>
              {' → '}
              <strong>{pendingChanges.email}</strong>
            </Descriptions.Item>
          )}
          {pendingChanges.roles !== undefined && (
            <Descriptions.Item label={t('admin.editUser.fields.roles')}>
              <span style={{ textDecoration: 'line-through', opacity: 0.6 }}>{user.roles.join(', ') || '-'}</span>
              {' → '}
              <strong>{pendingChanges.roles.join(', ') || '-'}</strong>
            </Descriptions.Item>
          )}
        </Descriptions>
      ) : (
        <Typography.Text type="secondary">{t('admin.editRow.noChanges')}</Typography.Text>
      )}
    </Modal>
  );
}

function AuditEventsView({ userId }: { userId: string }) {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const [events, setEvents] = useState<AdminUserAuditEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!accessToken) return;
    setLoading(true);
    fetchAdminUserAuditEvents(accessToken, userId)
      .then(setEvents)
      .finally(() => setLoading(false));
  }, [accessToken, userId]);

  return (
    <Table
      size="small"
      rowKey={(record) => `${record.time}-${record.resourcePath}`}
      loading={loading}
      dataSource={events}
      pagination={false}
      columns={[
        {
          title: t('admin.userAudit.column.time'),
          dataIndex: 'time',
          render: (v: string) => new Date(v).toLocaleString(),
        },
        { title: t('admin.userAudit.column.operationType'), dataIndex: 'operationType' },
        { title: t('admin.userAudit.column.resourcePath'), dataIndex: 'resourcePath' },
        {
          title: t('admin.userAudit.column.representation'),
          dataIndex: 'representation',
          render: (v: string | null) =>
            v ? (
              <Typography.Text code style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                {v}
              </Typography.Text>
            ) : (
              '-'
            ),
        },
      ]}
    />
  );
}

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
