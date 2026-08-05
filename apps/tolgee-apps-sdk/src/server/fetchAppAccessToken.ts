import { DEFAULT_TOLGEE_URL, loadTolgeeAppConfig } from './config'
import { appInstallStatePath, type AppInstallStoreOptions } from './installStore'

export type AppAccessTokenInput = {
  /** Base URL of the Tolgee instance that issued the credentials. */
  tolgeeUrl?: string
  clientId?: string
  clientSecret?: string
  /** Directory of the local state file; see `appInstallStatePath`. */
  stateDir?: string
}

export type AppAccessToken = {
  accessToken: string
  /** Lifetime in seconds, counted from the moment Tolgee issued the token. */
  expiresIn: number
}

/**
 * Exchanges the app's OAuth 2.0 client credentials for an access token, so an
 * app backend can call Tolgee on its own behalf (no user, no iframe context).
 *
 * Called with nothing, it uses the credentials `loadTolgeeAppConfig()` resolves:
 * `TOLGEE_APP_CLIENT_ID` / `TOLGEE_APP_CLIENT_SECRET` when set, otherwise the
 * ones `selfRegisterApp` stored for this Tolgee instance.
 *
 * The token is short-lived — do not cache it beyond `expiresIn`; fetch a fresh
 * one when it expires (or on a 401). The client secret must only ever be sent
 * to this endpoint: never to the browser, never as a bearer token on API calls.
 *
 *     const { accessToken } = await fetchAppAccessToken()
 */
export const fetchAppAccessToken = async (
  input: AppAccessTokenInput = {}
): Promise<AppAccessToken> => {
  const { tolgeeUrl, clientId, clientSecret } = resolveCredentials(input)
  const url = `${trimTrailingSlash(tolgeeUrl)}/v2/public/apps/token`
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      grant_type: 'client_credentials',
      client_id: clientId,
      client_secret: clientSecret,
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
): { tolgeeUrl: string; clientId: string; clientSecret: string } => {
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

  if (!clientId || !clientSecret) {
    throw new Error(
      `No Tolgee app credentials for ${tolgeeUrl}. They are stored automatically at ` +
        `${appInstallStatePath(options)} when the app self-registers — run the app once ` +
        'with TOLGEE_APP_REGISTRATION_SECRET set, or register it in Tolgee and set ' +
        'TOLGEE_APP_CLIENT_ID and TOLGEE_APP_CLIENT_SECRET yourself.'
    )
  }
  return { tolgeeUrl, clientId, clientSecret }
}

const trimTrailingSlash = (url: string): string => url.replace(/\/+$/, '')
