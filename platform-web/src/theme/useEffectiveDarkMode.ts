import { useEffect, useState } from 'react';
import { WHITE_THEME_COLOR, DARK_THEME_COLOR } from './themeColors';

function prefersDark(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

/**
 * "white" and "dark" are an explicit light/dark override, not just an accent pick - without this,
 * picking "white" while the OS is in dark mode left every surface (content background, antd
 * Cards/Tables, page text) still dark, with only the navbar actually turning white. Any other
 * swatch just follows the OS preference as before.
 */
export function useEffectiveDarkMode(themeColor: string): boolean {
  const [osDark, setOsDark] = useState(prefersDark);

  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const listener = (e: MediaQueryListEvent) => setOsDark(e.matches);
    media.addEventListener('change', listener);
    return () => media.removeEventListener('change', listener);
  }, []);

  if (themeColor === WHITE_THEME_COLOR) return false;
  if (themeColor === DARK_THEME_COLOR) return true;
  return osDark;
}