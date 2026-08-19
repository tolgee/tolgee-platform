import { defineConfig } from 'cspell';

export default defineConfig({
  version: '0.2',
  readonly: true,
  language: 'en', // === en-US
  minWordLength: 3,
  caseSensitive: true,
  words: [
    'argstable',
    'Ccw',
    'CSF',
    'hsl',
    'nums',
    'rgb',
    'sbdocs',
    'Segoe',
    'subpaths',
    'taskinfo',
    'Tolgee',
    'tsc',
    'Vite',
    'wordmark',
  ],
  flagWords: [],
  ignoreWords: [
    'autodocs',
    'Čeština',
    'Dansk',
    'data-testid',
    'Deutsch',
    'env',
    'esnext',
    'figma',
    'Español',
    'Français',
    'fns',
    'gfm',
    'Português',
    'tginternal',
    'tolgee',
  ],
  // SVG path data is coordinates, not prose.
  // Generated from webapp source paths and product copy, not prose written here.
  ignorePaths: [
    '/dist/',
    '/storybook-static/',
    '**/*.svg',
    '.storybook/docs/buttonUsages.ts',
    '.storybook/docs/restyledButtons.ts',
  ],
  ignoreRegExpList: ['tolgee-pat'],
  patterns: [
    {
      name: 'tolgee-pat',
      pattern: '/tgpak_[a-z0-9]*/g',
    },
  ],
});
