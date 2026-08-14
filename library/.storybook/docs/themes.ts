import { getTheme } from '../../src/theme/getTheme';
import { useThemeMode } from './useThemeMode';

export const LIGHT_THEME = getTheme('light');
export const DARK_THEME = getTheme('dark');

export const useThemes = () => {
  const dark = useThemeMode() === 'dark';
  return {
    active: dark ? DARK_THEME : LIGHT_THEME,
    other: dark ? LIGHT_THEME : DARK_THEME,
    dark,
  };
};
