import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * State lives under the app being developed — i.e. the directory `tolgee-app`
 * is invoked from — not under this package.
 */
export const STATE_DIR = join(process.cwd(), '.tolgee-dev')
export const INSTALL_FILE = join(STATE_DIR, 'install.json')
export const TUNNEL_FILE = join(STATE_DIR, 'tunnel.json')

export type InstallState = {
  tolgeeUrl: string
  organizationId: number
  installId: number
  /**
   * Plugin credentials Tolgee returned when this install was registered.
   * `clientSecret` (`X-API-Key: tgapps_…`) authenticates every outbound call
   * the plugin makes back to Tolgee — webhook handler lookups AND the
   * self-service manifest-url PATCH the dev orchestrator fires on every
   * restart. `webhookSecret` signs the inbound webhook bodies.
   */
  clientId: string
  clientSecret: string
  webhookSecret: string
}

export type TunnelState = {
  baseUrl: string
}

export const writeJson = (path: string, value: unknown): void => {
  mkdirSync(STATE_DIR, { recursive: true })
  writeFileSync(path, JSON.stringify(value, null, 2) + '\n', 'utf8')
}

export const readInstallState = (): InstallState | null => {
  try {
    return JSON.parse(readFileSync(INSTALL_FILE, 'utf8')) as InstallState
  } catch {
    return null
  }
}

export const writeTunnelState = (state: TunnelState): void => {
  writeJson(TUNNEL_FILE, state)
}

export const clearTunnelState = (): void => {
  const vitePort = Number(process.env.VITE_PORT ?? 5180)
  writeJson(TUNNEL_FILE, { baseUrl: `http://localhost:${vitePort}` })
}

export const readTunnelState = (): TunnelState | null => {
  try {
    return JSON.parse(readFileSync(TUNNEL_FILE, 'utf8')) as TunnelState
  } catch {
    return null
  }
}

export const patchManifestUrl = async (
  install: InstallState,
  manifestUrl: string
): Promise<void> => {
  // Self-service endpoint — the install authenticates as itself with its
  // clientSecret and may only update its own manifest URL.
  const url = `${install.tolgeeUrl}/v2/apps/self/manifest-url`
  const res = await fetch(url, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': install.clientSecret,
    },
    body: JSON.stringify({ manifestUrl }),
  })
  if (!res.ok) {
    throw new Error(
      `PATCH manifest-url failed (${res.status}): ${await res.text()}`
    )
  }
}

export const isLocalHost = (urlString: string): boolean => {
  try {
    const host = new URL(urlString).hostname
    return host === 'localhost' || host === '127.0.0.1' || host === '::1'
  } catch {
    return false
  }
}

/** Loads `.env.local` from the app dir if present (replaces tsx's --env-file-if-exists). */
export const loadEnvLocal = (): void => {
  try {
    process.loadEnvFile(join(process.cwd(), '.env.local'))
  } catch {
    // no .env.local — rely on the ambient environment
  }
}
