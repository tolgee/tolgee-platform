import { grey } from '@mui/material/colors';

const customGrey: Emphasis = {
  50: '#f0f2f4',
  100: '#e3e7ea',
  200: '#bbc2cb',
  300: '#9da7b4',
  400: '#8995a5',
  500: '#6c7b8f',
  600: '#627082',
  700: '#4d5766',
  800: '#2C3C52',
  900: '#1F2D40',
  A100: grey.A100,
  A200: grey.A200,
  A400: grey.A400,
  A700: grey.A700,
};

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

export type Activity = {
  removed: string;
  added: string;
  addedHighlight: string;
};

export type BillingProgress = {
  background: string;
  low: string;
  over: string;
  sufficient: string;
  separator: string;
};

export type Marker = {
  primary: string;
  secondary: string;
};

export type TopBanner = {
  background: string;
  mainText: string;
  linkText: string;
};

export type Cell = {
  hover: string;
  selected: string;
  inside: string;
};

export type Navbar = {
  background: string;
  text: string;
  logo: string;
};

export type QuickStart = {
  circleNormal: string;
  circleSuccess: string;
  successBackground: string;
  topBorder: string;
  progressBackground: string;
  highlightColor: string;
};

export const colors = {
  light: {
    white: '#fff',
    primary: '#EC407A',
    primaryText: '#D81B5F',
    secondary: '#2B5582',
    default: customGrey[700],
    text: '#4E5967',
    textSecondary: '#808080',
    divider1: '#E1E5EB',
    background: '#FDFDFF',
    backgroundPaper: '#ffffff',
    emphasis: customGrey,
    cell: {
      hover: '#f7f7f7',
      selected: '#EEEFF1',
      inside: '#F9F9F9',
    },
    navbar: {
      background: '#fff',
      text: '#2C3C52',
      logo: '#EC407A',
    },
    activity: {
      removed: '#822B55',
      added: '#006900',
      addedHighlight: '#c4f0da',
    },
    editor: {
      function: '#007300',
      other: '#002bff',
      main: '#2C3C52',
    },
    billingProgress: {
      background: '#C4C4C4',
      low: '#E80000',
      over: '#ffce00',
      sufficient: '#17AD18',
      separator: '#656565',
    },
    billingPlan: '#F8F8F8',
    globalLoading: '#c9a2b5',
    marker: {
      primary: '#ff0000',
      secondary: '#ffc0cb',
    },
    tooltip: {
      background: '#ffffff',
      text: '#111111',
    },
    info: '#009B85',
    topBanner: {
      background: '#BEF4E9',
      mainText: '#004437',
      linkText: '#009B85',
    },
    quickStart: {
      successBackground: '#F7F8FB',
      circleNormal: '#E7EBF5',
      circleSuccess: '#0ea459',
      topBorder: '#e9ecef',
      progressBackground: '#bcbcbc70',
    } as QuickStart,
  },
  dark: {
    white: '#dddddd',
    primary: '#ff6995',
    primaryText: '#ff6995',
    secondary: '#aed5ff',
    default: customGrey[400],
    text: '#dddddd',
    textSecondary: '#acacac',
    divider1: '#2c3c52',
    background: '#1f2d40',
    backgroundPaper: '#1e2b3e',
    cell: {
      hover: '#233043',
      selected: '#243245',
      inside: '#283a53',
    },
    cellSelected2: '#283a53',
    navbar: {
      background: '#182230',
      text: '#dddddd',
      logo: '#dddddd',
    },
    emphasis: {
      50: '#233043',
      100: '#243245',
      200: '#2c3c52',
      300: '#3e4651',
      400: '#4f555c',
      500: '#697079',
      600: '#b4bbc3',
      700: '#eeeeee',
      800: '#f5f5f5',
      900: '#fafafa',
      A100: '#616161',
      A200: '#bdbdbd',
      A400: '#eeeeee',
      A700: '#f5f5f5',
    },
    activity: {
      removed: '#ff7e7e',
      added: '#7fe07f',
      addedHighlight: '#006300',
    },
    editor: {
      function: '#9ac99a',
      other: '#99aaff',
      main: '#eeeeee',
    },
    billingProgress: {
      background: '#565656',
      low: '#ca0000',
      over: '#ffce00',
      sufficient: '#1e991e',
      separator: '#656565',
    },
    billingPlan: '#233043',
    globalLoading: '#ff6995',
    marker: {
      primary: '#ff0000',
      secondary: '#ffc0cb',
    },
    tooltip: {
      background: '#394556',
      text: '#efefef',
    },
    info: '#6db2a4',
    topBanner: {
      background: '#008371',
      mainText: '#BEF4E9',
      linkText: '#dddddd',
    },
    quickStart: {
      successBackground: '#20362b',
      circleNormal: '#2c3c52',
      circleSuccess: '#3bac21',
      topBorder: '#2a384c',
      progressBackground: '#2c3c52',
    } as QuickStart,
  },
} as const;
