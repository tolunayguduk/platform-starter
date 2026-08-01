import { useEffect, useState } from 'react';
import { Table, Tag } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { fetchAdminTableRows } from '../../../api/adminApi';
import type { UserFunctionGrantRow } from '../../../types/admin';
import { ACCESS_LEVEL_COLOR } from './tableFormat';

/** Only functions actually assigned to one of this user's roles - not the full catalog, and not a
 * single merged "what wins" status. A user can hold several roles, and the same function can be
 * configured differently per role, so each (function, role) grant gets its own row, with the role
 * it came from right next to it. Read-only: this is a raw view of role_permission, not a table to
 * edit (edit from the Role-Function tab, against the role, not here). */
export function UserFunctionAccessView({ userId, userRoles }: { userId: string; userRoles: string[] }) {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const [rows, setRows] = useState<UserFunctionGrantRow[]>([]);
  const [loading, setLoading] = useState(true);
  const roleSetKey = userRoles.join(',');

  useEffect(() => {
    if (!accessToken) return;
    setLoading(true);
    Promise.all([fetchAdminTableRows(accessToken, 'PERMISSION'), fetchAdminTableRows(accessToken, 'ROLE_PERMISSION')])
      .then(([catalog, rolePermissions]) => {
        const catalogById = new Map(
          catalog.rows.map((p) => [
            Number(p.id),
            { key: String(p.key), description: p.description == null ? null : String(p.description) },
          ]),
        );
        const grantRows = rolePermissions.rows
          .filter((r) => userRoles.includes(String(r.role_name)))
          .map((r) => {
            const permission = catalogById.get(Number(r.permission_id));
            return {
              rowKey: `${r.permission_id}-${r.role_name}`,
              functionKey: permission?.key ?? `#${r.permission_id}`,
              description: permission?.description ?? null,
              role: String(r.role_name),
              accessLevel: String(r.access_level),
            };
          })
          .sort((a, b) => a.functionKey.localeCompare(b.functionKey) || a.role.localeCompare(b.role));
        setRows(grantRows);
      })
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken, userId, roleSetKey]);

  return (
    <Table
      size="small"
      rowKey="rowKey"
      loading={loading}
      dataSource={rows}
      pagination={false}
      columns={[
        { title: t('admin.functionAccess.column.function'), dataIndex: 'functionKey' },
        { title: t('admin.roleFunctions.column.description'), dataIndex: 'description', render: (v: string | null) => v ?? '-' },
        { title: t('admin.functionAccess.column.role'), dataIndex: 'role' },
        {
          title: t('admin.functionAccess.column.status'),
          dataIndex: 'accessLevel',
          render: (accessLevel: string) => (
            <Tag color={ACCESS_LEVEL_COLOR[accessLevel] ?? 'default'}>{t(`admin.roleFunctions.accessLevel.${accessLevel}`)}</Tag>
          ),
        },
      ]}
    />
  );
}
