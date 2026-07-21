import { createRequire } from 'node:module';
import type { StorybookConfig } from '@storybook/react-vite';

// The Storybook preview pulls the theme from webapp/src (see preview.tsx, issue #3326),
// which lives in a separate node_modules with a different MUI/date-pickers/emotion set
// than the library. Resolve the whole MUI/emotion/react stack to webapp's copies so
// Storybook runs a single, self-consistent version set (webapp's proven combination)
// instead of loading two conflicting ones.
const requireFromWebapp = createRequire(
  new URL('../../webapp/package.json', import.meta.url),
);
const pkgDir = (id: string) =>
  requireFromWebapp
    .resolve(`${id}/package.json`)
    .replace(/\/package\.json$/, '');

const SHARED_PACKAGES = [
  '@mui/material',
  '@mui/lab',
  '@mui/x-date-pickers',
  '@emotion/react',
  '@emotion/styled',
  'hoist-non-react-statics',
];

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
  async viteFinal(config) {
    const { mergeConfig } = await import('vite');
    return mergeConfig(config, {
      resolve: {
        alias: [
          { find: /^react$/, replacement: pkgDir('react') },
          { find: /^react-dom$/, replacement: pkgDir('react-dom') },
          ...SHARED_PACKAGES.map((id) => ({
            find: new RegExp(`^${id}(/.*)?$`),
            replacement: `${pkgDir(id)}$1`,
          })),
        ],
      },
      optimizeDeps: {
        include: [
          '@mui/material',
          '@mui/material/styles',
          '@mui/lab',
          '@mui/x-date-pickers',
          '@mui/x-date-pickers/AdapterDateFns',
          '@emotion/react',
          '@emotion/styled',
          'hoist-non-react-statics',
        ],
      },
    });
  },
} satisfies StorybookConfig;
