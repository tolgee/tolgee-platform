import { grey } from '@mui/material/colors';
import { ALL_TOKENS } from './tokens';

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

export type Tile = {
  background: string;
  backgroundHover: string;
};

export type Cell = {
  hover: string;
  selected: string;
};

export type Navbar = {
  background: string;
  text: string;
  logo: string;
};

export type ExampleBanner = {
  background: string;
  text: string;
  border: string;
};

export type TipsBanner = {
  background: string;
};

export type Placeholder = {
  border: string;
  background: string;
  text: string;
};

export type Placeholders = {
  variable: Placeholder;
  tag: Placeholder;
  variant: Placeholder;
  inactive: Placeholder;
};

export type QuickStart = {
  highlight: string;
  circleNormal: string;
  circleSuccess: string;
  topBorder: string;
  progressBackground: string;
  itemBorder: string;
  finishIcon: string;
  finishBackground: string;
  finishCircle: string;
};

const getTokensByMode = (mode: 'light' | 'dark') => {
  const result = {} as Record<keyof typeof ALL_TOKENS, string>;
  Object.entries(ALL_TOKENS).map(([tokenName, value]) => {
    if (typeof value === 'string') {
      result[tokenName] = value;
    } else {
      result[tokenName] = value[mode];
    }
  });
  return result;
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
    tile: {
      background: '#F7F8FB',
      backgroundHover: '#f4f4f6',
    },
    cell: {
      hover: '#f9f9f9',
      selected: '#F6F6F8',
    } satisfies Cell,
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
    } satisfies Editor,
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
      highlight: '#F7F8FB',
      circleNormal: '#E7EBF5',
      circleSuccess: '#0ea459',
      topBorder: '#e9ecef',
      progressBackground: '#bcbcbc70',
      itemBorder: '#EDF0F7',
      finishIcon: '#fff',
      finishBackground: '#EC407A19',
      finishCircle: '#EC407A',
    } as QuickStart,
    import: {
      progressDone: '#00B962',
      progressWorking: '#EC407A',
      progressBackground: '#D9D9D9',
    },
    exampleBanner: {
      background: '#F6F6F8',
      text: '#9DA7B4',
      border: '#E1E5EB',
    },
    tipsBanner: {
      background: '#FDECF280',
    },
    tokens: getTokensByMode('light'),
    placeholders: {
      variable: {
        border: '#7AD3C1',
        background: '#BEF3E9',
        text: '#008371',
      },
      tag: {
        border: '#F27FA6',
        background: '#F9C4D6',
        text: '#822343',
      },
      variant: {
        border: '#BBC2CB',
        background: '#F0F2F4',
        text: '#4D5B6E',
      },
      inactive: {
        background: '#e3e7ea',
        border: '#e3e7ea',
        text: '#808080',
      },
    } satisfies Placeholders,
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
    tile: {
      background: '#233043',
      backgroundHover: '#253245',
    },
    cell: {
      hover: '#233043',
      selected: '#243245',
    } satisfies Cell,
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
    } satisfies Editor,
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
      highlight: '#233043',
      circleNormal: '#2c3c52',
      circleSuccess: '#3bac21',
      topBorder: '#2a384c',
      progressBackground: '#2c3c52',
      finishIcon: '#fff',
      finishBackground: '#EC407A19',
      finishCircle: '#EC407A',
    } as QuickStart,
    import: {
      progressDone: '#00B962',
      progressWorking: '#EC407A',
      progressBackground: '#D9D9D9',
    },
    exampleBanner: {
      background: '#243245',
      text: '#b4bbc3',
      border: '#2c3c52',
    },
    tipsBanner: {
      background: '#29242580',
    },
    tokens: getTokensByMode('dark'),
    placeholders: {
      variable: {
        border: '#008371',
        background: '#008371',
        text: '#F0F2F4',
      },
      tag: {
        border: '#824563',
        background: '#824563',
        text: '#F0F2F4',
      },
      variant: {
        border: '#4D5B6E',
        background: '#4D5B6E',
        text: '#F0F2F4',
      },
      inactive: {
        border: '#2c3c52',
        background: '#2c3c52',
        text: '#acacac',
      },
    } satisfies Placeholders,
  },
} as const;
