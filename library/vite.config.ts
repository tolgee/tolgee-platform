import { defineConfig, UserConfig } from 'vite';
import { nodePolyfills } from 'vite-plugin-node-polyfills';
import react from '@vitejs/plugin-react';
import svgr from 'vite-plugin-svgr';
import viteTsconfigPaths from 'vite-tsconfig-paths';
import { extname, resolve, relative } from 'node:path';
import fg from 'fast-glob';
import pkg from './package.json';

// Only the directory's barrel is published; everything beside it stays internal.
const indexDirs = ['components', 'icons', 'illustrations'];
const otherDirs = ['hooks', 'constants', 'theme'];

const entryFiles = [
  ...indexDirs.flatMap((dir) =>
    fg.sync(`src/${dir}/**/index.{ts,tsx}`, { cwd: __dirname, absolute: true }),
  ),
  ...otherDirs.flatMap((dir) =>
    fg.sync(`src/${dir}/**/*.{ts,tsx}`, {
      cwd: __dirname,
      absolute: true,
      ignore: ['**/*stories.*', '**/*.test.*'],
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
    svgr(),
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
    // `preserveSymlinks: true` keeps the symlinked `webapp/node_modules` path distinct from this
    // package's own, so a module reached through both becomes two instances even at identical
    // versions — and for anything carrying a React context that means providers and consumers stop
    // seeing each other.
    dedupe: [
      ...Object.keys(pkg.dependencies),
      ...Object.keys(pkg.peerDependencies),
    ],
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
