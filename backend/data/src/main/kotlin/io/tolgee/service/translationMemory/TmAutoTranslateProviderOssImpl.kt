package io.tolgee.service.translationMemory

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.service.translation.TranslationMemoryService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

/**
 * Resolves a TM match for auto-translate / batch pre-translate. Plan filtering is applied
 * inside [TranslationMemoryService] — free plan only sees the project's own TM, paid plan
 * sees project + shared TMs.
 */
@Component
class TmAutoTranslateProviderOssImpl(
  private val translationMemoryService: TranslationMemoryService,
) : TmAutoTranslateProvider {
  override fun getAutoTranslatedValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView? =
    translationMemoryService
      .getSuggestions(
        key,
        LanguageDto.fromEntity(targetLanguage, baseLanguageId = null),
        PageRequest.of(0, 1),
      ).content
      .firstOrNull()
}
