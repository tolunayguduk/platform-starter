import { useEffect, useState } from 'react';
import { App, Button, Card, Input, Select, Space, Table, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../auth/AuthContext';
import { ApiError } from '../../api/client';
import {
  createAdminRole,
  createAdminTableRow,
  deleteAdminTableRow,
  fetchAdminRoles,
  fetchAdminTableRows,
  updateAdminTableRow,
} from '../../api/admin';

// Every function is one of exactly these three statuses for a given role - see AccessLevel.
const ACCESS_LEVEL_OPTIONS = ['GRANTED', 'VISIBLE_DENIED', 'HIDDEN'];

// A new function's fallback behavior for any role that has no explicit status set - see UiPolicy.
const UI_POLICY_OPTIONS = ['HIDE_IF_DENIED', 'DISABLE_IF_DENIED'];

interface FunctionRow {
  permissionId: number;
  functionKey: string;
  grantId: string | null;
  accessLevel: string | null;
}

/** Functions are managed through roles, not per user - a user's roles determine their functions,
 * so granting/updating/revoking always happens here, against a role, never against a single user. */
export function RoleFunctionManager() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [roles, setRoles] = useState<string[]>([]);
  const [selectedRole, setSelectedRole] = useState<string | null>(null);
  const [rows, setRows] = useState<FunctionRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [newRoleName, setNewRoleName] = useState('');
  const [creatingRole, setCreatingRole] = useState(false);
  const [newFunctionKey, setNewFunctionKey] = useState('');
  const [newFunctionUiPolicy, setNewFunctionUiPolicy] = useState<string | undefined>(undefined);
  const [creatingFunction, setCreatingFunction] = useState(false);

  function loadRoles() {
    if (!accessToken) return Promise.resolve();
    return fetchAdminRoles(accessToken).then(setRoles);
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { loadRoles(); }, [accessToken]);

  function loadRows() {
    if (!accessToken || !selectedRole) {
      setRows([]);
      return;
    }
    setLoading(true);
    Promise.all([fetchAdminTableRows(accessToken, 'PERMISSION'), fetchAdminTableRows(accessToken, 'ROLE_PERMISSION')])
      .then(([permissions, rolePermissions]) => {
        const grantsByPermissionId = new Map(
          rolePermissions.rows
            .filter((row) => row.role_name === selectedRole)
            .map((row) => [Number(row.permission_id), row]),
        );
        const merged = permissions.rows
          .map((permission) => {
            const permissionId = Number(permission.id);
            const grant = grantsByPermissionId.get(permissionId);
            return {
              permissionId,
              functionKey: String(permission.key),
              grantId: grant ? String(grant.id) : null,
              accessLevel: grant ? String(grant.access_level) : null,
            };
          })
          .sort((a, b) => a.functionKey.localeCompare(b.functionKey));
        setRows(merged);
      })
      .finally(() => setLoading(false));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadRows, [accessToken, selectedRole]);

  async function handleCreateRole() {
    const name = newRoleName.trim();
    if (!accessToken || !name) return;
    setCreatingRole(true);
    try {
      await createAdminRole(accessToken, name);
      message.success(t('admin.roleFunctions.newRole.success'));
      await loadRoles();
      setSelectedRole(name);
      setNewRoleName('');
    } catch (e) {
      message.error(
        e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.newRole.error')) : t('admin.roleFunctions.newRole.error'),
      );
    } finally {
      setCreatingRole(false);
    }
  }

  async function handleCreateFunction() {
    const key = newFunctionKey.trim();
    if (!accessToken || !key || !newFunctionUiPolicy) return;
    setCreatingFunction(true);
    try {
      await createAdminTableRow(accessToken, 'PERMISSION', { key, ui_policy: newFunctionUiPolicy });
      message.success(t('admin.roleFunctions.newFunction.success'));
      setNewFunctionKey('');
      setNewFunctionUiPolicy(undefined);
      loadRows();
    } catch (e) {
      message.error(
        e instanceof ApiError
          ? (e.body?.message ?? t('admin.roleFunctions.newFunction.error'))
          : t('admin.roleFunctions.newFunction.error'),
      );
    } finally {
      setCreatingFunction(false);
    }
  }

  async function handleAccessLevelChange(row: FunctionRow, accessLevel: string | undefined) {
    if (!accessToken || !selectedRole) return;
    setSavingId(row.permissionId);
    try {
      if (accessLevel === undefined) {
        if (row.grantId) {
          await deleteAdminTableRow(accessToken, 'ROLE_PERMISSION', row.grantId);
          message.success(t('admin.roleFunctions.revoked'));
        }
      } else if (row.grantId) {
        await updateAdminTableRow(accessToken, 'ROLE_PERMISSION', row.grantId, { access_level: accessLevel });
        message.success(t('admin.roleFunctions.updated'));
      } else {
        await createAdminTableRow(accessToken, 'ROLE_PERMISSION', {
          role_name: selectedRole,
          permission_id: row.permissionId,
          access_level: accessLevel,
        });
        message.success(t('admin.roleFunctions.granted'));
      }
      loadRows();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.error')) : t('admin.roleFunctions.error'));
    } finally {
      setSavingId(null);
    }
  }

  return (
    <Card title={t('admin.roleFunctions.title')}>
      <Typography.Paragraph type="secondary">{t('admin.roleFunctions.hint')}</Typography.Paragraph>

      <Typography.Title level={5}>{t('admin.roleFunctions.newRole.title')}</Typography.Title>
      <Space.Compact style={{ marginBottom: 24 }}>
        <Input
          style={{ width: 240 }}
          placeholder={t('admin.roleFunctions.newRole.placeholder')}
          value={newRoleName}
          onChange={(e) => setNewRoleName(e.target.value)}
          onPressEnter={handleCreateRole}
        />
        <Button type="primary" loading={creatingRole} disabled={!newRoleName.trim()} onClick={handleCreateRole}>
          {t('admin.roleFunctions.newRole.button')}
        </Button>
      </Space.Compact>

      <Typography.Title level={5}>{t('admin.roleFunctions.newFunction.title')}</Typography.Title>
      <Space style={{ marginBottom: 24 }} wrap>
        <Input
          style={{ width: 240 }}
          placeholder={t('admin.roleFunctions.newFunction.keyPlaceholder')}
          value={newFunctionKey}
          onChange={(e) => setNewFunctionKey(e.target.value)}
        />
        <Select
          style={{ width: 220 }}
          placeholder={t('admin.roleFunctions.newFunction.uiPolicyPlaceholder')}
          value={newFunctionUiPolicy}
          onChange={setNewFunctionUiPolicy}
          options={UI_POLICY_OPTIONS.map((policy) => ({ value: policy, label: t(`admin.roleFunctions.uiPolicy.${policy}`) }))}
        />
        <Button
          type="primary"
          loading={creatingFunction}
          disabled={!newFunctionKey.trim() || !newFunctionUiPolicy}
          onClick={handleCreateFunction}
        >
          {t('admin.roleFunctions.newFunction.button')}
        </Button>
      </Space>

      <Select
        style={{ width: 240, marginBottom: 16 }}
        placeholder={t('admin.roleFunctions.rolePlaceholder')}
        value={selectedRole ?? undefined}
        onChange={setSelectedRole}
        options={roles.map((role) => ({ value: role, label: role }))}
      />
      {!selectedRole ? (
        <Typography.Paragraph type="secondary">{t('admin.roleFunctions.emptyState')}</Typography.Paragraph>
      ) : (
        <Table
          rowKey="permissionId"
          loading={loading}
          dataSource={rows}
          pagination={false}
          columns={[
            { title: t('admin.functionAccess.column.function'), dataIndex: 'functionKey' },
            {
              title: t('admin.roleFunctions.column.accessLevel'),
              key: 'accessLevel',
              render: (_: unknown, row: FunctionRow) => (
                <Select
                  allowClear
                  style={{ width: 220 }}
                  placeholder={t('admin.roleFunctions.notGranted')}
                  loading={savingId === row.permissionId}
                  value={row.accessLevel ?? undefined}
                  onChange={(value) => value && handleAccessLevelChange(row, value)}
                  onClear={() => handleAccessLevelChange(row, undefined)}
                  options={ACCESS_LEVEL_OPTIONS.map((level) => ({
                    value: level,
                    label: t(`admin.roleFunctions.accessLevel.${level}`),
                  }))}
                />
              ),
            },
          ]}
        />
      )}
    </Card>
  );
}
