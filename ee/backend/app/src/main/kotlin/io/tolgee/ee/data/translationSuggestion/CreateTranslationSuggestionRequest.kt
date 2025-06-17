package io.tolgee.ee.data.translationSuggestion

data class CreateTranslationSuggestionRequest(
  val keyId: Long,
  val languageId: Long,
  val translation: String,
)
