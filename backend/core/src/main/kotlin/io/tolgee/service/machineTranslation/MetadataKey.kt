package io.tolgee.service.machineTranslation

data class MetadataKey(
  val keyId: Long?,
  val baseTranslationText: String,
  val targetLanguageId: Long,
)
