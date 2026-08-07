import { normalizeTolgeeUrl } from '../shared/url'
import {
  appInstallStatePath,
  saveApp,
  saveAppInstall,
  type AppInstallStoreOptions,
} from './installStore'

export type SelfRegisterInput = {
  /** Base URL of the Tolgee instance to register with. */
  tolgeeUrl: string
  /** Instance-wide secret Tolgee requires to accept a self-registration. */
  registrationSecret: string
  /**
   * Organization to install into. Omit — the normal case — to register a
   * **native** app: one owned by no organization, which a server admin then
   * grants to organizations under Administration → Apps. Pass a slug only to
   * install the app into that single organization instead.
   */
  organizationSlug?: string | null
  /** Publicly reachable URL Tolgee fetches the manifest from. */
  manifestUrl: string
  /**
   * Set to false to keep the issued credentials out of the local state file —
   * only for apps that capture the secret themselves. Defaults to true.
   */
  persist?: boolean
  /** Directory of the local state file; see `appInstallStatePath`. */
  stateDir?: string
}

export type SelfRegisterResult = {
  installId: number
  /** False when an existing install was repointed at `manifestUrl` instead. */
  created: boolean
  /** True when the install belongs to no organization (see `organizationSlug`). */
  native: boolean
  /**
   * App-level credentials, disclosed only by the call that first registered the
   * app on this Tolgee. Null on every later call, including a repoint.
   */
  app: SelfRegisteredApp | null
  /** Where the credentials were stored; null when `persist` was false. */
  credentialsPath: string | null
}

export type SelfRegisteredApp = {
  id: number | null
  appId: string | null
  clientId: string | null
  clientSecret: string | null
  webhookSecret: string | null
}

/**
 * Registers (or repoints) this app on a Tolgee instance without anyone
 * clicking through the UI — the flow a dev app uses on startup, when its
 * tunnel URL changes on every restart.
 *
 * Tolgee disclosed the app-level credentials only on the call that registered
 * the app; a repoint discloses nothing and leaves them valid, which is what
 * `created` reflects. The install and the app credentials are stored locally,
 * so `loadTolgeeAppConfig()` and `fetchAppAccessToken()` pick them up
 * afterwards without anyone copying a secret by hand — never print them.
 *
 *     const { installId, created, credentialsPath } = await selfRegisterApp({
 *       tolgeeUrl: config.tolgeeUrl,
 *       registrationSecret: config.registrationSecret!,
 *       organizationSlug: config.organizationSlug!,
 *       manifestUrl: `${baseUrl}/manifest.json`,
 *     })
 */
export const selfRegisterApp = async (
  input: SelfRegisterInput
): Promise<SelfRegisterResult> => {
  const url = `${normalizeTolgeeUrl(input.tolgeeUrl)}/v2/public/apps/self-register`
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Tolgee-App-Registration-Secret': input.registrationSecret,
    },
    body: JSON.stringify({
      manifestUrl: input.manifestUrl,
      // Omitted entirely rather than sent as null/"" so Tolgee takes the native path.
      ...(input.organizationSlug
        ? { organizationSlug: input.organizationSlug }
        : {}),
    }),
  })

  if (!response.ok) {
    throw new Error(
      `Tolgee app self-registration failed: ${response.status} ${response.statusText} — ${await response.text()}`
    )
  }

  const body = (await response.json()) as {
    id?: unknown
    created?: unknown
    app?: {
      id?: unknown
      appId?: unknown
      clientId?: unknown
      clientSecret?: unknown
      webhookSecret?: unknown
    }
  }
  if (typeof body.id !== 'number') {
    throw new Error(
      `Tolgee app self-registration returned no install id: ${JSON.stringify(body)}`
    )
  }
  const result: SelfRegisterResult = {
    installId: body.id,
    created: body.created === true,
    native: !input.organizationSlug,
    app: readApp(body.app),
    credentialsPath: null,
  }

  if (input.persist === false) return result
  return { ...result, credentialsPath: persist(input, result) }
}

/**
 * Returns the path the install was stored at, or null when storing failed and
 * nothing was lost by it. Newly disclosed app credentials exist nowhere else,
 * so failing to store *those* is fatal rather than a warning.
 */
const persist = (
  input: SelfRegisterInput,
  result: SelfRegisterResult
): string | null => {
  const options: AppInstallStoreOptions = input.stateDir
    ? { stateDir: input.stateDir }
    : {}
  try {
    // The app block is disclosed only by the call that created the app, so it is
    // stored before the install — losing it means no app-level rotation, ever.
    if (result.app) {
      saveApp(
        {
          tolgeeUrl: input.tolgeeUrl,
          id: result.app.id,
          appId: result.app.appId,
          clientId: result.app.clientId,
          clientSecret: result.app.clientSecret,
          webhookSecret: result.app.webhookSecret,
        },
        options
      )
    }
    saveAppInstall(
      {
        tolgeeUrl: input.tolgeeUrl,
        installId: result.installId,
        native: result.native,
        organizationSlug: input.organizationSlug ?? null,
      },
      options
    )
    return appInstallStatePath(options)
  } catch (error) {
    if (result.app?.clientSecret == null) return null
    throw new Error(
      `Tolgee disclosed the app credentials for ${result.app.clientId} but they could not be ` +
        `stored at ${appInstallStatePath(options)}: ` +
        `${error instanceof Error ? error.message : String(error)}. ` +
        'The client secret is shown only once, so remove the app in Tolgee and ' +
        'register again once the path is writable.'
    )
  }
}

const str = (v: unknown): string | null => (typeof v === 'string' ? v : null)

/**
 * Only a block carrying a client id is treated as a disclosure; Tolgee omits the
 * credentials when the call merely installed an app somebody had already
 * registered, and storing an empty record would overwrite what is held.
 */
const readApp = (
  app:
    | {
        id?: unknown
        appId?: unknown
        clientId?: unknown
        clientSecret?: unknown
        webhookSecret?: unknown
      }
    | undefined
): SelfRegisteredApp | null => {
  if (!app) return null
  const clientId = str(app.clientId)
  if (!clientId) return null
  return {
    id: typeof app.id === 'number' ? app.id : null,
    appId: str(app.appId),
    clientId,
    clientSecret: str(app.clientSecret),
    webhookSecret: str(app.webhookSecret),
  }
}
