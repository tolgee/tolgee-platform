import { execSync } from 'node:child_process';
import { createRequire } from 'node:module';
import type { StorybookConfig } from '@storybook/react-vite';

const muiVersion = () =>
  createRequire(import.meta.url)('@mui/material/package.json').version;

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
  // Order matters and storySort does not reach docs entries: the main page first, then any
  // further docs pages attached to the same component, then the stories themselves.
  stories: [
    '../src/**/stories.@(md|mdx)',
    '../src/**/*.@(md|mdx)',
    '../src/**/stories.@(js|jsx|ts|tsx)',
  ],
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
      'import.meta.env.VITE_MUI_VERSION': JSON.stringify(muiVersion()),
    },
  }),
} satisfies StorybookConfig;
