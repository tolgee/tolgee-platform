package io.tolgee.ee.service.translationMemory

/**
 * One row in the TM content browser. A row is either stored (user-managed; cells in
 * [entries]) or virtual (computed from a project translation; cells in [virtualEntries]).
 * Pagination is row-level, so a single source text can fan out into N rows when multiple
 * stored buckets or virtual origins share it.
 */
data class TmRow(
  val sourceText: String,
  val kind: Kind,
  /** Stable identity within (sourceText, kind). For stored: `tuid` (or "manual" for null-tuid
   *  entries). For virtual: "<projectId>:<keyName>". */
  val originId: String,
  /** Stored cells of this row (one per target language). Empty for virtual rows. */
  val entries: List<TmStoredCell>,
  /** Virtual cells of this row (one per target language). Empty for stored rows. */
  val virtualEntries: List<VirtualEntry>,
  /** Originating project key name. Only set on virtual rows. */
  val keyName: String?,
  /** Originating project id. Only set on virtual rows. */
  val projectId: Long?,
  /** Originating project name. Only set on virtual rows. */
  val projectName: String?,
) {
  enum class Kind {
    STORED,
    VIRTUAL,
  }
}
