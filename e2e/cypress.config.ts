import { defineConfig } from 'cypress';
import { GLOBAL_RETRIES } from './cypress/common/globalRetries';

export default defineConfig({
  scrollBehavior: 'center',
  video: false,
  chromeWebSecurity: false,
  viewportHeight: 1080,
  viewportWidth: 1440,
  defaultCommandTimeout: 20000,
  e2e: {
    // We've imported your old cypress plugins here.
    // You may want to clean this up later by importing these.
    setupNodeEvents(on, config) {
      // eslint-disable-next-line @typescript-eslint/no-var-requires
      return require('./cypress/plugins/index.js')(on, config);
    },
    retries: { runMode: GLOBAL_RETRIES },
  },
});
