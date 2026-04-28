package io.tolgee.service.translationMemory

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.service.translation.TranslationMemoryService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

/**
 * Resolves a TM match for auto-translate / batch pre-translate.
 *
 * Dispatch rule (mirrors [TranslationSuggestionController.suggestTranslationMemory]):
 * - If the project has any readable TM assignments → [ManagedTranslationMemorySuggestionService]
 *   (queries `translation_memory_entry`; plan-aware filter applied internally — free plan only
 *   sees the project's own TM, paid plan sees project + shared TMs).
 * - Otherwise → fall back to the legacy [TranslationMemoryService] which queries the `translation`
 *   table directly. This keeps behavior identical for free-plan projects that pre-date the new
 *   TM data model.
 */
@Component
class TmAutoTranslateProviderOssImpl(
  private val translationMemoryService: TranslationMemoryService,
  private val managedTranslationMemorySuggestionService: ManagedTranslationMemorySuggestionService,
  private val translationMemoryManagementService: TranslationMemoryManagementService,
) : TmAutoTranslateProvider {
  override fun getAutoTranslatedValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView? {
    val readableTmIds = translationMemoryManagementService.getReadableTmIds(key.project.id)
    if (readableTmIds.isNotEmpty()) {
      return managedTranslationMemorySuggestionService
        .getSuggestions(
          key,
          LanguageDto.fromEntity(targetLanguage, baseLanguageId = null),
          PageRequest.of(0, 1),
        ).content
        .firstOrNull()
    }
    return translationMemoryService.getAutoTranslatedValue(key, targetLanguage)
  }
}
