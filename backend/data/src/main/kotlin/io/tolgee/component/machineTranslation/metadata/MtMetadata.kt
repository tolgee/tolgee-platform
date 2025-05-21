package io.tolgee.component.machineTranslation.metadata

data class MtMetadata(
  val prompt: String,
  val provider: String,
  val keyId: Long,
  val organizationId: Long,
)
