import { defineConfig } from 'cspell';

export default defineConfig({
  version: '0.2',
  readonly: true,
  language: 'en', // === en-US
  minWordLength: 3,
  caseSensitive: true,
  words: ['CSF', 'subpaths', 'Tolgee', 'tsc', 'Vite'],
  flagWords: [],
  ignoreWords: [
    'autodocs',
    'Čeština',
    'Dansk',
    'Deutsch',
    'env',
    'esnext',
    'Español',
    'Français',
    'fns',
    'gfm',
    'Português',
    'tginternal',
    'tolgee',
  ],
  ignorePaths: ['/dist/', '/src/i18n/*.json', '/storybook-static/'],
});
