export type AppAccessTokenInput = {
  /** Base URL of the Tolgee instance that issued the credentials. */
  tolgeeUrl: string
  clientId: string
  clientSecret: string
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
 * The token is short-lived — do not cache it beyond `expiresIn`; fetch a fresh
 * one when it expires (or on a 401). The client secret must only ever be sent
 * to this endpoint: never to the browser, never as a bearer token on API calls.
 *
 *     const { accessToken } = await fetchAppAccessToken({
 *       tolgeeUrl: config.tolgeeUrl,
 *       clientId: config.clientId!,
 *       clientSecret: config.clientSecret!,
 *     })
 */
export const fetchAppAccessToken = async (
  input: AppAccessTokenInput
): Promise<AppAccessToken> => {
  const url = `${trimTrailingSlash(input.tolgeeUrl)}/v2/public/apps/token`
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      grant_type: 'client_credentials',
      client_id: input.clientId,
      client_secret: input.clientSecret,
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

const trimTrailingSlash = (url: string): string => url.replace(/\/+$/, '')
