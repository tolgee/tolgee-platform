package io.tolgee.service.translation

import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.project.ProjectService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class TranslationMemoryService(
  private val translationsService: TranslationService,
  private val projectService: ProjectService,
  private val translationRepository: TranslationRepository,
) {
  fun getAutoTranslatedValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView? {
    val baseLanguage =
      projectService.getOrCreateBaseLanguage(targetLanguage.project.id)
        ?: throw NotFoundException(Message.BASE_LANGUAGE_NOT_FOUND)

    val baseTranslationText = findBaseTranslation(key)?.text ?: return null

    return translationRepository.getTranslationMemoryValue(
      baseTranslationText,
      key,
      baseLanguage,
      targetLanguage,
    ).firstOrNull()
  }

  fun suggest(
    key: Key,
    targetLanguage: Language,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView> {
    val baseTranslation = findBaseTranslation(key) ?: return Page.empty()

    val baseTranslationText = baseTranslation.text ?: return Page.empty(pageable)

    return getTranslationMemorySuggestions(baseTranslationText, key, targetLanguage, pageable)
  }

  fun suggest(
    baseTranslationText: String,
    targetLanguage: Language,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView> {
    return getTranslationMemorySuggestions(
      baseTranslationText,
      null,
      targetLanguage,
      pageable,
    )
  }

  fun getTranslationMemorySuggestions(
    baseTranslationText: String,
    key: Key?,
    targetLanguage: Language,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView> {
    val baseLanguage =
      projectService.getOrCreateBaseLanguage(targetLanguage.project.id)
        ?: throw NotFoundException(Message.BASE_LANGUAGE_NOT_FOUND)

    return getTranslationMemorySuggestions(baseTranslationText, key, baseLanguage, targetLanguage, pageable)
  }

  fun getTranslationMemorySuggestions(
    sourceTranslationText: String,
    key: Key?,
    sourceLanguage: Language,
    targetLanguage: Language,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView> {
    if ((sourceTranslationText.length) < 3) {
      return Page.empty(pageable)
    }

    return translationRepository.getTranslateMemorySuggestions(
      baseTranslationText = sourceTranslationText,
      key = key,
      baseLanguage = sourceLanguage,
      targetLanguage = targetLanguage,
      pageable = pageable,
    )
  }

  private fun findBaseTranslation(key: Key): Translation? {
    projectService.getOrCreateBaseLanguage(key.project.id)?.let {
      return translationsService.find(key, it).orElse(null)
    }
    return null
  }
}
