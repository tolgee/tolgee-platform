import React, { useContext, useState } from 'react';
import { createTheme, PaletteMode, useMediaQuery } from '@mui/material';
import { ThemeProvider as MuiThemeProvider } from '@mui/material/styles';

import { TOP_BAR_HEIGHT } from 'tg.component/layout/TopBar/TopBar';
// @ts-ignore
import RighteousLatinExtWoff2 from './fonts/Righteous/righteous-latin-ext.woff2';
// @ts-ignore
import RighteousLatinWoff2 from './fonts/Righteous/righteous-latin.woff2';
// @ts-ignore
import RubikWoff2 from './fonts/Rubik/Rubik-Regular.woff2';
import { colors } from './colors';
import { tolgeeColors, tolgeePalette } from 'figmaTheme';
import { fromFigmaColor } from 'tg.fixtures/figma';

const LOCALSTORAGE_THEME_MODE = 'themeMode';

const { palette } = createTheme();
const { augmentColor } = palette;
const createColor = (mainColor) => augmentColor({ color: { main: mainColor } });

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

const getTheme = (mode: PaletteMode) => {
  const c = mode === 'light' ? colors.light : colors.dark;
  const tPalette = mode === 'light' ? tolgeePalette.Light : tolgeePalette.Dark;

  return createTheme({
    typography: {
      fontFamily:
        '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol"',
      htmlFontSize: 15,
      h1: {
        fontSize: 42,
        fontWeight: 300,
      },
      h2: {
        fontSize: 36,
        fontWeight: 300,
      },
      h3: {
        fontSize: 28,
        fontWeight: 400,
      },
      h4: {
        fontSize: 24,
        fontWeight: 400,
      },
      h5: {
        fontSize: 20,
        fontWeight: 400,
      },
      h6: {
        fontSize: 18,
        fontWeight: 500,
      },
      subtitle1: {
        fontSize: 18,
        fontWeight: 400,
      },
      subtitle2: {
        fontSize: 16,
        fontWeight: 500,
      },
      body1: {
        fontSize: 16,
        fontWeight: 400,
      },
      body2: {
        fontSize: 15,
        fontWeight: 400,
      },
      button: {
        fontSize: 14,
        fontWeight: 500,
      },
      caption: {
        fontSize: 12,
        fontWeight: 400,
      },
      overline: {
        fontWeight: 400,
        fontSize: 10,
      },
    },
    palette: {
      mode,
      primary: fromFigmaColor(tPalette.primary),
      primaryText: c.primaryText,
      secondary: fromFigmaColor(tPalette.secondary),
      default: createColor(c.default),
      info: fromFigmaColor(tPalette.info),
      warning: fromFigmaColor(tPalette.warning),
      error: fromFigmaColor(tPalette.error),
      common: {
        white: c.white,
      },
      text: {
        primary: tPalette.text.primary,
        secondary: tPalette.text.secondary,
      },
      divider1: c.divider1,
      tile: c.tile,
      cell: c.cell,
      background: {
        default: c.background,
        paper: c.backgroundPaper,
      },
      tooltip: c.tooltip,
      navbar: c.navbar,
      activity: c.activity,
      emphasis: c.emphasis,
      editor: c.editor,
      globalLoading: createColor(c.globalLoading),
      marker: c.marker,
      topBanner: c.topBanner,
      quickStart: c.quickStart,
      import: c.import,
      exampleBanner: c.exampleBanner,
      tipsBanner: c.tipsBanner,
      tokens: tPalette,
      colors: tolgeeColors,
      placeholders: c.placeholders,
      languageChips: c.languageChips,
      login: c.login,
      input: c.input,
      revisionFilterBanner: c.revisionFilterBanner,
    },
    mixins: {
      toolbar: {
        minHeight: TOP_BAR_HEIGHT,
      },
    },
    components: {
      MuiTooltip: {
        styleOverrides: {
          tooltip: {
            fontSize: 12,
            boxShadow: '1px 1px 6px rgba(0, 0, 0, 0.25)',
            borderRadius: '11px',
            color: c.tooltip.text,
            backgroundColor: c.tooltip.background,
          },
        },
      },
      MuiInputBase: {
        styleOverrides: {
          root: {
            '&.MuiOutlinedInput-root': {
              backgroundColor: c.input.background,
            },
          },
        },
      },
      MuiCssBaseline: {
        styleOverrides: {
          '@font-face': [rubik, righteousLatinExt, righteousLatin],
          html: {
            height: '100%',
          },
          body: {
            minHeight: '100%',
            position: 'relative',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'stretch',
            fontSize: 15,
          },
          '#root': {
            flexGrow: 1,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'stretch',
          },
        },
      },
      MuiButton: {
        defaultProps: {
          color: 'default',
        },
        styleOverrides: {
          containedPrimary: {
            filter: mode === 'dark' ? 'brightness(0.9)' : undefined,
          },
          root: {
            borderRadius: 3,
            padding: '6px 16px',
            minHeight: 40,
            '&.Mui-disabled.MuiButton-containedPrimary': {
              backgroundColor: tPalette.primary.disabled,
            },
            '&.Mui-disabled.MuiButton-containedSecondary': {
              backgroundColor: tPalette.secondary.disabled,
            },
          },
          sizeSmall: {
            minHeight: 32,
            padding: '4px 16px',
          },
        },
      },
      MuiFab: {
        styleOverrides: {
          primary: {
            filter: mode === 'dark' ? 'brightness(0.9)' : undefined,
          },
        },
      },
      MuiIconButton: {
        styleOverrides: {
          root: {
            filter: mode === 'dark' ? 'brightness(0.9)' : undefined,
          },
        },
      },
      MuiDialog: {
        styleOverrides: {
          paper: {
            backgroundImage: 'none',
          },
        },
      },
      MuiDialogActions: {
        styleOverrides: {
          root: {
            padding: '8px 24px 16px 24px',
          },
        },
      },
      MuiList: {
        styleOverrides: {
          padding: {
            paddingTop: 0,
            paddingBottom: 0,
          },
        },
      },
      MuiMenu: {
        styleOverrides: {
          list: {
            paddingTop: 8,
            paddingBottom: 8,
          },
        },
      },
      MuiMenuItem: {
        styleOverrides: {
          root: {
            '&.Mui-focusVisible': {
              backgroundColor: tPalette.text._states.hover,
            },
          },
        },
      },
      MuiLink: {
        styleOverrides: {
          root: {
            textDecoration: 'none',
          },
        },
      },
      MuiFormHelperText: {
        styleOverrides: {
          root: {
            marginLeft: 0,
          },
        },
      },
    },
  });
};

declare module '@mui/material/Button' {
  interface ButtonPropsColorOverrides {
    contrast: true;
  }
}

const ThemeContext = React.createContext({
  mode: undefined as PaletteMode | undefined,
  setMode: (mode: PaletteMode | undefined) => {},
});

export const useThemeContext = () => useContext(ThemeContext);

export const ThemeProvider: React.FC = ({ children }) => {
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
        theme={getTheme(value.mode ?? (prefersDarkMode ? 'dark' : 'light'))}
      >
        {children}
      </MuiThemeProvider>
    </ThemeContext.Provider>
  );
};
