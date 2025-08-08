package io.tolgee.service.translation

import io.tolgee.model.views.TranslationSuggestionView
import org.springframework.stereotype.Component

@Component
class TranslationSuggestionServiceOssImpl : TranslationSuggestionService {
  override fun getKeysWithSuggestions(
    projectId: Long,
    keyIds: List<Long>,
    languageIds: List<Long>
  ): Map<Long, List<TranslationSuggestionView>> {
    return emptyMap()
  }

  override fun deleteAllByLanguage(id: Long) {
    // doing nothing
  }

  override fun deleteAllByProject(id: Long) {
    // doing nothing
  }
}
