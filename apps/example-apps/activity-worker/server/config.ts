import { loadTolgeeAppConfig } from '@tolgee/apps-sdk/server'
import { localDevUrls, readDevUrls, type DevUrls } from './devTunnel'

const positiveNumber = (value: string | undefined, fallback: number): number => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed <= 0) return fallback
  return parsed
}

/**
 * Ports differ from the SDK's defaults (which keys-showcase uses) so both
 * example apps can run at once. `vite.config.ts` repeats these defaults — it
 * cannot import this module.
 */
export const config = {
  ...loadTolgeeAppConfig(),
  vitePort: positiveNumber(process.env.VITE_PORT, 5182),
  serverPort: positiveNumber(process.env.SERVER_PORT ?? process.env.PORT, 5183),
}

export const workerConfig = {
  /** How often each enabled project's activity is re-read. */
  activityPollIntervalMs: positiveNumber(
    process.env.ACTIVITY_POLL_INTERVAL_MS,
    15_000
  ),
  /**
   * How often the list of enabled projects is re-read, so a project that
   * enables the app starts being polled without a restart.
   */
  installationsRefreshIntervalMs: positiveNumber(
    process.env.INSTALLATIONS_REFRESH_INTERVAL_MS,
    60_000
  ),
  /** Activity revisions read per project per poll. */
  activityPageSize: positiveNumber(process.env.ACTIVITY_PAGE_SIZE, 20),
  /** Feed entries kept in memory per project. */
  feedSize: positiveNumber(process.env.ACTIVITY_FEED_SIZE, 50),
}

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
