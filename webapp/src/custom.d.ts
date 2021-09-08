declare module '*.svg' {
  const content: React.FunctionComponent<React.SVGAttributes<SVGElement>>;
  export default content;
}

import { PaletteColor } from '@material-ui/core/styles/createPalette';
import '@material-ui/core/styles';
import { PaletteColorOptions } from '@material-ui/core';

declare module '@material-ui/core/styles/createPalette' {
  interface Palette {
    lightDivider: PaletteColor;
    extraLightDivider: PaletteColor;
    chipBackground: PaletteColor;
  }

  interface PaletteOptions {
    lightDivider: PaletteColorOptions;
    extraLightDivider: PaletteColorOptions;
    chipBackground: PaletteColorOptions;
  }
}
