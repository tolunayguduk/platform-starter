import { useEffect, useState } from 'react';
import { App, Button, Input, Popconfirm, Select, Space, Table, Tag, Tooltip } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { deleteAdminTableRow, fetchAdminTableRows, updateAdminTableRow } from '../../../api/adminApi';
import type { FunctionGrantRow, PermissionCatalogEntry } from '../../../types/admin';
import { ACCESS_LEVEL_OPTIONS } from './constants';
import { AddFunctionModal } from './AddFunctionModal';

export function RoleFunctionsPanel({ role, roleEnabled }: { role: string; roleEnabled: boolean }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [catalog, setCatalog] = useState<PermissionCatalogEntry[]>([]);
  const [grants, setGrants] = useState<FunctionGrantRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [addModalOpen, setAddModalOpen] = useState(false);

  function loadData() {
    if (!accessToken) return;
    setLoading(true);
    Promise.all([fetchAdminTableRows(accessToken, 'PERMISSION'), fetchAdminTableRows(accessToken, 'ROLE_PERMISSION')])
      .then(([permissions, rolePermissions]) => {
        const catalogEntries = permissions.rows.map((p) => ({ id: Number(p.id), key: String(p.key), enabled: Boolean(p.enabled) }));
        setCatalog(catalogEntries);
        const catalogById = new Map(catalogEntries.map((p) => [p.id, p]));
        const grantRows = rolePermissions.rows
          .filter((r) => r.role_name === role)
          .map((r) => {
            const permissionId = Number(r.permission_id);
            const permission = catalogById.get(permissionId);
            return {
              grantId: String(r.id),
              permissionId,
              functionKey: permission?.key ?? `#${permissionId}`,
              accessLevel: String(r.access_level),
              functionEnabled: permission?.enabled ?? true,
            };
          })
          .sort((a, b) => a.functionKey.localeCompare(b.functionKey));
        setGrants(grantRows);
      })
      .finally(() => setLoading(false));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadData, [accessToken, role]);

  const visibleGrants = grants.filter(
    (g) => g.functionKey.toLowerCase().includes(search.toLowerCase()) && (statusFilter === undefined || g.accessLevel === statusFilter),
  );

  async function handleAccessLevelChange(grant: FunctionGrantRow, accessLevel: string) {
    if (!accessToken) return;
    setSavingId(grant.grantId);
    try {
      await updateAdminTableRow(accessToken, 'ROLE_PERMISSION', grant.grantId, { access_level: accessLevel });
      message.success(t('admin.roleFunctions.updated'));
      loadData();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.error')) : t('admin.roleFunctions.error'));
    } finally {
      setSavingId(null);
    }
  }

  async function handleRevoke(grant: FunctionGrantRow) {
    if (!accessToken) return;
    setSavingId(grant.grantId);
    try {
      await deleteAdminTableRow(accessToken, 'ROLE_PERMISSION', grant.grantId);
      message.success(t('admin.roleFunctions.revoked'));
      loadData();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.error')) : t('admin.roleFunctions.error'));
    } finally {
      setSavingId(null);
    }
  }

  return (
    <div>
      <Space style={{ marginBottom: 12 }} wrap>
        <Input.Search
          style={{ width: 240 }}
          allowClear
          placeholder={t('admin.roleFunctions.searchFunctions')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <Select
          style={{ width: 220 }}
          allowClear
          placeholder={t('admin.roleFunctions.filterStatus')}
          value={statusFilter}
          onChange={setStatusFilter}
          options={ACCESS_LEVEL_OPTIONS.map((level) => ({ value: level, label: t(`admin.roleFunctions.accessLevel.${level}`) }))}
        />
        <Tooltip title={roleEnabled ? undefined : t('admin.roleFunctions.roleDisabledHint')}>
          <Button type="primary" disabled={!roleEnabled} onClick={() => setAddModalOpen(true)}>
            {t('admin.roleFunctions.newFunction.button')}
          </Button>
        </Tooltip>
      </Space>
      <Table
        size="small"
        rowKey="grantId"
        loading={loading}
        dataSource={visibleGrants}
        pagination={false}
        columns={[
          {
            title: t('admin.functionAccess.column.function'),
            dataIndex: 'functionKey',
            render: (functionKey: string, grant: FunctionGrantRow) => (
              <Space size={6}>
                {functionKey}
                {!grant.functionEnabled && <Tag color="red">{t('admin.roleFunctions.disabledTag')}</Tag>}
              </Space>
            ),
          },
          {
            title: t('admin.roleFunctions.column.accessLevel'),
            key: 'accessLevel',
            render: (_: unknown, grant: FunctionGrantRow) => (
              <Select
                style={{ width: 220 }}
                loading={savingId === grant.grantId}
                value={grant.accessLevel}
                onChange={(value) => handleAccessLevelChange(grant, value)}
                options={ACCESS_LEVEL_OPTIONS.map((level) => ({ value: level, label: t(`admin.roleFunctions.accessLevel.${level}`) }))}
              />
            ),
          },
          {
            title: t('admin.editRow.actionsColumn'),
            key: 'actions',
            render: (_: unknown, grant: FunctionGrantRow) => (
              <Popconfirm title={t('admin.roleFunctions.confirmRevoke')} onConfirm={() => handleRevoke(grant)}>
                <Button size="small" danger loading={savingId === grant.grantId}>
                  {t('admin.roleFunctions.revokeAction')}
                </Button>
              </Popconfirm>
            ),
          },
        ]}
      />
      <AddFunctionModal
        open={addModalOpen}
        role={role}
        catalog={catalog}
        alreadyGrantedPermissionIds={new Set(grants.map((g) => g.permissionId))}
        onClose={() => setAddModalOpen(false)}
        onCreated={() => {
          setAddModalOpen(false);
          loadData();
        }}
      />
    </div>
  );
}
