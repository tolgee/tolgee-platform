package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.translationMemory.TranslationMemoryEntry

class TranslationMemoryEntryBuilder(
  val translationMemoryBuilder: TranslationMemoryBuilder,
) : BaseEntityDataBuilder<TranslationMemoryEntry, TranslationMemoryEntryBuilder>() {
  override var self: TranslationMemoryEntry =
    TranslationMemoryEntry().apply {
      translationMemory = translationMemoryBuilder.self
    }
}
