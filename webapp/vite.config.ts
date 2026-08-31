import { defineConfig, loadEnv, UserConfigFn } from 'vite';
import react from '@vitejs/plugin-react';
import viteTsconfigPaths from 'vite-tsconfig-paths';
import svgr from 'vite-plugin-svgr';
import { nodePolyfills } from 'vite-plugin-node-polyfills';
import mdx from '@mdx-js/rollup';
import { resolve } from 'node:path';
import { existsSync, readFileSync } from 'node:fs';

import { extractDataCy } from './dataCy.plugin';
import rehypeHighlight from 'rehype-highlight';
import { sentryVitePlugin } from '@sentry/vite-plugin';

const billingFrontendDir = resolve(__dirname, '../../billing/frontend');
const hasBilling = existsSync(billingFrontendDir);

// Optional trusted HTTPS for local OAuth extension testing (chrome.identity needs it); see docs/oauth/README.md.
const localCert = resolve(__dirname, 'localhost.pem');
const localCertKey = resolve(__dirname, 'localhost-key.pem');
const devHttps =
  existsSync(localCert) && existsSync(localCertKey)
    ? { cert: readFileSync(localCert), key: readFileSync(localCertKey) }
    : undefined;

export default defineConfig(({ mode }) => {
  process.env = { ...process.env, ...loadEnv(mode, process.cwd()) };

  return {
    // depending on your application, base can also be "/"
    base: '/',
    plugins: [
      react(),
      viteTsconfigPaths({
        projects: [
          resolve(__dirname, 'tsconfig.vite.json'),
          resolve(__dirname, '../library/tsconfig.json'),
          ...(hasBilling
            ? [resolve(billingFrontendDir, 'tsconfig.vite.json')]
            : []),
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
        '@codemirror/lint',
        '@codemirror/state',
        '@codemirror/view',
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
        'react-router-dom',
        'react-query',
        'formik',
        'yup',
        'clsx',
      ],
      alias: {
        '@tginternal/library': resolve(__dirname, '../library/src'),
        ...(hasBilling && {
          'tg.billing': resolve(billingFrontendDir, 'src'),
          'tg.service/billingApiSchema.generated': resolve(
            billingFrontendDir,
            'billingApiSchema.generated'
          ),
        }),
      },
    },
    optimizeDeps: {
      exclude: ['@tginternal/library'],
    },
    server: {
      https: devHttps,
      // this ensures that the browser opens upon server start
      open: true,
      host: process.env.VITE_HOST || undefined,
      // this sets a default port to 3000
      port: Number(process.env.VITE_PORT) || 3000,
      // Single-origin proxy for local OAuth testing; see docs/oauth/README.md.
      proxy: process.env.VITE_DEV_PROXY_TARGET
        ? Object.fromEntries(
            [
              '/v2',
              '/api',
              '/oauth2/authorize',
              '/oauth2/token',
              '/oauth2/revoke',
              '/.well-known',
            ].map((path) => [
              path,
              // Not the string shorthand: Vite expands that to changeOrigin: true, and the backend does not read
              // X-Forwarded-* (see the `server:` comment in application.yaml), so it would emit URLs for its own host.
              {
                target: process.env.VITE_DEV_PROXY_TARGET,
                changeOrigin: false,
              },
            ])
          )
        : undefined,
      // this enables direct access to library sources
      fs: {
        allow: [
          resolve(__dirname, '../library/src'),
          ...(hasBilling ? [billingFrontendDir] : []),
          __dirname,
        ],
      },
    },
    preview: {
      port:
        Number(process.env.VITE_PREVIEW_PORT) ||
        (Number(process.env.VITE_PORT)
          ? Number(process.env.VITE_PORT) + 1000
          : 4173),
    },
    build: {
      rollupOptions: {
        external: ['src/eeModule.ee.tsx', 'src/eeModule.oss.tsx'],
      },
    },
  };
}) satisfies UserConfigFn;
