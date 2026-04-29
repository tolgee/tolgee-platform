package io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory

import io.tolgee.ee.api.v2.controllers.translationMemory.TranslationMemoryEntryController
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryEntryModel
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.repository.translationMemory.TranslationMemoryEntrySourceRepository
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationMemoryEntryModelAssembler(
  private val translationMemoryEntrySourceRepository: TranslationMemoryEntrySourceRepository,
) : RepresentationModelAssemblerSupport<TranslationMemoryEntry, TranslationMemoryEntryModel>(
    TranslationMemoryEntryController::class.java,
    TranslationMemoryEntryModel::class.java,
  ) {
  override fun toModel(entity: TranslationMemoryEntry): TranslationMemoryEntryModel {
    val keyNames =
      if (entity.isManual) {
        emptyList()
      } else {
        translationMemoryEntrySourceRepository.findKeyNamesByEntryId(entity.id)
      }
    return TranslationMemoryEntryModel(
      id = entity.id,
      sourceText = entity.sourceText,
      targetText = entity.targetText,
      targetLanguageTag = entity.targetLanguageTag,
      isManual = entity.isManual,
      keyNames = keyNames,
      createdAt = entity.createdAt?.time ?: 0,
      updatedAt = entity.updatedAt?.time ?: 0,
    )
  }
}
