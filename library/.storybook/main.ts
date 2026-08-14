import { execSync } from 'node:child_process';
import type { StorybookConfig } from '@storybook/react-vite';

const branchName = () => {
  if (process.env.GITHUB_HEAD_REF) return process.env.GITHUB_HEAD_REF;
  if (process.env.GITHUB_REF_NAME) return process.env.GITHUB_REF_NAME;
  try {
    return execSync('git rev-parse --abbrev-ref HEAD', {
      stdio: ['ignore', 'pipe', 'ignore'],
    })
      .toString()
      .trim();
  } catch {
    // Tarball or Docker builds have no .git.
    return 'local';
  }
};

export default {
  staticDirs: ['./assets'],
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
  viteFinal: (config) => ({
    ...config,
    define: {
      ...config.define,
      'import.meta.env.VITE_BRANCH_NAME': JSON.stringify(branchName()),
    },
  }),
} satisfies StorybookConfig;
