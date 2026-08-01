import type { CSSProperties } from 'react';
import { Typography, Space } from 'antd';
import { CheckOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useThemeSettings } from '../../store/ThemeSettingsContext';
import { THEME_COLORS } from '../../utils/themeColors';
import { getContrastOnColor } from '../../utils/contrast';
import styles from './ThemeSettingsTab.module.css';

export function ThemeSettingsTab() {
  const { t } = useTranslation();
  const { themeColor, setThemeColor } = useThemeSettings();

  return (
    <>
      <Typography.Title level={4}>{t('settings.categories.theme')}</Typography.Title>
      <Typography.Paragraph type="secondary">{t('settings.theme.description')}</Typography.Paragraph>
      <Space size={16}>
        {THEME_COLORS.map((option) => {
          const selected = option.color === themeColor;
          const { text: checkColor } = getContrastOnColor(option.color);
          const vars = {
            '--swatch-color': option.color,
            '--outline-color': selected ? option.color : 'transparent',
            '--check-color': checkColor,
          } as CSSProperties;

          return (
            <button
              key={option.key}
              type="button"
              onClick={() => setThemeColor(option.color)}
              title={t(`settings.theme.colors.${option.key}`)}
              aria-label={t(`settings.theme.colors.${option.key}`)}
              className={styles.swatch}
              style={vars}
            >
              {selected && <CheckOutlined className={styles.checkIcon} />}
            </button>
          );
        })}
      </Space>
    </>
  );
}