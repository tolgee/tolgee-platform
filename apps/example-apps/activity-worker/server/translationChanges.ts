import type { TolgeeApiSchemas } from '@tolgee/apps-sdk/server'
import type { TranslationChange } from '../src/feedTypes'

type ActivityRevision = TolgeeApiSchemas['ProjectActivityModel']
type ModifiedEntity = TolgeeApiSchemas['ModifiedEntityModel']

/**
 * Pulls the translation edits out of one activity revision.
 *
 * A revision groups every entity the action touched, so this looks at the
 * `Translation` entities rather than at the activity type: `SET_TRANSLATIONS`,
 * `COMPLEX_EDIT`, `IMPORT`, `AUTO_TRANSLATE` and the batch jobs all produce
 * them, and a revision of any of those types can also touch nothing else.
 */
export const translationChangesOf = (
  revision: ActivityRevision
): TranslationChange[] =>
  (revision.modifiedEntities?.['Translation'] ?? [])
    .filter((entity) => entity.modifications?.['text'] !== undefined)
    .map((entity) => toChange(revision, entity))

const toChange = (
  revision: ActivityRevision,
  entity: ModifiedEntity
): TranslationChange => {
  const text = entity.modifications?.['text']
  const key = entity.relations?.['key']
  return {
    revisionId: revision.revisionId,
    timestamp: revision.timestamp,
    authorName: revision.author?.name ?? revision.author?.username ?? null,
    keyName: asText(key?.data?.['name']),
    namespace: asText(key?.relations?.['namespace']?.data?.['name']),
    languageTag: asText(entity.relations?.['language']?.data?.['tag']),
    oldText: asText(text?.old),
    newText: asText(text?.new),
    activityType: revision.type,
  }
}

/** Activity values are `unknown` in the schema — a translation text is a string. */
const asText = (value: unknown): string | null =>
  typeof value === 'string' ? value : null
