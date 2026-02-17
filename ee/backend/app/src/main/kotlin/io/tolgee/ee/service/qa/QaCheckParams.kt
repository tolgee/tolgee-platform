package io.tolgee.ee.service.qa

data class QaCheckParams(
  val text: String,
  val baseTranslationText: String?,
  val languageTag: String,
  val keyId: Long,
)
