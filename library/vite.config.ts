import { defineConfig, UserConfig } from 'vite';
import { nodePolyfills } from 'vite-plugin-node-polyfills';
import react from '@vitejs/plugin-react';
import viteTsconfigPaths from 'vite-tsconfig-paths';
import { extname, resolve, relative } from 'node:path';
import fg from 'fast-glob';

const componentDirs = ['components'];
const otherDirs = ['hooks', 'constants', 'theme'];

const entryFiles = [
  ...componentDirs.flatMap((dir) =>
    fg.sync(`src/${dir}/**/index.{ts,tsx}`, { cwd: __dirname, absolute: true }),
  ),
  ...otherDirs.flatMap((dir) =>
    fg.sync(`src/${dir}/**/*.{ts,tsx}`, {
      cwd: __dirname,
      absolute: true,
      // Story files are named `stories.ts`, with nothing before the dot, so the usual
      // `*.stories.*` pattern never matches them and they end up as published entry points.
      ignore: ['**/stories.*', '**/*.stories.*', '**/*.test.*'],
    }),
  ),
];

const entryPoints = Object.fromEntries(
  entryFiles.map((filePath) => {
    const rel = relative(resolve(__dirname, 'src'), filePath);
    return [rel.slice(0, -extname(rel).length), filePath];
  }),
);

export default defineConfig({
  plugins: [
    react(),
    viteTsconfigPaths({
      projects: [resolve(__dirname, 'tsconfig.json')],
    }),
    nodePolyfills(),
  ],
  build: {
    lib: { entry: entryPoints, formats: ['es'] },
    rollupOptions: {
      output: {
        preserveModules: true,
        preserveModulesRoot: 'src',
      },
    },
  },
  resolve: {
    preserveSymlinks: true,
    dedupe: ['react', 'react-dom', '@tolgee/react'],
    alias: {
      '@tolgee/storybook-addon': resolve(
        __dirname,
        '../storybook-tolgee-addon/src',
      ),
    },
  },
  optimizeDeps: {
    exclude: ['@tolgee/storybook-addon'],
  },
  server: {
    fs: {
      allow: [
        resolve(__dirname, '../storybook-tolgee-addon/src'),
        resolve(__dirname, '../webapp/src'), // TODO remove under https://github.com/tolgee/tolgee-platform/issues/3326
      ],
    },
  },
}) satisfies UserConfig;
