import { defineConfig, mergeConfig } from 'vitest/config';
import { resolve } from 'node:path';
import { existsSync } from 'node:fs';
import viteConfig from './vite.config';

const billingFrontendDir = resolve(__dirname, '../../billing/frontend');
const hasBilling = existsSync(billingFrontendDir);

export default defineConfig((env) =>
  mergeConfig(viteConfig(env), {
    resolve: {
      conditions: ['module', 'browser', 'development', 'default'],
    },
    test: {
      globals: true,
      environment: 'jsdom',
      include: [
        'src/**/*.test.{ts,tsx}',
        ...(hasBilling ? [`${billingFrontendDir}/src/**/*.test.{ts,tsx}`] : []),
      ],
    },
  })
);
