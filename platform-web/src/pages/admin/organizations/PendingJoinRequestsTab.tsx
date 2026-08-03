import { useEffect, useState } from 'react';
import { App, Button, Popconfirm, Space, Table, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { ApiError } from '../../../api/client';
import { approveJoinRequest, fetchPendingJoinRequests, rejectJoinRequest } from '../../../api/adminApi';
import type { OrganizationMembershipRequest } from '../../../types/admin';

/** Pending self-service join requests (not invites - a manager never sees invites they sent,
 * only the target user does, on their own dashboard). */
export function PendingJoinRequestsTab({ organizationId, onResolved }: { organizationId: string; onResolved: () => void }) {
  const { t } = useTranslation();
  const { message } = App.useApp();
  const { accessToken } = useAuth();
  const [requests, setRequests] = useState<OrganizationMembershipRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [resolvingId, setResolvingId] = useState<number | null>(null);

  function loadRequests() {
    if (!accessToken) return;
    setLoading(true);
    fetchPendingJoinRequests(accessToken, organizationId)
      .then(setRequests)
      .finally(() => setLoading(false));
  }

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(loadRequests, [accessToken, organizationId]);

  async function handleApprove(request: OrganizationMembershipRequest) {
    if (!accessToken) return;
    setResolvingId(request.id);
    try {
      await approveJoinRequest(accessToken, organizationId, request.id);
      message.success(t('admin.organizations.pendingRequests.approved'));
      loadRequests();
      onResolved();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.organizations.pendingRequests.error')) : t('admin.organizations.pendingRequests.error'));
    } finally {
      setResolvingId(null);
    }
  }

  async function handleReject(request: OrganizationMembershipRequest) {
    if (!accessToken) return;
    setResolvingId(request.id);
    try {
      await rejectJoinRequest(accessToken, organizationId, request.id);
      message.success(t('admin.organizations.pendingRequests.rejected'));
      loadRequests();
    } catch (e) {
      message.error(e instanceof ApiError ? (e.body?.message ?? t('admin.organizations.pendingRequests.error')) : t('admin.organizations.pendingRequests.error'));
    } finally {
      setResolvingId(null);
    }
  }

  return (
    <div>
      {requests.length === 0 && !loading ? (
        <Typography.Paragraph type="secondary">{t('admin.organizations.pendingRequests.empty')}</Typography.Paragraph>
      ) : (
        <Table
          size="small"
          rowKey="id"
          loading={loading}
          dataSource={requests}
          pagination={false}
          columns={[
            { title: t('admin.column.username'), dataIndex: 'username' },
            {
              title: t('admin.organizations.pendingRequests.column.requestedAt'),
              dataIndex: 'createdAt',
              render: (createdAt: string) => new Date(createdAt).toLocaleString(),
            },
            {
              title: t('admin.editRow.actionsColumn'),
              key: 'actions',
              render: (_: unknown, request: OrganizationMembershipRequest) => (
                <Space>
                  <Button size="small" type="primary" loading={resolvingId === request.id} onClick={() => handleApprove(request)}>
                    {t('admin.organizations.pendingRequests.approve')}
                  </Button>
                  <Popconfirm title={t('admin.organizations.pendingRequests.confirmReject')} onConfirm={() => handleReject(request)}>
                    <Button size="small" danger loading={resolvingId === request.id}>
                      {t('admin.organizations.pendingRequests.reject')}
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />
      )}
    </div>
  );
}
