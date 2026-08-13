import { addons } from 'storybook/manager-api';
import { themes } from 'storybook/theming';

// The manager theme is read once at load and cannot follow the toolbar's theme global, so the
// chrome stays light while the preview and docs pages follow the switcher.
addons.setConfig({
  theme: {
    ...themes.light,
    brandTitle: 'Tolgee',
    brandImage: './tolgee-brand.svg',
    brandUrl: '/',
  },
});
