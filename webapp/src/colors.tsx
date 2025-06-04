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

export type Tooltip = {
  background: string;
  text: string;
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

export type RevisionFilterBanner = {
  background: string;
  highlightText: string;
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

export type Label = {
  lightSkyBlue: string;
  skyBlue: string;
  lightBlue: string;
  blue: string;
  lightPurple: string;
  purple: string;
  lightPink: string;
  pink: string;
  lightRed: string;
  red: string;
  lightGrey: string;
  grey: string;
  lightTeal: string;
  teal: string;
  lightGreen: string;
  green: string;
  lightYellow: string;
  yellow: string;
  lightOrange: string;
  orange: string;
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

export type Login = {
  backgroundPrimary: string;
};

export type Input = {
  background: string;
};

export type LanguageChips = {
  background: string;
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
    globalLoading: '#c9a2b5',
    marker: {
      primary: '#ff0000',
      secondary: '#ffc0cb54',
    },
    tooltip: {
      background: '#ffffff',
      text: '#111111',
    } satisfies Tooltip,
    info: '#009B85',
    topBanner: {
      background: '#BEF4E9',
      mainText: '#004437',
      linkText: '#009B85',
    },
    revisionFilterBanner: {
      background: '#00AF9A14',
      highlightText: '#00AF9A',
    } satisfies RevisionFilterBanner,
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
    languageChips: {
      background: '#F6F6F8',
    } satisfies LanguageChips,
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
    login: {
      backgroundPrimary: '#fff',
    } satisfies Login,
    input: {
      background: '#ffffff00',
    } satisfies Input,
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
    globalLoading: '#ff6995',
    marker: {
      primary: '#ff0000',
      secondary: '#ffc0cb51',
    },
    tooltip: {
      background: '#394556',
      text: '#efefef',
    } satisfies Tooltip,
    info: '#6db2a4',
    topBanner: {
      background: '#008371',
      mainText: '#BEF4E9',
      linkText: '#dddddd',
    },
    revisionFilterBanner: {
      background: '#99E5D629',
      highlightText: '#99E5D6',
    } satisfies RevisionFilterBanner,
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
    languageChips: {
      background: '#243245',
    } satisfies LanguageChips,
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
    login: {
      backgroundPrimary: '#ffffff0a',
    } satisfies Login,
    input: {
      background: '#ffffff08',
    } satisfies Input,
  },
} as const;
