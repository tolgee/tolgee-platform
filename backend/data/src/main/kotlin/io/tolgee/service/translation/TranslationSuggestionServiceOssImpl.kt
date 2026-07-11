package io.tolgee.service.translation

import io.tolgee.model.views.TranslationSuggestionView
import org.springframework.stereotype.Component

@Component
class TranslationSuggestionServiceOssImpl : TranslationSuggestionService {
  override fun getKeysWithSuggestions(
    projectId: Long,
    keyIds: List<Long>,
    languageIds: List<Long>,
  ): Map<Pair<Long, String>, List<TranslationSuggestionView>> {
    return mutableMapOf()
  }

  override fun deleteAllByLanguage(id: Long) {
    // doing nothing
  }

  override fun deleteAllByProject(id: Long) {
    // doing nothing
  }
}
