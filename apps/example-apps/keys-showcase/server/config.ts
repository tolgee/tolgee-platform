import { loadTolgeeAppConfig } from '@tolgee/apps-sdk/server'
import { localDevUrls, readDevUrls, type DevUrls } from './devTunnel'

export const config = loadTolgeeAppConfig()

/**
 * APP_BASE_URL / APP_MANIFEST_URL pin the URLs of a deployed app; in dev they
 * are left unset and the tunnel process decides instead.
 */
export const applyUrlOverrides = (urls: DevUrls): DevUrls => ({
  baseUrl: process.env.APP_BASE_URL ?? urls.baseUrl,
  manifestUrl: process.env.APP_MANIFEST_URL ?? urls.manifestUrl,
  tunnelled: urls.tunnelled,
})

/** URLs as they stand right now: tunnel state when there is one, overrides on top. */
export const currentUrls = (): DevUrls =>
  applyUrlOverrides(
    readDevUrls() ?? localDevUrls(config.vitePort, config.serverPort)
  )
