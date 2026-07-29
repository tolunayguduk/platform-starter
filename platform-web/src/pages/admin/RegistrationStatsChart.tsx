import { useEffect, useState, type CSSProperties } from 'react';
import { Card, Segmented, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../auth/AuthContext';
import { useThemeSettings } from '../../theme/ThemeSettingsContext';
import { useEffectiveDarkMode } from '../../theme/useEffectiveDarkMode';
import { getEffectiveAccentColor } from '../../theme/contrast';
import { fetchRegistrationStats, type RegistrationStatsPoint, type StatsRange } from '../../api/admin';
import styles from './RegistrationStatsChart.module.css';

export function RegistrationStatsChart() {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const { themeColor } = useThemeSettings();
  const dark = useEffectiveDarkMode(themeColor);
  const barColor = getEffectiveAccentColor(themeColor, dark);

  const [range, setRange] = useState<StatsRange>('WEEK');
  const [points, setPoints] = useState<RegistrationStatsPoint[]>([]);
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);

  useEffect(() => {
    if (!accessToken) return;
    fetchRegistrationStats(accessToken, range).then(setPoints);
  }, [accessToken, range]);

  const max = Math.max(1, ...points.map((p) => p.count));
  const total = points.reduce((sum, p) => sum + p.count, 0);
  const labelStep = points.length > 14 ? Math.ceil(points.length / 8) : 1;

  return (
    <Card title={t('admin.registrationsTitle')}>
      <div className={styles.headerRow}>
        <div>
          <div className={styles.total}>{total.toLocaleString()}</div>
          <Typography.Text type="secondary" className={styles.totalLabel}>
            {t('admin.registrationsTotalFor', { period: t(`admin.range.${range.toLowerCase()}`) })}
          </Typography.Text>
        </div>
        <Segmented
          value={range}
          onChange={(value) => setRange(value as StatsRange)}
          options={[
            { label: t('admin.range.day'), value: 'DAY' },
            { label: t('admin.range.week'), value: 'WEEK' },
            { label: t('admin.range.month'), value: 'MONTH' },
            { label: t('admin.range.year'), value: 'YEAR' },
          ]}
        />
      </div>

      <div className={styles.chartArea}>
        {points.map((p, i) => (
          <div
            key={p.bucket}
            className={styles.barWrap}
            tabIndex={0}
            onMouseEnter={() => setHoverIndex(i)}
            onMouseLeave={() => setHoverIndex(null)}
            onFocus={() => setHoverIndex(i)}
            onBlur={() => setHoverIndex(null)}
          >
            {hoverIndex === i && (
              <div className={styles.tooltip}>
                <span className={styles.tooltipValue}>{p.count}</span>
                <span>{p.bucket}</span>
              </div>
            )}
            <div
              className={styles.bar}
              style={{ height: `${(p.count / max) * 100}%`, '--bar-color': barColor } as CSSProperties}
            />
          </div>
        ))}
      </div>
      <div className={styles.labels}>
        {points.map((p, i) => (
          <span key={p.bucket} className={styles.label}>
            {i % labelStep === 0 ? p.bucket : ''}
          </span>
        ))}
      </div>
    </Card>
  );
}