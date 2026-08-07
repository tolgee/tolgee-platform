import { normalizeTolgeeUrl } from '../shared/url'
import { DEFAULT_TOLGEE_URL, loadTolgeeAppConfig } from './config'
import { appInstallStatePath, type AppInstallStoreOptions } from './installStore'

export type AppAccessTokenInput = {
  /** Base URL of the Tolgee instance that issued the credentials. */
  tolgeeUrl?: string
  /** App-level client id (`tgpub_…`). */
  clientId?: string
  /** App-level client secret (`tgpubs_…`). */
  clientSecret?: string
  /**
   * Installation the token acts as. Defaults to the install `selfRegisterApp`
   * stored for this Tolgee instance; an app serving several installs passes the
   * id it got from `fetchAppInstallations()`.
   */
  installId?: number
  /** Directory of the local state file; see `appInstallStatePath`. */
  stateDir?: string
  /** Mint a fresh token even when a cached one is still live. */
  forceRefresh?: boolean
}

export type AppAccessToken = {
  accessToken: string
  /** Lifetime in seconds, counted from the moment Tolgee issued the token. */
  expiresIn: number
}

/**
 * Refresh this long before the token would expire, so a token handed out by
 * the cache never dies mid-request.
 */
const EXPIRY_SLACK_MS = 60_000

type CachedToken = { token: AppAccessToken; staleAt: number }

const tokenCache = new Map<string, CachedToken>()

/**
 * Exchanges the app's credentials plus an install id for a short-lived access
 * token, so an app backend can call Tolgee as that install (no user, no iframe
 * context).
 *
 * Called with nothing, it uses what `loadTolgeeAppConfig()` resolves:
 * `TOLGEE_APP_CLIENT_ID` / `TOLGEE_APP_CLIENT_SECRET` when set, otherwise the
 * app credentials `selfRegisterApp` stored — and the stored install's id.
 *
 * Tokens are cached in memory per (instance, credentials, install) and
 * refreshed shortly before they expire, so calling this per request is fine.
 * Nothing derived is ever written to disk. On a 401 from an API call, drop the
 * cached token with {@link invalidateAppAccessToken} and call this again —
 * revoking an app secret kills every token minted before the revocation, and
 * the replacement mints from whatever live secret the app holds.
 *
 *     const { accessToken } = await fetchAppAccessToken()
 */
export const fetchAppAccessToken = async (
  input: AppAccessTokenInput = {}
): Promise<AppAccessToken> => {
  const resolved = resolveCredentials(input)
  const key = cacheKey(resolved)

  if (!input.forceRefresh) {
    const cached = tokenCache.get(key)
    if (cached && cached.staleAt > Date.now()) return cached.token
  }

  const token = await mint(resolved)
  tokenCache.set(key, {
    token,
    staleAt: Date.now() + Math.max(0, token.expiresIn * 1000 - EXPIRY_SLACK_MS),
  })
  return token
}

/**
 * Drops the cached token for these inputs, so the next call mints a fresh one.
 * Call it when an API request comes back 401 — the token may have been killed
 * by a secret revocation long before its own expiry.
 */
export const invalidateAppAccessToken = (
  input: AppAccessTokenInput = {}
): void => {
  try {
    tokenCache.delete(cacheKey(resolveCredentials(input)))
  } catch {
    // No resolvable credentials means nothing can be cached under them either.
  }
}

/** Drops every cached token. For tests and credential rotations. */
export const clearAppAccessTokenCache = (): void => {
  tokenCache.clear()
}

type ResolvedCredentials = {
  tolgeeUrl: string
  clientId: string
  clientSecret: string
  installId: number
}

const cacheKey = (resolved: ResolvedCredentials): string =>
  `${resolved.tolgeeUrl}|${resolved.clientId}|${resolved.installId}`

const mint = async (resolved: ResolvedCredentials): Promise<AppAccessToken> => {
  const url = `${normalizeTolgeeUrl(resolved.tolgeeUrl)}/v2/public/apps/token`
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      grant_type: 'client_credentials',
      client_id: resolved.clientId,
      client_secret: resolved.clientSecret,
      install_id: resolved.installId,
    }),
  })

  if (!response.ok) {
    throw new Error(
      `Tolgee app token request failed: ${response.status} ${response.statusText} — ${await response.text()}`
    )
  }

  const body = (await response.json()) as {
    access_token?: unknown
    expires_in?: unknown
  }
  if (typeof body.access_token !== 'string') {
    throw new Error(
      `Tolgee app token response contained no access_token: ${JSON.stringify(body)}`
    )
  }
  return {
    accessToken: body.access_token,
    expiresIn: Number(body.expires_in),
  }
}

const resolveCredentials = (
  input: AppAccessTokenInput
): ResolvedCredentials => {
  const options: AppInstallStoreOptions = input.stateDir
    ? { stateDir: input.stateDir }
    : {}
  const tolgeeUrl =
    input.tolgeeUrl ?? process.env.TOLGEE_URL ?? DEFAULT_TOLGEE_URL
  // Credentials are stored per instance, so they must be looked up for the URL
  // this call actually targets, not whatever TOLGEE_URL happens to say.
  const config = loadTolgeeAppConfig(
    { ...process.env, TOLGEE_URL: tolgeeUrl },
    options
  )
  const clientId = input.clientId ?? config.clientId
  const clientSecret = input.clientSecret ?? config.clientSecret
  const installId = input.installId ?? config.installId ?? undefined

  if (!clientId || !clientSecret) {
    throw new Error(
      `No Tolgee app credentials for ${tolgeeUrl}. They are stored automatically at ` +
        `${appInstallStatePath(options)} when the app self-registers — run the app once ` +
        'with TOLGEE_APP_REGISTRATION_SECRET set, or register it in Tolgee and set ' +
        'TOLGEE_APP_CLIENT_ID and TOLGEE_APP_CLIENT_SECRET yourself.'
    )
  }
  if (installId === undefined) {
    throw new Error(
      `No Tolgee install id for ${tolgeeUrl}. Pass installId — ` +
        'fetchAppInstallations() lists the installs these credentials may act as.'
    )
  }
  return { tolgeeUrl, clientId, clientSecret, installId }
}
