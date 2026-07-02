package io.tolgee.service.projectExportImport

/**
 * Classification of every JPA `@Entity` for the admin project export/import.
 *
 * Every entity in the application must map to exactly one policy; a new unclassified entity fails
 * the build-gate guard (see `ProjectExportImportPolicyGuardTest`) until a developer assigns it one.
 */
enum class ExportImportPolicy {
  /** Project content that must round-trip: serialized on export, recreated on import. */
  OWNED,

  /** `UserAccount`: emitted as a username handle and remapped on import, never serialized as a row. */
  USER_REF,

  /** `Project`: the root; not recreated — on import it is the admin-selected target project. */
  PROJECT_ROOT,

  /**
   * Not traversed by the generic graph: instance-specific, derived, transient, or out-of-scope.
   * IGNORED usually means not serialized either, but this is not guaranteed — at least one IGNORED
   * entity (`KeysDistance`) is round-tripped via a side channel (see its entry in
   * `ProjectExportImportPolicyRegistry`). Do not treat IGNORED as a hard "never leaves this instance"
   * guarantee.
   */
  IGNORED,
}
