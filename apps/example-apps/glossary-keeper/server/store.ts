import { mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { STORE_DIR, STORE_FILE } from './config'

export type Suggestion = {
  /** `${keyId}:${languageTag}` — one pending suggestion per cell. */
  id: string
  projectId: number
  keyId: number
  keyName: string
  /** Language whose changed translation triggered the suggestion. */
  languageTag: string
  /** Canonical term, in the project base language. */
  term: string
  /** Suggested translation of the term in `languageTag`. */
  translation: string
  description: string
  /** The translation text that triggered this suggestion (used for dedup). */
  sourceText: string
  createdAt: string
}

export const suggestionId = (keyId: number, languageTag: string): string =>
  `${keyId}:${languageTag}`

type Persisted = { suggestions: Suggestion[] }

const ensureDir = (): void => {
  try {
    mkdirSync(STORE_DIR, { recursive: true })
  } catch {
    /* directory may already exist */
  }
}

const readPersisted = (): Persisted => {
  try {
    const parsed = JSON.parse(readFileSync(STORE_FILE, 'utf8')) as Persisted
    return parsed.suggestions ? parsed : { suggestions: [] }
  } catch {
    return { suggestions: [] }
  }
}

const map = new Map<string, Suggestion>(
  readPersisted().suggestions.map((s) => [s.id, s] as const)
)

const flush = (): void => {
  ensureDir()
  writeFileSync(
    STORE_FILE,
    JSON.stringify({ suggestions: Array.from(map.values()) }, null, 2) + '\n',
    'utf8'
  )
}

export const upsertSuggestion = (suggestion: Suggestion): void => {
  map.set(suggestion.id, suggestion)
  flush()
}

export const getSuggestion = (id: string): Suggestion | undefined => map.get(id)

export const removeSuggestion = (id: string): void => {
  if (map.delete(id)) flush()
}

export const listByProject = (projectId: number): Suggestion[] =>
  Array.from(map.values())
    .filter((s) => s.projectId === projectId)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
