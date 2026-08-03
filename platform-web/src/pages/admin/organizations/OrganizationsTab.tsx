import { useEffect, useState } from 'react';
import { App, Button, Card, Input, Popconfirm, Space, Table, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { useAdminAccessScope } from '../../../hooks/useAdminAccessScope';
import { ApiError } from '../../../api/client';
import { deleteOrganization, fetchOrganizations, updateOrganizationDescription } from '../../../api/adminApi';
import type { Organization } from '../../../types/admin';
import { AddOrganizationModal } from './AddOrganizationModal';
import { OrganizationDetailPanel } from './OrganizationDetailPanel';

/** PLATFORM-scoped callers see and manage every organization; ORGANIZATION-scoped callers only
 * their own (a user may belong to more than one) - the backend enforces this independently
 * (AdminOrganizationService), this just hides actions the caller couldn't perform anyway. */
export function OrganizationsTab() {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const { scope } = useAdminAccessScope();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [savingDescriptionFor, setSavingDescriptionFor] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  function loadOrganizations() {
    if (!accessToken) return;
    setLoading(true);
    fetchOrganizations(accessToken)
      .then(setOrganizations)
      .finally(() => setLoading(false));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadOrganizations, [accessToken]);

  async function handleDescriptionChange(org: Organization, description: string) {
    if (!accessToken || description === (org.description ?? '')) return;
    setSavingDescriptionFor(org.id);
    try {
      await updateOrganizationDescription(accessToken, org.id, description);
      message.success(t('admin.organizations.updated'));
      loadOrganizations();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.organizations.updateError')) : t('admin.organizations.updateError'));
    } finally {
      setSavingDescriptionFor(null);
    }
  }

  async function handleDelete(org: Organization) {
    if (!accessToken) return;
    setDeletingId(org.id);
    try {
      await deleteOrganization(accessToken, org.id);
      message.success(t('admin.organizations.deleted'));
      loadOrganizations();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.organizations.deleteError')) : t('admin.organizations.deleteError'));
    } finally {
      setDeletingId(null);
    }
  }

  const visibleOrganizations = organizations.filter(
    (org) => org.name.toLowerCase().includes(search.toLowerCase()) || (org.description ?? '').toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <Card title={t('admin.organizations.title')}>
      <Typography.Paragraph type="secondary">{t('admin.organizations.hint')}</Typography.Paragraph>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          style={{ width: 240 }}
          allowClear
          placeholder={t('admin.organizations.search')}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        {scope?.platformScoped && (
          <Button type="primary" onClick={() => setAddModalOpen(true)}>
            {t('admin.organizations.newOrganization.button')}
          </Button>
        )}
      </Space>
      <Table
        rowKey="id"
        loading={loading}
        dataSource={visibleOrganizations}
        pagination={{ pageSize: 10 }}
        scroll={{ x: 'max-content' }}
        expandable={{
          expandedRowRender: (record) => <OrganizationDetailPanel organization={record} onOrganizationChanged={loadOrganizations} />,
        }}
        columns={[
          { title: t('admin.organizations.column.name'), dataIndex: 'name', width: 220 },
          {
            title: t('admin.organizations.column.description'),
            key: 'description',
            render: (_: unknown, org: Organization) => (
              <Input
                key={org.id + (org.description ?? '')}
                defaultValue={org.description ?? ''}
                placeholder={t('admin.roleFunctions.descriptionPlaceholder')}
                disabled={savingDescriptionFor === org.id}
                onBlur={(e) => handleDescriptionChange(org, e.target.value.trim())}
                onPressEnter={(e) => e.currentTarget.blur()}
              />
            ),
          },
          { title: t('admin.organizations.column.memberCount'), dataIndex: 'memberCount', width: 140 },
          ...(scope?.platformScoped
            ? [
                {
                  title: t('admin.editRow.actionsColumn'),
                  key: 'actions',
                  width: 100,
                  render: (_: unknown, org: Organization) => (
                    <Popconfirm title={t('admin.organizations.confirmDelete')} onConfirm={() => handleDelete(org)}>
                      <Button size="small" danger loading={deletingId === org.id}>
                        {t('admin.roleFunctions.deleteRoleAction')}
                      </Button>
                    </Popconfirm>
                  ),
                },
              ]
            : []),
        ]}
      />
      <AddOrganizationModal
        open={addModalOpen}
        onClose={() => setAddModalOpen(false)}
        onCreated={() => {
          setAddModalOpen(false);
          loadOrganizations();
        }}
      />
    </Card>
  );
}
