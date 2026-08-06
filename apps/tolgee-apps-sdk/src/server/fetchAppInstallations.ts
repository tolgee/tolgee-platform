import { normalizeTolgeeUrl } from '../shared/url'
import { DEFAULT_TOLGEE_URL } from './config'
import {
  fetchAppAccessToken,
  type AppAccessTokenInput,
} from './fetchAppAccessToken'

/** Organization owning a project the app is enabled for. */
export type AppInstallationOrganization = {
  id: number
  name: string
  slug: string
}

export type AppEnabledProject = {
  id: number
  name: string
  organization: AppInstallationOrganization
}

/** One install of this app, as Tolgee describes it to the app's own backend. */
export type AppInstallation = {
  id: number
  /** Manifest `id` of the app this install was created from. */
  appId: string
  name: string
  version: string
  /** True when the install belongs to no organization (registered server-wide). */
  native: boolean
  /** Permission scopes granted to the install at consent time. */
  scopes: string[]
  /**
   * Projects the app is currently enabled for — the only ones its token may act
   * on. Enabling and disabling happens in Tolgee, so this changes under the
   * app's feet; re-read it periodically rather than caching it for the process
   * lifetime.
   */
  enabledProjects: AppEnabledProject[]
}

export type AppInstallationsInput = AppAccessTokenInput & {
  /**
   * Access token to use instead of exchanging the client credentials for a new
   * one. Pass a token the caller already holds to save a round trip.
   */
  accessToken?: string
}

/**
 * Asks Tolgee which installs this app has and which projects each is enabled
 * for — what an app backend needs before it can do any work of its own, since
 * nothing else tells it what it is allowed to touch.
 *
 * Credentials are resolved exactly like `fetchAppAccessToken()`: the ones in
 * `TOLGEE_APP_CLIENT_ID` / `TOLGEE_APP_CLIENT_SECRET`, otherwise the ones
 * `selfRegisterApp` stored for this Tolgee instance.
 *
 *     for (const install of await fetchAppInstallations()) {
 *       for (const project of install.enabledProjects) {
 *         // …poll project.id
 *       }
 *     }
 *
 * Only an install-context token reaches this endpoint. The user-context token
 * the dashboard iframe receives is refused — the iframe is already told which
 * project and organization it was opened in.
 */
export const fetchAppInstallations = async (
  input: AppInstallationsInput = {}
): Promise<AppInstallation[]> => {
  const tolgeeUrl = normalizeTolgeeUrl(
    input.tolgeeUrl ?? process.env.TOLGEE_URL ?? DEFAULT_TOLGEE_URL
  )
  const accessToken =
    input.accessToken ??
    (await fetchAppAccessToken({ ...input, tolgeeUrl })).accessToken

  const response = await fetch(`${tolgeeUrl}/v2/apps/self/installations`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  })

  if (!response.ok) {
    throw new Error(
      `Tolgee app installations request failed: ${response.status} ${response.statusText} — ${await response.text()}`
    )
  }

  const body = (await response.json()) as {
    _embedded?: { installations?: AppInstallation[] }
  }
  // A HAL collection omits `_embedded` entirely when it is empty, which is what
  // an app that is registered but not yet granted to anyone sees.
  return body._embedded?.installations ?? []
}
