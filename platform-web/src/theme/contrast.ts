/**
 * Theme colors include near-white/near-black entries (see themeColors.ts), so navbar text and
 * primary buttons can no longer assume a fixed color - both have to react to how light the
 * chosen color is, and to the light/dark mode it's rendered against.
 */
import { WHITE_THEME_COLOR, DARK_THEME_COLOR, PRIMARY_BUTTON_FALLBACK_COLOR } from './themeColors';

function hexToRgb(hex: string): [number, number, number] {
  return [parseInt(hex.slice(1, 3), 16), parseInt(hex.slice(3, 5), 16), parseInt(hex.slice(5, 7), 16)];
}

function rgbToHex(r: number, g: number, b: number): string {
  const clamp = (v: number) => Math.max(0, Math.min(255, Math.round(v)));
  return `#${[r, g, b].map((v) => clamp(v).toString(16).padStart(2, '0')).join('')}`;
}

function getLuminance(hex: string): number {
  const [r, g, b] = hexToRgb(hex);
  return (0.299 * r + 0.587 * g + 0.114 * b) / 255;
}

function mix(hex: string, target: string, amount: number): string {
  const [r1, g1, b1] = hexToRgb(hex);
  const [r2, g2, b2] = hexToRgb(target);
  return rgbToHex(r1 + (r2 - r1) * amount, g1 + (g2 - g1) * amount, b1 + (b2 - b1) * amount);
}

export function getContrastOnColor(hex: string): { text: string; overlay: string } {
  return getLuminance(hex) > 0.6
    ? { text: 'rgba(0, 0, 0, 0.85)', overlay: 'rgba(0, 0, 0, 0.08)' }
    : { text: '#ffffff', overlay: 'rgba(255, 255, 255, 0.3)' };
}

/**
 * The raw theme color is used as-is for the navbar (it has its own text-contrast handling above)
 * and as antd's colorPrimary token, which drives every primary button/link/focus ring app-wide.
 * A near-black accent picked while the OS is in dark mode - or a near-white one in light mode -
 * would make those buttons blend straight into the page background, so this nudges the color
 * away from the background's own tone specifically for that use, without changing the swatch
 * the user actually picked (still shown as-is in the theme picker).
 */
export function getSafeAccentColor(hex: string, isDarkMode: boolean): string {
  const luminance = getLuminance(hex);

  if (isDarkMode && luminance < 0.3) {
    return mix(hex, '#ffffff', 0.5);
  }
  if (!isDarkMode && luminance > 0.8) {
    return mix(hex, '#000000', 0.5);
  }
  return hex;
}

/**
 * What buttons/links/focus rings and the admin chart's bars actually render in. White/dark are a
 * light/dark override, not an accent color - keying colorPrimary off them directly (even through
 * getSafeAccentColor) reads as washed-out gray, so they fall back to a plain primary blue instead.
 * Every other swatch is vivid enough to drive these directly.
 */
export function getEffectiveAccentColor(themeColor: string, isDarkMode: boolean): string {
  if (themeColor === WHITE_THEME_COLOR || themeColor === DARK_THEME_COLOR) {
    return PRIMARY_BUTTON_FALLBACK_COLOR;
  }
  return getSafeAccentColor(themeColor, isDarkMode);
}