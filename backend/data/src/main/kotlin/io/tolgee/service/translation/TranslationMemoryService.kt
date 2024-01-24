package io.tolgee.service.translation

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class TranslationMemoryService(
  private val translationsService: TranslationService,
) {
  fun getAutoTranslatedValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView? {
    return translationsService.getTranslationMemoryValue(key, targetLanguage)
  }

  fun suggest(
    key: Key,
    targetLanguage: LanguageDto,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView> {
    return translationsService.getTranslationMemorySuggestions(key, targetLanguage, pageable)
  }

  fun suggest(
    baseTranslationText: String,
    targetLanguage: LanguageDto,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView> {
    return translationsService.getTranslationMemorySuggestions(
      baseTranslationText,
      null,
      targetLanguage,
      pageable,
    )
  }
}
