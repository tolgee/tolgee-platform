import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const STATE_DIR = join(
  dirname(fileURLToPath(import.meta.url)),
  '..',
  '.tolgee-dev'
)

/** Written by `scripts/dev-tunnel.ts`, read by the server. Gitignored. */
export const TUNNEL_STATE_FILE = join(STATE_DIR, 'tunnel.json')

/** How long the server waits for the tunnel process to publish its URL. */
const TUNNEL_WAIT_MS = 120_000

const POLL_INTERVAL_MS = 250

export type DevUrls = {
  /** Origin Tolgee loads the app's iframe from — the manifest's `baseUrl`. */
  baseUrl: string
  /** URL Tolgee fetches the manifest from. */
  manifestUrl: string
  /** False when Tolgee reaches this machine directly and no tunnel was started. */
  tunnelled: boolean
}

const isLocalUrl = (value: string): boolean => {
  try {
    const { hostname } = new URL(value)
    return ['localhost', '127.0.0.1', '::1', '[::1]'].includes(hostname)
  } catch {
    return false
  }
}

/**
 * Tolgee fetches the manifest and loads the iframe from its own server, so a
 * public URL is needed exactly when Tolgee does not run on this machine.
 * `TOLGEE_DEV_TUNNEL=none` forces the tunnel off.
 */
export const tunnelNeeded = (tolgeeUrl: string): boolean =>
  process.env.TOLGEE_DEV_TUNNEL !== 'none' && !isLocalUrl(tolgeeUrl)

export const localDevUrls = (vitePort: number, serverPort: number): DevUrls => ({
  baseUrl: `http://localhost:${vitePort}`,
  manifestUrl: `http://localhost:${serverPort}/manifest.json`,
  tunnelled: false,
})

/**
 * A quick tunnel exposes a single port — Vite's — so the manifest is reached
 * through Vite's `/manifest.json` proxy rather than the Express port.
 */
export const tunnelDevUrls = (publicUrl: string): DevUrls => ({
  baseUrl: publicUrl,
  manifestUrl: `${publicUrl}/manifest.json`,
  tunnelled: true,
})

export const writeDevUrls = (urls: DevUrls): void => {
  mkdirSync(STATE_DIR, { recursive: true })
  writeFileSync(TUNNEL_STATE_FILE, JSON.stringify(urls, null, 2) + '\n', 'utf8')
}

export const readDevUrls = (): DevUrls | null => {
  try {
    const parsed = JSON.parse(
      readFileSync(TUNNEL_STATE_FILE, 'utf8')
    ) as Partial<DevUrls>
    if (typeof parsed.baseUrl !== 'string') return null
    if (typeof parsed.manifestUrl !== 'string') return null
    return {
      baseUrl: parsed.baseUrl,
      manifestUrl: parsed.manifestUrl,
      tunnelled: parsed.tunnelled === true,
    }
  } catch {
    return null
  }
}

const sleep = (ms: number): Promise<void> =>
  new Promise((resolve) => setTimeout(resolve, ms))

/**
 * The URLs this app is currently reachable at, as decided by the tunnel
 * process.
 *
 * Registration tells Tolgee where to fetch the manifest, and a quick tunnel
 * gets a different hostname on every run — so nothing may register before this
 * resolves. Returns null when a tunnel was required but never came up:
 * installing a localhost URL on a remote Tolgee would point it at an address it
 * cannot reach.
 */
export const resolveDevUrls = async (input: {
  tolgeeUrl: string
  vitePort: number
  serverPort: number
}): Promise<DevUrls | null> => {
  if (!tunnelNeeded(input.tolgeeUrl)) {
    return localDevUrls(input.vitePort, input.serverPort)
  }

  const deadline = Date.now() + TUNNEL_WAIT_MS
  for (;;) {
    const urls = readDevUrls()
    if (urls) return urls
    if (Date.now() >= deadline) return null
    await sleep(POLL_INTERVAL_MS)
  }
}
