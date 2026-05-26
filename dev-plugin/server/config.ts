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
