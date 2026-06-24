package io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory

import io.tolgee.ee.api.v2.controllers.translationMemory.TranslationMemoryEntryController
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryEntryModel
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationMemoryEntryModelAssembler :
  RepresentationModelAssemblerSupport<TranslationMemoryEntry, TranslationMemoryEntryModel>(
    TranslationMemoryEntryController::class.java,
    TranslationMemoryEntryModel::class.java,
  ) {
  override fun toModel(entity: TranslationMemoryEntry): TranslationMemoryEntryModel {
    return TranslationMemoryEntryModel(
      id = entity.id,
      sourceText = entity.sourceText,
      targetText = entity.targetText,
      targetLanguageTag = entity.targetLanguageTag,
      createdAt = entity.createdAt?.time ?: 0,
      updatedAt = entity.updatedAt?.time ?: 0,
    )
  }
}
