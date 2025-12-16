import { CssBaseline, ThemeProvider } from '@mui/material';
import type { Preview } from '@storybook/react-vite';
import { withThemeFromJSXProvider } from '@storybook/addon-themes';
import { configure } from 'storybook/test';
import { withTolgeeProvider } from '@tolgee/storybook-addon';
import { MuiLocalizationProvider } from '@tginternal/library/components/MuiLocalizationProvider';
import { locales } from '@tginternal/library/constants/locales';

import { getTheme } from '../../webapp/src/ThemeProvider'; // TODO migrate https://github.com/tolgee/tolgee-platform/issues/3326
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
  },
  decorators: [
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
