import API from '@openreplay/tracker';
import { PaletteColor } from '@mui/material/styles';
import { PaletteColorOptions } from '@mui/material';

declare module '*.svg' {
  const content: React.FunctionComponent<React.SVGAttributes<SVGElement>>;
  export default content;
}

declare module '@mui/material/styles/createPalette' {
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
