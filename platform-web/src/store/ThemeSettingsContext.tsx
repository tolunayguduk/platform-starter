import { createContext, useContext, useState, type ReactNode } from 'react';
import { DEFAULT_THEME_COLOR } from '../utils/themeColors';

const STORAGE_KEY = 'platform.themeColor';

interface ThemeSettingsValue {
  themeColor: string;
  setThemeColor: (color: string) => void;
}

const ThemeSettingsContext = createContext<ThemeSettingsValue | null>(null);

export function ThemeSettingsProvider({ children }: { children: ReactNode }) {
  const [themeColor, setThemeColorState] = useState(
    () => localStorage.getItem(STORAGE_KEY) ?? DEFAULT_THEME_COLOR,
  );

  function setThemeColor(color: string) {
    setThemeColorState(color);
    localStorage.setItem(STORAGE_KEY, color);
  }

  return (
    <ThemeSettingsContext.Provider value={{ themeColor, setThemeColor }}>
      {children}
    </ThemeSettingsContext.Provider>
  );
}

export function useThemeSettings(): ThemeSettingsValue {
  const context = useContext(ThemeSettingsContext);
  if (!context) {
    throw new Error('useThemeSettings must be used within a ThemeSettingsProvider');
  }
  return context;
}
