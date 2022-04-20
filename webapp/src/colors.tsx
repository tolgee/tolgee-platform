import { grey } from '@mui/material/colors';

export type Emphasis = {
  50: string;
  100: string;
  200: string;
  300: string;
  400: string;
  500: string;
  600: string;
  700: string;
  800: string;
  900: string;
  A100: string;
  A200: string;
  A400: string;
  A700: string;
};

export type Editor = {
  function: string;
  other: string;
  main: string;
};

export const colors = {
  light: {
    white: '#fff',
    primary: '#822B55',
    secondary: '#2B5582',
    default: grey[700],
    navbarBackground: '#822B55',
    text: '#000000',
    textSecondary: '#808080',
    divider1: '#00000014',
    divider2: '#0000001e',
    background: '#ffffff',
    cellSelected1: '#f5f5f5',
    cellSelected2: '#ffffff',
    backgroundPaper: '#ffffff',
    emphasis: grey as Emphasis,
    editor: {
      function: '#007300',
      other: '#002bff',
      main: '#000000',
    } as Editor,
  },
  dark: {
    white: '#dddddd',
    primary: '#ff6995',
    secondary: '#aed5ff',
    default: grey[400],
    navbarBackground: '#182230',
    text: '#dddddd',
    textSecondary: '#d2d2d2',
    divider1: '#eeeeee14',
    divider2: '#ffffff1e',
    background: '#1f2d40',
    backgroundPaper: '#1e2b3e',
    cellSelected1: '#24344b',
    cellSelected2: '#283a53',
    emphasis: {
      50: '#202e41',
      100: '#233144',
      200: '#303842',
      300: '#3e4750',
      400: '#575d65',
      500: '#bdbdbd',
      600: '#e0e0e0',
      700: '#eeeeee',
      800: '#f5f5f5',
      900: '#fafafa',
      A100: '#616161',
      A200: '#bdbdbd',
      A400: '#eeeeee',
      A700: '#f5f5f5',
    } as Emphasis,
    editor: {
      function: '#9ac99a',
      other: '#99aaff',
      main: '#eeeeee',
    } as Editor,
  },
} as const;
