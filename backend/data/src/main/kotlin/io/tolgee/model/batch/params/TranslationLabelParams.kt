package io.tolgee.model.batch.params

data class TranslationLabelParams(
  var languageIds: List<Long> = listOf(),
  var labelIds: List<Long> = listOf()
)
