import { defineConfig, UserConfig } from 'vite';
import { nodePolyfills } from 'vite-plugin-node-polyfills';
import react from '@vitejs/plugin-react';
import viteTsconfigPaths from 'vite-tsconfig-paths';
import { extname, resolve, relative } from 'node:path';
import fg from 'fast-glob';

const componentDirs = ['components'];
const otherDirs = ['hooks', 'constants'];

const entryFiles = [
  ...componentDirs.flatMap((dir) =>
    fg.sync(`src/${dir}/**/index.{ts,tsx}`, { cwd: __dirname, absolute: true }),
  ),
  ...otherDirs.flatMap((dir) =>
    fg.sync(`src/${dir}/**/*.{ts,tsx}`, {
      cwd: __dirname,
      absolute: true,
      ignore: ['**/*.stories.*', '**/*.test.*'],
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
      projects: [
        resolve(__dirname, 'tsconfig.json'),
        resolve(__dirname, '../webapp/tsconfig.json'), // TODO remove once https://github.com/tolgee/tolgee-platform/issues/3326
      ],
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
