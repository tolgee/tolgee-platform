export type TolgeeAppConfig = {
  /** Base URL of the Tolgee instance the app is installed on. */
  tolgeeUrl: string
  /** Vite dev-server port (iframe assets). */
  vitePort: number
  /** App server port (manifest and custom API routes). */
  serverPort: number
  /** Organization the app self-registers into; null when unset. */
  organizationSlug: string | null
  /** Instance-wide secret authorizing self-registration; null when unset. */
  registrationSecret: string | null
  /** OAuth client id issued by Tolgee at registration; null before first register. */
  clientId: string | null
  /** OAuth client secret issued by Tolgee at registration; null before first register. */
  clientSecret: string | null
}

/**
 * Reads a Tolgee App's standard environment variables into a typed config.
 * Centralizes the env-var contract (names + defaults) so every app reads them
 * the same way.
 */
export const loadTolgeeAppConfig = (
  env: NodeJS.ProcessEnv = process.env
): TolgeeAppConfig => ({
  tolgeeUrl: env.TOLGEE_URL ?? 'http://localhost:8718',
  vitePort: Number(env.VITE_PORT ?? 5180),
  serverPort: Number(env.SERVER_PORT ?? env.PORT ?? 5181),
  organizationSlug: env.TOLGEE_ORGANIZATION_SLUG ?? null,
  registrationSecret: env.TOLGEE_APP_REGISTRATION_SECRET ?? null,
  clientId: env.TOLGEE_APP_CLIENT_ID ?? null,
  clientSecret: env.TOLGEE_APP_CLIENT_SECRET ?? null,
})
