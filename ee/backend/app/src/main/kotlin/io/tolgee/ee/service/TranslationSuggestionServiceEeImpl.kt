package io.tolgee.ee.service

import io.tolgee.ee.repository.TranslationSuggestionRepository
import io.tolgee.model.TranslationSuggestion
import io.tolgee.service.translation.TranslationSuggestionService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class TranslationSuggestionServiceEeImpl(private val translationSuggestionRepository: TranslationSuggestionRepository) : TranslationSuggestionService {
  override fun getKeysWithSuggestions(projectId: Long, keyIds: List<Long>): Map<Long, List<TranslationSuggestion>> {
    val data = translationSuggestionRepository.getByKeyId(projectId, keyIds)
    val result = mutableMapOf<Long, MutableList<TranslationSuggestion>>()
    data.forEach {
      val keyId = it.key?.id
      if (keyId != null) {
        val existing = result[keyId] ?: mutableListOf()
        existing.add(it)
        result.set(keyId, existing)
      }
    }
    return result
  }
}
