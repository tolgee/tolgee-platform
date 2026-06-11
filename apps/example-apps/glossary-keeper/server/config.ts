import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))

/** Vite dev server port. Tunnel + iframe URLs use this. */
export const VITE_PORT = Number(process.env.VITE_PORT ?? 5182)

/** Express server port. Manifest/webhook/custom routes live here. */
export const SERVER_PORT = Number(
  process.env.SERVER_PORT ?? process.env.PORT ?? 5183
)

export const LOCAL_BASE_URL = `http://localhost:${VITE_PORT}`

export const WEBHOOK_SECRET = process.env.TOLGEE_WEBHOOK_SECRET ?? null
export const TOLGEE_URL = process.env.TOLGEE_URL ?? 'https://app.tolgee.io'

/** Anthropic API key. Set in .env.local. */
export const ANTHROPIC_API_KEY = process.env.ANTHROPIC_API_KEY ?? null

/** Fast model that flags whether a change is suspicious for a missing glossary term. */
export const ANTHROPIC_MODEL_DETECT =
  process.env.ANTHROPIC_MODEL_DETECT ?? 'claude-haiku-4-5-20251001'

/** Stronger model that drafts the actual glossary entry suggestion. */
export const ANTHROPIC_MODEL_SUGGEST =
  process.env.ANTHROPIC_MODEL_SUGGEST ?? 'claude-sonnet-4-6'

/**
 * Tunnel state file written by scripts/dev.ts. The manifest route reads
 * `baseUrl` from it on every request so manifest URLs always reflect the
 * active tunnel without restarting the server.
 */
export const TUNNEL_STATE_DIR = join(__dirname, '..', '.tolgee-dev')
export const TUNNEL_STATE_FILE = join(TUNNEL_STATE_DIR, 'tunnel.json')

/**
 * Local install record written by `npm run register`. The server reads
 * `clientSecret` (for `X-API-Key: tgapps_…`) and `organizationId` (the org
 * whose glossaries this install may write) from here.
 */
export const INSTALL_FILE = join(TUNNEL_STATE_DIR, 'install.json')

/** Persistent JSON store of pending glossary suggestions. */
export const STORE_DIR = join(__dirname, '.data')
export const STORE_FILE = join(STORE_DIR, 'suggestions.json')
