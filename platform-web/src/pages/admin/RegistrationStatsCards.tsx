import { useEffect, useState, type ReactNode } from 'react';
import { Card, Col, Row, Statistic } from 'antd';
import { CalendarOutlined, ClockCircleOutlined, ScheduleOutlined, TrophyOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../store/AuthContext';
import { fetchRegistrationStats } from '../../api/adminApi';
import type { StatsRange } from '../../types/admin';
import { THEME_COLORS } from '../../utils/themeColors';

const RANGES: StatsRange[] = ['DAY', 'WEEK', 'MONTH', 'YEAR'];

function colorFor(key: string): string {
  return THEME_COLORS.find((c) => c.key === key)!.color;
}

// Reuses the app's own theme palette so these cards stay in tune with whatever accent the user
// picks, rather than introducing a second, unrelated color scheme.
const RANGE_META: Record<StatsRange, { color: string; icon: ReactNode }> = {
  DAY: { color: colorFor('blue'), icon: <ClockCircleOutlined /> },
  WEEK: { color: colorFor('green'), icon: <CalendarOutlined /> },
  MONTH: { color: colorFor('orange'), icon: <ScheduleOutlined /> },
  YEAR: { color: colorFor('purple'), icon: <TrophyOutlined /> },
};

export function RegistrationStatsCards() {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const [totals, setTotals] = useState<Record<StatsRange, number>>({ DAY: 0, WEEK: 0, MONTH: 0, YEAR: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!accessToken) return;
    setLoading(true);
    Promise.all(RANGES.map((range) => fetchRegistrationStats(accessToken, range)))
      .then((results) => {
        const next = {} as Record<StatsRange, number>;
        RANGES.forEach((range, i) => {
          next[range] = results[i].reduce((sum, p) => sum + p.count, 0);
        });
        setTotals(next);
      })
      .finally(() => setLoading(false));
  }, [accessToken]);

  return (
    <Row gutter={16}>
      {RANGES.map((range) => {
        const { color, icon } = RANGE_META[range];
        return (
          <Col key={range} xs={12} md={6}>
            <Card loading={loading} style={{ borderTop: `3px solid ${color}` }}>
              <Statistic
                title={t('admin.registrationsTotalFor', { period: t(`admin.range.${range.toLowerCase()}`) })}
                value={totals[range]}
                prefix={icon}
                valueStyle={{ color }}
              />
            </Card>
          </Col>
        );
      })}
    </Row>
  );
}
