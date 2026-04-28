package io.tolgee.service.translationMemory

import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView

/**
 * Resolves a TM match for auto-translate / batch pre-translate.
 *
 * Single CE implementation [TmAutoTranslateProviderOssImpl] dispatches between the new managed
 * TM path (`translation_memory_entry`) and the legacy [io.tolgee.service.translation.TranslationMemoryService]
 * (`translation` table) based on whether the project has any readable TM assignments. Plan-aware
 * filtering (free vs paid) lives inside the managed suggestion service.
 */
interface TmAutoTranslateProvider {
  fun getAutoTranslatedValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView?
}
