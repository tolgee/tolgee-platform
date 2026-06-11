import { afterEach, describe, expect, it } from 'vitest'
import {
  getSuggestion,
  listByProject,
  removeSuggestion,
  suggestionId,
  upsertSuggestion,
  type Suggestion,
} from './store'

// A project id unlikely to collide with anything persisted in .data/.
const PROJECT = 987654

const make = (keyId: number, languageTag: string, term: string): Suggestion => ({
  id: suggestionId(keyId, languageTag),
  projectId: PROJECT,
  keyId,
  keyName: `key-${keyId}`,
  languageTag,
  term,
  translation: `${term}-translated`,
  description: 'desc',
  sourceText: 'src',
  createdAt: new Date().toISOString(),
})

afterEach(() => {
  for (const s of listByProject(PROJECT)) removeSuggestion(s.id)
})

describe('suggestion store', () => {
  it('upserts and lists by project', () => {
    upsertSuggestion(make(1, 'de', 'Cobalt'))
    upsertSuggestion(make(2, 'fr', 'Widget'))
    expect(
      listByProject(PROJECT)
        .map((s) => s.term)
        .sort()
    ).toEqual(['Cobalt', 'Widget'])
  })

  it('dedupes by key + language (a re-edit overwrites)', () => {
    upsertSuggestion(make(1, 'de', 'Cobalt'))
    upsertSuggestion(make(1, 'de', 'Kobalt'))
    const list = listByProject(PROJECT)
    expect(list).toHaveLength(1)
    expect(list[0].term).toBe('Kobalt')
  })

  it('removes an accepted suggestion', () => {
    const s = make(3, 'es', 'Foo')
    upsertSuggestion(s)
    expect(getSuggestion(s.id)).toBeDefined()
    removeSuggestion(s.id)
    expect(getSuggestion(s.id)).toBeUndefined()
  })
})
