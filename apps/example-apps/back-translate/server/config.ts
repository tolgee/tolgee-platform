import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))

/** Vite dev server port. Tunnel + iframe URLs use this. */
export const VITE_PORT = Number(process.env.VITE_PORT ?? 5180)

/** Express server port. Manifest/webhook/decorator routes live here. */
export const SERVER_PORT = Number(
  process.env.SERVER_PORT ?? process.env.PORT ?? 5181
)

export const LOCAL_BASE_URL = `http://localhost:${VITE_PORT}`

export const WEBHOOK_SECRET = process.env.TOLGEE_WEBHOOK_SECRET ?? null
export const TOLGEE_URL =
  process.env.TOLGEE_URL ?? 'https://app.tolgee.io'

/** Anthropic API key for back-translation. Set in .env.local. */
export const ANTHROPIC_API_KEY = process.env.ANTHROPIC_API_KEY ?? null

/** Override which Claude model performs the back-translation + verdict. */
export const ANTHROPIC_MODEL =
  process.env.ANTHROPIC_MODEL ?? 'claude-haiku-4-5-20251001'

/**
 * Tunnel state file written by scripts/dev.ts when the Cloudflare quick
 * tunnel comes up. The manifest route reads `baseUrl` from this file on
 * every request so manifest URLs always reflect the currently-active
 * tunnel without restarting the server.
 */
export const TUNNEL_STATE_DIR = join(__dirname, '..', '.tolgee-dev')
export const TUNNEL_STATE_FILE = join(TUNNEL_STATE_DIR, 'tunnel.json')

/**
 * Local install record. Written by `npm run register`; the webhook
 * handler reads `clientSecret` from here to authenticate REST calls
 * back into Tolgee (via `X-API-Key: tgapps_…`).
 */
export const INSTALL_FILE = join(TUNNEL_STATE_DIR, 'install.json')

/** Persistent JSON store written by the webhook + read by decoratorsUrl. */
export const STORE_DIR = join(__dirname, '.data')
export const STORE_FILE = join(STORE_DIR, 'analyses.json')
