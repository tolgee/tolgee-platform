package io.tolgee.service.translationMemory

import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView

/**
 * Resolves a TM match for auto-translate / batch pre-translate. The CE implementation
 * delegates to [io.tolgee.service.translation.TranslationMemoryService], which applies plan-aware
 * filtering internally (free plan → project's own TM only; paid plan → project + shared TMs).
 */
interface TmAutoTranslateProvider {
  fun getAutoTranslatedValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView?
}
