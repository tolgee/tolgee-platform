import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { LOCAL_BASE_URL, TUNNEL_STATE_FILE } from './config'

const __dirname = dirname(fileURLToPath(import.meta.url))

const TEMPLATE_PATH = join(__dirname, 'manifest.template.json')

const PLACEHOLDER = '__BASE_URL__'

/**
 * Reads the tunnel state written by scripts/dev.ts and returns the
 * public base URL the plugin is reachable at, or the localhost default
 * when no tunnel is active.
 */
const readBaseUrl = (): string => {
  try {
    const raw = readFileSync(TUNNEL_STATE_FILE, 'utf8')
    const parsed = JSON.parse(raw) as { baseUrl?: string }
    if (typeof parsed.baseUrl === 'string' && parsed.baseUrl.length > 0) {
      return parsed.baseUrl
    }
  } catch {
    // file missing or unreadable — fall through to localhost default
  }
  return LOCAL_BASE_URL
}

export const renderManifest = (): string => {
  const template = readFileSync(TEMPLATE_PATH, 'utf8')
  const baseUrl = readBaseUrl()
  return template.replaceAll(PLACEHOLDER, baseUrl)
}
