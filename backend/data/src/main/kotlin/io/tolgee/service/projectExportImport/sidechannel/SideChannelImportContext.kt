package io.tolgee.service.projectExportImport.sidechannel

data class SideChannelImportContext(
  val targetProjectId: Long,
  val keyIdBySourceId: Map<Long, Long>,
)
