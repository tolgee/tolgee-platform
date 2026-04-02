import { Theme } from '@mui/material';

export const getHighlightColor = (theme: Theme, custom: boolean) =>
  custom ? theme.palette.tokens.info.main : theme.palette.tokens.primary.main;
