import { normalizeTolgeeUrl } from '../shared/url'
import { DEFAULT_TOLGEE_URL, loadTolgeeAppConfig } from './config'
import {
  appInstallStatePath,
  saveApp,
  type AppInstallStoreOptions,
} from './installStore'

/** Rotate a credential this old, or older. */
const DEFAULT_MAX_SECRET_AGE_MS = 30 * 24 * 60 * 60 * 1000

export type RotateAppClientSecretInput = {
  /** Base URL of the Tolgee instance that issued the credentials. */
  tolgeeUrl?: string
  /** App-level client id to authenticate with, instead of the resolved one. */
  clientId?: string
  /** App-level client secret to authenticate with, instead of the resolved one. */
  clientSecret?: string
  /** Directory of the local state file; see `appInstallStatePath`. */
  stateDir?: string
}

export type RotatedAppClientSecret = {
  /** Id of the newly issued secret, as Tolgee knows it. */
  secretId: number
  /** Where the new secret was stored. */
  credentialsPath: string
  /** When Tolgee issued it. */
  issuedAt: string
}

export type EnsureAppCredentialsInput = RotateAppClientSecretInput & {
  /**
   * Rotate once the stored secret is this old. Defaults to 30 days. A store that
   * does not record an issue date — one written before this existed — counts as
   * older than any age.
   */
  maxAgeMs?: number
}

export type EnsureAppCredentialsResult = {
  rotated: boolean
  /** Why nothing was rotated, when `rotated` is false. */
  reason?: 'fresh' | 'credentials-from-env' | 'no-stored-credentials'
  secretId?: number
}

/**
 * Asks Tolgee for a **new app-level client secret** and stores it in place of
 * the current one — the automatic half of a two-step rotation.
 *
 * The secret the call authenticated with keeps working until somebody revokes
 * it, which is what makes this safe to do while the app is serving traffic: if
 * the write fails, the app carries on with the credential it already had.
 * Whoever operates the app revokes the old one afterwards, once `lastUsedAt`
 * in Tolgee shows nothing is using it — and revoking is also what kills every
 * access token the old secret ever minted.
 *
 * The new secret is never returned or logged — it goes straight into the state
 * file (`appInstallStatePath()`), where `fetchAppAccessToken()` picks it up.
 *
 *     await rotateAppClientSecret()
 *
 * Refused when the credentials come from `TOLGEE_APP_CLIENT_ID` /
 * `TOLGEE_APP_CLIENT_SECRET`: those win over the state file, so a rotation
 * would write a secret the app is never going to read. Rotate a deployment by
 * issuing a secret in Tolgee and injecting it.
 *
 * Running several replicas? Only one of them should call this — each call
 * mints another secret, and Tolgee caps how many an app may hold at once.
 */
export const rotateAppClientSecret = async (
  input: RotateAppClientSecretInput = {}
): Promise<RotatedAppClientSecret> => {
  const options: AppInstallStoreOptions = input.stateDir
    ? { stateDir: input.stateDir }
    : {}
  const tolgeeUrl = normalizeTolgeeUrl(
    input.tolgeeUrl ?? process.env.TOLGEE_URL ?? DEFAULT_TOLGEE_URL
  )
  const config = loadTolgeeAppConfig(
    { ...process.env, TOLGEE_URL: tolgeeUrl },
    options
  )

  if (config.credentialsSource === 'env' && !input.clientSecret) {
    throw new Error(
      'Tolgee app credentials come from TOLGEE_APP_CLIENT_ID / TOLGEE_APP_CLIENT_SECRET, which ' +
        'take precedence over the stored ones — rotating would write a secret this app would ' +
        'never read. Issue a new secret in Tolgee and inject it instead.'
    )
  }
  const clientId = input.clientId ?? config.clientId
  const clientSecret = input.clientSecret ?? config.clientSecret
  if (!clientId || !clientSecret) {
    throw new Error(
      `No stored Tolgee app credentials for ${tolgeeUrl}, so there is nothing to rotate. Register ` +
        `the app first; credentials land in ${appInstallStatePath(options)}.`
    )
  }

  const response = await fetch(`${tolgeeUrl}/v2/public/apps/app-secrets/issue`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ client_id: clientId, client_secret: clientSecret }),
  })
  if (!response.ok) {
    throw new Error(
      `Tolgee app secret rotation failed: ${response.status} ${response.statusText} — ${await response.text()}`
    )
  }

  const body = (await response.json()) as { id?: unknown; secret?: unknown }
  if (typeof body.id !== 'number' || typeof body.secret !== 'string') {
    throw new Error(
      'Tolgee issued no usable secret; the stored credentials were left untouched.'
    )
  }

  const issuedAt = new Date().toISOString()
  saveApp(
    {
      tolgeeUrl,
      clientId,
      clientSecret: body.secret,
      secretIssuedAt: issuedAt,
    },
    options
  )

  return {
    secretId: body.id,
    credentialsPath: appInstallStatePath(options),
    issuedAt,
  }
}

/**
 * Rotates the stored client secret if it has been in use for longer than
 * `maxAgeMs`, and does nothing otherwise — meant to be called once on boot, so
 * an app's credential ages out on its own and an operator only ever has to
 * revoke, never to hand a new secret over.
 *
 *     await selfRegisterApp({ ... })
 *     await ensureAppCredentialsFresh()
 *
 * Never throws for the ordinary "nothing to do" cases: an app whose credentials
 * are injected through the environment, or that has none stored yet, is
 * reported rather than interrupted.
 */
export const ensureAppCredentialsFresh = async (
  input: EnsureAppCredentialsInput = {}
): Promise<EnsureAppCredentialsResult> => {
  const options: AppInstallStoreOptions = input.stateDir
    ? { stateDir: input.stateDir }
    : {}
  const tolgeeUrl = normalizeTolgeeUrl(
    input.tolgeeUrl ?? process.env.TOLGEE_URL ?? DEFAULT_TOLGEE_URL
  )
  const config = loadTolgeeAppConfig(
    { ...process.env, TOLGEE_URL: tolgeeUrl },
    options
  )

  if (config.credentialsSource === 'env') {
    return { rotated: false, reason: 'credentials-from-env' }
  }
  if (config.credentialsSource === null) {
    return { rotated: false, reason: 'no-stored-credentials' }
  }
  if (!isStale(config.secretIssuedAt, input.maxAgeMs)) {
    return { rotated: false, reason: 'fresh' }
  }

  const rotated = await rotateAppClientSecret({ ...input, tolgeeUrl })
  return { rotated: true, secretId: rotated.secretId }
}

const isStale = (
  secretIssuedAt: string | null,
  maxAgeMs: number = DEFAULT_MAX_SECRET_AGE_MS
): boolean => {
  if (secretIssuedAt === null) return true
  const issued = Date.parse(secretIssuedAt)
  if (Number.isNaN(issued)) return true
  return Date.now() - issued >= maxAgeMs
}
