import type { PaletteColor, PaletteColorOptions } from '@mui/material';

import type {
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
import type { tolgeeColors, tolgeePalette } from './figmaTheme';

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

export {};
