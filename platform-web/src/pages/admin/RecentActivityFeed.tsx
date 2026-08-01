import { useEffect, useState } from 'react';
import { Card, Empty, Space, Tag, Timeline, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../store/AuthContext';
import { fetchRecentActivity } from '../../api/adminApi';
import type { AdminUserAuditEvent } from '../../types/admin';

// Keycloak's own admin-event operation types - anything else falls back to gray rather than
// being silently dropped.
const OPERATION_COLOR: Record<string, string> = { CREATE: 'green', UPDATE: 'blue', DELETE: 'red', ACTION: 'purple' };

/** Realm-wide feed of Keycloak's own admin event log (identity edits, role-mapping changes,
 * role/client changes) - not scoped to one user, unlike AuditEventsView. Read-only, nothing here
 * is ever cached locally. */
export function RecentActivityFeed() {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const [events, setEvents] = useState<AdminUserAuditEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!accessToken) return;
    setLoading(true);
    fetchRecentActivity(accessToken, 20)
      .then(setEvents)
      .finally(() => setLoading(false));
  }, [accessToken]);

  return (
    <Card title={t('admin.recentActivity.title')} loading={loading}>
      {events.length === 0 ? (
        <Empty description={t('admin.recentActivity.empty')} />
      ) : (
        <div style={{ maxHeight: 360, overflowY: 'auto' }}>
          <Timeline
            items={events.map((e) => ({
              key: `${e.time}-${e.resourcePath}`,
              color: OPERATION_COLOR[e.operationType] ?? 'gray',
              children: (
                <>
                  <Space>
                    <Tag color={OPERATION_COLOR[e.operationType] ?? 'default'}>{e.operationType}</Tag>
                    <Typography.Text type="secondary">{new Date(e.time).toLocaleString()}</Typography.Text>
                  </Space>
                  <div>
                    <Typography.Text code>{e.resourcePath}</Typography.Text>
                  </div>
                </>
              ),
            }))}
          />
        </div>
      )}
    </Card>
  );
}
