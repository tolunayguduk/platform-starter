import { useEffect, useState } from 'react';
import { App, Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../auth/AuthContext';
import { ApiError } from '../../api/client';
import {
  createAdminRole,
  createAdminTableRow,
  deleteAdminRole,
  deleteAdminTableRow,
  fetchAdminRoles,
  fetchAdminTableRows,
  updateAdminTableRow,
} from '../../api/admin';

// Every function is one of exactly these three statuses for a given role - see AccessLevel.
const ACCESS_LEVEL_OPTIONS = ['GRANTED', 'VISIBLE_DENIED', 'HIDDEN'];

// A new function's fallback behavior for any role that has no explicit status set - see UiPolicy.
const UI_POLICY_OPTIONS = ['HIDE_IF_DENIED', 'DISABLE_IF_DENIED'];

// The role every admin action in this app is gated on - deleting it would lock everyone out of
// ever managing roles/functions again. Rejected server-side too; disabled here for a clearer UX.
const PROTECTED_ROLE = 'ADMIN';

interface PermissionCatalogEntry {
  id: number;
  key: string;
}

interface FunctionGrantRow {
  grantId: string;
  permissionId: number;
  functionKey: string;
  accessLevel: string;
}

function AddRoleModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  function handleClose() {
    form.resetFields();
    onClose();
  }

  async function handleSubmit() {
    if (!accessToken) return;
    const values = await form.validateFields();
    setSaving(true);
    try {
      await createAdminRole(accessToken, values.name.trim());
      message.success(t('admin.roleFunctions.newRole.success'));
      handleClose();
      onCreated();
    } catch (e) {
      message.error(
        e instanceof ApiError ? (e.body?.message ?? t('admin.roleFunctions.newRole.error')) : t('admin.roleFunctions.newRole.error'),
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open}
      title={t('admin.roleFunctions.newRole.title')}
      onCancel={handleClose}
      onOk={handleSubmit}
      confirmLoading={saving}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="name" label={t('admin.roleFunctions.newRole.placeholder')} rules={[{ required: true, whitespace: true }]}>
          <Input placeholder={t('admin.roleFunctions.newRole.placeholder')} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

interface AddFunctionModalProps {
  open: boolean;
  role: string;
  catalog: PermissionCatalogEntry[];
  alreadyGrantedPermissionIds: Set<number>;
  onClose: () => void;
  onCreated: () => void;
}

function AddFunctionModal({ open, role, catalog, alreadyGrantedPermissionIds, onClose, onCreated }: AddFunctionModalProps) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [mode, setMode] = useState<'existing' | 'new'>('existing');
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  const availableCatalog = catalog.filter((p) => !alreadyGrantedPermissionIds.has(p.id));

  function handleClose() {
    form.resetFields();
    setMode('existing');
    onClose();
  }

  async function handleSubmit() {
    if (!accessToken) return;
    const values = await form.validateFields();
    setSaving(true);
    try {
      let permissionId: number;
      if (mode === 'existing') {
        permissionId = values.permissionId;
      } else {
        const created = await createAdminTableRow(accessToken, 'PERMISSION', {
          key: values.key.trim(),
          ui_policy: values.uiPolicy,
        });
        permissionId = Number(created.id);
      }
      await createAdminTableRow(accessToken, 'ROLE_PERMISSION', {
        role_name: role,
        permission_id: permissionId,
        access_level: values.accessLevel,
      });
      message.success(t('admin.roleFunctions.granted'));
      handleClose();
      onCreated();
    } catch (e) {
      message.error(
        e instanceof ApiError
          ? (e.body?.message ?? t('admin.roleFunctions.newFunction.error'))
          : t('admin.roleFunctions.newFunction.error'),
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open}
      title={t('admin.roleFunctions.newFunction.title')}
      onCancel={handleClose}
      onOk={handleSubmit}
      confirmLoading={saving}
      destroyOnHidden
    >
      <Space style={{ marginBottom: 16 }}>
        <Button type={mode === 'existing' ? 'primary' : 'default'} onClick={() => setMode('existing')}>
          {t('admin.roleFunctions.newFunction.modeExisting')}
        </Button>
        <Button type={mode === 'new' ? 'primary' : 'default'} onClick={() => setMode('new')}>
          {t('admin.roleFunctions.newFunction.modeNew')}
        </Button>
      </Space>
      <Form form={form} layout="vertical" initialValues={{ accessLevel: 'GRANTED' }}>
        {mode === 'existing' ? (
          <Form.Item name="permissionId" label={t('admin.functionAccess.column.function')} rules={[{ required: true }]}>
            <Select
              showSearch
              optionFilterProp="label"
              placeholder={t('admin.roleFunctions.newFunction.keyPlaceholder')}
              options={availableCatalog.map((p) => ({ value: p.id, label: p.key }))}
            />
          </Form.Item>
        ) : (
          <>
            <Form.Item name="key" label={t('admin.roleFunctions.newFunction.keyPlaceholder')} rules={[{ required: true, whitespace: true }]}>
              <Input placeholder={t('admin.roleFunctions.newFunction.keyPlaceholder')} />
            </Form.Item>
            <Form.Item name="uiPolicy" label={t('admin.roleFunctions.newFunction.uiPolicyPlaceholder')} rules={[{ required: true }]}>
              <Select options={UI_POLICY_OPTIONS.map((policy) => ({ value: policy, label: t(`admin.roleFunctions.uiPolicy.${policy}`) }))} />
            </Form.Item>
          </>
        )}
        <Form.Item name="accessLevel" label={t('admin.roleFunctions.column.accessLevel')} rules={[{ required: true }]}>
          <Select options={ACCESS_LEVEL_OPTIONS.map((level) => ({ value: level, label: t(`admin.roleFunctions.accessLevel.${level}`) }))} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

function RoleFunctionsPanel({ role }: { role: string }) {
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
        const catalogEntries = permissions.rows.map((p) => ({ id: Number(p.id), key: String(p.key) }));
        setCatalog(catalogEntries);
        const catalogById = new Map(catalogEntries.map((p) => [p.id, p]));
        const grantRows = rolePermissions.rows
          .filter((r) => r.role_name === role)
          .map((r) => {
            const permissionId = Number(r.permission_id);
            return {
              grantId: String(r.id),
              permissionId,
              functionKey: catalogById.get(permissionId)?.key ?? `#${permissionId}`,
              accessLevel: String(r.access_level),
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
        <Button type="primary" onClick={() => setAddModalOpen(true)}>
          {t('admin.roleFunctions.newFunction.button')}
        </Button>
      </Space>
      <Table
        size="small"
        rowKey="grantId"
        loading={loading}
        dataSource={visibleGrants}
        pagination={false}
        columns={[
          { title: t('admin.functionAccess.column.function'), dataIndex: 'functionKey' },
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

interface FunctionCatalogRow {
  id: string;
  key: string;
  uiPolicy: string;
}

function AddFunctionCatalogModal({ open, onClose, onCreated }: { open: boolean; onClose: () => void; onCreated: () => void }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  function handleClose() {
    form.resetFields();
    onClose();
  }

  async function handleSubmit() {
    if (!accessToken) return;
    const values = await form.validateFields();
    setSaving(true);
    try {
      await createAdminTableRow(accessToken, 'PERMISSION', { key: values.key.trim(), ui_policy: values.uiPolicy });
      message.success(t('admin.roleFunctions.newFunction.success'));
      handleClose();
      onCreated();
    } catch (e) {
      message.error(
        e instanceof ApiError
          ? (e.body?.message ?? t('admin.roleFunctions.newFunction.error'))
          : t('admin.roleFunctions.newFunction.error'),
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal
      open={open}
      title={t('admin.roleFunctions.newFunction.title')}
      onCancel={handleClose}
      onOk={handleSubmit}
      confirmLoading={saving}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item name="key" label={t('admin.roleFunctions.newFunction.keyPlaceholder')} rules={[{ required: true, whitespace: true }]}>
          <Input placeholder={t('admin.roleFunctions.newFunction.keyPlaceholder')} />
        </Form.Item>
        <Form.Item
          name="uiPolicy"
          label={t('admin.roleFunctions.newFunction.uiPolicyPlaceholder')}
          rules={[{ required: true }]}
          initialValue="HIDE_IF_DENIED"
        >
          <Select options={UI_POLICY_OPTIONS.map((policy) => ({ value: policy, label: t(`admin.roleFunctions.uiPolicy.${policy}`) }))} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

/** The Permission catalog itself - independent of any one role. Editing key/ui_policy here
 * affects every role that has (or will have) this function; deleting it cascades to every role's
 * grant of it too (see AdminTableServiceImpl.deletePermission on the backend). */
function FunctionsCatalogTable() {
  const { t } = useTranslation();
  const { message } = App.useApp();
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
          .map((r) => ({ id: String(r.id), key: String(r.key), uiPolicy: String(r.ui_policy) }))
          .sort((a, b) => a.key.localeCompare(b.key));
        setFunctions(rows);
      })
      .finally(() => setLoading(false));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadFunctions, [accessToken]);

  const visibleFunctions = functions.filter(
    (fn) => fn.key.toLowerCase().includes(search.toLowerCase()) && (uiPolicyFilter === undefined || fn.uiPolicy === uiPolicyFilter),
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

  return (
    <Card title={t('admin.roleFunctions.catalogTitle')}>
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
        pagination={false}
        columns={[
          {
            title: t('admin.functionAccess.column.function'),
            key: 'key',
            render: (_: unknown, fn: FunctionCatalogRow) => (
              <Input key={fn.id + fn.key} defaultValue={fn.key} onBlur={(e) => handleKeyChange(fn, e.target.value.trim())} onPressEnter={(e) => e.currentTarget.blur()} />
            ),
          },
          {
            title: t('admin.roleFunctions.newFunction.uiPolicyPlaceholder'),
            key: 'uiPolicy',
            render: (_: unknown, fn: FunctionCatalogRow) => (
              <Select
                style={{ width: 260 }}
                loading={savingId === fn.id}
                value={fn.uiPolicy}
                onChange={(value) => handleUiPolicyChange(fn, value)}
                options={UI_POLICY_OPTIONS.map((policy) => ({ value: policy, label: t(`admin.roleFunctions.uiPolicy.${policy}`) }))}
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

/** Functions are managed through roles, not per user - a user's roles determine their functions,
 * so granting/updating/revoking always happens here, against a role, never against a single user. */
export function RoleFunctionManager() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [roles, setRoles] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [roleSearch, setRoleSearch] = useState('');
  const [addRoleModalOpen, setAddRoleModalOpen] = useState(false);
  const [deletingRole, setDeletingRole] = useState<string | null>(null);

  function loadRoles() {
    if (!accessToken) return;
    setLoading(true);
    fetchAdminRoles(accessToken)
      .then(setRoles)
      .finally(() => setLoading(false));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadRoles, [accessToken]);

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

  const visibleRoles = roles.filter((role) => role.toLowerCase().includes(roleSearch.toLowerCase())).map((role) => ({ role }));

  return (
    <>
      <Card title={t('admin.roleFunctions.title')} style={{ marginBottom: 24 }}>
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
          rowKey="role"
          loading={loading}
          dataSource={visibleRoles}
          pagination={false}
          expandable={{ expandedRowRender: (record) => <RoleFunctionsPanel role={record.role} /> }}
          columns={[
            { title: t('admin.roleFunctions.column.role'), dataIndex: 'role' },
            {
              title: t('admin.editRow.actionsColumn'),
              key: 'actions',
              render: (_: unknown, record: { role: string }) => (
                <Popconfirm
                  title={t('admin.roleFunctions.confirmDeleteRole')}
                  onConfirm={() => handleDeleteRole(record.role)}
                  disabled={record.role === PROTECTED_ROLE}
                >
                  <Button size="small" danger disabled={record.role === PROTECTED_ROLE} loading={deletingRole === record.role}>
                    {t('admin.roleFunctions.deleteRoleAction')}
                  </Button>
                </Popconfirm>
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
      </Card>
      <FunctionsCatalogTable />
    </>
  );
}
