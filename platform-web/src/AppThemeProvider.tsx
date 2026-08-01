import { useEffect, type ReactNode } from 'react';
import { App, ConfigProvider } from 'antd';
import { useAppTheme } from './hooks/useAppTheme';
import { useEffectiveDarkMode } from './hooks/useEffectiveDarkMode';
import { ThemeSettingsProvider, useThemeSettings } from './store/ThemeSettingsContext';

function ConfigProviderWithTheme({ children }: { children: ReactNode }) {
  const { themeColor } = useThemeSettings();
  const dark = useEffectiveDarkMode(themeColor);
  const theme = useAppTheme(themeColor, dark);

  // Drives index.css's [data-theme] blocks, so the plain-CSS parts of the app (Login/Register,
  // AppLayout's content tint) follow the same light/dark decision as antd's own algorithm above -
  // otherwise picking "white"/"dark" would only ever repaint antd's components.
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
  }, [dark]);

  return (
    <ConfigProvider theme={theme}>
      {/* antd's App wrapper lets message/notification/Modal consume this theme via App.useApp(),
          instead of the static antd.message API which ignores ConfigProvider entirely. */}
      <App>{children}</App>
    </ConfigProvider>
  );
}

export function AppThemeProvider({ children }: { children: ReactNode }) {
  return (
    <ThemeSettingsProvider>
      <ConfigProviderWithTheme>{children}</ConfigProviderWithTheme>
    </ThemeSettingsProvider>
  );
}