import {
  readStoredApp,
  readStoredAppInstall,
  type AppInstallStoreOptions,
} from './installStore'

export const DEFAULT_TOLGEE_URL = 'http://localhost:8718'

/** Where the credentials in a config came from. */
export type TolgeeAppCredentialsSource = 'env' | 'stored' | null

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
  /** OAuth client id issued by Tolgee at registration; null when unknown. */
  clientId: string | null
  /** OAuth client secret issued by Tolgee at registration; null when unknown. */
  clientSecret: string | null
  /** Install the stored credentials belong to; null when nothing is stored. */
  installId: number | null
  /**
   * App-level client id Tolgee issued when the app was registered (`tgpub_…`),
   * or null when no registration delivery ever arrived.
   */
  appClientId: string | null
  /**
   * The secret Tolgee signs this app's lifecycle deliveries with. Holding it is
   * what proves a delivery really is Tolgee — it authenticates nothing towards
   * Tolgee, so it is never sent anywhere.
   */
  webhookSecret: string | null
  /**
   * When the stored client secret was issued, or null when it came from the
   * environment or predates this being recorded. See `ensureAppCredentialsFresh`.
   */
  secretIssuedAt: string | null
  credentialsSource: TolgeeAppCredentialsSource
}

/**
 * Reads a Tolgee App's standard environment variables into a typed config,
 * falling back to the credentials `selfRegisterApp` stored locally for the same
 * `tolgeeUrl` (see `appInstallStatePath`).
 *
 * `TOLGEE_APP_CLIENT_ID` / `TOLGEE_APP_CLIENT_SECRET` win: a deployment injects
 * its own credentials and must not pick up a developer's stale local file.
 * Setting either of them ignores the stored file entirely, so an env client id
 * can never be paired with a secret from somewhere else.
 */
export const loadTolgeeAppConfig = (
  env: NodeJS.ProcessEnv = process.env,
  options: AppInstallStoreOptions = {}
): TolgeeAppConfig => {
  const tolgeeUrl = env.TOLGEE_URL ?? DEFAULT_TOLGEE_URL
  const envClientId = env.TOLGEE_APP_CLIENT_ID ?? null
  const envClientSecret = env.TOLGEE_APP_CLIENT_SECRET ?? null
  const fromEnv = envClientId !== null || envClientSecret !== null
  const stored = fromEnv ? null : readStoredAppInstall(tolgeeUrl, options)
  const storedCredentials =
    stored !== null && (stored.clientId !== null || stored.clientSecret !== null)
  // App-level credentials are a separate layer: they never authenticate an API
  // call, so the client-credential env override above has no say over them.
  const storedApp = readStoredApp(tolgeeUrl, options)

  return {
    tolgeeUrl,
    vitePort: Number(env.VITE_PORT ?? 5180),
    serverPort: Number(env.SERVER_PORT ?? env.PORT ?? 5181),
    organizationSlug: env.TOLGEE_ORGANIZATION_SLUG ?? null,
    registrationSecret: env.TOLGEE_APP_REGISTRATION_SECRET ?? null,
    clientId: fromEnv ? envClientId : (stored?.clientId ?? null),
    clientSecret: fromEnv ? envClientSecret : (stored?.clientSecret ?? null),
    installId: stored?.installId ?? null,
    appClientId: storedApp?.clientId ?? null,
    webhookSecret:
      env.TOLGEE_APP_WEBHOOK_SECRET ?? storedApp?.webhookSecret ?? null,
    secretIssuedAt: fromEnv ? null : (stored?.secretIssuedAt ?? null),
    credentialsSource: fromEnv ? 'env' : storedCredentials ? 'stored' : null,
  }
}
