import React, { useContext, useState } from 'react';
import { createTheme, PaletteMode, useMediaQuery } from '@mui/material';
import { ThemeProvider as MuiThemeProvider } from '@mui/material/styles';
import { getTheme } from '@tginternal/library/theme/getTheme';

// @ts-ignore
import RighteousLatinExtWoff2 from './fonts/Righteous/righteous-latin-ext.woff2';
// @ts-ignore
import RighteousLatinWoff2 from './fonts/Righteous/righteous-latin.woff2';
// @ts-ignore
import RubikWoff2 from './fonts/Rubik/Rubik-Regular.woff2';

const LOCALSTORAGE_THEME_MODE = 'themeMode';

const rubik = {
  fontFamily: 'Rubik',
  fontStyle: 'normal',
  fontDisplay: 'swap',
  fontWeight: 400,
  src: `
    url(${RubikWoff2}) format('woff2')
    local('Rubik'),
    local('Rubik-Regular'),
  `,
  unicodeRange:
    'U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+2000-206F, U+2074, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF',
};

const righteousLatin = {
  fontFamily: 'Righteous',
  fontStyle: 'normal',
  fontDisplay: 'swap',
  fontWeight: 400,
  src: `
    local('Righteous'),
    local('Righteous-Regular'),
    url(${RighteousLatinWoff2}) format('woff2')
  `,
  unicodeRange:
    'U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA, U+02DC, U+2000-206F, U+2074, U+20AC, U+2122, U+2191, U+2193, U+2212, U+2215, U+FEFF, U+FFFD',
};

const righteousLatinExt = {
  fontFamily: 'Righteous',
  fontStyle: 'normal',
  fontDisplay: 'swap',
  fontWeight: 400,
  src: `
    local('Righteous'),
    local('Righteous-Regular'),
    url(${RighteousLatinExtWoff2}) format('woff2')
  `,
  unicodeRange:
    'U+0100-024F, U+0259, U+1E00-1EFF, U+2020, U+20A0-20AB, U+20AD-20CF, U+2113, U+2C60-2C7F, U+A720-A7FF',
};

// The fonts are webapp branding (logo wordmark), not part of the shared theme, so they are layered
// on top of the library theme here rather than moved into it.
const getWebappTheme = (mode: PaletteMode) =>
  createTheme(getTheme(mode), {
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          '@font-face': [rubik, righteousLatinExt, righteousLatin],
        },
      },
    },
  });

const ThemeContext = React.createContext({
  mode: undefined as PaletteMode | undefined,
  setMode: (mode: PaletteMode | undefined) => {},
});

export const useThemeContext = () => useContext(ThemeContext);

export const ThemeProvider: React.FC<React.PropsWithChildren<unknown>> = ({
  children,
}) => {
  const prefersDarkMode = useMediaQuery('(prefers-color-scheme: dark)', {
    noSsr: true,
  });

  const [mode, _setMode] = useState<PaletteMode | undefined>(
    (localStorage.getItem(LOCALSTORAGE_THEME_MODE) as PaletteMode) || undefined
  );

  const setMode = (mode: PaletteMode | undefined) => {
    if (mode) {
      localStorage?.setItem(LOCALSTORAGE_THEME_MODE, mode);
    } else {
      localStorage?.removeItem(LOCALSTORAGE_THEME_MODE);
    }
    _setMode(mode);
  };

  const value = {
    mode: mode as PaletteMode,
    setMode,
  };

  return (
    <ThemeContext.Provider value={value}>
      <MuiThemeProvider
        theme={getWebappTheme(
          value.mode ?? (prefersDarkMode ? 'dark' : 'light')
        )}
      >
        {children}
      </MuiThemeProvider>
    </ThemeContext.Provider>
  );
};
