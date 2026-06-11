import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { renderManifest } from '@tolgee/apps-sdk/server'
import { LOCAL_BASE_URL, TUNNEL_STATE_FILE } from './config'

const __dirname = dirname(fileURLToPath(import.meta.url))

const TEMPLATE_PATH = join(__dirname, 'manifest.template.json')

/**
 * Reads the tunnel state written by the dev orchestrator and returns the public
 * base URL the plugin is reachable at, or the localhost default when no tunnel
 * is active.
 */
const readBaseUrl = (): string => {
  try {
    const parsed = JSON.parse(readFileSync(TUNNEL_STATE_FILE, 'utf8')) as {
      baseUrl?: string
    }
    if (typeof parsed.baseUrl === 'string' && parsed.baseUrl.length > 0) {
      return parsed.baseUrl
    }
  } catch {
    // file missing or unreadable — fall through to localhost default
  }
  return LOCAL_BASE_URL
}

export const getManifest = (): string =>
  renderManifest(readFileSync(TEMPLATE_PATH, 'utf8'), readBaseUrl())
