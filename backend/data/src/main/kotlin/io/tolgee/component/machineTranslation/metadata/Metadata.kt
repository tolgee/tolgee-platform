package io.tolgee.component.machineTranslation.metadata

data class Metadata(
  val promptId: Long,
  val keyId: Long,
  val projectId: Long,
  val targetLanguageId: Long,
  val sourceLanguageId: Long,
)
