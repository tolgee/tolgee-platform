import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))

export const ROOT = join(__dirname, '..')
export const STATE_DIR = join(ROOT, '.tolgee-dev')
export const INSTALL_FILE = join(STATE_DIR, 'install.json')
export const TUNNEL_FILE = join(STATE_DIR, 'tunnel.json')

export type InstallState = {
  tolgeeUrl: string
  organizationId: number
  installId: number
  /**
   * Personal access token used to authenticate the manifest-url PATCH on
   * every dev restart. Stored locally because the dev-plugin is single-
   * developer; treat as a dev secret.
   */
  pat: string
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
  writeJson(TUNNEL_FILE, { baseUrl: 'http://localhost:5180' })
}

export const patchManifestUrl = async (
  install: InstallState,
  manifestUrl: string
): Promise<void> => {
  const url = `${install.tolgeeUrl}/v2/organizations/${install.organizationId}/apps/${install.installId}/manifest-url`
  const res = await fetch(url, {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': install.pat,
    },
    body: JSON.stringify({ manifestUrl }),
  })
  if (!res.ok) {
    throw new Error(
      `PATCH manifest-url failed (${res.status}): ${await res.text()}`
    )
  }
}
