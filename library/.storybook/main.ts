import type { StorybookConfig } from '@storybook/react-vite';

export default {
  stories: ['../src/**/stories.@(js|jsx|ts|tsx)', '../src/**/*.@(md|mdx)'],
  addons: [
    '@storybook/addon-docs',
    '@storybook/addon-a11y',
    '@storybook/addon-themes',
    '@tolgee/storybook-addon',
  ],
  framework: {
    name: '@storybook/react-vite',
    options: {},
  },
  typescript: {
    check: true,
    reactDocgen: 'react-docgen-typescript',
  },
} satisfies StorybookConfig;
