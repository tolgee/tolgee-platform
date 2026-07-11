import { defineConfig } from 'eslint/config';
import globals from 'globals';
import jsPlugin from '@eslint/js';
import jsonPlugin from '@eslint/json';
import markdownPlugin from '@eslint/markdown';
import reactPlugin from 'eslint-plugin-react';
import reactHooksPlugin from 'eslint-plugin-react-hooks';
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
  {
    ignores: ['dist/*'],
  },
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
    extends: [reactHooksPlugin.configs.flat.recommended],
  },
  {
    files: ['**/*.json'],
    ignores: ['package-lock.json'],
    language: 'json/json',
    plugins: { json: jsonPlugin },
    extends: [jsonPlugin.configs.recommended],
  },
  {
    files: ['tsconfig.json'],
    language: 'json/jsonc',
    plugins: { json: jsonPlugin },
    extends: [jsonPlugin.configs.recommended],
  },
  {
    files: ['**/*.md'],
    language: 'markdown/gfm',
    plugins: { markdown: markdownPlugin },
    extends: [markdownPlugin.configs.recommended],
    rules: {
      'markdown/no-missing-label-refs': [
        'error',
        {
          allowLabels: ['!NOTE', '!TIP', '!IMPORTANT', '!WARNING', '!CAUTION'],
        },
      ],
    },
  },
  compatPlugin.configs['flat/recommended'],
  prettierConfig,
]);
