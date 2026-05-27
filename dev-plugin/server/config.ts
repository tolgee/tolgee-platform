import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))

export const PORT = Number(process.env.PORT ?? 5181)
export const WEBHOOK_SECRET = process.env.TOLGEE_WEBHOOK_SECRET ?? null

/**
 * Local-only data directory for the dev plugin. The leading dot signals
 * "hidden / not part of the source tree"; the whole directory is
 * gitignored.
 */
export const DATA_DIR = join(__dirname, '.data')
export const DATA_FILE = join(DATA_DIR, 'data.json')
export const ACTIVITY_RETENTION_DAYS = 14

/**
 * Tunnel state file written by scripts/dev.ts when the Cloudflare quick
 * tunnel comes up. The manifest route reads `baseUrl` from this file on
 * every request so manifest URLs always reflect the currently-active
 * tunnel without restarting the server.
 */
export const TUNNEL_STATE_DIR = join(__dirname, '..', '.tolgee-dev')
export const TUNNEL_STATE_FILE = join(TUNNEL_STATE_DIR, 'tunnel.json')
