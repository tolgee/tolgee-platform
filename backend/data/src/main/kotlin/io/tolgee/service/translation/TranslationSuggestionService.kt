package io.tolgee.service.translation

import io.tolgee.model.TranslationSuggestion

interface TranslationSuggestionService {
  fun getKeysWithSuggestions(projectId: Long, keyIds: List<Long>): Map<Long, List<TranslationSuggestion>>
}
