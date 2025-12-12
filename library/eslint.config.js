import { defineConfig, globalIgnores } from 'eslint/config';
import globals from 'globals';
import jsPlugin from '@eslint/js';
import jsonPlugin from '@eslint/json';
import markdownPlugin from '@eslint/markdown';
import reactPlugin from 'eslint-plugin-react';
import reactHooksPlugin from 'eslint-plugin-react-hooks';
import reactRefreshPlugin from 'eslint-plugin-react-refresh';
import storybookPlugin from 'eslint-plugin-storybook';
import tsPlugin from 'typescript-eslint';
import compatPlugin from 'eslint-plugin-compat';
import prettierConfig from 'eslint-config-prettier/flat';

/**
 * @type {import('eslint').Linter.Config[]}
 */
export default defineConfig([
  {
    settings: {
      react: {
        version: 'detect',
      },
    },
  },
  {
    languageOptions: {
      globals: {
        ...globals.browser,
      },
      parserOptions: {
        ecmaFeatures: {
          jsx: true,
        },
      },
      sourceType: 'module',
    },
  },
  globalIgnores(['!.storybook', 'dist/*', 'storybook-static/*']),
  {
    files: ['**/*.js', '**/*.ts', '**/*.jsx', '**/*.tsx'],
    extends: [
      jsPlugin.configs.recommended,
      tsPlugin.configs.recommended,
      reactPlugin.configs.flat.recommended,
      reactPlugin.configs.flat['jsx-runtime'],
    ],
  },
  {
    files: ['**/*.jsx', '**/*.tsx'],
    extends: [
      reactHooksPlugin.configs.flat.recommended,
      reactRefreshPlugin.configs.vite,
    ],
  },
  {
    files: ['src/**/stories.jsx', 'src/**/stories.tsx'],
    rules: {
      'react-refresh/only-export-components': [
        'error',
        { allowExportNames: ['meta'] },
      ],
    },
  },
  {
    files: ['**/*.json'],
    ignores: ['package-lock.json'],
    language: 'json/json',
    plugins: { json: jsonPlugin },
    extends: [jsonPlugin.configs.recommended],
  },
  {
    files: ['**/tsconfig*.json'],
    language: 'json/jsonc',
    plugins: { json: jsonPlugin },
    extends: [jsonPlugin.configs.recommended],
  },
  {
    files: ['**/*.md'], // MDX support awaits implementation https://github.com/eslint/markdown/issues/316
    language: 'markdown/gfm',
    plugins: { markdown: markdownPlugin },
    extends: [markdownPlugin.configs.recommended],
  },
  storybookPlugin.configs['flat/recommended'],
  compatPlugin.configs['flat/recommended'],
  prettierConfig,
]);
