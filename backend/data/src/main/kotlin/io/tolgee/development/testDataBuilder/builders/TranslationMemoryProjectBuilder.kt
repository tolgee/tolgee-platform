package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.translationMemory.TranslationMemoryProject

class TranslationMemoryProjectBuilder(
  val translationMemoryBuilder: TranslationMemoryBuilder,
) : BaseEntityDataBuilder<TranslationMemoryProject, TranslationMemoryProjectBuilder>() {
  override var self: TranslationMemoryProject =
    TranslationMemoryProject().apply {
      translationMemory = translationMemoryBuilder.self
    }
}
