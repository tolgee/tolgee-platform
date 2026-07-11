import { PaletteColorOptions } from '@mui/material';

type FigmaColor = {
  main: string;
  dark: string;
  light: string;
  contrast: string;
};

/**
 * Transforms color from figma to mui theme format
 */
export const fromFigmaColor = ({
  main,
  dark,
  light,
  contrast,
}: FigmaColor): PaletteColorOptions => ({
  main,
  dark,
  light,
  contrastText: contrast,
});
