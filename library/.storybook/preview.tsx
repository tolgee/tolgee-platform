import { Box, CssBaseline, ThemeProvider } from '@mui/material';
import type { Preview } from '@storybook/react-vite';
import { withThemeFromJSXProvider } from '@storybook/addon-themes';
import { configure } from 'storybook/test';
import { withTolgeeProvider } from '@tolgee/storybook-addon';
import { DocsContainer } from './docs/DocsContainer';
import { MuiLocalizationProvider } from '@tginternal/library/components/MuiLocalizationProvider';
import { locales } from '@tginternal/library/constants/locales';

import { LIGHT_THEME, DARK_THEME } from './docs/themes';
import { THEME_KEYS } from './themeKeys';

const LANGUAGE = 'en';
const FEATURE_TAG = `draft: ${import.meta.env.VITE_BRANCH_NAME.split('/').pop()}`;

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
          [
            'Theme',
            'Colors',
            'Typography',
            'Icons',
            'Illustrations',
            'Spacing',
            'Breakpoints',
            'Radius and elevation',
            'Component styles',
          ],
          'Components',
          [
            'Buttons',
            ['Button', 'IconButton', 'ButtonGroup', 'Fab'],
            'Forms',
            ['TextField', 'Checkbox'],
            '*',
          ],
          '*',
        ],
      },
    },
    // Without headingSelector the contents list only picks up h3, so the main sections are missing.
    docs: {
      container: DocsContainer,
      toc: { headingSelector: 'h2, h3' },
      // Every story here is a `render` function with no args, so Storybook has nothing to
      // serialize and the code panel opens empty. Hide the control rather than promise a
      // snippet that never arrives; the MDX pages quote real call sites instead.
      canvas: { sourceState: 'none' },
    },
    backgrounds: { disable: true },
  },
  decorators: [
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
        [THEME_KEYS.light]: LIGHT_THEME,
        [THEME_KEYS.dark]: DARK_THEME,
      },
      defaultTheme: THEME_KEYS.light,
    }),
  ],
} satisfies Preview;

export default preview;
