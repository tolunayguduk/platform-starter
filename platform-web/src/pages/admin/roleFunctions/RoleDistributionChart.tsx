import { useEffect, useState } from 'react';
import { Card, Empty } from 'antd';
import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../../store/AuthContext';
import { fetchAdminRoles, fetchAdminUsers } from '../../../api/adminApi';
import { THEME_COLORS } from '../../../utils/themeColors';

interface Slice {
  name: string;
  value: number;
}

// White/dark are light/dark overrides, not distinct hues - not useful as pie slice colors.
const PALETTE = THEME_COLORS.filter((c) => c.key !== 'white' && c.key !== 'dark').map((c) => c.color);

/** Roles with zero users are left out - an empty slice adds legend noise without anything
 * visible to show for it. Use the roles table's User Count column to spot those instead. */
export function RoleDistributionChart() {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const [slices, setSlices] = useState<Slice[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!accessToken) return;
    setLoading(true);
    Promise.all([fetchAdminRoles(accessToken), fetchAdminUsers(accessToken)])
      .then(([roles, users]) => {
        const counts = new Map<string, number>();
        for (const user of users) {
          for (const roleName of user.roles) {
            counts.set(roleName, (counts.get(roleName) ?? 0) + 1);
          }
        }
        setSlices(
          roles
            .map((r) => ({ name: r.name, value: counts.get(r.name) ?? 0 }))
            .filter((s) => s.value > 0)
            .sort((a, b) => b.value - a.value),
        );
      })
      .finally(() => setLoading(false));
  }, [accessToken]);

  return (
    <Card title={t('admin.roleFunctions.stats.distributionTitle')} loading={loading}>
      {slices.length === 0 ? (
        <Empty description={t('admin.roleFunctions.stats.distributionEmpty')} />
      ) : (
        <ResponsiveContainer width="100%" height={260}>
          <PieChart>
            <Pie data={slices} dataKey="value" nameKey="name" innerRadius={60} outerRadius={90} paddingAngle={2}>
              {slices.map((s, i) => (
                <Cell key={s.name} fill={PALETTE[i % PALETTE.length]} />
              ))}
            </Pie>
            <Tooltip contentStyle={{ background: 'var(--bg)', border: '1px solid var(--border)', borderRadius: 6 }} />
            <Legend wrapperStyle={{ color: 'var(--text)' }} />
          </PieChart>
        </ResponsiveContainer>
      )}
    </Card>
  );
}
