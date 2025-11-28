/// <reference types="storybook/test" />
import { CssBaseline, ThemeProvider } from '@mui/material';
import type { Preview } from '@storybook/react-vite';
import { withThemeFromJSXProvider } from '@storybook/addon-themes';

import { getTheme } from '../../webapp/src/ThemeProvider'; // TODO migrate https://github.com/tolgee/tolgee-platform/issues/3326

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
  },
  decorators: [
    withThemeFromJSXProvider({
      GlobalStyles: CssBaseline,
      Provider: ThemeProvider,
      themes: {
        Light: getTheme('light'),
        Dark: getTheme('dark'),
      },
      defaultTheme: 'Light',
    }),
  ],
} satisfies Preview;

export default preview;
