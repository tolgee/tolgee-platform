import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { loadTolgeeAppConfig } from '@tolgee/apps-sdk/server'

const __dirname = dirname(fileURLToPath(import.meta.url))

const appConfig = loadTolgeeAppConfig()

export const VITE_PORT = appConfig.vitePort
export const SERVER_PORT = appConfig.serverPort
export const TOLGEE_URL = appConfig.tolgeeUrl
export const LOCAL_BASE_URL = `http://localhost:${VITE_PORT}`

/**
 * State dir written by `tolgee-app register` / `tolgee-app dev`:
 *  - `install.json` holds the credentials from registration (incl. webhookSecret)
 *  - `tunnel.json` holds the current public base URL (read by the manifest route)
 */
export const TUNNEL_STATE_DIR = join(__dirname, '..', '.tolgee-dev')
export const TUNNEL_STATE_FILE = join(TUNNEL_STATE_DIR, 'tunnel.json')
const INSTALL_FILE = join(TUNNEL_STATE_DIR, 'install.json')

const installWebhookSecret = (): string | null => {
  try {
    const parsed = JSON.parse(readFileSync(INSTALL_FILE, 'utf8')) as {
      webhookSecret?: string
    }
    return parsed.webhookSecret ?? null
  } catch {
    return null
  }
}

/**
 * In dev the secret comes from the install record `tolgee-app register` wrote — no
 * manual copying. `TOLGEE_WEBHOOK_SECRET` is a production override for deploys where
 * there is no install file.
 */
export const WEBHOOK_SECRET = appConfig.webhookSecret ?? installWebhookSecret()
