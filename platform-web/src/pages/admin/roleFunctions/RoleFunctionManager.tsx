import { useEffect, useState } from 'react';
import { App, Button, Card, Col, Input, Popconfirm, Row, Segmented, Space, Table, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import {
  deleteAdminRole,
  fetchAdminRoles,
  fetchAdminUsers,
  updateRoleDescription,
  updateRoleScope,
  updateRoleStatus,
} from '../../../api/adminApi';
import type { AdminRole, RoleScope } from '../../../types/admin';
import { ROLE_SCOPE_OPTIONS } from './constants';
import { AddRoleModal } from './AddRoleModal';
import { CopyRoleModal } from './CopyRoleModal';
import { RoleFunctionsPanel } from './RoleFunctionsPanel';
import { FunctionsCatalogTable } from './FunctionsCatalogTable';
import { RoleFunctionStatsCards } from './RoleFunctionStatsCards';
import { RoleDistributionChart } from './RoleDistributionChart';

/** Functions are managed through roles, not per user - a user's roles determine their functions,
 * so granting/updating/revoking always happens here, against a role, never against a single user. */
export function RoleFunctionManager() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { accessToken } = useAuth();
  const [roles, setRoles] = useState<AdminRole[]>([]);
  const [roleUserCounts, setRoleUserCounts] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [roleSearch, setRoleSearch] = useState('');
  const [addRoleModalOpen, setAddRoleModalOpen] = useState(false);
  const [copyingRole, setCopyingRole] = useState<string | null>(null);
  const [deletingRole, setDeletingRole] = useState<string | null>(null);
  const [savingDescriptionFor, setSavingDescriptionFor] = useState<string | null>(null);
  const [savingStatusFor, setSavingStatusFor] = useState<string | null>(null);
  const [savingScopeFor, setSavingScopeFor] = useState<string | null>(null);

  function loadRoles() {
    if (!accessToken) return;
    setLoading(true);
    fetchAdminRoles(accessToken)
      .then(setRoles)
      .finally(() => setLoading(false));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadRoles, [accessToken]);

  useEffect(() => {
    if (!accessToken) return;
    fetchAdminUsers(accessToken).then((users) => {
      const counts: Record<string, number> = {};
      for (const user of users) {
        for (const roleName of user.roles) {
          counts[roleName] = (counts[roleName] ?? 0) + 1;
        }
      }
      setRoleUserCounts(counts);
    });
  }, [accessToken]);

  async function handleDeleteRole(role: string) {
    if (!accessToken) return;
    setDeletingRole(role);
    try {
      await deleteAdminRole(accessToken, role);
      message.success(t('admin.roleFunctions.roleDeleted'));
      loadRoles();
    } catch (e) {
      message.error(
        e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.roleDeleteError')) : t('admin.roleFunctions.roleDeleteError'),
      );
    } finally {
      setDeletingRole(null);
    }
  }

  async function handleDescriptionChange(role: AdminRole, description: string) {
    if (!accessToken || description === (role.description ?? '')) return;
    setSavingDescriptionFor(role.name);
    try {
      await updateRoleDescription(accessToken, role.name, description);
      message.success(t('admin.roleFunctions.roleUpdated'));
      loadRoles();
    } catch (e) {
      message.error(
        e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.roleUpdateError')) : t('admin.roleFunctions.roleUpdateError'),
      );
    } finally {
      setSavingDescriptionFor(null);
    }
  }

  async function handleRoleStatusChange(role: AdminRole, enabled: boolean) {
    if (!accessToken) return;
    setSavingStatusFor(role.name);
    try {
      await updateRoleStatus(accessToken, role.name, enabled);
      message.success(enabled ? t('admin.roleFunctions.roleEnabled') : t('admin.roleFunctions.roleDisabled'));
      loadRoles();
    } catch (e) {
      message.error(
        e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.roleUpdateError')) : t('admin.roleFunctions.roleUpdateError'),
      );
    } finally {
      setSavingStatusFor(null);
    }
  }

  async function handleRoleScopeChange(role: AdminRole, scope: RoleScope) {
    if (!accessToken) return;
    setSavingScopeFor(role.name);
    try {
      await updateRoleScope(accessToken, role.name, scope);
      message.success(t('admin.roleFunctions.scopeUpdated'));
      loadRoles();
    } catch (e) {
      message.error(
        e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.scopeUpdateError')) : t('admin.roleFunctions.scopeUpdateError'),
      );
    } finally {
      setSavingScopeFor(null);
    }
  }

  const visibleRoles = roles.filter(
    (role) =>
      role.name.toLowerCase().includes(roleSearch.toLowerCase()) ||
      (role.description ?? '').toLowerCase().includes(roleSearch.toLowerCase()),
  );

  return (
    <Space orientation="vertical" size={24} style={{ width: '100%' }}>
      <RoleFunctionStatsCards />
      <RoleDistributionChart />
      <Row gutter={[24, 24]} align="stretch" style={{ alignItems: 'stretch' }}>
        <Col xs={24} xl={11} style={{ display: 'flex' }}>
          <Card title={t('admin.roleFunctions.title')} style={{ width: '100%', height: '100%' }}>
            <Typography.Paragraph type="secondary">{t('admin.roleFunctions.hint')}</Typography.Paragraph>
            <Space style={{ marginBottom: 16 }} wrap>
              <Input.Search
                style={{ width: 240 }}
                allowClear
                placeholder={t('admin.roleFunctions.searchRoles')}
                value={roleSearch}
                onChange={(e) => setRoleSearch(e.target.value)}
              />
              <Button type="primary" onClick={() => setAddRoleModalOpen(true)}>
                {t('admin.roleFunctions.newRole.button')}
              </Button>
            </Space>
            <Table
              rowKey="name"
              loading={loading}
              dataSource={visibleRoles}
              pagination={{ pageSize: 10 }}
              scroll={{ x: 'max-content' }}
              expandable={{ expandedRowRender: (record) => <RoleFunctionsPanel role={record.name} roleEnabled={record.enabled} /> }}
              columns={[
                { title: t('admin.roleFunctions.column.role'), dataIndex: 'name', width: 200 },
                {
                  title: t('admin.roleFunctions.column.description'),
                  key: 'description',
                  render: (_: unknown, role: AdminRole) => (
                    <Input
                      key={role.name + (role.description ?? '')}
                      defaultValue={role.description ?? ''}
                      placeholder={t('admin.roleFunctions.descriptionPlaceholder')}
                      disabled={savingDescriptionFor === role.name}
                      onBlur={(e) => handleDescriptionChange(role, e.target.value.trim())}
                      onPressEnter={(e) => e.currentTarget.blur()}
                    />
                  ),
                },
                {
                  title: t('admin.roleFunctions.column.userCount'),
                  key: 'userCount',
                  width: 120,
                  render: (_: unknown, role: AdminRole) => roleUserCounts[role.name] ?? 0,
                },
                {
                  title: t('admin.roleFunctions.column.scope'),
                  key: 'scope',
                  width: 220,
                  render: (_: unknown, role: AdminRole) => (
                    <Segmented
                      size="small"
                      value={role.scope}
                      disabled={savingScopeFor === role.name}
                      onChange={(value) => {
                        const scope = value as RoleScope;
                        if (scope === 'PLATFORM') {
                          modal.confirm({
                            title: t('admin.roleFunctions.confirmPlatformScope'),
                            content: t('admin.roleFunctions.confirmPlatformScopeDetail'),
                            okButtonProps: { danger: true },
                            okText: t('admin.roleFunctions.scope.PLATFORM'),
                            cancelText: t('admin.editRow.cancel'),
                            onOk: () => handleRoleScopeChange(role, scope),
                          });
                          return;
                        }
                        handleRoleScopeChange(role, scope);
                      }}
                      options={ROLE_SCOPE_OPTIONS.map((scope) => ({ label: t(`admin.roleFunctions.scope.${scope}`), value: scope }))}
                    />
                  ),
                },
                {
                  title: t('admin.column.status'),
                  key: 'status',
                  width: 160,
                  render: (_: unknown, role: AdminRole) => (
                    <Segmented
                      size="small"
                      value={role.enabled ? 'enabled' : 'disabled'}
                      disabled={savingStatusFor === role.name}
                      onChange={(value) => {
                        if (value === 'enabled') {
                          handleRoleStatusChange(role, true);
                          return;
                        }
                        modal.confirm({
                          title: t('admin.roleFunctions.confirmDisableRole'),
                          okButtonProps: { danger: true },
                          okText: t('admin.roleFunctions.disableAction'),
                          cancelText: t('admin.editRow.cancel'),
                          onOk: () => handleRoleStatusChange(role, false),
                        });
                      }}
                      options={[
                        { label: t('admin.roleFunctions.enabledTag'), value: 'enabled' },
                        { label: t('admin.roleFunctions.disabledTag'), value: 'disabled' },
                      ]}
                    />
                  ),
                },
                {
                  title: t('admin.editRow.actionsColumn'),
                  key: 'actions',
                  width: 170,
                  render: (_: unknown, role: AdminRole) => (
                    <Space size={4}>
                      <Button size="small" onClick={() => setCopyingRole(role.name)}>
                        {t('admin.roleFunctions.copyRoleAction')}
                      </Button>
                      <Popconfirm
                        title={t('admin.roleFunctions.confirmDeleteRole')}
                        onConfirm={() => handleDeleteRole(role.name)}
                      >
                        <Button size="small" danger loading={deletingRole === role.name}>
                          {t('admin.roleFunctions.deleteRoleAction')}
                        </Button>
                      </Popconfirm>
                    </Space>
                  ),
                },
              ]}
            />
            <AddRoleModal
              open={addRoleModalOpen}
              onClose={() => setAddRoleModalOpen(false)}
              onCreated={() => {
                setAddRoleModalOpen(false);
                loadRoles();
              }}
            />
            {copyingRole && (
              <CopyRoleModal
                open
                sourceRole={copyingRole}
                onClose={() => setCopyingRole(null)}
                onCopied={() => {
                  setCopyingRole(null);
                  loadRoles();
                }}
              />
            )}
          </Card>
        </Col>
        <Col xs={24} xl={13} style={{ display: 'flex' }}>
          <FunctionsCatalogTable />
        </Col>
      </Row>
    </Space>
  );
}
