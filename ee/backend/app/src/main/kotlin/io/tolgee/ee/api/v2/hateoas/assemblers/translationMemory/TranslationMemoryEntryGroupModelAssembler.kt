package io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory

import io.tolgee.ee.api.v2.controllers.translationMemory.TranslationMemoryEntryController
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryEntryGroupModel
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.VirtualTranslationMemoryEntryModel
import io.tolgee.ee.service.translationMemory.TranslationMemoryEntryGroup
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationMemoryEntryGroupModelAssembler(
  private val entryAssembler: TranslationMemoryEntryModelAssembler,
) : RepresentationModelAssemblerSupport<TranslationMemoryEntryGroup, TranslationMemoryEntryGroupModel>(
    TranslationMemoryEntryController::class.java,
    TranslationMemoryEntryGroupModel::class.java,
  ) {
  override fun toModel(entity: TranslationMemoryEntryGroup): TranslationMemoryEntryGroupModel {
    return TranslationMemoryEntryGroupModel(
      sourceText = entity.sourceText,
      keyNames = entity.keyNames,
      isManual = entity.isManual,
      entries = entity.entries.map { entryAssembler.toModel(it) },
      virtualEntries =
        entity.virtualEntries.map {
          VirtualTranslationMemoryEntryModel(
            sourceText = it.sourceText,
            targetText = it.targetText,
            targetLanguageTag = it.targetLanguageTag,
          )
        },
    )
  }
}
