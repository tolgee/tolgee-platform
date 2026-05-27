import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { ACTIVITY_RETENTION_DAYS, DATA_DIR, DATA_FILE } from './config'

export type Data = {
  emojis: Record<string, string>
  updatedAt: Record<string, string>
  /** ISO timestamps of every translation-update webhook, keyed by translation id. */
  events: Record<string, string[]>
}

export type StateSubset = Data

const EMPTY: Data = { emojis: {}, updatedAt: {}, events: {} }

const load = (): Data => {
  if (!existsSync(DATA_FILE)) return clone(EMPTY)
  try {
    return { ...clone(EMPTY), ...JSON.parse(readFileSync(DATA_FILE, 'utf8')) }
  } catch (err) {
    console.warn('Failed to parse data.json — starting empty:', err)
    return clone(EMPTY)
  }
}

const clone = (d: Data): Data => ({
  emojis: { ...d.emojis },
  updatedAt: { ...d.updatedAt },
  events: { ...d.events },
})

const data: Data = load()

const persist = () => {
  ensureDataDir()
  writeFileSync(DATA_FILE, JSON.stringify(data, null, 2))
}

const ensureDataDir = () => {
  if (!existsSync(DATA_DIR)) mkdirSync(DATA_DIR, { recursive: true })
}

const cutoffMs = (): number =>
  Date.now() - ACTIVITY_RETENTION_DAYS * 24 * 60 * 60 * 1000

const recordOne = (translationId: number, occurredAt: string) => {
  const id = String(translationId)
  data.updatedAt[id] = occurredAt
  const log = data.events[id] ?? []
  log.push(occurredAt)
  data.events[id] = log.filter((iso) => Date.parse(iso) >= cutoffMs())
}

/**
 * Records edits for any translation entity that carries a numeric id.
 * Returns the number of recorded entities so callers can short-circuit.
 */
const recordTranslationEdits = (
  translations: ReadonlyArray<{ entityId?: number }>,
  occurredAt: string
): number => {
  let touched = 0
  for (const entity of translations) {
    if (typeof entity.entityId === 'number') {
      recordOne(entity.entityId, occurredAt)
      touched++
    }
  }
  if (touched > 0) persist()
  return touched
}

const setEmoji = (translationId: number, emoji: string | null) => {
  const id = String(translationId)
  if (emoji == null || emoji === '') delete data.emojis[id]
  else data.emojis[id] = emoji
  persist()
}

const getState = (ids: ReadonlyArray<string>): Data => {
  const out: Data = { emojis: {}, updatedAt: {}, events: {} }
  for (const id of ids) {
    if (data.emojis[id]) out.emojis[id] = data.emojis[id]
    if (data.updatedAt[id]) out.updatedAt[id] = data.updatedAt[id]
    if (data.events[id]?.length) out.events[id] = data.events[id]
  }
  return out
}

export const store = {
  recordTranslationEdits,
  setEmoji,
  getState,
}
