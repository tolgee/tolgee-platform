import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path/posix';
import svgr from 'vite-plugin-svgr';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [svgr(), react()],
  define: {
    global: {},
  },
  resolve: {
    alias: {
      'tg.component': path.resolve(__dirname, './src/component'),
      'tg.store': path.resolve(__dirname, './src/store'),
      'tg.hooks': path.resolve(__dirname, './src/hooks'),
      'tg.constants': path.resolve(__dirname, './src/constants'),
      'tg.fixtures': path.resolve(__dirname, './src/fixtures'),
      'tg.globalContext': path.resolve(__dirname, './src/globalContext'),
      'tg.service': path.resolve(__dirname, './src/service'),
      'tg.views': path.resolve(__dirname, './src/views'),
      'tg.error': path.resolve(__dirname, './src/error'),
      'tg.svgs': path.resolve(__dirname, './src/svgs'),
    },
  },
});
