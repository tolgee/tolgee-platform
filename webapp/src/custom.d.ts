import API from '@openreplay/tracker';
import { PaletteColor } from '@mui/material/styles';
import { PaletteColorOptions } from '@mui/material';
import {
  Activity,
  Cell,
  Editor,
  Emphasis,
  ExampleBanner,
  Input,
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
import { tolgeeColors, tolgeePalette } from 'figmaTheme';

declare module '*.svg' {
  const content: React.FunctionComponent<React.SVGAttributes<SVGElement>>;
  export default content;
}

type TolgeeTokens =
  | (typeof tolgeePalette)['Light']
  | (typeof tolgeePalette)['Dark'];

type TolgeeColors = typeof tolgeeColors;

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
    emailNotVerifiedBanner: EmailNotVerifiedBanner;
    quickStart: QuickStart;
    import: typeof all.import;
    exampleBanner: ExampleBanner;
    tipsBanner: TipsBanner;
    tokens: TolgeeTokens;
    colors: TolgeeColors;
    placeholders: Placeholders;
    languageChips: LanguageChips;
    login: Login;
    input: Input;
    revisionFilterBanner: RevisionFilterBanner;
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
    import: typeof all.import;
    exampleBanner: ExampleBanner;
    tipsBanner: TipsBanner;
    tokens: TolgeeTokens;
    colors: TolgeeColors;
    placeholders: Placeholders;
    languageChips: LanguageChips;
    login: Login;
    input: Input;
    revisionFilterBanner: RevisionFilterBanner;
  }
}

declare module '@mui/material/Button' {
  interface ButtonPropsColorOverrides {
    default: true;
  }
}

declare global {
  interface Window {
    openReplayTracker?: API;
  }
}

declare module 'react' {
  interface HTMLAttributes<T> extends AriaAttributes, DOMAttributes<T> {
    webkitdirectory?: boolean;
  }

  type KeyOf<T> = {
    [K in keyof T]-?: T[K] extends Key ? K : never;
  }[keyof T];
}
