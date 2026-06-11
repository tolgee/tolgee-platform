import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { STORE_DIR, STORE_FILE } from './config'

export type Verdict = 'match' | 'close' | 'mismatch'

export type Analysis = {
  backTranslation: string
  verdict: Verdict
  explanation: string
}

export type StoreEntry = Analysis & {
  keyId: number
  /** Language tag of the focused (non-base) cell, e.g. "de". */
  languageTag: string
  baseLang: string
  baseText: string
  /** The translated text that was analysed. Used as cache key. */
  text: string
  analysedAt: string
}

type Persisted = {
  entries: StoreEntry[]
}

const entryKey = (keyId: number, languageTag: string): string =>
  `${keyId}:${languageTag}`

const ensureDir = (): void => {
  try {
    mkdirSync(STORE_DIR, { recursive: true })
  } catch {
    /* directory may already exist */
  }
}

const readPersisted = (): Persisted => {
  try {
    const raw = readFileSync(STORE_FILE, 'utf8')
    const parsed = JSON.parse(raw) as Persisted
    return parsed.entries ? parsed : { entries: [] }
  } catch {
    return { entries: [] }
  }
}

const writePersisted = (data: Persisted): void => {
  ensureDir()
  writeFileSync(STORE_FILE, JSON.stringify(data, null, 2) + '\n', 'utf8')
}

const map = new Map<string, StoreEntry>(
  readPersisted().entries.map((e) => [entryKey(e.keyId, e.languageTag), e])
)

const flush = (): void => {
  writePersisted({ entries: Array.from(map.values()) })
}

export const getAnalysis = (
  keyId: number,
  languageTag: string
): StoreEntry | undefined => map.get(entryKey(keyId, languageTag))

export const upsertAnalysis = (entry: StoreEntry): void => {
  map.set(entryKey(entry.keyId, entry.languageTag), entry)
  flush()
}

/**
 * Drop every cached entry for a key. Useful when the base text changes
 * — all per-language analyses for the same key become stale.
 */
export const invalidateKey = (keyId: number): void => {
  let changed = false
  for (const [k, v] of map) {
    if (v.keyId === keyId) {
      map.delete(k)
      changed = true
    }
  }
  if (changed) flush()
}

export const allEntries = (): StoreEntry[] => Array.from(map.values())
