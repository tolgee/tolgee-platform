# Storybook Tolgee Addon

> [!NOTE]
> The addon is pending internal testing and publication. This documentation is
> reflecting the expected state, not the current one. Though the addon should
> already work and setting it up is not difficult. Your feedback is welcome.

`@tolgee/storybook-addon` package provides Tolgee localization tool for Storybook stories.

Only React SDK is supported at the moment. Ask for more SDKs on [tolgee.io](https://tolgee.io/).

## Getting Started

### Add the addon to an existing Storybook

```bash
npx storybook add @tolgee/storybook-addon
```

### Configure the addon

In `.storybook/preview.js`, set the Tolgee provider:

```js
import { withTolgeeProvider } from '@tolgee/storybook-addon';
import { LocalizationProvider } from '@mui/x-date-pickers';

export default {
  decorators: [
    withTolgeeProvider({
      messageFormat: 'icu',
      locales: {
        en: { name: 'English', flag: '🇬🇧' },
        cs: { name: 'Čeština', flag: '🇨🇿' },
      },
      LocalizationProvider: LocalizationProvider,
      tolgee: {
        language: 'en',
        apiUrl: 'https://app.tolgee.io',
        apiKey: '<your API key>',
      },
    }),
  ],
};
```

Config options explained:

- `messageFormat`: Translated string format. Either [`'simple'`](https://docs.tolgee.io/js-sdk/api/core_package/format-simple) or [`'icu'`](https://docs.tolgee.io/platform/translation_process/icu_message_format). Default is `'simple'`.
- `locales`: Brings language names and flags to the Storybook menu. Match its keys with your language names. Optional – if not provided, based on `availableLanguages` or `staticData` Tolgee options.
- `LocalizationProvider`: Localization provider. Optional.
- `tolgee`: Good old [standard Tolgee options](https://docs.tolgee.io/js-sdk/api/core_package/options).

### Override language

To override the default language, set `globals.tolgeeLanguage`:

```js
import React from 'react';
import { ConfirmButton } from './ConfirmButton';

export default {
  component: ConfirmButton,
  globals: { tolgeeLanguage: 'fr' }, // meta level override
};

export const ButtonFr = {};

export const ButtonDe = {
  globals: { tolgeeLanguage: 'de' }, // story level override
};
```

### Disable provider

To disable Tolgee provider, set `parameters.tolgee.disable`:

```js
import React from 'react';
import { ConfirmButton } from './ConfirmButton';

export default {
  component: ConfirmButton,
  parameters: { tolgee: { disable: true } }, // meta level disable
};

export const ButtonWithoutTolgee = {};

export const ButtonWithTolgee = {
  parameters: { tolgee: { disable: false } }, // story level enable
};
```

---

Learn more about Tolgee at [tolgee.io](https://tolgee.io/)

Learn more about Storybook at [storybook.js.org](https://storybook.js.org/).
