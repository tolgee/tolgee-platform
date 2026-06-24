package io.tolgee.service.projectExportImport.model

/**
 * One row of an OWNED entity in the export zip (`entities/<EntityType>.json` holds an array of these);
 * the entity type is implied by the file name.
 *
 * @property handle the source primary key — a scalar for a simple id, or a map of id components for a
 *   composite (`@IdClass`) id (e.g. `KeyScreenshotReference` → `{key, screenshot}`).
 * @property attrs persistent non-association, non-id columns by attribute name.
 * @property assocs owning-side associations by attribute name: a source-PK handle of an
 *   OWNED/PROJECT_ROOT target, a [UserRef] for a `UserAccount` target, a list of those for a to-many,
 *   or `null` for an absent to-one.
 */
data class SerializedEntity(
  val handle: Any,
  val attrs: Map<String, Any?>,
  val assocs: Map<String, Any?>,
)

/** An association to a `UserAccount`, serialized as its `username` rather than a row handle. */
data class UserRef(
  val username: String,
)
