import {
  createTolgeeAppServerClient,
  fetchAppAccessToken,
  type TolgeeAppServerClient,
} from '@tolgee/apps-sdk/server'
import { config } from './config'

/**
 * Re-authenticate this early so a token never expires mid-request; the token
 * endpoint is cheap and rate-limited per app, not per call.
 */
const EXPIRY_MARGIN_MS = 60_000

type Access = {
  client: TolgeeAppServerClient
  expiresAt: number
}

let cached: Access | null = null

/**
 * The app acting as itself: a typed Tolgee client backed by an install-context
 * token, minted from the client credentials the SDK resolved (stored install
 * record, or `TOLGEE_APP_CLIENT_ID` / `TOLGEE_APP_CLIENT_SECRET`).
 */
export const tolgeeClient = async (): Promise<TolgeeAppServerClient> => {
  if (cached && cached.expiresAt > Date.now()) return cached.client

  const { accessToken, expiresIn } = await fetchAppAccessToken({
    tolgeeUrl: config.tolgeeUrl,
  })
  cached = {
    client: createTolgeeAppServerClient({
      tolgeeUrl: config.tolgeeUrl,
      accessToken,
    }),
    expiresAt: Date.now() + expiresIn * 1000 - EXPIRY_MARGIN_MS,
  }
  return cached.client
}

export const forgetTolgeeClient = (): void => {
  cached = null
}
