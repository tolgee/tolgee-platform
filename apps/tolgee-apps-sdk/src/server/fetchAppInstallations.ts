import { normalizeTolgeeUrl } from '../shared/url'
import { DEFAULT_TOLGEE_URL, loadTolgeeAppConfig } from './config'
import type { AppInstallStoreOptions } from './installStore'

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

export type AppInstallationsInput = {
  /** Base URL of the Tolgee instance that issued the credentials. */
  tolgeeUrl?: string
  /** App-level client id (`tgpub_…`). */
  clientId?: string
  /** App-level client secret (`tgpubs_…`). */
  clientSecret?: string
  /** Directory of the local state file; see `appInstallStatePath`. */
  stateDir?: string
}

/**
 * Asks Tolgee which installs this app has and which projects each is enabled
 * for — the entry point of the machine-to-machine flow. The install ids in the
 * answer are what `fetchAppAccessToken({ installId })` exchanges for tokens,
 * so this is the one call that needs no install id to already be known.
 *
 * Authenticates with the app-level credentials alone: the ones in
 * `TOLGEE_APP_CLIENT_ID` / `TOLGEE_APP_CLIENT_SECRET`, otherwise the ones
 * `selfRegisterApp` stored for this Tolgee instance.
 *
 *     for (const install of await fetchAppInstallations()) {
 *       for (const project of install.enabledProjects) {
 *         // …poll project.id
 *       }
 *     }
 */
export const fetchAppInstallations = async (
  input: AppInstallationsInput = {}
): Promise<AppInstallation[]> => {
  const tolgeeUrl = normalizeTolgeeUrl(
    input.tolgeeUrl ?? process.env.TOLGEE_URL ?? DEFAULT_TOLGEE_URL
  )
  const options: AppInstallStoreOptions = input.stateDir
    ? { stateDir: input.stateDir }
    : {}
  const config = loadTolgeeAppConfig(
    { ...process.env, TOLGEE_URL: tolgeeUrl },
    options
  )
  const clientId = input.clientId ?? config.clientId
  const clientSecret = input.clientSecret ?? config.clientSecret
  if (!clientId || !clientSecret) {
    throw new Error(
      `No Tolgee app credentials for ${tolgeeUrl}. They are stored automatically when the app ` +
        'self-registers, or set TOLGEE_APP_CLIENT_ID and TOLGEE_APP_CLIENT_SECRET yourself.'
    )
  }

  const response = await fetch(`${tolgeeUrl}/v2/public/apps/installations/list`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ client_id: clientId, client_secret: clientSecret }),
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
