package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.TranslationSuggestion

class SuggestionBuilder(
  val keyBuilder: KeyBuilder,
) : BaseEntityDataBuilder<TranslationSuggestion, SuggestionBuilder>() {
  override val self: TranslationSuggestion =
    TranslationSuggestion(keyBuilder.projectBuilder.self).apply { this.key = keyBuilder.self }
}
