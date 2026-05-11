package io.tolgee.ee.api.v2.hateoas.assemblers.translationMemory

import io.tolgee.ee.api.v2.controllers.translationMemory.TranslationMemoryEntryController
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.TranslationMemoryRowModel
import io.tolgee.ee.api.v2.hateoas.model.translationMemory.VirtualTranslationMemoryEntryModel
import io.tolgee.ee.service.translationMemory.TmRow
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class TranslationMemoryRowModelAssembler(
  private val entryAssembler: TranslationMemoryEntryModelAssembler,
) : RepresentationModelAssemblerSupport<TmRow, TranslationMemoryRowModel>(
    TranslationMemoryEntryController::class.java,
    TranslationMemoryRowModel::class.java,
  ) {
  override fun toModel(entity: TmRow): TranslationMemoryRowModel {
    return TranslationMemoryRowModel(
      sourceText = entity.sourceText,
      kind =
        when (entity.kind) {
          TmRow.Kind.STORED -> TranslationMemoryRowModel.Kind.STORED
          TmRow.Kind.VIRTUAL -> TranslationMemoryRowModel.Kind.VIRTUAL
        },
      entries = entity.entries.map { entryAssembler.toModel(it) },
      virtualEntries =
        entity.virtualEntries.map {
          VirtualTranslationMemoryEntryModel(
            sourceText = it.sourceText,
            targetText = it.targetText,
            targetLanguageTag = it.targetLanguageTag,
            projectId = it.projectId,
            projectName = it.projectName,
            keyName = it.keyName,
          )
        },
      keyName = entity.keyName,
      projectId = entity.projectId,
      projectName = entity.projectName,
    )
  }
}
