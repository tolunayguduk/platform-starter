import { useEffect, useState } from 'react';
import { Table, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { fetchAdminUserAuditEvents } from '../../../api/adminApi';
import type { AdminUserAuditEvent } from '../../../types/admin';

export function AuditEventsView({ userId }: { userId: string }) {
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
