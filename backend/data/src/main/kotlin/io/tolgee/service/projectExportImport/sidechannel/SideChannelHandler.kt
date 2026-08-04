package io.tolgee.service.projectExportImport.sidechannel

import kotlin.reflect.KClass

/**
 * Round-trips one `SIDE_CHANNEL`-classified entity that can't travel the generic OWNED graph. The
 * exporter writes [collectForExport]'s rows as a JSON array under [entryName]; the importer feeds that
 * array's bytes back to [restore] once the imported keys exist.
 */
interface SideChannelHandler {
  /** The `@Entity` this handler round-trips; its policy must be `SIDE_CHANNEL`. */
  val entityClass: KClass<*>

  /**
   * The `*.json` zip entry this handler owns, under
   * [io.tolgee.service.projectExportImport.model.ExportZipLayout.SIDE_CHANNELS_DIR] and distinct from
   * every other handler's; the reader routes by that prefix.
   */
  val entryName: String

  /** The project's rows as JSON-serializable records carrying source ids. */
  fun collectForExport(projectId: Long): List<Any>

  /**
   * Restores this handler's [entryName] bytes onto [context]'s target project, remapping source ids. May
   * throw [tools.jackson.core.JacksonException] on a malformed array, which the caller maps to 400.
   */
  fun restore(
    json: ByteArray,
    context: SideChannelImportContext,
  )
}
