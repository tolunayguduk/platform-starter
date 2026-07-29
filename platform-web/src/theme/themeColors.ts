export interface ThemeColorOption {
  key: string;
  color: string;
}

// A small, fixed palette rather than a free color picker - keeps every combination
// (header background, primary buttons, content tint) guaranteed to look intentional.
export const THEME_COLORS: ThemeColorOption[] = [
  { key: 'dark', color: '#1f1f1f' },
  { key: 'white', color: '#ffffff' },
  { key: 'purple', color: '#aa3bff' },
  { key: 'blue', color: '#2f6fed' },
  { key: 'green', color: '#16a34a' },
  { key: 'orange', color: '#ea7f1a' },
  { key: 'rose', color: '#e0355b' },
];

// The two neutral swatches double as an explicit light/dark override for the whole app (not
// just the navbar) - picking "white" should look like light mode everywhere, "dark" like dark
// mode everywhere, regardless of what the OS is currently set to. See useEffectiveDarkMode.
export const WHITE_THEME_COLOR = THEME_COLORS.find((c) => c.key === 'white')!.color;
export const DARK_THEME_COLOR = THEME_COLORS.find((c) => c.key === 'dark')!.color;

// Deliberately looked up by key, not THEME_COLORS[0] - the array's order is purely for how
// swatches are listed in the Theme tab and can be reshuffled freely without silently changing
// which color new users start with.
export const DEFAULT_THEME_COLOR = WHITE_THEME_COLOR;

// White/dark are neutral picks, not accent colors - buttons/links keyed off them directly would
// render grayish (see useAppTheme). Every other swatch is vivid enough to use as-is.
export const PRIMARY_BUTTON_FALLBACK_COLOR = '#1677ff';