import { defineConfig, Plugin } from 'vite';
import react from '@vitejs/plugin-react';
import viteTsconfigPaths from 'vite-tsconfig-paths';
import svgr from 'vite-plugin-svgr';
import { nodePolyfills } from 'vite-plugin-node-polyfills';
import mdx from '@mdx-js/rollup';

export default defineConfig({
  // depending on your application, base can also be "/"
  base: '',
  plugins: [
    react(),
    viteTsconfigPaths(),
    svgr(),
    mdx() as Plugin,
    nodePolyfills(),
  ],
  server: {
    // this ensures that the browser opens upon server start
    open: true,
    // this sets a default port to 3000
    port: 3000,
  },
});
