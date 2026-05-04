package io.tolgee.service.translation

import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Suggestion lookup against the user's accessible translation memories.
 *
 * Two implementations:
 *  - [TranslationMemoryServiceOssImpl] (OSS) — queries virtual content only (project TMs).
 *  - `io.tolgee.ee.service.translationMemory.TranslationMemoryServiceEeImpl` (EE, `@Primary`) —
 *    additionally unions stored entries from assigned shared TMs.
 *
 * Plan-aware filtering happens inside
 * [io.tolgee.service.translationMemory.TranslationMemoryManagementService.getReadableTmIdsForSuggestions]:
 * free plan returns only the project's own TM, paid plan returns project + readable shared TMs.
 */
interface TranslationMemoryService {
  fun getSuggestions(
    key: Key,
    targetLanguageTag: String,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView>

  fun getSuggestions(
    baseTranslationText: String,
    isPlural: Boolean,
    keyId: Long?,
    projectId: Long,
    organizationId: Long,
    targetLanguageTag: String,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView>

  fun getSuggestionsList(
    baseTranslationText: String,
    isPlural: Boolean,
    keyId: Long?,
    projectId: Long,
    organizationId: Long,
    targetLanguageTag: String,
    limit: Int,
  ): List<TranslationMemoryItemView>
}
