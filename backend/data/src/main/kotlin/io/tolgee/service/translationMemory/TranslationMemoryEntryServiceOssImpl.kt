package io.tolgee.service.translationMemory

import io.tolgee.model.translation.Translation
import org.springframework.stereotype.Service

/**
 * No-op fallback. Free-plan projects have no writable shared-TM assignments, so the synchronization
 * pass would short-circuit on every save anyway — keeping this stub means the OSS bundle doesn't
 * even ship the sync code path.
 */
@Service
class TranslationMemoryEntryServiceOssImpl : TranslationMemoryEntryService {
  override fun onTranslationSaved(translation: Translation) {
    // Shared-TM synchronization is paid-only; the EE module overrides with @Primary.
  }
}
