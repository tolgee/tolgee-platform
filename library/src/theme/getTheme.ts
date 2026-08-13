import { createTheme, PaletteColor, PaletteColorOptions } from '@mui/material';
import type { PaletteMode } from '@mui/material';

import {
  Activity,
  Cell,
  colors,
  Editor,
  Emphasis,
  ExampleBanner,
  Input,
  Label,
  LanguageChips,
  Login,
  Marker,
  Navbar,
  Placeholders,
  QuickStart,
  RevisionFilterBanner,
  Tile,
  TipsBanner,
  Tooltip,
  TopBanner,
} from './colors';
import { tolgeeColors, tolgeePalette } from './figmaTheme';
import { fromFigmaColor } from './figma';

type TolgeeTokens =
  | (typeof tolgeePalette)['Light']
  | (typeof tolgeePalette)['Dark'];

type TolgeeColors = typeof tolgeeColors;

type Import = (typeof colors)['light']['import'];

declare module '@mui/material/styles/createPalette' {
  interface Palette {
    primaryText: string;
    divider1: string;
    tooltip: Tooltip;
    tile: Tile;
    cell: Cell;
    default: PaletteColor;
    navbar: Navbar;
    emphasis: Emphasis;
    activity: Activity;
    editor: Editor;
    globalLoading: PaletteColor;
    marker: Marker;
    topBanner: TopBanner;
    quickStart: QuickStart;
    import: Import;
    exampleBanner: ExampleBanner;
    tipsBanner: TipsBanner;
    tokens: TolgeeTokens;
    colors: TolgeeColors;
    placeholders: Placeholders;
    languageChips: LanguageChips;
    login: Login;
    input: Input;
    revisionFilterBanner: RevisionFilterBanner;
    label: Label;
  }

  interface PaletteOptions {
    primaryText: string;
    divider1: string;
    tooltip: Tooltip;
    tile: Tile;
    cell: Cell;
    default: PaletteColor;
    navbar: Navbar;
    emphasis: Emphasis;
    activity: Activity;
    editor: Editor;
    globalLoading: PaletteColorOptions;
    marker: Marker;
    topBanner: TopBanner;
    quickStart: QuickStart;
    import: Import;
    exampleBanner: ExampleBanner;
    tipsBanner: TipsBanner;
    tokens: TolgeeTokens;
    colors: TolgeeColors;
    placeholders: Placeholders;
    languageChips: LanguageChips;
    login: Login;
    input: Input;
    revisionFilterBanner: RevisionFilterBanner;
    label: Label;
  }
}

declare module '@mui/material/Button' {
  interface ButtonPropsColorOverrides {
    default: true;
    contrast: true;
  }
}

const { palette } = createTheme();
const { augmentColor } = palette;
const createColor = (mainColor: string) =>
  augmentColor({ color: { main: mainColor } });

export const getTheme = (mode: PaletteMode) => {
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
      label: tPalette.label,
    },
    mixins: {
      toolbar: {
        minHeight: 52,
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
            minHeight: 30,
            padding: '4px 10px',
            fontSize: '13px',
            lineHeight: 'normal',
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
            '&:hover': {
              textDecoration: 'underline',
            },
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
