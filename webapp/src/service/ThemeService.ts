import { Theme } from '@material-ui/core';
import { singleton } from 'tsyringe';
// @ts-ignore
import RighteousLatinExtWoff2 from '../fonts/Righteous/righteous-latin-ext.woff2';
// @ts-ignore
import RighteousLatinWoff2 from '../fonts/Righteous/righteous-latin.woff2';
// @ts-ignore
import RubikWoff2 from '../fonts/Rubik/Rubik-Regular.woff2';

import { createTheme, PaletteType } from '@material-ui/core';

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

const typography = {
  fontFamily:
    '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol"',
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
};

const mixins = {
  toolbar: {
    minHeight: 52,
  },
};

const overrides = {
  MuiCssBaseline: {
    '@global': {
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
    root: {
      borderRadius: 3,
      padding: '6px 16px',
      minHeight: 40,
    },
    sizeSmall: {
      minHeight: 32,
      padding: '4px 16px',
    },
  },
  MuiList: {
    padding: {
      paddingTop: 0,
      paddingBottom: 0,
    },
  },
};

const getDesignTokens = (type: PaletteType) =>
  createTheme({
    typography,
    mixins,
    // @ts-ignore
    overrides: {
      ...overrides,
      ...(type === 'light'
        ? {
            MuiTooltip: {
              tooltip: {
                fontSize: 12,
                boxShadow: '1px 1px 6px rgba(0, 0, 0, 0.25)',
                borderRadius: '11px',
                color: 'black',
                backgroundColor: 'white',
              },
            },
          }
        : {}),
    },
    palette: {
      type,
      ...(type === 'light'
        ? {
            primary: {
              main: '#822B55',
            },
            secondary: {
              main: '#2B5582',
            },
            text: {
              secondary: '#808080',
            },
            lightDivider: {
              main: 'rgba(0, 0, 0, 0.12)',
            },
            extraLightDivider: {
              main: 'rgba(0, 0, 0, 0.08)',
            },
            lightBackground: {
              main: '#E5E5E5',
              contrastText: 'black',
            },
            extraLightBackground: {
              main: '#F8F8F8',
              contrastText: 'black',
            },
            background: {
              default: 'rgb(255, 255, 255)',
            },
          }
        : {
            primary: {
              main: 'hsl(331deg 60% 56%)',
            },
            secondary: {
              main: '#2B5582',
            },
            text: {
              secondary: '#DEDEDE',
            },
            lightDivider: {
              main: 'rgba(0, 0, 0, 0.12)',
            },
            extraLightDivider: {
              main: 'rgba(0, 0, 0, 0.08)',
            },
            lightBackground: {
              main: 'rgb(40, 40, 40)',
              contrastText: '#DEDEDE',
            },
            extraLightBackground: {
              main: 'rgb(55, 55, 55)',
              contrastText: '#DEDEDE',
            },
            background: {
              default: 'rgb(22, 22, 22)',
            },
          }),
    },
  });

@singleton()
export class ThemeService {
  public paletteType: PaletteType =
    (localStorage.getItem('paletteType') as PaletteType) || 'light';
  public theme: Theme = getDesignTokens(this.paletteType);
  public setTheme: (t: Theme) => void = (t) => {};
  public setPaletteType(type: PaletteType) {
    if (type === 'dark' || type === 'light') {
      this.theme = getDesignTokens(type);
      this.paletteType = type;
      this.setTheme(this.theme);
      localStorage.setItem('paletteType', type);
    }
  }
}
