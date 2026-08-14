import { addons } from 'storybook/manager-api';
import { themes } from 'storybook/theming';
import { tolgeePalette } from '../src/theme/figmaTheme';

// The manager theme is read once at load, so it cannot follow the toolbar's theme global.
addons.setConfig({
  theme: {
    ...themes.light,
    colorPrimary: tolgeePalette.Light.primary.main,
    colorSecondary: tolgeePalette.Light.secondary.main,
    brandTitle: 'Tolgee',
    brandImage: './tolgee-brand.svg',
    brandUrl: '/',
  },
});
