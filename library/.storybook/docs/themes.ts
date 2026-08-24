import { createTheme } from '@mui/material';
import { getThemeOptions } from '../../src/theme/getTheme';
import { useThemeMode } from './useThemeMode';

// Built from the options rather than merged onto a finished theme: `pxToRem` closes over
// htmlFontSize, so patching the value on an existing theme changes the number and not the function
// that reads it. Worth knowing before anyone tries the shorter version.
const themeFor = (mode: 'light' | 'dark', htmlFontSize?: number) => {
  const options = getThemeOptions(mode);
  return createTheme({
    ...options,
    typography: {
      ...(options.typography as object),
      ...(htmlFontSize ? { htmlFontSize } : {}),
    },
  });
};

export const LIGHT_THEME = themeFor('light');
export const DARK_THEME = themeFor('dark');

// What the app ships. Identical to the themes above — the Spec blocks compare against it so that
// if the theme and the document root ever disagree again, the difference is printed rather than
// discovered by measuring a screenshot.
export const SHIPPED_LIGHT = LIGHT_THEME;
export const SHIPPED_DARK = DARK_THEME;

export const useThemes = () => {
  const dark = useThemeMode() === 'dark';
  return {
    active: dark ? DARK_THEME : LIGHT_THEME,
    other: dark ? LIGHT_THEME : DARK_THEME,
    dark,
  };
};
