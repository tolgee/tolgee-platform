import { existsSync } from 'fs';
import { resolve } from 'path';
import { defineConfig, mergeConfig } from 'vitest/config';
import viteConfig from './vite.config';

// The billing frontend carries no test tooling of its own, so its tests run here or nowhere.
const billingFrontendDir = resolve(__dirname, '../../billing/frontend');
const billingFrontendPresent = existsSync(billingFrontendDir);
if (billingFrontendPresent) {
  // eslint-disable-next-line no-console
  console.log(
    `vitest: also running billing frontend tests from ${billingFrontendDir}`
  );
}
const billingFrontendTests = billingFrontendPresent
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
