import { existsSync } from 'fs';
import { resolve } from 'path';
import { defineConfig, mergeConfig } from 'vitest/config';
import viteConfig from './vite.config';

// The billing frontend lives in a sibling repo checkout (billing/frontend) that is
// only present in billing-side workspaces; its tests must run there too, or they
// run nowhere (billing has no vitest of its own).
const billingFrontendTests = existsSync(resolve(__dirname, '../../billing/frontend'))
  ? ['../../billing/frontend/src/**/*.test.{ts,tsx}']
  : [];

export default defineConfig((env) =>
  mergeConfig(viteConfig(env), {
    resolve: {
      conditions: ['module', 'browser', 'development', 'default'],
    },
    test: {
      globals: true,
      environment: 'jsdom',
      include: ['src/**/*.test.{ts,tsx}', ...billingFrontendTests],
    },
  })
);
