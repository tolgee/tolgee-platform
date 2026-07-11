import { defineConfig } from 'cspell';

export default defineConfig({
  version: '0.2',
  readonly: true,
  language: 'en', // === en-US
  minWordLength: 3,
  caseSensitive: true,
  words: ['chainer', 'Čeština', 'Molnár', 'Tolgee', 'tsc'],
  flagWords: [],
  ignoreWords: ['esnext', 'gfm', 'icu', 'tolgee'],
  ignorePaths: ['/dist/**/*'],
});
