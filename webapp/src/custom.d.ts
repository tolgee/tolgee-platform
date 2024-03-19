import { PaletteColor } from '@mui/material/styles';
import { PaletteColorOptions } from '@mui/material';
import {
  Tooltip,
  Activity,
  BillingProgress,
  Cell,
  colors,
  Editor,
  Emphasis,
  ExampleBanner,
  Marker,
  Navbar,
  Placeholders,
  QuickStart,
  Tile,
  TipsBanner,
  TopBanner,
} from './colors';

declare module '*.svg' {
  const content: React.FunctionComponent<React.SVGAttributes<SVGElement>>;
  export default content;
}

const all = { ...colors.light, ...colors.dark };

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
    billingProgress: BillingProgress;
    billingPlan: PaletteColor;
    globalLoading: PaletteColor;
    marker: Marker;
    topBanner: TopBanner;
    quickStart: QuickStart;
    import: typeof all.import;
    exampleBanner: ExampleBanner;
    tipsBanner: TipsBanner;
    tokens: typeof all.tokens;
    placeholders: Placeholders;
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
    billingProgress: BillingProgress;
    billingPlan: PaletteColorOptions;
    globalLoading: PaletteColorOptions;
    marker: Marker;
    topBanner: TopBanner;
    quickStart: QuickStart;
    import: typeof all.import;
    exampleBanner: ExampleBanner;
    tipsBanner: TipsBanner;
    tokens: typeof all.tokens;
    placeholders: Placeholders;
  }
}

declare module '@mui/material/Button' {
  interface ButtonPropsColorOverrides {
    default: true;
  }
}

declare module '@mui/material/ButtonBase' {
  interface ButtonBaseOwnProps<TProps, T extends React.ElementType<TProps>>
    extends TProps {
    component?: T;
  }
}

declare module 'react' {
  interface HTMLAttributes<T> extends AriaAttributes, DOMAttributes<T> {
    webkitdirectory?: boolean;
  }
}
