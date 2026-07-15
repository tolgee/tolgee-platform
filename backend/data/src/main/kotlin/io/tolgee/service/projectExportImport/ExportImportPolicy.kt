package io.tolgee.service.projectExportImport

/**
 * Classification of every JPA `@Entity` for the admin project export/import.
 *
 * Every entity in the application must map to exactly one policy; a new unclassified entity fails
 * the build-gate guard (see `ProjectExportImportPolicyGuardTest`) until a developer assigns it one.
 * See [ProjectExportImportPolicyRegistry.policyOf] for the one exception, the billing package.
 */
enum class ExportImportPolicy {
  /** Project content that must round-trip: serialized on export, recreated on import. */
  OWNED,

  /** `UserAccount`: emitted as a username handle and remapped on import, never serialized as a row. */
  USER_REF,

  /** `Project`: the root; not recreated — on import it is the admin-selected target project. */
  PROJECT_ROOT,

  /**
   * Serialized and restored outside the generic graph (the metamodel walk can't handle it, e.g.
   * `KeysDistance`), by a dedicated
   * [io.tolgee.service.projectExportImport.sidechannel.SideChannelHandler] per type.
   */
  SIDE_CHANNEL,

  /** Not serialized and not traversed: instance-specific, derived, transient, or out-of-scope. */
  IGNORED,
  ;

  /**
   * True for policies the generic entity graph does not carry.
   */
  val isNotGraphCarried: Boolean
    get() =
      when (this) {
        IGNORED, SIDE_CHANNEL -> true
        OWNED, USER_REF, PROJECT_ROOT -> false
      }

  /**
   * True for policies for which import may delete rows of.
   */
  val mayBeDeletedByImport: Boolean
    get() =
      when (this) {
        OWNED, SIDE_CHANNEL, IGNORED -> true
        USER_REF, PROJECT_ROOT -> false
      }
}
