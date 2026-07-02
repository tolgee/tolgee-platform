package io.tolgee.service.projectExportImport.sidechannel

import io.tolgee.service.projectExportImport.model.ExportZipLayout
import org.springframework.stereotype.Component

@Component
class SideChannelHandlerRegistry(
  handlers: List<SideChannelHandler>,
) {
  private val handlerByEntryName: Map<String, SideChannelHandler> = handlers.associateBy { it.entryName }

  // Keyed by the JVM binary name (java.name) to match EntityType.javaType.name and the java.name keys in
  // ProjectExportImportPolicyRegistry.sideChannelClassNames — not qualifiedName, which differs for a
  // nested @Entity (`Outer.Inner` vs `Outer$Inner`).
  private val handlerByEntityClassName: Map<String, SideChannelHandler> =
    handlers.associateBy { it.entityClass.java.name }

  /** Sorted by entryName so the export zip's entry order and counts map are reproducible (bean-order independent). */
  val handlersInWriteOrder: List<SideChannelHandler> = handlers.sortedBy { it.entryName }

  val handledEntityClassNames: Set<String> get() = handlerByEntityClassName.keys

  init {
    require(handlerByEntryName.size == handlers.size) {
      "Two SideChannelHandlers claim the same zip entry: ${handlers.map { it.entryName }}"
    }
    require(handlerByEntityClassName.size == handlers.size) {
      "Two SideChannelHandlers claim the same entity: ${handlers.map { it.entityClass.java.name }}"
    }
    require(handlers.all { it.entryName.startsWith(ExportZipLayout.SIDE_CHANNELS_DIR) }) {
      "SideChannelHandler entryName must live under '${ExportZipLayout.SIDE_CHANNELS_DIR}': " +
        handlers.map { it.entryName }
    }
  }

  fun byEntryName(entryName: String): SideChannelHandler? = handlerByEntryName[entryName]
}
