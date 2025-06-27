package io.tolgee.service.translation

import io.tolgee.model.views.TranslationSuggestionView

interface TranslationSuggestionService {
  fun getKeysWithSuggestions(
    projectId: Long, keyIds: List<Long>, languageIds: List<Long>
  ): Map<Long, List<TranslationSuggestionView>>
}
