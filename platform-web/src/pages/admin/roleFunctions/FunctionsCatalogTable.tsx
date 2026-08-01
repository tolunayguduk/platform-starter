import { useEffect, useState } from 'react';
import { App, Button, Card, Input, Popconfirm, Segmented, Select, Space, Table, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { deleteAdminTableRow, fetchAdminTableRows, updateAdminTableRow } from '../../../api/adminApi';
import type { FunctionCatalogRow } from '../../../types/admin';
import { UI_POLICY_OPTIONS } from './constants';
import { AddFunctionCatalogModal } from './AddFunctionCatalogModal';

/** The Permission catalog itself - independent of any one role. Editing key/ui_policy here
 * affects every role that has (or will have) this function; deleting it cascades to every role's
 * grant of it too (see AdminTableServiceImpl.deletePermission on the backend). */
export function FunctionsCatalogTable() {
  const { t } = useTranslation();
  const { message, modal } = App.useApp();
  const { accessToken } = useAuth();
  const [functions, setFunctions] = useState<FunctionCatalogRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [uiPolicyFilter, setUiPolicyFilter] = useState<string | undefined>(undefined);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [addModalOpen, setAddModalOpen] = useState(false);

  function loadFunctions() {
    if (!accessToken) return;
    setLoading(true);
    fetchAdminTableRows(accessToken, 'PERMISSION')
      .then((data) => {
        const rows = data.rows
          .map((r) => ({
            id: String(r.id),
            key: String(r.key),
            uiPolicy: String(r.ui_policy),
            description: r.description == null ? null : String(r.description),
            enabled: Boolean(r.enabled),
          }))
          .sort((a, b) => a.key.localeCompare(b.key));
        setFunctions(rows);
      })
      .finally(() => setLoading(false));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadFunctions, [accessToken]);

  const visibleFunctions = functions.filter(
    (fn) =>
      (fn.key.toLowerCase().includes(search.toLowerCase()) || (fn.description ?? '').toLowerCase().includes(search.toLowerCase())) &&
      (uiPolicyFilter === undefined || fn.uiPolicy === uiPolicyFilter),
  );

  async function handleKeyChange(fn: FunctionCatalogRow, newKey: string) {
    if (!accessToken || !newKey || newKey === fn.key) return;
    setSavingId(fn.id);
    try {
      await updateAdminTableRow(accessToken, 'PERMISSION', fn.id, { key: newKey });
      message.success(t('admin.roleFunctions.functionUpdated'));
      loadFunctions();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.error')) : t('admin.roleFunctions.error'));
    } finally {
      setSavingId(null);
    }
  }

  async function handleDescriptionChange(fn: FunctionCatalogRow, description: string) {
    if (!accessToken || description === (fn.description ?? '')) return;
    setSavingId(fn.id);
    try {
      await updateAdminTableRow(accessToken, 'PERMISSION', fn.id, { description: description || null });
      message.success(t('admin.roleFunctions.functionUpdated'));
      loadFunctions();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.error')) : t('admin.roleFunctions.error'));
    } finally {
      setSavingId(null);
    }
  }

  async function handleUiPolicyChange(fn: FunctionCatalogRow, uiPolicy: string) {
    if (!accessToken) return;
    setSavingId(fn.id);
    try {
      await updateAdminTableRow(accessToken, 'PERMISSION', fn.id, { ui_policy: uiPolicy });
      message.success(t('admin.roleFunctions.functionUpdated'));
      loadFunctions();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.error')) : t('admin.roleFunctions.error'));
    } finally {
      setSavingId(null);
    }
  }

  async function handleDelete(fn: FunctionCatalogRow) {
    if (!accessToken) return;
    setSavingId(fn.id);
    try {
      await deleteAdminTableRow(accessToken, 'PERMISSION', fn.id);
      message.success(t('admin.roleFunctions.functionDeleted'));
      loadFunctions();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.error')) : t('admin.roleFunctions.error'));
    } finally {
      setSavingId(null);
    }
  }

  async function handleFunctionStatusChange(fn: FunctionCatalogRow, enabled: boolean) {
    if (!accessToken) return;
    setSavingId(fn.id);
    try {
      await updateAdminTableRow(accessToken, 'PERMISSION', fn.id, { enabled });
      message.success(enabled ? t('admin.roleFunctions.functionEnabled') : t('admin.roleFunctions.functionDisabled'));
      loadFunctions();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.error')) : t('admin.roleFunctions.error'));
    } finally {
      setSavingId(null);
    }
  }

  return (
    <Card title={t('admin.roleFunctions.catalogTitle')} style={{ width: '100%', height: '100%' }}>
      <Typography.Paragraph type="secondary">{t('admin.roleFunctions.catalogHint')}</Typography.Paragraph>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          style={{ width: 240 }}
          allowClear
          placeholder={t('admin.roleFunctions.searchFunctions')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <Select
          style={{ width: 240 }}
          allowClear
          placeholder={t('admin.roleFunctions.filterUiPolicy')}
          value={uiPolicyFilter}
          onChange={setUiPolicyFilter}
          options={UI_POLICY_OPTIONS.map((policy) => ({ value: policy, label: t(`admin.roleFunctions.uiPolicy.${policy}`) }))}
        />
        <Button type="primary" onClick={() => setAddModalOpen(true)}>
          {t('admin.roleFunctions.newFunction.button')}
        </Button>
      </Space>
      <Table
        rowKey="id"
        loading={loading}
        dataSource={visibleFunctions}
        pagination={{ pageSize: 10 }}
        scroll={{ x: 'max-content' }}
        columns={[
          {
            title: t('admin.functionAccess.column.function'),
            key: 'key',
            width: 220,
            render: (_: unknown, fn: FunctionCatalogRow) => (
              <Input key={fn.id + fn.key} defaultValue={fn.key} onBlur={(e) => handleKeyChange(fn, e.target.value.trim())} onPressEnter={(e) => e.currentTarget.blur()} />
            ),
          },
          {
            title: t('admin.roleFunctions.column.description'),
            key: 'description',
            width: 200,
            render: (_: unknown, fn: FunctionCatalogRow) => (
              <Input
                key={fn.id + (fn.description ?? '')}
                defaultValue={fn.description ?? ''}
                placeholder={t('admin.roleFunctions.descriptionPlaceholder')}
                onBlur={(e) => handleDescriptionChange(fn, e.target.value.trim())}
                onPressEnter={(e) => e.currentTarget.blur()}
              />
            ),
          },
          {
            title: t('admin.roleFunctions.column.uiPolicy'),
            key: 'uiPolicy',
            width: 200,
            render: (_: unknown, fn: FunctionCatalogRow) => (
              <Select
                style={{ width: 200 }}
                loading={savingId === fn.id}
                value={fn.uiPolicy}
                onChange={(value) => handleUiPolicyChange(fn, value)}
                options={UI_POLICY_OPTIONS.map((policy) => ({ value: policy, label: t(`admin.roleFunctions.uiPolicy.${policy}`) }))}
              />
            ),
          },
          {
            title: t('admin.column.status'),
            key: 'status',
            width: 160,
            render: (_: unknown, fn: FunctionCatalogRow) => (
              <Segmented
                size="small"
                value={fn.enabled ? 'enabled' : 'disabled'}
                disabled={savingId === fn.id}
                onChange={(value) => {
                  if (value === 'enabled') {
                    handleFunctionStatusChange(fn, true);
                    return;
                  }
                  modal.confirm({
                    title: t('admin.roleFunctions.confirmDisableFunction'),
                    okButtonProps: { danger: true },
                    okText: t('admin.roleFunctions.disableAction'),
                    cancelText: t('admin.editRow.cancel'),
                    onOk: () => handleFunctionStatusChange(fn, false),
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
            render: (_: unknown, fn: FunctionCatalogRow) => (
              <Popconfirm title={t('admin.roleFunctions.confirmDeleteFunction')} onConfirm={() => handleDelete(fn)}>
                <Button size="small" danger loading={savingId === fn.id}>
                  {t('admin.roleFunctions.deleteFunctionAction')}
                </Button>
              </Popconfirm>
            ),
          },
        ]}
      />
      <AddFunctionCatalogModal
        open={addModalOpen}
        onClose={() => setAddModalOpen(false)}
        onCreated={() => {
          setAddModalOpen(false);
          loadFunctions();
        }}
      />
    </Card>
  );
}
