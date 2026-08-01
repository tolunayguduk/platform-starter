import { useEffect, useState } from 'react';
import { Card, Segmented } from 'antd';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../store/AuthContext';
import { useThemeSettings } from '../../store/ThemeSettingsContext';
import { useEffectiveDarkMode } from '../../hooks/useEffectiveDarkMode';
import { getEffectiveAccentColor } from '../../utils/contrast';
import { fetchRegistrationStats } from '../../api/adminApi';
import type { RegistrationStatsPoint, StatsRange } from '../../types/admin';

const RANGES: StatsRange[] = ['DAY', 'WEEK', 'MONTH', 'YEAR'];

export function RegistrationTrendChart() {
  const { t } = useTranslation();
  const { accessToken } = useAuth();
  const { themeColor } = useThemeSettings();
  const dark = useEffectiveDarkMode(themeColor);
  const accentColor = getEffectiveAccentColor(themeColor, dark);
  const [range, setRange] = useState<StatsRange>('WEEK');
  const [points, setPoints] = useState<RegistrationStatsPoint[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!accessToken) return;
    setLoading(true);
    fetchRegistrationStats(accessToken, range)
      .then(setPoints)
      .finally(() => setLoading(false));
  }, [accessToken, range]);

  return (
    <Card
      title={t('admin.registrationsTrendTitle')}
      loading={loading}
      extra={
        <Segmented
          size="small"
          value={range}
          onChange={(value) => setRange(value as StatsRange)}
          options={RANGES.map((r) => ({ label: t(`admin.range.${r.toLowerCase()}`), value: r }))}
        />
      }
    >
      <ResponsiveContainer width="100%" height={260}>
        <AreaChart data={points} margin={{ top: 8, right: 16, left: -16, bottom: 0 }}>
          <defs>
            <linearGradient id="registrationTrendFill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor={accentColor} stopOpacity={0.35} />
              <stop offset="95%" stopColor={accentColor} stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
          <XAxis dataKey="bucket" stroke="var(--text)" tick={{ fontSize: 12 }} />
          <YAxis allowDecimals={false} stroke="var(--text)" tick={{ fontSize: 12 }} width={32} />
          <Tooltip
            contentStyle={{ background: 'var(--bg)', border: '1px solid var(--border)', borderRadius: 6 }}
            labelStyle={{ color: 'var(--text-h)' }}
          />
          <Area type="monotone" dataKey="count" name={t('admin.registrationsTrendCount')} stroke={accentColor} strokeWidth={2} fill="url(#registrationTrendFill)" />
        </AreaChart>
      </ResponsiveContainer>
    </Card>
  );
}
