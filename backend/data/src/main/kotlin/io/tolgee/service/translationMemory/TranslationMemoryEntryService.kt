package io.tolgee.service.translationMemory

import io.tolgee.model.translation.Translation

/**
 * Hooks into the translation save pipeline to maintain synced entries on assigned **shared** TMs.
 * Shared TMs are an EE feature; the OSS bundle ships [TranslationMemoryEntryServiceOssImpl] as a
 * no-op so free-plan saves don't carry the synchronization machinery. The EE module overrides
 * with `@Primary` on `TranslationMemoryEntryServiceEeImpl`, which performs the actual sync.
 */
interface TranslationMemoryEntryService {
  fun onTranslationSaved(translation: Translation)
}
