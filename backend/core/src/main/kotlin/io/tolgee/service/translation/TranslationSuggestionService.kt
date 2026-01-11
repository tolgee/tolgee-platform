package io.tolgee.service.translation

import io.tolgee.model.views.TranslationSuggestionView

interface TranslationSuggestionService {
  fun getKeysWithSuggestions(
    projectId: Long,
    keyIds: List<Long>,
    languageIds: List<Long>,
  ): Map<Pair<Long, String>, List<TranslationSuggestionView>>

  fun deleteAllByLanguage(id: Long)

  fun deleteAllByProject(id: Long)
}
