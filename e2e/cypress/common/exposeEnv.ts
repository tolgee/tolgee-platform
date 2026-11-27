const EXPOSED_ENV_KEYS = [
  'HOST',
  'API_URL',
  'DEFAULT_USERNAME',
  'DEFAULT_PASSWORD',
  'MAIL_API_URL',
];

/**
 * CYPRESS_* variables and the dotenv file only populate `env`, which specs can no longer read
 * synchronously; the constants in constants.ts read these keys through Cypress.expose().
 */
export function exposeEnv(
  config: Cypress.PluginConfigOptions
): Cypress.PluginConfigOptions {
  config.expose = { ...config.expose };
  for (const key of EXPOSED_ENV_KEYS) {
    if (config.env?.[key] !== undefined) {
      config.expose[key] = config.env[key];
    }
  }
  return config;
}
