import { theme as antdTheme, type ThemeConfig } from 'antd';
import { getEffectiveAccentColor } from './contrast';

export function useAppTheme(primaryColor: string, dark: boolean): ThemeConfig {
  return {
    algorithm: dark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
    token: {
      // Buttons/links/focus rings app-wide use this.
      colorPrimary: getEffectiveAccentColor(primaryColor, dark),
      borderRadius: 6,
    },
  };
}