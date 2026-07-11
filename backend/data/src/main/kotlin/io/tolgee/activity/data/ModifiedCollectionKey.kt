package io.tolgee.activity.data

/**
 * Identifies a `@OneToMany` / `@ManyToMany` field on a specific entity
 * instance, used as the key in `ActivityHolder.modifiedCollections`.
 *
 * Holds `(entityClass, entityId, fieldName)` rather than the owner
 * instance itself so the activity holder doesn't pin entities through
 * `flushAndClear`.
 */
data class ModifiedCollectionKey(
  val entityClass: String,
  val entityId: Long,
  val fieldName: String,
)
