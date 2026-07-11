package io.tolgee.hateoas.translations.suggestions

class TranslationSuggestionAcceptResponse(
  val accepted: TranslationSuggestionModel,
  val declined: List<Long> = emptyList(),
)
