import { Box, CssBaseline, ThemeProvider } from '@mui/material';
import type { Preview } from '@storybook/react-vite';
import { withThemeFromJSXProvider } from '@storybook/addon-themes';
import { configure } from 'storybook/test';
import { withTolgeeProvider } from '@tolgee/storybook-addon';
import { DocsContainer } from './docs/DocsContainer';
import { MuiLocalizationProvider } from '@tginternal/library/components/MuiLocalizationProvider';
import { locales } from '@tginternal/library/constants/locales';

import { getTheme } from '../src/theme/getTheme';
import { branchName } from '../../webapp/src/branch.json';

const LANGUAGE = 'en';
const FEATURE_TAG = `draft: ${branchName.split('/').pop()}`;

configure({ testIdAttribute: 'data-cy' }); // instead of data-testid in findByTestId, getAllByTestId...

const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    options: {
      storySort: {
        order: [
          'Foundations',
          ['Colors', 'Typography', 'Component styles'],
          'components',
          '*',
        ],
      },
    },
    docs: { container: DocsContainer },
    // The theme switcher already sets the surface color; a second background control would paint
    // over it and allow a light canvas under a dark theme.
    backgrounds: { disable: true },
  },
  decorators: [
    // Innermost, so it sits inside the theme provider below and reads the very theme the story
    // was given. The canvas must never be painted from a second source: a dark surface under a
    // light component is exactly what the theme switcher exists to rule out.
    (Story) => (
      <Box
        sx={{
          bgcolor: 'background.default',
          color: 'text.primary',
          p: 2,
          minHeight: '100%',
        }}
      >
        <Story />
      </Box>
    ),
    withTolgeeProvider({
      messageFormat: 'icu',
      locales,
      LocalizationProvider: MuiLocalizationProvider,
      tolgee: {
        language: LANGUAGE,
        fallbackLanguage: LANGUAGE,
        apiUrl: import.meta.env.VITE_APP_TOLGEE_API_URL,
        apiKey: import.meta.env.VITE_APP_TOLGEE_API_KEY,
        staticData: Object.fromEntries(
          Object.entries(locales).map(([k, v]) => [k, v.translations]),
        ),
        tagNewKeys: [FEATURE_TAG],
      },
    }),
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
