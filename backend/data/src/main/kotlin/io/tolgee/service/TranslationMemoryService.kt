package io.tolgee.service

import io.tolgee.dtos.TranslationMemoryItem
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import org.apache.commons.text.similarity.LevenshteinDistance
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class TranslationMemoryService(
  private val translationsService: TranslationService,
) {
  fun suggest(key: Key, targetLanguage: Language, pageable: Pageable): Page<TranslationMemoryItem> {

    val inputTranslation = translationsService.findBaseTranslation(key) ?: return Page.empty(pageable)

    val page = translationsService.getTranslationMemorySuggestions(key, targetLanguage, pageable)

    val content = page.content.asSequence()
      .map {
        val distance = LevenshteinDistance.getDefaultInstance().apply(inputTranslation.text, it.baseTranslationText)
        val match = (it.baseTranslationText.length - distance).toFloat() / it.baseTranslationText.length

        TranslationMemoryItem(
          targetText = it.targetTranslationText,
          baseText = it.baseTranslationText,
          keyName = it.keyName,
          match = match
        )
      }.sortedByDescending { it.match }.toList()

    return PageImpl(content, pageable, page.totalElements)
  }
}
