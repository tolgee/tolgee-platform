package io.tolgee.model.batch.params

data class TranslationLabelParams(
  val languageIds: List<Long> = listOf(),
  val labelIds: List<Long> = listOf(),
)
