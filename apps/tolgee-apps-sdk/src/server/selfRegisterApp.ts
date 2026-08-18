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
  /**
   * The server's registration secret, obtained from the Tolgee administrator. Tolgee's
   * configuration holds only its hash (`tolgee.apps.registration-secret-hash`); the plaintext
   * lives with the app's deployment.
   */
  registrationToken: string
  /** Publicly reachable URL Tolgee fetches the manifest from. */
  manifestUrl: string
  /**
   * Slug of the organization the app registers into, which owns it. Omitted, the app registers
   * into the server's initial organization.
   */
  organizationSlug?: string | null
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

/** A self-registration Tolgee rejected — kept distinct so boot retries can log it. */
export class SelfRegisterError extends Error {
  constructor(
    message: string,
    readonly status: number | null
  ) {
    super(message)
    this.name = 'SelfRegisterError'
  }
}

/**
 * Registers (or repoints) this app on a Tolgee instance without anyone
 * clicking through the UI — the flow a dev app uses on startup, when its
 * tunnel URL changes on every restart. The app registers into the organization
 * `organizationSlug` names, or the server's initial organization when unset.
 *
 * Tolgee disclosed the app-level credentials only on the call that registered
 * the app; a repoint discloses nothing and leaves them valid, which is what
 * `created` reflects. The install and the app credentials are stored locally,
 * so `loadTolgeeAppConfig()` and `fetchAppAccessToken()` pick them up
 * afterwards without anyone copying a secret by hand — never print them.
 *
 *     const { installId, created, credentialsPath } = await selfRegisterApp({
 *       tolgeeUrl: config.tolgeeUrl,
 *       registrationToken: config.registrationToken!,
 *       manifestUrl: `${baseUrl}/manifest.json`,
 *     })
 */
export const selfRegisterApp = async (
  input: SelfRegisterInput
): Promise<SelfRegisterResult> => {
  const url = `${normalizeTolgeeUrl(input.tolgeeUrl)}/v2/public/apps/self-register`
  let response: Response
  try {
    response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Tolgee-App-Registration-Token': input.registrationToken,
      },
      body: JSON.stringify({
        manifestUrl: input.manifestUrl,
        ...(input.organizationSlug
          ? { organizationSlug: input.organizationSlug }
          : {}),
      }),
    })
  } catch (error) {
    // A server that is not up yet is the ordinary case at boot; surface it as a
    // retryable SelfRegisterError rather than a raw network error.
    throw new SelfRegisterError(
      `Tolgee app self-registration could not reach ${url}: ${
        error instanceof Error ? error.message : String(error)
      }`,
      null
    )
  }

  if (!response.ok) {
    throw new SelfRegisterError(
      `Tolgee app self-registration failed: ${response.status} ${response.statusText} — ${await response.text()}`,
      response.status
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
    throw new SelfRegisterError(
      `Tolgee app self-registration returned no install id: ${JSON.stringify(body)}`,
      response.status
    )
  }
  const result: SelfRegisterResult = {
    installId: body.id,
    created: body.created === true,
    app: readApp(body.app),
    credentialsPath: null,
  }

  if (input.persist === false) return result
  return { ...result, credentialsPath: persist(input, result) }
}

export type SelfRegisterRetryOptions = {
  /** Cap on attempts before giving up; Infinity keeps retrying forever. Defaults to Infinity. */
  maxAttempts?: number
  /** First backoff in ms; doubles each attempt up to `maxDelayMs`. Defaults to 1000. */
  initialDelayMs?: number
  /** Ceiling for the backoff. Defaults to 30000. */
  maxDelayMs?: number
  /** Called before each wait, so an app can log that Tolgee is not reachable yet. */
  onRetry?: (error: SelfRegisterError, attempt: number, nextDelayMs: number) => void
  /** Injectable sleeper, for tests. */
  sleep?: (ms: number) => Promise<void>
}

/**
 * `selfRegisterApp` with exponential backoff, so an app booting before its Tolgee — or before its
 * organization or registration token exists — keeps serving its manifest and registers itself the
 * moment Tolgee is ready. GitOps ordering therefore does not matter. Only rejections and unreachable
 * servers are retried; a malformed response is not.
 */
export const selfRegisterAppWithRetry = async (
  input: SelfRegisterInput,
  options: SelfRegisterRetryOptions = {}
): Promise<SelfRegisterResult> => {
  const maxAttempts = options.maxAttempts ?? Infinity
  const initialDelayMs = options.initialDelayMs ?? 1000
  const maxDelayMs = options.maxDelayMs ?? 30000
  const sleep =
    options.sleep ?? ((ms: number) => new Promise<void>((r) => setTimeout(r, ms)))

  let attempt = 0
  let delay = initialDelayMs
  for (;;) {
    attempt += 1
    try {
      return await selfRegisterApp(input)
    } catch (error) {
      if (!(error instanceof SelfRegisterError) || attempt >= maxAttempts) throw error
      options.onRetry?.(error, attempt, delay)
      await sleep(delay)
      delay = Math.min(delay * 2, maxDelayMs)
    }
  }
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
