import { loadTolgeeAppConfig } from '@tolgee/apps-sdk/server'
import { localDevUrls } from './devTunnel'

export const config = loadTolgeeAppConfig()

/**
 * Where the app lives when nothing is tunnelled. `scripts/dev-tunnel.ts`
 * publishes public URLs instead when Tolgee runs on another host.
 */
export const LOCAL_URLS = localDevUrls(config.vitePort, config.serverPort)
