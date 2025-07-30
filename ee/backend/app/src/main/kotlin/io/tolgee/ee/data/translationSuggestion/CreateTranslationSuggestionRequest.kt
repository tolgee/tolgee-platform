package io.tolgee.ee.data.translationSuggestion

import jakarta.validation.constraints.NotBlank

data class CreateTranslationSuggestionRequest(
  @field:NotBlank
  val translation: String,
)
