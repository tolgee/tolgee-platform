package io.tolgee.service.translation

import io.tolgee.dtos.request.suggestion.SuggestionFilters
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.views.TranslationSuggestionView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface TranslationSuggestionService {
  fun getKeysWithSuggestions(
    projectId: Long,
    keyIds: List<Long>,
    languageIds: List<Long>,
  ): Map<Pair<Long, String>, List<TranslationSuggestionView>>

  fun getSuggestionsPaged(
    pageable: Pageable,
    projectId: Long,
    languageId: Long,
    keyId: Long,
    filters: SuggestionFilters,
  ): Page<TranslationSuggestion>

  fun acceptSuggestion(
    projectId: Long,
    languageId: Long,
    keyId: Long,
    suggestionId: Long,
    declineOther: Boolean,
  ): Pair<TranslationSuggestion, List<Long>>?

  fun declineSuggestion(
    projectId: Long,
    languageId: Long,
    keyId: Long,
    suggestionId: Long,
  ): TranslationSuggestion?

  fun deleteAllByLanguage(id: Long)

  fun deleteAllByProject(id: Long)

  fun deleteAllByKeyIds(keyIds: Collection<Long>)
}
