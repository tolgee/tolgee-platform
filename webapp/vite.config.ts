import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path/posix';
import svgr from 'vite-plugin-svgr';
import { viteStaticCopy } from 'vite-plugin-static-copy';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    server: { host: env.HOST || 'localhost', port: Number(env.PORT) || 8080 },
    plugins: [
      svgr(),
      react(),
      viteStaticCopy({
        targets: [
          {
            src: 'node_modules/@tginternal/language-util/flags/*',
            dest: 'static/flags',
          },
        ],
      }),
    ],
    optimizeDeps: {
      esbuildOptions: {
        define: {
          global: 'globalThis',
        },
      },
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
  };
});
