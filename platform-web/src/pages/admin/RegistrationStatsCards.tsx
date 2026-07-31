import { useEffect, useState } from 'react';
import { Card, Col, Row, Statistic } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../auth/AuthContext';
import { fetchRegistrationStats, type StatsRange } from '../../api/admin';

const RANGES: StatsRange[] = ['DAY', 'WEEK', 'MONTH', 'YEAR'];

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
      {RANGES.map((range) => (
        <Col key={range} xs={12} md={6}>
          <Card loading={loading}>
            <Statistic
              title={t('admin.registrationsTotalFor', { period: t(`admin.range.${range.toLowerCase()}`) })}
              value={totals[range]}
            />
          </Card>
        </Col>
      ))}
    </Row>
  );
}
