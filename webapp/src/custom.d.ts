import API from '@openreplay/tracker';
import { PaletteColor } from '@material-ui/core/styles/createPalette';
import '@material-ui/core/styles';
import { PaletteColorOptions } from '@material-ui/core';

declare module '*.svg' {
  const content: React.FunctionComponent<React.SVGAttributes<SVGElement>>;
  export default content;
}

declare module '@material-ui/core/styles/createPalette' {
  interface Palette {
    lightDivider: PaletteColor;
    extraLightDivider: PaletteColor;
    lightBackground: PaletteColor;
    extraLightBackground: PaletteColor;
  }

  interface PaletteOptions {
    lightDivider: PaletteColorOptions;
    extraLightDivider: PaletteColorOptions;
    lightBackground: PaletteColorOptions;
    extraLightBackground: PaletteColorOptions;
  }
}

declare global {
  interface Window {
    openReplayTracker?: API;
  }
}
