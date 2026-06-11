export type TolgeeAppConfig = {
  /** Base URL of the Tolgee instance the app is installed on. */
  tolgeeUrl: string
  /** Per-install webhook secret; null when unset (dev: signatures unverified). */
  webhookSecret: string | null
  /** Vite dev-server port (iframe assets). */
  vitePort: number
  /** App server port (manifest/webhook/decorator routes). */
  serverPort: number
}

/**
 * Reads a Tolgee App's standard environment variables into a typed config.
 * Centralizes the env-var contract (names + defaults) so every app reads them
 * the same way.
 */
export const loadTolgeeAppConfig = (
  env: NodeJS.ProcessEnv = process.env
): TolgeeAppConfig => ({
  tolgeeUrl: env.TOLGEE_URL ?? 'https://app.tolgee.io',
  webhookSecret: env.TOLGEE_WEBHOOK_SECRET ?? null,
  vitePort: Number(env.VITE_PORT ?? 5180),
  serverPort: Number(env.SERVER_PORT ?? env.PORT ?? 5181),
})
