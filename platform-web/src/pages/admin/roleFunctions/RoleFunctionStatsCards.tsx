import { useEffect, useState } from 'react';
import { Card, Col, Row, Statistic } from 'antd';
import { ApiOutlined, CheckCircleOutlined, TeamOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { fetchAdminRoles, fetchAdminTableRows, fetchAdminUsers } from '../../../api/adminApi';
import { THEME_COLORS } from '../../../utils/themeColors';

function colorFor(key: string): string {
  return THEME_COLORS.find((c) => c.key === key)!.color;
}

interface Totals {
  roles: number;
  assignedRoles: number;
  functions: number;
  assignedFunctions: number;
}

const EMPTY_TOTALS: Totals = { roles: 0, assignedRoles: 0, functions: 0, assignedFunctions: 0 };

/** "Assigned" means at least one grant exists - a role with at least one user holding it, a
 * function with at least one ROLE_PERMISSION grant on any role, at any access level. */
export function RoleFunctionStatsCards() {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const [totals, setTotals] = useState<Totals>(EMPTY_TOTALS);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!accessToken) return;
    setLoading(true);
    Promise.all([
      fetchAdminRoles(accessToken),
      fetchAdminUsers(accessToken),
      fetchAdminTableRows(accessToken, 'PERMISSION'),
      fetchAdminTableRows(accessToken, 'ROLE_PERMISSION'),
    ])
      .then(([roles, users, permissions, rolePermissions]) => {
        const assignedRoleNames = new Set(users.flatMap((u) => u.roles));
        const assignedFunctionIds = new Set(rolePermissions.rows.map((r) => r.permission_id));
        setTotals({
          roles: roles.length,
          assignedRoles: roles.filter((r) => assignedRoleNames.has(r.name)).length,
          functions: permissions.rows.length,
          assignedFunctions: assignedFunctionIds.size,
        });
      })
      .finally(() => setLoading(false));
  }, [accessToken]);

  const cards = [
    { key: 'roles', label: t('admin.roleFunctions.stats.totalRoles'), value: totals.roles, color: colorFor('blue'), icon: <TeamOutlined /> },
    {
      key: 'assignedRoles',
      label: t('admin.roleFunctions.stats.assignedRoles'),
      value: totals.assignedRoles,
      color: colorFor('green'),
      icon: <CheckCircleOutlined />,
    },
    { key: 'functions', label: t('admin.roleFunctions.stats.totalFunctions'), value: totals.functions, color: colorFor('purple'), icon: <ApiOutlined /> },
    {
      key: 'assignedFunctions',
      label: t('admin.roleFunctions.stats.assignedFunctions'),
      value: totals.assignedFunctions,
      color: colorFor('green'),
      icon: <CheckCircleOutlined />,
    },
  ];

  return (
    <Row gutter={16}>
      {cards.map((c) => (
        <Col key={c.key} xs={12} md={6}>
          <Card loading={loading} style={{ borderTop: `3px solid ${c.color}` }}>
            <Statistic title={c.label} value={c.value} prefix={c.icon} valueStyle={{ color: c.color }} />
          </Card>
        </Col>
      ))}
    </Row>
  );
}
