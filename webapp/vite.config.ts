import { defineConfig, loadEnv, UserConfigFn } from 'vite';
import react from '@vitejs/plugin-react';
import viteTsconfigPaths from 'vite-tsconfig-paths';
import svgr from 'vite-plugin-svgr';
import { nodePolyfills } from 'vite-plugin-node-polyfills';
import mdx from '@mdx-js/rollup';
import { resolve } from 'node:path';

import { extractDataCy } from './dataCy.plugin';
import rehypeHighlight from 'rehype-highlight';
import { sentryVitePlugin } from '@sentry/vite-plugin';

export default defineConfig(({ mode }) => {
  process.env = { ...process.env, ...loadEnv(mode, process.cwd()) };

  return {
    // depending on your application, base can also be "/"
    base: '/',
    plugins: [
      react(),
      viteTsconfigPaths({
        projects: [
          resolve(__dirname, 'tsconfig.json'),
          resolve(__dirname, '../library/tsconfig.json'),
        ],
      }),
      svgr(),
      mdx({ rehypePlugins: [rehypeHighlight] }),
      nodePolyfills(),
      extractDataCy(),
      sentryVitePlugin({
        authToken: process.env.SENTRY_AUTH_TOKEN,
        org: 'tolgee',
        project: 'tolgee-client',
      }),
    ],
    resolve: {
      preserveSymlinks: true,
      dedupe: [
        '@emotion/react',
        '@emotion/styled',
        '@mui/material',
        '@mui/x-date-pickers',
        '@tginternal/language-util',
        '@tolgee/react',
        '@untitled-ui/icons-react',
        'date-fns',
        'react',
        'react-dom',
      ],
      alias: {
        '@tginternal/library': resolve(__dirname, '../library/src'),
      },
    },
    optimizeDeps: {
      exclude: ['@tginternal/library'],
    },
    server: {
      // this ensures that the browser opens upon server start
      open: true,
      host: process.env.VITE_HOST || undefined,
      // this sets a default port to 3000
      port: Number(process.env.VITE_PORT) || 3000,
      // this enables direct access to library sources
      fs: {
        allow: [resolve(__dirname, '../library/src'), __dirname],
      },
    },
    build: {
      rollupOptions: {
        external: ['src/eeModule.ee.tsx', 'src/eeModule.oss.tsx'],
      },
    },
  };
}) satisfies UserConfigFn;
