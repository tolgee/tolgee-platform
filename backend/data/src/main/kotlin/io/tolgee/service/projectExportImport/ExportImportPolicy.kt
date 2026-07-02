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
   * Serialized and restored, but outside the generic graph: an entity the metamodel walk can't handle
   * (e.g. `KeysDistance` — see its entry in `ProjectExportImportPolicyRegistry`). Each SIDE_CHANNEL type
   * is round-tripped by a dedicated [io.tolgee.service.projectExportImport.sidechannel.SideChannelHandler],
   * and `ProjectExportImportPolicyGuardTest` fails the build unless every SIDE_CHANNEL type has one.
   */
  SIDE_CHANNEL,

  /** Not serialized and not traversed: instance-specific, derived, transient, or out-of-scope. */
  IGNORED,
  ;

  /**
   * True for policies the generic entity graph does not carry: a reference from an OWNED entity to such a
   * target is dropped (nulled) on import, so a non-nullable FK to one is a build-gate violation.
   */
  val isNotGraphCarried: Boolean
    get() = this == IGNORED || this == SIDE_CHANNEL
}
